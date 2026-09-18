# Nanny-App

![Nanny-App logo](NannyApp.Web/assets/Icon_Logo.png)

A three-role childcare booking marketplace connecting **parents** with verified **nannies**, moderated by an **admin**.

**Module:** XISD6329 — Work Integrated Learning 3B

**Demo video:** [Watch on YouTube](https://youtu.be/nO85AqjW2E4)

---

## What this is

Nanny-App lets parents find, book and pay verified nannies through an escrow-style
payment flow (money is only released to a nanny after a PIN-verified check-in,
check-out, and parent confirmation), with an admin dashboard to verify nannies,
moderate the platform and manage disputes. It ships three ways: as a PHP website,
an installable PWA / Cordova-wrapped Android APK of that same site, and a separate
native Android app talking to a JSON REST API.

## Project layout

This repo has three top-level projects, each with its own README:

| Folder | What it is |
| --- | --- |
| **[`NannyApp.Web/`](NannyApp.Web/README.md)** | The main product: a plain PHP 8 + MySQL website (no framework, no build step — runs on XAMPP). Also installable as a PWA and packaged as a Cordova Android APK that wraps the same site in a WebView. |
| **[`NannyApp.Mobile/`](NannyApp.Mobile/README.md)** | A separate, native Android app (Kotlin, Jetpack Compose) with its own PHP REST API layer, for a true native mobile experience against the same database. |
| **[`NannyApp.Shared/`](NannyApp.Shared/README.md)** | The single source of truth both projects above depend on: the MySQL schema/migrations, the shared DB connection, and the shared upload-storage layer (local disk or S3-compatible object storage). |

```text
Nanny-App
├── NannyApp.Web/       PHP website (+ PWA/Cordova Android packaging of it)
├── NannyApp.Mobile/    Native Android app + its own PHP REST API
└── NannyApp.Shared/    Shared MySQL schema/config + shared upload storage
```

Both `NannyApp.Web` and `NannyApp.Mobile`'s API read/write the exact same MySQL
database and the exact same uploaded files via `NannyApp.Shared` — but each also
works completely standalone (own DB credentials, own local upload folder) if
`NannyApp.Shared` isn't checked out next to it. See `NannyApp.Shared/README.md`
for how that wiring works and how to switch uploads to S3-compatible storage in
production.

## Getting started

Pick the project you want to run and follow its own README:

- Want the website? → [`NannyApp.Web/README.md`](NannyApp.Web/README.md)
- Want the Android app + API? → [`NannyApp.Mobile/README.md`](NannyApp.Mobile/README.md)
- Setting up the database once for either (or both)? → [`NannyApp.Shared/README.md`](NannyApp.Shared/README.md)

## Demo accounts

All seeded accounts use the password `Password123!`:

| Role | Email |
| --- | --- |
| Admin | `admin@nanny.app` |
| Parent | `parent@nanny.app` |
| Nanny (verified) | `amelia@nanny.app` |
| Nanny (pending) | `jasmine@nanny.app` |

> Remove or disable these accounts before any public deployment.

The rest of this repo's details — features, tech stack, folder structure, setup
steps, architecture notes, known limitations, roadmap and security notes — live
in [`NannyApp.Web/README.md`](NannyApp.Web/README.md) and
[`NannyApp.Mobile/README.md`](NannyApp.Mobile/README.md), which each describe
their own project in depth.

---
