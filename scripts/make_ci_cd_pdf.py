from pathlib import Path

out = Path(__file__).resolve().parents[1] / 'docs' / 'deliverables' / '07-ci-cd-and-release.pdf'
lines = [
    'NANNY-APP - CI/CD AND APK RELEASE PLAN',
    'Task 2 implementation evidence | Version 1.0 | 19 October 2026',
    '',
    '1. PIPELINE',
    'Commit or pull request -> PHP/security checks -> Android tests/lint/build',
    '-> review and tag -> signed APK GitHub Release.',
    'Every change is checked by GitHub Actions. Releases are created from tags',
    'so the source, APK and notes remain traceable.',
    '',
    '2. GITHUB ACTIONS CHECKS',
    'CI: PHP/security suite, Android debug build, JVM tests, lint and unsigned',
    'release compilation. Output: test summary and build artifacts.',
    'Android release: JDK 21, Android SDK 36, HTTPS API validation, signing',
    'and APK signature verification. Output: NannyApp.apk GitHub asset.',
    'Supabase migration: protected migration endpoint and health check.',
    '',
    '3. REQUIRED REPOSITORY CONFIGURATION',
    'NANNYAPP_RELEASE_KEYSTORE_BASE64',
    'NANNYAPP_RELEASE_KEYSTORE_PASSWORD',
    'NANNYAPP_RELEASE_KEY_ALIAS',
    'NANNYAPP_RELEASE_KEY_PASSWORD',
    'Set NANNYAPP_RELEASE_API_URL to an HTTPS API ending in /api/. Never',
    'commit secrets or the signing keystore.',
    '',
    '4. RELEASE PROCEDURE',
    '1. Run CI and confirm tests, lint and release compilation pass.',
    '2. Create and push an annotated version tag such as v1.0.2.',
    '3. GitHub Actions builds, signs and verifies the APK and attaches it.',
    '4. Set NANNYAPP_APK_URL on the web host to the latest-release URL.',
    '5. Install on a real Android phone and record the result.',
    '',
    '5. QUALITY AND ROLLBACK',
    'Automated checks pass; API is HTTPS; migrations are applied; signature',
    'verifies; registration, verification, login and settings work on a real',
    'device; and the previous GitHub release remains available for rollback.',
    '',
    '6. DEMONSTRATION EVIDENCE',
    'Show the green Actions run, GitHub Release APK asset, website download',
    'page, phone installation, registration/login, database/API records and',
    'a voice-over explaining the release pipeline.',
]

def esc(value):
    return value.replace('\\', '\\\\').replace('(', '\\(').replace(')', '\\)')

content = ['BT', '/F1 11 Tf', '50 790 Td']
for line in lines:
    content.append(f'({esc(line)}) Tj')
    content.append('0 -16 Td')
content.append('ET')
stream = ('\n'.join(content)).encode('latin-1', 'replace')
objects = [
    b'<< /Type /Catalog /Pages 2 0 R >>',
    b'<< /Type /Pages /Kids [3 0 R] /Count 1 >>',
    b'<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>',
    b'<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>',
    b'<< /Length ' + str(len(stream)).encode() + b' >>\nstream\n' + stream + b'\nendstream',
]
pdf = bytearray(b'%PDF-1.4\n')
offsets = [0]
for number, obj in enumerate(objects, 1):
    offsets.append(len(pdf)); pdf += f'{number} 0 obj\n'.encode() + obj + b'\nendobj\n'
xref = len(pdf); pdf += f'xref\n0 {len(objects)+1}\n0000000000 65535 f \n'.encode()
for offset in offsets[1:]: pdf += f'{offset:010d} 00000 n \n'.encode()
pdf += f'trailer\n<< /Size {len(objects)+1} /Root 1 0 R >>\nstartxref\n{xref}\n%%EOF\n'.encode()
out.write_bytes(pdf)
print(out)
