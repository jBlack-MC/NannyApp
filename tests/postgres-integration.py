"""Real PHP web/API tests on a disposable, loopback PostgreSQL database only."""
import hashlib
import http.cookiejar
import json
import os
from pathlib import Path
import re
import secrets
import socket
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.parse
import urllib.request

import psycopg2
from psycopg2 import sql

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'scripts'))
import bootstrap_supabase as bootstrap


def check(ok, label):
    if not ok:
        raise AssertionError(label)
    print('PASS', label, flush=True)


class Client:
    def __init__(self, base):
        self.base = base
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
        self.token = None

    def request(self, path, data=None, method=None, form=False):
        headers = {}
        if self.token:
            headers['Authorization'] = 'Bearer ' + self.token
        if data is not None:
            headers['Content-Type'] = 'application/x-www-form-urlencoded' if form else 'application/json'
            data = (urllib.parse.urlencode(data) if form else json.dumps(data)).encode()
        req = urllib.request.Request(self.base + path, data=data, headers=headers, method=method)
        try:
            response = self.opener.open(req, timeout=15)
        except urllib.error.HTTPError as error:
            response = error
        with response:
            body = response.read().decode()
            if '/auth/' in path:
                check('no-store' in response.headers.get('Cache-Control', ''), 'authentication responses cannot be cached')
            check(response.status < 500, f'{path} does not crash ({response.status})')
            return response.status, body, response.url

    def api(self, path, data=None, method=None):
        status, body, _ = self.request('/api/' + path, data, method)
        result = json.loads(body)
        check(status == 200 and result.get('success'), path + ' succeeds')
        return result['data']

    def login(self, email, password, role):
        status, body, _ = self.request('/web/auth/login.php')
        csrf = re.search(r'name="csrf" value="([^"]+)"', body).group(1)
        status, body, url = self.request('/web/auth/login.php', {'email': email, 'password': password, 'csrf': csrf}, form=True)
        check(status == 200 and url.endswith('/' + role + '/dashboard.php'), role + ' web login reaches dashboard')


