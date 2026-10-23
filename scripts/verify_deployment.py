"""Read-only hosted web/API checks. No login credentials or database mutations."""
import argparse
import json
from urllib.error import HTTPError
from urllib.parse import urlsplit
from urllib.request import Request, build_opener, HTTPRedirectHandler


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def verify(base, fetch=None):
    parsed = urlsplit(base)
    if (parsed.scheme != 'https' or not parsed.hostname or parsed.username or parsed.password
            or parsed.path not in ('', '/') or parsed.query or parsed.fragment):
        raise ValueError('APP_URL must be an HTTPS origin without credentials or a path.')
    base = base.rstrip('/')
    opener = build_opener(NoRedirect)

    def request(path):
        try:
            response = opener.open(Request(base + path, headers={'User-Agent': 'NannyApp-deployment-check'}), timeout=30)
        except HTTPError as error:
            response = error
        with response:
            return response.status, response.headers, response.read(1048576).decode('utf-8')

    fetch = fetch or request
    for path in ('/', '/auth/login.php', '/auth/register.php', '/api/health.php', '/api/user/profile.php'):
        status, headers, body = fetch(path)
        expected = 401 if path == '/api/user/profile.php' else 200
        if status != expected:
            raise RuntimeError(f'{path}: expected HTTP {expected}, received {status}')
        if path.startswith(('/auth/', '/api/')) and 'no-store' not in headers.get('Cache-Control', '').lower():
            raise RuntimeError(f'{path}: missing no-store cache policy')
        if path == '/api/health.php' and json.loads(body).get('status') != 'ok':
            raise RuntimeError('API health reports degraded service')
        if path.startswith('/auth/') and 'name="csrf"' not in body:
            raise RuntimeError(f'{path}: authentication form is missing')
        print(f'PASS {path}: HTTP {status}')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--url', required=True)
    args = parser.parse_args()
    verify(args.url)
