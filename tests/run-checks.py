"""Cross-platform checks; --backend requires a disposable loopback MariaDB."""
import argparse
import pathlib
import shutil
import subprocess
import sys

root = pathlib.Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('--backend', action='store_true')
parser.add_argument('--php', default='php')
args = parser.parse_args()
php = shutil.which(args.php)
if not php:
    raise SystemExit('PHP executable not found; specify --php /path/to/php')
def run(command):
    print('+ ' + ' '.join(map(str, command)), flush=True)
    subprocess.run(command, cwd=root, check=True)
try:
    for folder in ['NannyApp.Shared', 'NannyApp.Web', 'NannyApp.Mobile/NannyApp/api', 'NannyApp.Mobile/NannyApp/includes', 'tests/security']:
        for path in sorted((root / folder).rglob('*.php')):
            if 'storage' not in path.parts:
                run([php, '-l', str(path.relative_to(root))])
    for path in sorted((root / 'NannyApp.Web/assets/js').glob('*.js')):
        run(['node', '--check', str(path.relative_to(root))])
    run(['node', '--check', 'NannyApp.Web/service-worker.js'])
    run([php, 'tests/security/public-urls.test.php'])
    run([php, 'tests/security/db-settings.test.php'])
    run([sys.executable, 'tests/deploy-database.test.py'])
    run([sys.executable, 'tests/apk-endpoint.test.py'])
    run([php, 'tests/security/postgres-dashboard.test.php', 'pgsql'])
    run([php, 'tests/security/postgres-dashboard.test.php', 'mysql'])
    run([php, 'tests/security/web-sessions.test.php'])
    run([php, 'tests/security/session-policy.test.php'])
    run([php, 'tests/security/demo-bots.test.php'])
    for test in ['service-worker.test.cjs', 'install.test.cjs']:
        run(['node', 'tests/security/' + test])
    run([sys.executable, 'tests/security/android-cache.test.py'])
    run([sys.executable, 'tests/security/public-content.test.py', php])
    run([sys.executable, 'tests/security/home-content.test.py', php])
    run([sys.executable, 'tests/security/download.test.py', php])
    if shutil.which('apache2'):
        run([sys.executable, 'tests/security/apache.test.py'])
    if args.backend:
        run([php, 'tests/security/migrations.test.php'])
        run([php, 'tests/security/operations.test.php'])
        run([sys.executable, 'tests/security/run-backend.py', php])
        run([php, 'tests/security/resend.test.php'])
except subprocess.CalledProcessError as error:
    raise SystemExit(error.returncode)
print('All requested checks passed. Android and real-device checks run separately.')
