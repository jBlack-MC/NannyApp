# Demo accounts and legacy bots

## Legacy bots

Older prototypes may contain labelled bot accounts:

- names end in **(Bot)**, for example `Amelia (Bot)`, and each nanny bio says "Demo bot profile, not a real person";
- e-mails end in **`@bots.nanny-app.invalid`**, a reserved domain, so no e-mail can reach anyone;
- passwords are random and never shown, so nobody can sign in as a bot.

Do not assume those accounts or their sample bookings still exist. The current
`Demo accounts` workflow only offers `add` for the five named sign-ins below;
it does not create or delete legacy bots. Automatic bot deletion is disabled.

Preview legacy users before considering manual cleanup:

```sql
SELECT id, email, role, status FROM users
WHERE email LIKE '%@bots.nanny-app.invalid'
   OR email LIKE 'demo.parent%@nanny.app'
   OR email LIKE 'demo.nanny%@nanny.app';
```

Review dependent profiles, bookings, reviews, payments and messages for those
IDs and take a backup before any manual deletion. No broad deletion query is
safe to run without that review.

## Named demo accounts

`Password123!` does not sign in to the hosted database. Use the private password
issued for each account. Passwords are never recovered from their database hashes.
The Supabase dashboard login, Supabase API keys and database password are not
application-user passwords. Web authentication and the mobile PHP API share
`public.users` records; they do not use Supabase Auth accounts.

The Supabase prototype has used these seeded accounts. Check the actual database
before relying on them. Newly created accounts use the private
`NANNYAPP_DEMO_PASSWORD` secret; existing credentials may differ. Never use the
public local fixture password `Password123!` on a deployed site:

- `parent@nanny.app` — parent web/mobile account
- `james@nanny.app` — parent web/mobile account
- `amelia@nanny.app` — nanny web/mobile account
- `grace@nanny.app` — nanny web/mobile account
- `admin@nanny.app` — web-only administrator account

Cleaning up these five named accounts is optional while the site is a closed
prototype. Remove or disable them before admitting real users. Back up the
database and review linked demo profiles and bookings before deletion. Rotate
the demo password if the old shared password was used on a deployed site.

To create missing accounts on a deployed prototype, add a repository secret
**`NANNYAPP_DEMO_PASSWORD`** with a password of your own (12 or more
characters), then run **Actions → Demo accounts → Run workflow → add** from
`main` under the protected `production-db` environment. It uses the temporary
`NANNYAPP_MIGRATION_TOKEN` configured in both GitHub and Vercel, and leaves
existing named accounts unchanged. The separate `Supabase database sync`
workflow updates schema only by default. Select `seed_demo` to create missing
accounts. To repair forgotten demo passwords, select both `seed_demo` and
`reset_demo_passwords`; direct database access is required for rotation.
This rotates all five passwords and revokes their sessions. Do not run seeding after cleanup unless those
accounts are intended to return. Check the actual
database before assuming any account exists.

To check which demo accounts exist, run this in the Supabase SQL editor:

```sql
SELECT email, role, status, email_verified, created_at FROM users
WHERE email LIKE '%@nanny.app' OR email LIKE '%@bots.nanny-app.invalid' ORDER BY email;
```

## Adding an admin

The optional demo seed can create `admin@nanny.app`. For a real administrator,
use a separate account; admins can sign in on the website, not in the app.

1. Register normally on the website with the person's real e-mail address and confirm the e-mail.
2. In Supabase → **SQL editor**, promote the account:

   ```sql
   UPDATE users SET role = 'admin', email_verified = 1, status = 'active' WHERE email = 'owner@example.com';
   ```

3. Sign out and back in on the website. The admin profile is created on the first visit to the admin profile page.

To remove admin access, set `role` back to `'parent'`.
