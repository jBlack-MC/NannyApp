# AWS, storage and app publishing: owner checklist

## INACTIVE: superseded by the zero-cost requirement

Do not execute the AWS provisioning, purchasing or paid enrollment steps below. They are historical planning only. Use [FREE_DEPLOYMENT_PLAN.md](FREE_DEPLOYMENT_PLAN.md), including its current owner checklist. No AWS login or new AWS resources are required for that plan.

Updated 2026-09-28. Small team with pilot users. AWS is the selected deployment provider. No AWS resources, accounts, billing subscriptions or production release were created in this session.

## First: sign in yourself

Open [AWS sign-in](https://signin.aws.amazon.com/) in your own browser. This coding session has no shared interactive browser; its attempt to launch your browser was blocked by automatic approval review. Never paste your password, MFA codes, secret access keys or signing passwords into chat.

- [ ] Create/verify the AWS account or sign in to your existing account. Complete identity/payment checks yourself.
- [ ] Enable root-account MFA, retain recovery information securely, and use a separate least-privilege operator identity for routine work. Never create root access keys.
- [ ] Check actual account credits and service prices; choose a monthly spending limit and configure billing alerts before provisioning. Alerts do not cap spending.
- [ ] Choose the domain, AWS region, support email, operating owner and acceptable backup data loss/recovery time. Share only these non-secret choices with the developer.

AWS's current new-customer free plan/credits are time-limited and eligibility-dependent. They are not permanent free hosting. Check the [AWS Free Tier terms](https://aws.amazon.com/free/free-tier-faqs/) in your account. Domains, storage requests/transfer, snapshots and server usage can incur separate charges.

## Deployment and storage decisions

Start with one Lightsail Linux instance running Apache, PHP-FPM and a loopback-only MariaDB database, following [the pilot release plan](PILOT_DEPLOYMENT.md). Check the chosen region and bundle in the console before purchase. This is a single-server pilot, not high availability. Do not expose port 3306. Open HTTPS/HTTP for the application; restrict SSH to the operator's address. Add a static IP, DNS and TLS certificate before public testing.

Use **private Amazon S3** for uploaded images and identity/portfolio documents. S3 is object storage, not a second relational database:

| Data | Location | Access |
| --- | --- | --- |
| Accounts, ownership, bookings, messages, file keys and manual-payment ledger | MariaDB | PHP server only; no public DB port |
| Profile images and private documents | Dedicated S3 uploads bucket | PHP checks the database reference/authorization before returning the media response; private documents use short-lived signed links |
| Database backups | Separate private S3 backup bucket | Backup operator credentials; application upload credentials cannot read it |
| Signed Android APK | Persistent releases directory outside the webroot initially | Public download endpoint streams only the configured release file; never place customer uploads here |

- [ ] Create the uploads bucket with a unique name in the agreed region. Keep **all Block Public Access controls enabled**, ACLs disabled, default encryption enabled, and enable versioning. See [AWS access guidance](https://docs.aws.amazon.com/AmazonS3/latest/userguide/access-control-block-public-access.html) and [S3 security practices](https://docs.aws.amazon.com/AmazonS3/latest/userguide/security-best-practices.html).
- [ ] Create a separate backup bucket. Choose retention/lifecycle periods with the team; versioning retains old sensitive files and costs money. Do not configure immediate expiration of live uploads.
- [ ] Provision a dedicated application identity with only GetObject/PutObject/DeleteObject for the uploads bucket's actual upload prefixes; no wildcard access to other buckets or account administration. Keep backup privileges separate.
- [ ] Supply the current adapter's `NANNYAPP_STORAGE_DRIVER=s3`, `NANNYAPP_S3_BUCKET`, `NANNYAPP_S3_REGION`, `NANNYAPP_S3_ACCESS_KEY` and `NANNYAPP_S3_SECRET_KEY` through protected server environment files. Leave `NANNYAPP_S3_ENDPOINT` and `NANNYAPP_S3_PUBLIC_BASE_URL` empty for private AWS S3. Rotate keys and never put them in an APK, repository or browser.
- [ ] Test synthetic upload/read/delete, unrelated-user denial and expired signed URLs against the real bucket. Source/synthetic signing checks do not certify a live bucket policy.
- [ ] Copy existing local uploads preserving their exact database object keys; compare inventory/checksums and verify reads before switching the driver. Keep a rollback copy. Changing the environment variable alone does not migrate files.
- [ ] Resolve the documented orphan-document deletion defect before relying on deletion as a retention guarantee; include old object versions in the deletion policy. Rehearse database-plus-object restore to an isolated environment.

The custom S3 adapter currently expects explicit access/secret keys and does not implement the AWS SDK instance-role credential chain/session-token refresh. Do not claim that attaching a role alone enables it. A later EC2/SDK migration can remove long-lived application keys; it is separate implementation work.

## Accounts and settings you must supply

| Service | Owner action | Cost expectation |
| --- | --- | --- |
| AWS | Account verification, MFA, billing, region/bucket/server approval | Credits may apply; ongoing hosting/storage is billable |
| Domain registrar / DNS | Register or bring a domain and approve DNS records | Domain registration/renewal usually paid; no extra DNS account needed if your registrar provides DNS |
| Resend | Create account, verify sender domain with DNS, create send-only key and put it in worker environment | [Free tier](https://resend.com/pricing) has limits; verify current allowance before relying on it |
| GitHub, if not already used | Own the private repository and authorize deployment access when needed | Use your existing account; no additional account needed for local builds |
| Apple Developer, only for native iOS distribution | Organization/identity verification, agreements, membership and signing ownership | [Apple enrollment](https://developer.apple.com/programs/enroll/) currently lists USD 99/year; Home Screen web installation does not require enrollment |
| Google Play, if choosing store distribution | Create/verify developer account, accept terms and complete applicable testing/release requirements | Check the actual enrollment charge and requirements at registration; direct APK distribution does not require Play enrollment |

Do not create extra accounts simply because they have free plans. This pilot does not need Firebase, a second hosted database, a separate image service or Kubernetes.

## Publish install options

- [x] A public Get the app page offers Android download when a release exists, iPhone/iPad Home Screen instructions and supported-browser installation.
- [x] Android download reads the configured server file; no debug APK is published as a production release.
- [ ] Set the real HTTPS Android API URL; its current release placeholder is still a blocker. Build and test a signed release, keep the signing key/password offline or in protected signing storage, and retain the same key for updates.
- [ ] Upload the signed APK outside the webroot and set `NANNYAPP_APK_PATH` to its absolute path. Verify the download bytes/signature and installation on a physical phone. The button remains unavailable until that file exists.
- [ ] Test Home Screen installation on iPhone/iPad Safari and installation on supported desktop/Android browsers over the live HTTPS domain. Private bookings/messages require a connection; this is not an offline account-data cache.
- [ ] For a true native iOS app, approve that separate scope, provide Apple enrollment and access to a Mac/Xcode build/signing environment, implement an iOS client against the API, then test through TestFlight and submit for review. Kotlin Android code/APKs do not run on iOS. No native iOS binary exists in this delivery.

Apple documents the immediate iPhone option in [Add a website as a web app](https://support.apple.com/guide/iphone/open-as-web-app-iphea86e5236/ios). Installation support differs by browser; do not advertise native Windows/macOS/Linux binaries that do not exist.

## Before inviting users

- [ ] Fix the remaining authorization, booking/ledger concurrency and Android release defects in [the review](CODE_REVIEW_2026-09-27.md).
- [ ] Apply reviewed migrations v6 and v7 when enabling Resend; never import the destructive development schema into an existing database.
- [ ] Configure the email worker, live-delivery test, backup timer, external HTTPS monitoring, internal DB readiness check and queue/failure alerts.
- [ ] Provide approved privacy/support/cancellation text, real support contacts and manual-payment operating rules; remove demo accounts.
- [ ] Test login, booking, messaging, upload authorization, logout/account switching, dark/light themes, keyboard/touch navigation and rollback on real target devices.

The developer can prepare code, deployment files, tests and troubleshoot with sanitized output. You must own account enrollment, MFA, billing, domain ownership, secret provisioning, store/legal agreements and signing identities. Deployment can proceed once that access/configuration is available and the remaining release checks pass; it has not happened yet.
