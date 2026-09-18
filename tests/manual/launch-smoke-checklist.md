# Launch smoke checklist

Run this on a staging server over HTTPS before promoting a release.

## Parent app

- [ ] Register and verify a parent account.
- [ ] Search only verified nannies and open a nanny profile.
- [ ] Create a booking, handle a scheduling conflict, cancel it, and receive the expected status.
- [ ] Add and edit a child profile.
- [ ] Send and receive a message and open a support request.
- [ ] Confirm the manual-payment language never promises an automatic charge or escrow release.

## Nanny app

- [ ] Register, complete the profile, and submit verification details.
- [ ] Set availability, accept and reject bookings, and check in/out with a valid and invalid PIN.
- [ ] Confirm earnings reflect only the backend's approved booking status.

## Web portal

- [ ] Parent/nanny pages work at 320 px, 375 px, and desktop widths.
- [ ] An administrator can use the web portal for verification, support, disputes, and payout work.
- [ ] An administrator cannot sign in through the Android app or its mobile API session.

## Production safeguards

- [ ] Password-reset, verification, and support emails arrive from the configured SMTP sender.
- [ ] Uploads are private by default, available only through authorised application paths, and are backed up.
- [ ] API calls and Android release builds use HTTPS only.
- [ ] A failed login, expired session, invalid PIN, booking conflict, cancellation, and dispute produce safe errors.
- [ ] Database restore and upload-storage restore have been rehearsed.

