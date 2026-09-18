# API automation backlog

Automate these cases against a dedicated staging database:

- unauthenticated and expired-token requests return `401`;
- an admin account is rejected by the mobile session/API path;
- a parent cannot read or mutate another parent's children, bookings, messages, or support tickets;
- a nanny can act only on assigned bookings;
- booking status transitions and PIN attempts are server-authoritative;
- upload MIME type, size, ownership, and authorisation are validated;
- rate limiting applies to login, reset, messaging, and PIN endpoints.

