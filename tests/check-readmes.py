"""Validate tracked README links and obvious credential leaks without network access."""
from pathlib import Path
import re
import subprocess
from urllib.parse import unquote, urlsplit

ROOT = Path(__file__).resolve().parents[1]


def main():
    tracked = subprocess.check_output(['git', 'ls-files', '-z'], cwd=ROOT).decode().split('\0')
    files = [ROOT / name for name in tracked if Path(name).name.lower() == 'readme.md']
    errors = []
    for path in files:
        content = path.read_text(encoding='utf-8')
        # Private key material, Supabase privileged keys and credential-bearing DB URLs.
        if re.search(r'sb_secret_[A-Za-z0-9_-]{12,}|-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----|postgres(?:ql)?://[^\s:]+:[^\s@]+@', content):
            errors.append(f'{path.relative_to(ROOT)}: possible credential; remove it')
        for href in re.findall(r'\]\(([^)\s]+)(?:\s+"[^"]*")?\)', content):
            target = urlsplit(href.strip('<>'))
            if target.scheme or target.netloc or not target.path:
                continue
            local = (path.parent / unquote(target.path)).resolve()
            if not local.is_relative_to(ROOT) or not local.exists():
                errors.append(f'{path.relative_to(ROOT)}: missing local link {target.path}')
    if errors:
        raise SystemExit('\n'.join(errors))
    print(f'Validated {len(files)} README files: local link targets and credential patterns.')


if __name__ == '__main__':
    main()