def main():
    url = os.environ.get('NANNYAPP_TEST_PG_URL', '')
    parsed = urllib.parse.urlparse(url)
    if parsed.hostname not in ('127.0.0.1', 'localhost') or parsed.port != 13378 or parsed.path != '/postgres':
        raise SystemExit('Use the disposable loopback PostgreSQL server on port 13378, database postgres.')
    admin = psycopg2.connect(url)
    admin.autocommit = True
    name = 'nanny_security_test_' + secrets.token_hex(6)
    server = None
    conn = None
    with tempfile.TemporaryDirectory() as temp, open(Path(temp) / 'php.log', 'w+') as log:
        try:
            with admin.cursor() as cur:
                for role in ('anon', 'authenticated'):
                    cur.execute('SELECT 1 FROM pg_roles WHERE rolname=%s', (role,))
                    if not cur.fetchone():
                        cur.execute(sql.SQL('CREATE ROLE {} NOLOGIN').format(sql.Identifier(role)))
                cur.execute(sql.SQL('CREATE DATABASE {}').format(sql.Identifier(name)))
            test_url = urllib.parse.urlunparse(parsed._replace(path='/' + name))
            # Never inherit a production connection, mail service or migration token.
            env = {k: v for k, v in os.environ.items() if not k.startswith(('NANNYAPP_', 'POSTGRES_', 'VERCEL', 'RESEND_'))}
            env.update(NANNYAPP_DB_URL=test_url, NANNYAPP_DB_NAME=name, NANNYAPP_DB_USER='postgres',
                       NANNYAPP_DB_SSLMODE='disable', NANNYAPP_DEMO_PASSWORD='synthetic-ci-password-123',
                       NANNYAPP_REQUIRE_EMAIL_VERIFICATION='0', NANNYAPP_MAIL_TRANSPORT='log',
                       NANNYAPP_TEST_STORAGE=temp, NANNYAPP_STORAGE_DIR=temp, NANNYAPP_STORAGE_DRIVER='database')
            command = [sys.executable, str(ROOT / 'scripts/bootstrap_supabase.py')]
            subprocess.run(command, env=env, check=True)
            conn = psycopg2.connect(test_url)
            conn.autocommit = True
            with conn.cursor() as cur:
                cur.execute('SELECT count(*) FROM users')
                check(cur.fetchone()[0] == 0, 'schema migration never creates demo accounts')
            subprocess.run(command + ['--seed-demo'], env=env, check=True)
            with conn.cursor() as cur:
                cur.execute('SELECT email,password_hash,session_version FROM users ORDER BY email')
                before = cur.fetchall()
            subprocess.run(command + ['--seed-demo'], env=env, check=True)
            with conn.cursor() as cur:
                cur.execute('SELECT email,password_hash,session_version FROM users ORDER BY email')
                check(cur.fetchall() == before and len(before) == 5, 'repeated seeds preserve credentials and sessions')
                cur.execute((ROOT / 'tests/security/supabase-hardening.sql').read_text())
            subprocess.run(['php', str(ROOT / 'NannyApp.Shared/bin/migrate.php')], env=env, check=True)
            check(True, 'PHP and Python share migration checksums and native migration history')
            with conn.cursor() as cur:
                cur.execute('CREATE SCHEMA supabase_migrations')
                cur.execute('CREATE TABLE supabase_migrations.schema_migrations (name text, statements text[])')
                for path in sorted((bootstrap.DB / 'postgres').glob('*.sql')):
                    cur.execute('INSERT INTO supabase_migrations.schema_migrations VALUES (%s,%s)',
                                (path.stem.split('_', 1)[1], [path.read_text()]))
                cur.execute("DELETE FROM schema_migrations WHERE name LIKE 'postgres/%'")
            subprocess.run(command, env=env, check=True)
            check(True, 'existing Supabase migrations adopted without rerunning constraints')
            with conn.cursor() as cur:
                cur.execute("UPDATE schema_migrations SET checksum=%s WHERE name='schema.sql'", ('0' * 64,))
            failure = subprocess.run(command, env=env, capture_output=True, text=True)
            check(failure.returncode != 0 and 'Applied migration changed' in failure.stderr, 'migration checksum drift fails closed')
            with conn.cursor() as cur:
                checksum = hashlib.sha256((bootstrap.DB / 'schema.sql').read_text().encode()).hexdigest()
                cur.execute("UPDATE schema_migrations SET checksum=%s WHERE name='schema.sql'", (checksum,))
            # All dashboard SQL must be accepted by the real PostgreSQL parser.
            queries = json.loads(subprocess.check_output(['php', 'tests/security/postgres-dashboard.test.php', 'pgsql', '--export'], cwd=ROOT))
            with conn.cursor() as cur:
                for query in queries:
                    cur.execute('EXPLAIN ' + query)
            with socket.socket() as sock:
                sock.bind(('127.0.0.1', 0))
                port = sock.getsockname()[1]
            base = f'http://127.0.0.1:{port}'
            env['NANNYAPP_BASE_URL'] = base + '/web'
            server = subprocess.Popen(['php', '-S', f'127.0.0.1:{port}', 'tests/security/router.php'], cwd=ROOT, env=env, stdout=log, stderr=log)
            for _ in range(60):
                try:
                    urllib.request.urlopen(base + '/ready', timeout=1).close()
                    break
                except OSError:
                    time.sleep(0.1)
            clients = {}
            ids = {}
            for role, email in [('parent', 'parent@nanny.app'), ('nanny', 'amelia@nanny.app'), ('admin', 'admin@nanny.app')]:
                client = clients[role] = Client(base)
                client.login(email, env['NANNYAPP_DEMO_PASSWORD'], role)
                if role != 'admin':
                    result = client.api('auth/login.php', {'email': email, 'password': env['NANNYAPP_DEMO_PASSWORD']})
                    client.token = result['token']
                    ids[role] = result['user']['id']
                    client.api('bookings/list.php')
                    client.api('notifications/list.php')
                    client.api('user/profile.php')
            routes = {
                'parent': ['parent/bookings.php', 'parent/children.php', 'parent/saved.php', 'parent/nannies.php', 'parent/payments.php'],
                'nanny': ['nanny/bookings.php', 'nanny/profile.php', 'nanny/availability.php', 'nanny/earnings.php', 'nanny/reviews.php'],
                'admin': ['admin/users.php', 'admin/bookings.php', 'admin/payments.php', 'admin/verifications.php', 'admin/operations.php', 'admin/contacts.php', 'admin/support.php', 'admin/reports.php'],
            }
            for role, paths in routes.items():
                for path in paths + ['account.php', 'notifications.php', 'messages.php', 'pages/contact.php']:
                    status, _, _ = clients[role].request('/web/' + path)
                    check(status == 200, role + ' can load ' + path)
            parent, nanny = clients['parent'], clients['nanny']
            for role in ('parent', 'nanny'):
                password = 'Synthetic-new-account-123!'
                email = f'web-{role}@example.invalid'
                signup = Client(base)
                _, body, _ = signup.request('/web/auth/register.php')
                csrf = re.search(r'name="csrf" value="([^"]+)"', body).group(1)
                _, body, _ = signup.request('/web/auth/register.php', {'csrf': csrf, 'full_name': 'Web Signup', 'email': email, 'role': role, 'password': password}, form=True)
                check('Account Created!' in body, role + ' web registration succeeds')
                created = signup.api('auth/login.php', {'email': email, 'password': password})
                check(created['user']['role'] == role, 'web registration can sign in on mobile')
                email = f'api-{role}@example.invalid'
                created = signup.api('auth/register.php', {'fullName': 'API Signup', 'email': email, 'role': role, 'password': password})
                check(created['user']['role'] == role, role + ' API registration succeeds')
                signup.login(email, password, role)
                check(True, 'mobile registration can sign in on web')
            parent.api('user/profile.php', {'fullName': 'Shared API Name'}, 'PUT')
            check('Shared API Name' in parent.request('/web/account.php')[1], 'mobile profile update is visible on web')
            status, body, _ = parent.request(f'/web/messages.php?with={ids["nanny"]}')
            csrf = re.search(r'name="csrf" value="([^"]+)"', body).group(1)
            sent = json.loads(parent.request('/web/messages_send.php', {'csrf': csrf, 'to': ids['nanny'], 'message': 'Web to mobile'}, form=True)[1])
            check(sent.get('ok'), 'web message stored')
            incoming = nanny.api(f'messages/poll.php?with={ids["parent"]}&since_id=0')
            check(any(m['id'] == sent['id'] and m['content'] == 'Web to mobile' for m in incoming), 'web message reaches mobile polling')
            reply = nanny.api('messages/send.php', {'receiverId': ids['parent'], 'content': 'Mobile to web'})
            received = json.loads(parent.request(f'/web/messages_poll.php?with={ids["nanny"]}&after_id={sent["id"]}')[1])
            check(any(m['id'] == reply['id'] and m['content'] == 'Mobile to web' for m in received['messages']), 'mobile reply reaches web polling')
            child = parent.api('children/create.php', {'name': 'Cross client child', 'age': 4})
            check(child['id'] > 0 and 'Cross client child' in parent.request('/web/parent/children.php')[1], 'API-created child has a valid id and appears on web')
            # Enforce uniqueness and domain constraints using rolled-back invalid writes.
            for statement in ["UPDATE users SET role='invalid' WHERE email='parent@nanny.app'", "UPDATE bookings SET duration=-1", "INSERT INTO users(full_name,email,password_hash) VALUES ('Duplicate','PARENT@nanny.app','invalid')"]:
                try:
                    with conn.cursor() as cur:
                        cur.execute('BEGIN')
                        cur.execute(statement)
                    raise AssertionError('Invalid data was accepted')
                except psycopg2.IntegrityError:
                    pass
                finally:
                    with conn.cursor() as cur:
                        cur.execute('ROLLBACK')
            check(True, 'invalid data and duplicate identities rejected')
            parent.api('auth/logout.php', {})
            check(parent.request('/api/user/profile.php')[0] == 401, 'logout revokes mobile bearer token')
            check('/auth/login.php' in parent.request('/web/account.php')[2], 'mobile logout also revokes web session')
            print('All PostgreSQL migration and cross-client integration checks passed.', flush=True)
        except Exception:
            log.flush()
            log.seek(0)
            print(log.read()[-12000:], file=sys.stderr)
            raise
        finally:
            if server:
                server.terminate()
                server.wait(timeout=10)
            if conn:
                conn.close()
            with admin.cursor() as cur:
                cur.execute(sql.SQL('DROP DATABASE {} WITH (FORCE)').format(sql.Identifier(name)))
            admin.close()


if __name__ == '__main__':
    main()
