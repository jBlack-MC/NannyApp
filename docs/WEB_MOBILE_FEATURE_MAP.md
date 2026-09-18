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
