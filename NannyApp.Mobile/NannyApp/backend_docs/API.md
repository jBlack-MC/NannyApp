# NannyApp REST API

Base URL (configurable): `https://your-domain.example.com/nannyapp/api/`
Debug/emulator default: `http://10.0.2.2/nannyapp/api/` (see `app/build.gradle.kts`)

## Conventions

- Every endpoint returns: `{ "success": bool, "message": string|null, "data": <payload>|null }`
- Auth: send `Authorization: Bearer <token>` on every request except `auth/login.php`, `auth/register.php`, `auth/forgot.php`, `auth/reset.php`, `auth/verify-email.php`, `auth/resend-verification.php`, and `content/page.php`.
- A `nannyId` / `nanny_id` of `0` (or the parameter omitted) on nanny-scoped endpoints (`nannies/detail.php`, `nannies/availability.php`, `nannies/portfolio.php`) means **"the logged-in nanny"** — the server resolves it from the bearer token rather than trusting a client-supplied id.
- HTTP status codes are meaningful: `401` = expired/invalid session, `403` = wrong role or suspended, `404` = not found, `409` = conflict (e.g. double-booking, wrong booking status for this action), `429` = rate limited.

## Auth — `/api/auth/`
| Endpoint | Method | Body | Notes |
|---|---|---|---|
| `login.php` | POST | `email, password, rememberMe` | Returns `{token, user}`. Blocks unverified/suspended accounts with a friendly message. IP rate-limited (5 / 15 min). |
| `register.php` | POST | role + profile fields (see `RegisterRequestDto`) | Creates `users` + `parent_profiles`/`nanny_profiles` row. Returns `{token, user}`. |
| `logout.php` | POST | — | Revokes the current bearer token. |
| `forgot.php` | POST | `email` | Always returns success (doesn't leak whether the email exists). |
| `reset.php` | POST | `token, newPassword` | Consumes a `password_resets` row; revokes all existing tokens for that user. |
| `verify-email.php` | POST | `token` | Matches `users.verification_token`. |
| `resend-verification.php` | POST | `email` | Issues a fresh token if the account is unverified. |

## User — `/api/user/`
| Endpoint | Method | Notes |
|---|---|---|
| `profile.php` | GET / PUT | Get or update the logged-in user's core fields. |
| `change_password.php` | POST | `current_password, new_password`. |
| `upload_avatar.php` | POST (multipart, field `avatar`) | JPG/PNG/WEBP/GIF, 2MB max (reuses `save_uploaded_image()`). |

## Nannies — `/api/nannies/`
| Endpoint | Method | Notes |
|---|---|---|
| `search.php` | GET | Query params: `q, location, min_rate, max_rate, min_experience, min_rating, verified_only, slot, sort`. |
| `detail.php` | GET | `?id=`. Increments `profile_views` unless viewing your own profile. |
| `reviews.php` | GET | `?id=`. |
| `profile.php` | PUT | Nanny edits their own profile. |
| `availability.php` | GET / PUT | Weekly schedule + morning/afternoon/evening slots. |
| `portfolio.php` | GET / POST (multipart) / DELETE | Upload/list/delete ID, certificates, references, photos. |
| `earnings.php` | GET | Escrow-aware: total / held / released, recent payments, 30-day trend. Also triggers `auto_release_stale_payments()`. |
| `saved.php` / `save.php` | GET / POST | Parent's saved-nanny list and toggle. |

## Bookings — `/api/bookings/`
Implements the exact status machine from `nanny/bookings.php` / `parent/book.php`:
`pending → confirmed (accept) → in_progress (check-in PIN) → completed (parent confirms)`, with `rejected`/`cancelled`/`disputed` as side states.

| Endpoint | Method | Role | Notes |
|---|---|---|---|
| `list.php` | GET | parent/nanny | Own bookings. |
| `detail.php` | GET | owner or admin | `?id=`. |
| `create.php` | POST | parent | Creates status=`pending`; checks for a scheduling conflict via `nanny_has_booking_conflict()`. |
| `accept.php` | POST | nanny | `pending → confirmed`; generates the 6-digit `check_in_code` (only ever returned to the parent). |
| `reject.php` | POST | nanny | `pending → rejected`; refunds if already paid. |
| `cancel.php` | POST | parent | Allowed from `pending`/`confirmed`; refunds if already paid. |
| `reschedule.php` | POST | parent | `dateTime`; re-checks conflicts excluding itself. |
| `check_in.php` | POST | nanny | `checkInCode`; 5-attempt lockout; `confirmed → in_progress`. |
| `check_out.php` | POST | nanny | Sets `checked_out_at`; status stays `in_progress` pending parent confirmation. |
| `confirm.php` | POST | parent | `in_progress → completed`; releases escrow (`payments.payout_status = released`). |
| `dispute.php` | POST | parent | `reason`; freezes escrow, opens a `support_tickets` row automatically. |

## Payments — `/api/payments/`
Paystack secret key lives **only** on the server (`config/paystack.php`, gitignored) — never sent to the app.

| Endpoint | Method | Notes |
|---|---|---|
| `initialize.php` | POST | `booking_id`. Creates a `pending` `payments` row, calls Paystack `/transaction/initialize`, returns `{authorizationUrl, reference}`. |
| `verify.php` | GET | `?reference=`. Calls Paystack `/transaction/verify`; on success sets `status=paid, payout_status=held`. |
| `list.php` | GET | Payments visible to the logged-in parent/nanny. |

## Reviews — `/api/reviews/`
`create.php` (POST, parent, only for `completed` bookings, one review per booking) and `list.php` (GET `?nanny_id=`). Recomputes `nanny_profiles.average_rating` via `recompute_rating()`.

## Messages — `/api/messages/`
`conversations.php`, `thread.php?with=`, `poll.php?with=&since_id=` (polled every ~4s by the app), `send.php`, `mark_read.php`.

## Notifications — `/api/notifications/`
`list.php`, `mark_read.php`, `mark_all_read.php`.

## Support — `/api/support/`
`create.php`, `my_tickets.php`.

## Children — `/api/children/`
`list.php`, `create.php`, `update.php`, `delete.php` — parent-only.

## Admin — `/api/admin/`
All require `role = admin`.

| Endpoint | Method | Notes |
|---|---|---|
| `dashboard.php` | GET | Full KPI payload (users, bookings, revenue, top nannies/locations, 30-day trends). |
| `users.php` | GET | `?role=&q=`. |
| `users_suspend.php` / `users_activate.php` | POST | `userId`. Suspending also revokes all of that user's API tokens. |
| `verifications.php` | GET | Nannies with `verification_status = pending`. |
| `verify_nanny.php` / `reject_nanny.php` | POST | `nannyId, notes`. |
| `verify_document.php` | POST | `id, approve, notes` — one `nanny_portfolio` row. |
| `bookings.php` | GET | `?status=&q=`, all bookings. |
| `booking_status.php` | POST | Manual override, e.g. resolving a dispute. |
| `payments.php` | GET | `?status=`, all payments. |
| `refund.php` | POST | `payment_id` — flips local status; wire up Paystack's refund API for a real charge-back. |
| `support.php` / `support_update.php` | GET / POST | All tickets; update status + notes. |

## Content — `/api/content/`
`page.php?key=faq` — public, reads the `page_content` CMS table (About/Services/Safety/FAQ/Pricing/Community/Resources/Contact/Terms/Privacy). Falls back to a placeholder for keys the admin hasn't published yet.

## Known limitations / follow-ups for production

1. **Rate limiting** (`is_rate_limited()`/`increment_rate_limit()`, reused from `includes/functions.php`) is session-based. Since the Android client doesn't carry a session cookie, this degrades to best-effort per-request rather than a true persistent limiter — swap in a DB- or Redis-backed limiter keyed by IP before shipping.
2. **Email sending** (verification, password reset) is stubbed with `// TODO` markers pointing at `includes/email.php` — wire up real SMTP/provider credentials.
3. **File uploads** go through the existing `save_uploaded_image()` helper, which only accepts image formats (JPG/PNG/WEBP/GIF). For ID documents you may want to also accept PDF.
4. **Paystack refunds** (`admin/refund.php`) only update the local `payments` row; call Paystack's `/refund` endpoint for a real charge-back before going live.
5. This layer was written and reasoned through carefully but has **not been executed against a live MySQL instance** in the environment that produced it (no PHP/MySQL runtime available there) — run it against a real XAMPP/MySQL setup and fix any typos before production use.
