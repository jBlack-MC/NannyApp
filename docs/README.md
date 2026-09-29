# Documentation index

Use these current documents rather than historical infrastructure suggestions.

| Document | Purpose |
| --- | --- |
| [FREE_DEPLOYMENT_PLAN.md](FREE_DEPLOYMENT_PLAN.md) | Authoritative zero-paid-services constraint, free-host evaluation and owner actions |
| [PHASE1_OPERATIONS.md](PHASE1_OPERATIONS.md) | Booking/ledger policy, durable workers, storage limits and migration rollout |
| [AUTOMATION.md](AUTOMATION.md) | Local Docker/non-Docker checks, CI setup and Android release configuration |
| [LAUNCH_READINESS_AUDIT.md](LAUNCH_READINESS_AUDIT.md) | Completed source work versus remaining real-host/device/operations gates |
| [CODE_REVIEW_2026-09-27.md](CODE_REVIEW_2026-09-27.md) | Original findings with dated remediation updates; historical lines may have moved |
| [RESEND_INTEGRATION.md](RESEND_INTEGRATION.md) | Optional account-email adapter and worker; live activation is separate |
| [WEB_MOBILE_FEATURE_MAP.md](WEB_MOBILE_FEATURE_MAP.md) | Cross-client feature map |
| [../NannyApp.Shared/database/MIGRATION_ORDER.md](../NannyApp.Shared/database/MIGRATION_ORDER.md) | Database migration sequence and destructive-script warning |

[PILOT_DEPLOYMENT.md](PILOT_DEPLOYMENT.md) retains release/rollback principles, but its paid VPS/AWS selection is superseded by the free-only plan. [AWS_OWNER_CHECKLIST.md](AWS_OWNER_CHECKLIST.md) is inactive historical planning; do not follow its provisioning steps under the current budget. [PRODUCTION_LAUNCH.md](PRODUCTION_LAUNCH.md) preserves operational acceptance requirements, not evidence that deployment happened.

Password changes and recovery invalidate API tokens, remember-me cookies, outstanding reset links and prior browser sessions. Android clears the initiating account's local session/cache after a confirmed change and returns to sign-in; other devices lose authorization on their next request. Already-running requests are not cancelled retroactively. A timeout after server commit can leave the result uncertain: retry sign-in with the new password or use recovery rather than assuming no change occurred.

Local `.codex` notes contain detailed task/test history and are ignored by Git. Keep generated test logs, disposable database files and preview screenshots out of tracked documentation. Never delete runtime uploads or credentials as routine cleanup.
