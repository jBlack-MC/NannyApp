import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location('deployment', Path(__file__).resolve().parents[1] / 'scripts/verify_deployment.py')
deployment = importlib.util.module_from_spec(spec)
spec.loader.exec_module(deployment)


class DeploymentChecks(unittest.TestCase):
    def response(self, path):
        return (401 if path.endswith('/profile.php') else 200,
                {'Cache-Control': 'no-store'},
                '{"status":"ok"}' if path.endswith('health.php') else '<input name="csrf">')

    def test_success(self):
        deployment.verify('https://example.invalid', self.response)

    def test_failures_cannot_pass_as_healthy(self):
        for target, result in [('/auth/login.php', (200, {}, '<input name="csrf">')),
                               ('/auth/register.php', (200, {'Cache-Control': 'no-store'}, 'Something went wrong')),
                               ('/api/user/profile.php', (200, {'Cache-Control': 'no-store'}, '{}')),
                               ('/api/health.php', (200, {'Cache-Control': 'no-store'}, '{"status":"degraded"}')),
                               ('/', (302, {}, ''))]:
            with self.subTest(target=target), self.assertRaises(RuntimeError):
                deployment.verify('https://example.invalid', lambda path: result if path == target else self.response(path))

    def test_reject_invalid_origins_before_network(self):
        for url in ['http://example.invalid', 'https://user:pass@example.invalid', 'https://example.invalid/path', 'https://example.invalid?token=x']:
            with self.subTest(url=url), self.assertRaises(ValueError):
                deployment.verify(url, self.response)


if __name__ == '__main__':
    unittest.main()
