# Web and mobile feature map

The native app mirrors the public parent/nanny portal, while operational administration stays on the web.

| Web portal area | Android experience | Audience |
| --- | --- | --- |
| `parent/dashboard.php`, `parent/nannies.php`, `parent/book.php` | Parent dashboard, search, nanny details, booking wizard | Parent |
| `parent/bookings.php`, `parent/payments.php`, `parent/review.php` | Bookings, payment status, review | Parent |
| `parent/children.php`, `parent/saved.php` | Children and saved nannies | Parent |
| `nanny/dashboard.php`, `nanny/bookings.php`, `nanny/availability.php` | Nanny dashboard, bookings/check-in, availability | Nanny |
| `nanny/earnings.php`, `nanny/reviews.php`, `nanny/profile.php` | Earnings, reviews, profile/portfolio | Nanny |
| Shared messages, notifications, profile, support | Shared messages, notifications, profile, support | Parent and nanny |
| `admin/` | No Android route | Web administrator only |

When a public web feature is added, add the matching mobile route only if it is needed by a parent or nanny on a phone. Keep staff workflows, payouts, verification, disputes, and user moderation under `NannyApp.Web/admin/`.

## Device and administration additions (2026-10-10)

About, safety, pricing, FAQ, community, resources, contact, terms and privacy are
reachable from Welcome and both account profiles. Native informational content
still reads `page_content`; unpublished entries show their existing placeholder.
Each screen also links to the complete corresponding website page, including
contact forms. These links open the browser rather than creating a duplicate native form.

`admin/operations.php` adds a browser-only action centre for safety tickets, disputes, verification, receipts, contacts and worker queue counts. Android navigation continues to exclude admin and maintenance screens. Both role shells already include booking, messaging, notifications, profile and support routes; the parent shell also includes children, saved nannies and payments, and the nanny shell includes availability, earnings and reviews.

The native APK requires Android 8 or newer. iPhone/iPad and desktop users use the responsive web/PWA; no native iOS binary exists. Physical-device and large-text acceptance remain required. APK deployment uses `deploy/aws/releases/NannyApp-latest.apk`; local web downloads use `NannyApp.Shared/releases/NannyApp-latest.apk`.
