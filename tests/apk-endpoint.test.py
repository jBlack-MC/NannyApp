import importlib.util
from pathlib import Path
import tempfile
import unittest
from zipfile import ZipFile

spec = importlib.util.spec_from_file_location('apk_check', Path(__file__).resolve().parents[1] / 'scripts/check_apk_endpoint.py')
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class ApkEndpointTests(unittest.TestCase):
    def test_release_endpoint_is_embedded_and_obsolete_values_are_rejected(self):
        expected = 'https://example.invalid/api/'
        with tempfile.TemporaryDirectory() as directory:
            apk = Path(directory) / 'app.apk'
            for content, valid in [(expected.encode() + b'\0', True), (b'https://api.nannyapp.com/api/\0', False),
                                   (expected.encode() + b'\0https://ci.example.invalid/api/\0', False), (expected.encode() + b'wrong\0', False)]:
                with ZipFile(apk, 'w') as archive:
                    archive.writestr('classes.dex', content)
                if valid:
                    module.check_apk(apk, expected)
                else:
                    with self.assertRaises(ValueError):
                        module.check_apk(apk, expected)


if __name__ == '__main__':
    unittest.main()
