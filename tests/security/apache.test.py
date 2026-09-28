"""Exercise Apache deny rules against synthetic files only (Windows XAMPP)."""
from pathlib import Path
import subprocess
import tempfile
import time
import urllib.request
import urllib.error
import sys

root = Path(__file__).resolve().parents[2]
apache = Path(sys.argv[1] if len(sys.argv) > 1 else 'C:/xampp/apache')
with tempfile.TemporaryDirectory(prefix='nanny-apache-test-') as temporary:
    base = Path(temporary)
    doc = base / 'www'
    doc.mkdir()
    (doc / '.htaccess').write_bytes((root / 'NannyApp.Web/.htaccess').read_bytes())
    logs = doc / 'storage/email_logs'
    logs.mkdir(parents=True)
    (doc / 'storage/.htaccess').write_bytes((root / 'NannyApp.Web/storage/.htaccess').read_bytes())
    (logs / 'synthetic.log').write_text('SYNTHETIC ONLY')
    (doc / 'synthetic.log').write_text('SYNTHETIC ONLY')
    (doc / 'public.txt').write_text('public')
    config = base / 'httpd.conf'
    config.write_text(f'''ServerRoot "{apache.as_posix()}"
Listen 127.0.0.1:13383
ServerName localhost
PidFile "{(base / 'httpd.pid').as_posix()}"
ErrorLog "{(base / 'error.log').as_posix()}"
LoadModule authz_core_module modules/mod_authz_core.so
LoadModule authz_host_module modules/mod_authz_host.so
LoadModule dir_module modules/mod_dir.so
LoadModule alias_module modules/mod_alias.so
LoadModule rewrite_module modules/mod_rewrite.so
DocumentRoot "{doc.as_posix()}"
<Directory "{doc.as_posix()}">
AllowOverride All
Require all granted
</Directory>
''')
    process = subprocess.Popen([str(apache / 'bin/httpd.exe'), '-f', str(config)], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

    def status(path):
        try:
            with urllib.request.urlopen('http://127.0.0.1:13383' + path, timeout=2) as response:
                return response.status
        except urllib.error.HTTPError as error:
            return error.code

    try:
        for _ in range(50):
            try:
                if status('/public.txt') == 200:
                    break
            except OSError:
                time.sleep(.1)
        assert status('/public.txt') == 200
        for path in ['/storage/email_logs/synthetic.log', '/synthetic.log']:
            assert status(path) == 403, path
        print('PASS Apache denies synthetic mail logs and generic .log files')
    finally:
        subprocess.run([str(apache / 'bin/httpd.exe'), '-f', str(config), '-k', 'shutdown'], capture_output=True)
        try:
            process.wait(timeout=10)
        except subprocess.TimeoutExpired:
            process.terminate()
            process.wait(timeout=10)
