# Legacy email log directory

The application does not log email bodies, recipients or account links here. Configure delivery using the supported mail transport or Resend adapter described in [email operations](../../../docs/RESEND_INTEGRATION.md). Use the synthetic mail tests in [security tests](../../../tests/security/README.md) for development.

Historical local logs are ignored and denied by Apache. Previously tracked files may remain in Git history; deployment owners must assess exposure and revoke affected live tokens. Do not publish or upload local log contents as CI artifacts. Runtime delivery status belongs in the protected email outbox and operations dashboard.
