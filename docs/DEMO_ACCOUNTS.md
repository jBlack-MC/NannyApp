# Demo bots, demo accounts and admins

## Demo bots (make an empty site look lived-in)

Bots are fake but obviously labelled, so they are easy to spot and remove:

- names end in **(Bot)**, for example `Amelia (Bot)`, and each nanny bio says "Demo bot profile, not a real person";
- e-mails end in **`@bots.nanny-app.invalid`**, a reserved domain, so no e-mail can reach anyone;
- passwords are random and never shown, so nobody can sign in as a bot.

There are 6 verified nannies with photos, 4 parents, and 8 completed bookings with reviews. They fill the home page's nanny cards and testimonials, and the app's Welcome screen.

**Add or remove them:** GitHub → **Actions → Demo bots → Run workflow**, then choose `add` or `remove`. Running `add` again replaces the bots instead of duplicating them. Running `remove` deletes every bot together with its profiles, bookings, payments and reviews. Real accounts are never touched.

The workflow uses the existing `NANNYAPP_MIGRATION_TOKEN` secret. The same token must be set in Vercel while you run it (as for the Supabase migration).

To remove the bots by hand in the Supabase SQL editor:

```sql
DELETE FROM users WHERE email LIKE '%@bots.nanny-app.invalid';
```

## Named demo accounts

The Supabase prototype database currently contains these working seeded
accounts, all verified and active. For the coursework demonstration they use
the shared password `Password123!`:

- `parent@nanny.app` — parent web/mobile account
- `james@nanny.app` — parent web/mobile account
- `amelia@nanny.app` — nanny web/mobile account
- `admin@nanny.app` — web-only administrator account

Change or remove these public demonstration credentials before any real-user
launch. The production repair workflow should use a private
`NANNYAPP_DEMO_PASSWORD` secret instead.

To repair them on a deployed site, add a repository secret
**`NANNYAPP_DEMO_PASSWORD`** with a password of your own (12 or more
characters), then run **Demo bots → add**. Existing named demo accounts are
now repaired as well as missing accounts.

To check which demo accounts exist, run this in the Supabase SQL editor:

```sql
SELECT email, role, status, email_verified, created_at FROM users
WHERE email LIKE '%@nanny.app' OR email LIKE '%@bots.nanny-app.invalid' ORDER BY email;
```

## Adding an admin

No admin is created automatically, and admins can only sign in on the website, not in the app.

1. Register normally on the website with the person's real e-mail address and confirm the e-mail.
2. In Supabase → **SQL editor**, promote the account:

   ```sql
   UPDATE users SET role = 'admin', email_verified = 1, status = 'active' WHERE email = 'owner@example.com';
   ```

3. Sign out and back in on the website. The admin profile is created on the first visit to the admin profile page.

To remove admin access, set `role` back to `'parent'`.
