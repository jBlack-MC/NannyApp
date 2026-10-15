# Troubleshooting: finding out what broke

Start here when the website, the API or the Android app stops working. Commands assume the AWS deployment in [AWS_DEPLOYMENT.md](AWS_DEPLOYMENT.md) and are run from the repository root on the server.

To save typing, define this once per shell session:

```sh
alias dc='docker compose --env-file deploy/aws/.env -f deploy/aws/compose.yml'
```

## 1. Quick checks

| Check | How | Healthy result |
| --- | --- | --- |
| Is everything up? | `dc ps` | `proxy`, `app`, `database` and `worker` all `running`; `database` is `healthy` |
| Is the application healthy? | `curl -i https://YOUR-DOMAIN/api/health.php` | `200` and `{"status":"ok",...}` |
| What does an admin see? | Sign in on the website, open **Admin → Action centre → System health** | Every check shows **OK** |

`/api/health.php` returns `503` when the database is unreachable (`"status":"down"`), or when a queue has stalled, migrations are missing or the upload disk is nearly full (`"status":"degraded"`). The response names the failed check only; the detail is on the admin page and in the log.

## 2. A user reports an error with a reference code

Server-side failures show the user a reference such as `4F2A9C1B`: on the web error page, and in the app as "(Reference: 4F2A9C1B)". Every log line written for that request carries the same code.

```sh
dc logs --since 24h app worker | grep 4F2A9C1B
```

The matching line looks like this:

```text
nannyapp {"level":"error","event":"unhandled_exception","ref":"4F2A9C1B","method":"POST","path":"/api/bookings/create.php","type":"PDOException","code":"42S22","message":"...","at":"bookings.php:88","trace":["bookings.php:88 create_booking", "..."]}
```

`at` and `trace` give the file and line that failed. `path` is the endpoint or page the user was on.

If the user has no reference, ask for the time and what they were doing, then read the errors around that time:

```sh
dc logs --since 2h app | grep '"level":"error"'
```

## 3. Reading the logs

| Service | Command | What it contains |
| --- | --- | --- |
| Website and API | `dc logs --tail=200 app` | Application errors (`nannyapp {...}` lines), PHP warnings, one access line per request |
| Background jobs | `dc logs --tail=200 worker` | Email, notification and file-deletion jobs, and their failures |
| Database | `dc logs --tail=200 database` | MariaDB start-up, crashes, connection problems |
| HTTPS proxy | `dc logs --tail=200 proxy` | Certificate renewal and TLS problems |

Add `-f` to follow a log live. Each container keeps five 10 MB log files, so older entries are discarded automatically; copy out anything you need to keep.

Application events you will see in `event`:

| Event | Meaning |
| --- | --- |
| `unhandled_exception` | Code threw an error nobody caught. The user saw the reference page. |
| `fatal_error` | PHP itself stopped (missing file, syntax error, out of memory). Usually follows a bad deploy. |
| `api_server_error` | An API endpoint answered 5xx. `reason` is the message the app showed. |
| `database_connection_failed` | The app could not reach MariaDB. Check `dc ps` and the database log. |
| `login_failed`, `registration_failed`, `password_change_failed` | The named action failed for a server reason, not because of wrong input. |
| `mail_worker_failed`, `notification_worker_failed`, `storage_worker_failed` | A background job crashed. Pending jobs stay queued and are retried. |
| `health_degraded`, `health_database_down` | The health endpoint found a problem; `failed` lists the checks. |

## 4. Common symptoms

| Symptom | Likely cause | What to do |
| --- | --- | --- |
| Site and app both unreachable | Server down, proxy stopped or certificate problem | `dc ps`, then `dc logs --tail=100 proxy`. Restart with `dc up -d`. |
| "Database connection failed" page | Database container stopped or wrong credentials in `deploy/aws/.env` | `dc ps`, `dc logs --tail=100 database`. |
| Blank page or reference page right after a deploy | `fatal_error`, or a migration was not applied | Read the `app` log, then run `dc run --rm migrate`. |
| Verification or reset emails never arrive | Email worker failing or the Resend key is missing | `dc logs --tail=100 worker`; look for `mail_worker_configuration_missing` or `mail_worker_failed`. See [RESEND_INTEGRATION.md](RESEND_INTEGRATION.md). |
| Booking notifications do not appear | Notification worker stalled | Health shows `notifications` failed; read the `worker` log and restart with `dc restart worker`. |
| Uploads rejected | Upload quota or disk reserve reached | Health shows `storage`; see the quota section of [PHASE1_OPERATIONS.md](PHASE1_OPERATIONS.md). |
| The app says "You're offline" on good Wi-Fi | The app cannot reach the API URL it was built with | Open `https://YOUR-DOMAIN/api/health.php` in the phone's browser. If that works, the APK was built with the wrong `NANNYAPP_RELEASE_API_URL`. |
| The app says "Can't connect" | The app cannot reach its server. Debug builds print the address they tried under the message | Locally, start the server in [LOCAL_DEVELOPMENT.md](LOCAL_DEVELOPMENT.md). Otherwise open that address plus `health.php` in the phone's browser. |
| The app keeps returning to the login screen | The server rejected the saved sign-in (expired, revoked, or the password changed), so the app cleared it | Sign in again. If it happens straight after signing in, the server is not receiving the `Authorization` header; check that `request_authorization()` in `api/_bootstrap.php` is deployed. |
| The app crashes or closes | A bug in the app itself; the server log will show nothing | See section 6. |

## 5. Getting told before users notice

Point any uptime monitoring service at `https://YOUR-DOMAIN/api/health.php`, checking every five minutes and alerting on any status other than `200`. This is not configured by the repository; it needs an account with the monitoring service.

## 6. Android app problems

Server errors reach the app with a reference code (section 2). Crashes inside the app are **not reported automatically yet**, so they need the device:

```sh
adb logcat -d AndroidRuntime:E *:S
```

Run this with the phone connected by USB, straight after the crash; the output is the stack trace. Debug builds also log each API request and its status under the `okhttp.OkHttpClient` tag.

The information pages (About, Safety, Pricing and the rest) are bundled inside the APK from the website. If they are out of date, run `python tests/build-public-content.py` and rebuild the APK.

## 7. What must never be logged

This is a childcare product. Log lines must not contain names, email addresses, phone numbers, home addresses, children's details, message text, passwords or tokens. The shared logger in `NannyApp.Shared/config/logging.php` records the request path without its query string, strips emails and long tokens from error messages, and omits function arguments from traces. Keep to `log_event()` and `log_exception()` rather than writing request data to the log.
