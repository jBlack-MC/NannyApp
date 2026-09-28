# Nanny-App

## Automated checks

Run `python tests/docker-checks.py` from the repository root for an isolated PHP/MariaDB security test environment with automatic cleanup. For non-Docker and Android commands, see [Automation](docs/AUTOMATION.md). CI runs on pushes and pull requests; private repositories require `FREE_CI_CONFIRMED=true` after the owner disables paid overages. No workflow deploys or publishes an app.

Release builds require `NANNYAPP_RELEASE_API_URL` as a Gradle property or environment variable. The [free-only deployment plan](docs/FREE_DEPLOYMENT_PLAN.md) remains authoritative; Kubernetes is not needed for this stage.


![Nanny-App logo](NannyApp.Web/assets/Icon_Logo.png)

Nanny-App is a childcare booking marketplace that connects parents with verified nannies, supported by an admin moderation layer and a manually reconciled payment ledger. The project is designed as a full-stack platform with a PHP web app, a native Android app, and a shared backend/data layer used by both.

**Module:** XISD6329 — Work Integrated Learning 3B

**Demo video:** [Watch on YouTube](https://youtu.be/nO85AqjW2E4)

---

## Overview

Nanny-App enables families to:

- browse and filter verified nannies
- create child profiles and booking requests
- manage booking flow, check-ins, and session completion
- confirm session completion and record payment-release decisions
- communicate in-app with nannies and receive notifications

Nanny-App also gives nannies:

- a profile and availability management system
- job acceptance/rejection workflow
- earnings tracking and verification status
- messaging and review management

And admins can:

- verify nanny accounts
- manage users and bookings
- oversee payments and disputes
- handle support issues and broadcast notices

Payments are manual/offline in the current pilot. Held, released and refunded are ledger states; the app does not charge cards, transfer funds or provide automated escrow.

---

## Product architecture

This repository is split into three main parts:

| Folder | Purpose |
| --- | --- |
| [NannyApp.Web](NannyApp.Web/README.md) | Main PHP web application for desktop and mobile browsers. Runs on PHP + MySQL, supports a PWA and a wrapped Android APK experience. |
| [NannyApp.Mobile](NannyApp.Mobile/README.md) | Native Android app built with Kotlin and Jetpack Compose, backed by a REST API layer. |
| [NannyApp.Shared](NannyApp.Shared/README.md) | Shared database schema, config, and file-storage abstraction reused by both client apps. |

### How the system fits together

- The PHP web app and PHP API share the database; Android accesses it through the API.
- Both systems use the same upload logic and storage abstraction managed through the shared layer.
- The shared layer defines the canonical database schema and migration sequence.
- In local development, uploads can stay on disk. In production, the system supports S3-compatible object storage.
- Both PHP frontends require the adjacent `NannyApp.Shared` folder for security, email, database and storage services.

```text
Nanny-App
├── NannyApp.Web/        PHP marketplace + PWA + packaged APK wrapper
├── NannyApp.Mobile/     Native Android client + PHP JSON API
├── NannyApp.Shared/     Shared DB schema, config and upload storage layer
├── docs/                Project docs and deployment notes
├── tests/               Automated checks and validation scripts
├── compose.test.yml     Local test environment configuration
├── README.md            Project overview
└── ...
```

---

## Core platform features

### Parent features

- secure registration and profile creation
- browse verified nannies by location, rate, experience, and skills
- search and filter nanny listings
- complete booking workflow with child details and scheduling
- check booking overlap (concurrent-request hardening remains open in the code review)
- receive a one-time check-in PIN for a verified in-person arrival
- confirm session completion to update the manual payment ledger
- cancel eligible bookings and record refund decisions for manual reconciliation
- manage saved nannies and favourites
- view payment history and manual payout status
- review completed sessions with ratings and written feedback
- message nannies in-app
- access notifications and account settings

### Nanny features

- nanny registration and editable public profile
- upload profile image, banner image, and verification documents
- manage availability and schedules
- accept or reject booking requests
- perform check-in and check-out using the parent-issued PIN
- track held and released earnings
- manage reviews and reputation
- communicate with parents through direct messages
- monitor profile completeness and trust indicators

### Admin features

- admin dashboard with platform analytics
- nanny verification queue and approval/rejection flow
- user management, suspension, and account controls
- booking and payment oversight
- release/refund actions for held or disputed payments
- support ticket visibility and moderation
- contact message inbox
- broadcast notifications to users
- access migration and maintenance tools

### Security and platform controls

- role-based access control enforced on key pages and actions
- CSRF protection across forms
- bcrypt password hashing
- email verification and password reset flows
- rate limiting for login and messages
- dark mode persistence and responsive UI
- service worker and PWA support for the web experience

---

## Tech stack

| Layer | Stack |
| --- | --- |
| Web app | PHP 8.x, plain PHP with MySQL access via PDO |
| Database | MySQL / MariaDB |
| Native mobile app | Kotlin, Jetpack Compose |
| API layer | PHP REST API for the Android app |
| Storage | Local disk or S3-compatible object storage |
| Front-end | Vanilla JavaScript, custom CSS, responsive layout |
| Auth | PHP session auth, email verification, reset tokens |
| Packaging | PWA + Cordova-wrapped Android APK |
| Local dev | XAMPP or equivalent Apache + MySQL environment |

---

## Repository structure

```text
NannyApp/
├── README.md                        Project overview
├── compose.test.yml                 Test environment definition
├── docs/                            Product and deployment documentation
├── tests/                           Validation and security checks
├── NannyApp.Web/                    PHP web application
│   ├── README.md
│   ├── assets/
│   ├── admin/
│   ├── auth/
│   ├── config/
│   ├── includes/
│   ├── nanny/
│   ├── parent/
│   ├── pages/
│   ├── apk/
│   └── ...
├── NannyApp.Mobile/                 Native Android app + API layer
│   ├── README.md
│   └── NannyApp/
│       ├── app/
│       ├── api/
│       ├── config/
│       ├── includes/
│       ├── assets/
│       └── backend_docs/
├── NannyApp.Shared/                 Shared database + storage layer
│   ├── README.md
│   ├── database/
│   ├── config/
│   ├── storage/
│   └── releases/
└── ...
```

---

## Getting started

### Requirements

- PHP 8.3 for the automated test environment
- MariaDB 10.11 for container tests; validate migrations before using another database engine/version
- Apache / XAMPP or similar local stack
- Android Studio for native app development

### Quick start by project

Choose the project you want to work on:

- Web app: [NannyApp.Web/README.md](NannyApp.Web/README.md)
- Android app + API: [NannyApp.Mobile/README.md](NannyApp.Mobile/README.md)
- Shared database and storage setup: [NannyApp.Shared/README.md](NannyApp.Shared/README.md)

### Recommended local setup

1. Place the project folder in your local web server directory.
2. Keep `NannyApp.Shared` next to both PHP frontends; it is required.
3. For isolated regression checks, use `python tests/docker-checks.py`. For application setup, follow the migration-order guide; never import the drop-and-seed schema into an existing database.
4. Configure DB credentials and storage settings.
5. Run the web app in a browser or open the Android app in Android Studio.

---

## Database and migration notes

The shared database folder is the canonical schema source for the project.

- `schema.sql` is a destructive development reset/seed script, not a production upgrade.
- Migration files are imported in order as the system evolves.
- Do not use the base schema drop-and-seed file on production or staging.
- Keep migration ordering and release sequencing consistent.

The project includes migration guidance in:

- [NannyApp.Shared/database/MIGRATION_ORDER.md](NannyApp.Shared/database/MIGRATION_ORDER.md)
- [NannyApp.Shared/database](NannyApp.Shared/database)

---

## Demo accounts

Default seeded accounts use the password `Password123!`.

| Role | Email |
| --- | --- |
| Admin | `admin@nanny.app` |
| Parent | `parent@nanny.app` |
| Verified nanny | `amelia@nanny.app` |
| Pending nanny | `jasmine@nanny.app` |

> These demo accounts should be removed, altered, or disabled before exposing the system publicly.

---

## Deployment and production guidance

Start with the [documentation index](docs/README.md) and [free-only plan](docs/FREE_DEPLOYMENT_PLAN.md). Earlier AWS/VPS plans are inactive.

Before production:

- set real database credentials and avoid committing secrets
- move uploads to S3-compatible object storage if running multiple app instances or ephemeral containers
- keep shared storage protected from public web access
- configure email delivery for verification and password resets
- add operational procedures for backups, monitoring, and release management
- keep payments manual; provider charging/webhooks remain unimplemented and are not required for the current pilot

The project includes detailed guidance in the documentation folder and in the project-specific READMEs.

---

## Security notes

The platform has built-in protections, but production hardening still matters:

- enforce HTTPS everywhere
- limit admin access to the correct environment and browser context
- protect shared upload folders from direct public access
- rotate credentials and never commit production secrets
- verify all user-upload validation and file-serving rules remain in place
- review booking and payment code paths before external launch

---

## Roadmap and current status

The project includes web and Android booking workflows, a manual-payment ledger and web administration. Remaining authorization, booking concurrency, release and operational work is tracked in the code review and launch checklist.

Planned areas of improvement include:

- stronger automated migration/version tracking
- more production-oriented deployment automation
- deeper monitoring and release-check workflows
- tighter payment/notification configuration for live environments
- expanded QA and security validation

---

## Documentation

For deeper technical guidance, see:

- [NannyApp.Web/README.md](NannyApp.Web/README.md)
- [NannyApp.Mobile/README.md](NannyApp.Mobile/README.md)
- [NannyApp.Shared/README.md](NannyApp.Shared/README.md)
- [docs](docs)
- [tests](tests)

---

## Summary

Nanny-App is a full childcare marketplace platform built to solve the practical issues of trust, scheduling, payment safety, and moderation in a nanny booking flow. It combines web, mobile, and shared backend architecture in a way that keeps the core business logic consistent across channels while allowing independent client experiences.

If you are starting to work on the project, begin with the project-specific README for the app you want to run, then use the shared layer documentation to configure the database and storage correctly.
