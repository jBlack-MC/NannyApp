"""Build an explicit source-only context and always clean the disposable test stack."""
from pathlib import Path
import subprocess
import sys
import shutil
import tarfile
import tempfile

root = Path(__file__).resolve().parents[1]
folders = ['NannyApp.Shared/config', 'NannyApp.Shared/database', 'NannyApp.Shared/bin',
           'NannyApp.Web', 'NannyApp.Mobile/NannyApp/api', 'NannyApp.Mobile/NannyApp/includes',
           'NannyApp.Mobile/NannyApp/config', 'NannyApp.Mobile/NannyApp/app/src', 'tests']
source_types = {'.php', '.js', '.cjs', '.css', '.sql', '.kt', '.xml', '.py', '.webmanifest'}
if not shutil.which('docker'):
    raise SystemExit('Docker is required; start Docker Engine/Compose or use tests/run-checks.py locally.')
compose = ['docker', 'compose', '-p', 'nannyapp-tests', '-f', str(root / 'compose.test.yml')]
result = 1
try:
    with tempfile.TemporaryFile() as context:
        with tarfile.open(fileobj=context, mode='w', format=tarfile.GNU_FORMAT) as archive:
            archive.add(root / 'tests/docker/Dockerfile', arcname='Dockerfile')
            for folder in folders:
                for path in sorted((root / folder).rglob('*')):
                    if not path.is_file() or path.is_symlink():
                        continue
                    relative = path.relative_to(root)
                    if any(part in relative.parts for part in ['.git', 'node_modules', 'vendor', 'releases', '__pycache__']):
                        continue
                    if 'storage' in relative.parts and path.name != '.htaccess':
                        continue
                    if path.name in ['db_credentials.php', 'paystack.php', 'local.properties'] or path.name.startswith('.env'):
                        continue
                    if path.suffix not in source_types and path.name != '.htaccess':
                        continue
                    archive.add(path, arcname=relative.as_posix(), recursive=False)
        context.seek(0)
        subprocess.run(['docker', 'build', '--progress=plain', '-t', 'nannyapp-tests-tests', '-'],
                       stdin=context, cwd=root, check=True)
    result = subprocess.run(compose + ['up', '--no-build', '--abort-on-container-exit', '--exit-code-from', 'tests'], cwd=root).returncode
except subprocess.CalledProcessError as error:
    result = error.returncode
finally:
    cleanup = subprocess.run(compose + ['down', '--volumes', '--remove-orphans'], cwd=root).returncode
    if result == 0 and cleanup != 0:
        result = cleanup
sys.exit(result)
