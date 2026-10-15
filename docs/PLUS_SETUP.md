# Switching on Nanny-App Plus

Nanny-App Plus is one monthly subscription sold through Google Play Billing. The app and server code are in place; these steps connect them to your Google Play account. Until they are done, the app's Plus screen says "not switched on yet" and nothing can be bought.

## What Plus unlocks

| For | Unlocks |
| --- | --- |
| Parents | Nanny qualifications and specialisations; the list of a nanny's documents an admin has verified; unlimited saved nannies (free accounts: 3); booking requests marked Priority and shown first to the nanny |
| Nannies | Which families viewed their profile in the last 30 days (free nannies see the count); featured at the top of search results |
| Everyone, free | Verified badge, ratings, reviews and the Report button. Never put safety information behind the subscription. |

There is no service fee in the booking model today, so the "reduced service fees" line on the website's pricing page has nothing to apply to. Update that page to match this list.

## 1. Publish the app on Google Play

Play Billing only works for an app installed from Google Play. An APK downloaded from the website cannot buy subscriptions.

1. Create the app in [Google Play Console](https://play.google.com/console) with the package name `com.nannyapp`.
2. Upload a signed release build to the **Internal testing** track (closed or production later).
3. Add your own Google account as a tester and install the app from the testing link.

## 2. Create the subscription

1. Play Console → your app → **Monetize → Products → Subscriptions → Create subscription**.
2. Product ID: `nannyapp_plus`. To use another ID, build the app with `-PNANNYAPP_PLAY_SUBSCRIPTION_ID=your_id` and set `NANNYAPP_PLAY_SUBSCRIPTION_ID` on the server to the same value.
3. Add one **base plan**: auto-renewing, monthly, with your price in rand. The app shows the price Google returns; it is not written in code.
4. Activate the base plan.

## 3. Let the server check purchases

The server confirms every purchase with Google before unlocking anything.

1. In [Google Cloud Console](https://console.cloud.google.com), in the project linked to Play Console, enable the **Google Play Android Developer API**.
2. Create a **service account** and a **JSON key** for it. Keep the file secret; never commit it.
3. Play Console → **Users and permissions → Invite new users**: add the service account's email with **View financial data** and **Manage orders and subscriptions** for the app.
4. Copy the key to the server, for example `deploy/aws/play-service-account.json`, readable only by the app, and mount it into the `app` container.
5. Set in `deploy/aws/.env`:

   ```sh
   NANNYAPP_PLAY_PACKAGE=com.nannyapp
   NANNYAPP_PLAY_SUBSCRIPTION_ID=nannyapp_plus
   NANNYAPP_PLAY_SERVICE_ACCOUNT_FILE=/srv/nannyapp/private/play-service-account.json
   ```

6. Apply the database migration (`migrate_v10_plus_translation.sql`) with `dc run --rm migrate`, then restart the app.

New service accounts can take up to 24 hours before Google accepts them.

## 4. Test without being charged

Play Console → **Settings → License testing**: add tester accounts. Their purchases use test cards and renew every few minutes, so you can watch renewal and expiry. In the app, open Profile → Nanny-App Plus.

## How renewals stay correct

The app re-sends its Play purchases to `api/subscriptions/verify.php` each time the Plus screen opens, and the server stores the new expiry date from Google. Plus stays unlocked until that date, including after cancelling, as Google requires. For instant updates, a later improvement is Google's Real-time developer notifications.

## Translation

"Translate" on profiles, reviews and chat uses the free MyMemory API through the server. Without settings it allows about 5,000 characters a day for the whole server. Set `NANNYAPP_TRANSLATE_EMAIL` to a contact address you are happy to share with MyMemory to raise that to 50,000. Translations are cached, so each text is only translated once per language.
