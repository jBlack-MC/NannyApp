# Running everything locally

One command starts the website, the API and a database with demo data, so the Android app has a server to talk to. It needs Docker Desktop and nothing else.

## Start

From the repository root:

```sh
docker compose -f deploy/local/compose.yml up -d --build
docker compose -f deploy/local/compose.yml run --rm seed
```

The second command is needed once: it creates the tables and fills them with demo nannies, parents, bookings, reviews and messages. Running it again adds a second copy of the demo bookings; to start over, use `down -v` (below) and seed again. After pulling code that adds a migration, apply only the migration with `docker compose -f deploy/local/compose.yml run --rm seed php NannyApp.Shared/bin/migrate.php`.

| What | Where |
| --- | --- |
| Website | <http://localhost/> |
| API, from your computer | <http://localhost/api/> or <http://localhost/nannyapp/api/> |
| API, from the Android emulator | `http://10.0.2.2/nannyapp/api/`, which is the debug build's default, so nothing needs configuring |
| Health check | <http://localhost/api/health.php> |
| Database, for a SQL client | `127.0.0.1:13306`, user `root`, no password, database `nanny_app` |

## Demo accounts

Every account's password is `Password123!`. The login page shows one-click buttons for them in this setup.

| Account | Use |
| --- | --- |
| `parent@nanny.app` | Parent with no bookings yet |
| `lebo.khumalo@gmail.com` | Parent with bookings, messages and payments: the best one for trying the app |
| `amelia@nanny.app` | Nanny |
| `admin@nanny.app` | Admin, on the website only; the app refuses admin accounts by design |

## Using a real phone

The phone cannot use `10.0.2.2`. Connect it to the same Wi-Fi as your computer, find your computer's address (`ipconfig` on Windows, the "IPv4 Address" line), then build the app with that address:

```sh
./gradlew assembleDebug -PNANNYAPP_DEBUG_API_URL=http://192.168.1.20/nannyapp/api/
```

Windows Firewall may ask to allow Docker on private networks; allow it.

## Everyday commands

```sh
docker compose -f deploy/local/compose.yml logs -f app        # watch requests and errors
docker compose -f deploy/local/compose.yml up -d --build app  # pick up PHP changes
docker compose -f deploy/local/compose.yml down               # stop, keeping the data
docker compose -f deploy/local/compose.yml down -v            # stop and delete the data
```

The image copies the code in at build time, so rebuild the `app` service after editing PHP, CSS or JavaScript.

## If port 80 is taken

XAMPP's Apache or another web server may already use port 80. Either stop it, or start this stack on another port and build the app to match:

```sh
NANNYAPP_DEV_PORT=8080 docker compose -f deploy/local/compose.yml up -d
./gradlew assembleDebug -PNANNYAPP_DEBUG_API_URL=http://10.0.2.2:8080/nannyapp/api/
```

## What differs from production

- The database has no password and is reachable from your computer.
- The demo sign-in buttons are switched on (`NANNYAPP_DEMO_LOGIN=1`).
- Account emails (verification and password reset) are not sent, because there is no Resend key. In-app notifications work.
- There is no HTTPS.

Never use `deploy/local/` files on a server; production uses [AWS_DEPLOYMENT.md](AWS_DEPLOYMENT.md).
