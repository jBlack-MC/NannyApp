"""Reject a release APK that does not contain the expected backend URL."""
import argparse
import re
from urllib.parse import urlparse
from zipfile import ZipFile


def check_apk(path, expected):
    parsed = urlparse(expected)
    if parsed.scheme != 'https' or not parsed.hostname or not parsed.path.endswith('/api/'):
        raise ValueError('Expected an HTTPS backend URL ending in /api/.')
    with ZipFile(path) as apk:
        dex = [apk.read(name) for name in apk.namelist() if re.fullmatch(r'classes\d*\.dex', name)]
    if not any(expected.encode() + b'\0' in content for content in dex):
        raise ValueError('APK does not contain the configured backend URL; do not publish this artifact.')
    for obsolete in (b'https://api.nannyapp.com/api/\0', b'https://ci.example.invalid/api/\0'):
        if obsolete != expected.encode() + b'\0' and any(obsolete in content for content in dex):
            raise ValueError('APK contains an obsolete or synthetic backend URL.')
    print('APK backend URL verified:', expected)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('apk')
    parser.add_argument('--expected', required=True)
    args = parser.parse_args()
    check_apk(args.apk, args.expected)
