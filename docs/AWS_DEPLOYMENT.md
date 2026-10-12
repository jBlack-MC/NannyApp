# AWS deployment: web, Android API and database

The owner requested AWS again on 2026-10-10. This plan supersedes the earlier
free-only hosting selection. No cloud resources have been created. AWS resources
incur charges; choose instance size, region and backup retention in your account.

## Architecture

For a small pilot, use an Ubuntu EC2 instance with Docker Engine and Compose.
`deploy/aws/compose.yml` runs Caddy HTTPS, PHP 8.3/Apache, MariaDB 10.11 and the
private CLI workers. The website and native API share **one database**. Android
connects to `https://YOUR_DOMAIN/api/`, never directly to MariaDB.

Only ports 80/443 are published. Restrict EC2 SSH to your own IP; do not expose
3306. Point your domain's A record at an EC2 Elastic IP. Keep the source and `.env`
outside any public document root. EBS stores named database/upload volumes;
instance termination must not be your backup strategy.

## Local verification on 2026-10-10

PHP/JavaScript syntax, PWA isolation/install, account-cache isolation and APK
download tests passed. Disposable MariaDB 10.11 migration and booking/ledger
regressions passed, including preserving historical booking prices. The synthetic
HTTP/auth/media/recovery suite passed after fixing unread Windows test-server log
pipes; incoming/outgoing conversation isolation is covered. Android debug assembly,
seven unit tests and lint passed for the final navigation changes. Real devices
and live services were not tested.

Compose configuration validates with the example environment. The production
image and full Linux Docker suite could not build: Debian rejected package indexes
as future-dated relative to the Docker clock. Correct time synchronization in the
Docker/host environment and rerun both builds; package freshness checks remain
enabled. The CloudFormation template has not been validated by AWS or applied.

AWS [RDS supports MariaDB](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/CHAP_MariaDB.html)
if a managed database becomes necessary. Lightsail managed databases offer
[MySQL/PostgreSQL](https://docs.aws.amazon.com/lightsail/latest/userguide/amazon-lightsail-databases.html),
so do not substitute one without testing the MariaDB migrations. This deployment
uses MariaDB on EC2; it does not provision RDS or promise high availability.

## Deploy on the Linux instance

The supplied console context selects **Stockholm (`eu-north-1`)**. In that region,
open CloudFormation, choose Create stack / Upload a template file and upload
`deploy/aws/ec2-stack.yml`. Select your VPC and a public subnet in that VPC,
an existing EC2 key pair and your public IPv4 address with `/32`. The template
creates an encrypted 30 GiB Ubuntu host, security group and Elastic IP. Review
the estimated cost and account free-plan eligibility before creating the stack.
The default `t3.small` size is a starting point for the combined web/database;
measure memory before choosing `t3.micro`.

Use the stack's PublicIp output for DNS and SSH. The template retains the instance,
Elastic IP and disk to protect data when deleting the stack; retained resources
continue costing money and require deliberate cleanup after backup. It installs
no application and contains no secrets. Its AMI lookup uses the official
[Ubuntu SSM parameters](https://ubuntu.com/aws/docs/aws-how-to/instances/find-ubuntu-images/)
and [CloudFormation parameter types](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/cloudformation-supplied-parameter-types.html).

Your pasted console reports $100 credits and a free-period end of March 18, 2027.
Track remaining credits and export backups before access ends. Credits do not
make this an indefinite free hosting plan.

Copy the repository to `/srv/nannyapp` without development credentials, APK signing
keys, real uploads or `.codex`. Install Docker using its official Ubuntu instructions.
Create the environment file with separate random database passwords and a verified
email sender. The application does not load dotenv itself; Compose injects values.

```sh
cd /srv/nannyapp
cp deploy/aws/.env.example deploy/aws/.env
chmod 600 deploy/aws/.env
# Edit deploy/aws/.env with your real domain, passwords, sender and support details.
docker compose --env-file deploy/aws/.env -f deploy/aws/compose.yml build
docker compose --env-file deploy/aws/.env -f deploy/aws/compose.yml up -d database
docker compose --env-file deploy/aws/.env -f deploy/aws/compose.yml run --rm migrate
docker compose --env-file deploy/aws/.env -f deploy/aws/compose.yml up -d
docker compose --env-file deploy/aws/.env -f deploy/aws/compose.yml ps
```

The migration runner creates no demo users and tracks migration versions. On an
existing installation, back up and restore-test the database before migrations.
Changing passwords in `.env` does not rotate users in an initialized MariaDB volume;
rotate them in MariaDB as well. Do not run `down --volumes` on a live deployment.
The pilot database user has schema privileges for migrations; separate migration
and runtime users before a larger launch.

The internal Apache port is reachable only through Caddy. Caddy sets the forwarded
HTTPS header used by the PHP session configuration. Keep the app port unpublished.
Certificates require working DNS and inbound 80/443. Review worker logs for failures:

```sh
docker compose --env-file deploy/aws/.env -f deploy/aws/compose.yml logs --tail=100 worker
```

## APK download folder

Build and sign the native app with
`NANNYAPP_RELEASE_API_URL=https://YOUR_DOMAIN/api/`. Put the tested signed APK in
`deploy/aws/releases/NannyApp-latest.apk`. The website's **Download App** page
automatically enables its download button. This folder is mounted outside the
public root; downloads go through `download-app.php?platform=android`.

For local development, use `NannyApp.Shared/releases/NannyApp-latest.apk` instead.
No signed APK is supplied by this deployment; signing keys stay private.

Android 8+ uses the native APK. iPhone/iPad use Safari's Add to Home Screen;
desktops use the responsive website or supported browser installation. An APK
does not run on iOS. Test phones, tablets, desktop widths and large text before
claiming device compatibility. Admin/maintenance pages remain browser-only.

## Backups and release acceptance

Schedule database dumps and private upload backups to encrypted off-host storage
or a private, versioned S3 bucket. Encrypt EBS, retain snapshots, and test restoring
both database and uploads to a separate environment. Verify booking, messaging,
recovery email, private media denials, worker delivery and signed APK installation
using synthetic accounts before admitting real users. Monitor disk, worker errors,
database availability and external HTTPS. Preserve the previous image for rollback;
do not restore an old database over new bookings.

Live deployment still needs the AWS account/session, selected region, domain/DNS,
server access, email credentials and Android signing configuration. Never send
passwords, MFA codes or private signing keys in chat.
