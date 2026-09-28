Mail bodies, recipients and account links are no longer logged. Configure
`NANNYAPP_MAIL_TRANSPORT=mail`, `NANNYAPP_MAIL_FROM` and the host PHP mail transport.
Use the synthetic SMTP sink in `tests/security/` for development tests.

Historical local logs are ignored and denied by Apache. Two previously tracked
logs were removed from the index; existing local copies and Git history are
preserved. Deployment owners must assess historical exposure, revoke any
affected live tokens, and arrange history cleanup separately if required.
