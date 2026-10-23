"""Exercise production workflow decisions without credentials or external writes."""
import os
from pathlib import Path
import sys
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'scripts'))
import deploy_database


class DeploymentTests(unittest.TestCase):
    def test_http_schema_only_needs_no_demo_password(self):
        with patch.dict(os.environ, {'APP_URL': 'https://example.invalid', 'NANNYAPP_MIGRATION_TOKEN': 'synthetic'}, clear=True), patch.object(deploy_database, 'request', side_effect=[{'ok': True, 'schemaVersion': 'migrate_v14_contact_messages.sql', 'nativeMigrationCount': 3}, {'status': 'ok'}]) as request:
            deploy_database.main()
            self.assertEqual([c.args[1] for c in request.call_args_list], ['/api/migrate.php', '/api/health.php'])

    def test_old_deployed_endpoint_cannot_report_success(self):
        with patch.dict(os.environ, {'APP_URL': 'https://example.invalid', 'NANNYAPP_MIGRATION_TOKEN': 'synthetic'}, clear=True), patch.object(deploy_database, 'request', return_value={'ok': True}), self.assertRaisesRegex(RuntimeError, 'outdated'):
            deploy_database.main()

    def test_direct_migration_does_not_implicitly_seed(self):
        with patch.dict(os.environ, {'APP_URL': 'https://example.invalid', 'NANNYAPP_DB_URL': 'synthetic'}, clear=True), patch.object(deploy_database.subprocess, 'run') as run, patch.object(deploy_database, 'request', return_value={'status': 'ok'}):
            deploy_database.main()
            self.assertNotIn('--seed-demo', run.call_args.args[0])

    def test_reset_requires_explicit_seed(self):
        with patch.dict(os.environ, {'APP_URL': 'https://example.invalid', 'RESET_DEMO_PASSWORDS': 'true'}, clear=True), self.assertRaisesRegex(RuntimeError, 'explicit demo seeding'):
            deploy_database.main()

    def test_insecure_origin_rejected(self):
        with patch.dict(os.environ, {'APP_URL': 'http://example.invalid'}, clear=True), self.assertRaisesRegex(RuntimeError, 'HTTPS'):
            deploy_database.main()


if __name__ == '__main__':
    unittest.main()
