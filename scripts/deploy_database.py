"""Manual production database migration; never logs credentials or login tokens."""
import json
import os
from pathlib import Path
import subprocess
import sys
import urllib.request
import urllib.parse

ROOT = Path(__file__).resolve().parents[1]


def request(base, path, body=None, token=None):
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['X-Nanny-Migration-Token'] = token
    req = urllib.request.Request(base + path, data=json.dumps(body).encode() if body is not None else None, headers=headers)
    with urllib.request.urlopen(req, timeout=90) as response:
        return json.load(response)


def main():
    base = os.environ.get('APP_URL', '').rstrip('/')
    parsed = urllib.parse.urlparse(base)
    if parsed.scheme != 'https' or not parsed.hostname or parsed.username or parsed.password or parsed.query or parsed.fragment:
        raise RuntimeError('APP_URL must be the HTTPS production origin.')
    seed = os.environ.get('SEED_DEMO') == 'true'
    reset = os.environ.get('RESET_DEMO_PASSWORDS') == 'true'
    if reset and not seed:
        raise RuntimeError('Password rotation requires explicit demo seeding.')
    if seed and len(os.environ.get('NANNYAPP_DEMO_PASSWORD', '')) < 12:
        raise RuntimeError('Demo seeding requires the private NANNYAPP_DEMO_PASSWORD secret.')
    if os.environ.get('NANNYAPP_DB_URL'):
        command = [sys.executable, str(ROOT / 'scripts/bootstrap_supabase.py')]
        if seed:
            command.append('--seed-demo')
        if reset:
            command.append('--reset-demo-passwords')
        subprocess.run(command, check=True)
    else:
        token = os.environ.get('NANNYAPP_MIGRATION_TOKEN')
        if not token:
            raise RuntimeError('Configure NANNYAPP_DB_URL or the deployed NANNYAPP_MIGRATION_TOKEN secret.')
        if reset:
            raise RuntimeError('Password rotation requires direct database access; HTTP migrations preserve credentials.')
        result = request(base, '/api/migrate.php', {}, token)
        if result.get('ok') is not True or result.get('schemaVersion') != 'migrate_v13_shared_sessions.sql' or result.get('nativeMigrationCount', 0) < 2:
            raise RuntimeError('The deployed migration endpoint is outdated or incomplete. Deploy the reviewed code first.')
        if seed:
            result = request(base, '/api/bots.php', {'action': 'add', 'demoPassword': os.environ['NANNYAPP_DEMO_PASSWORD']}, token)
            if result.get('ok') is not True:
                raise RuntimeError('Demo account creation failed.')
    health = request(base, '/api/health.php')
    if health.get('status') != 'ok':
        raise RuntimeError('Deployed application health check failed.')
    print('Database migrations completed; deployed health check passed. Existing credentials preserved unless rotation was selected.')


if __name__ == '__main__':
    main()
