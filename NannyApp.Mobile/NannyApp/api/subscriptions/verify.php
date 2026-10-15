<?php
/**
 * POST /api/subscriptions/verify.php { productId, purchaseToken }
 * Called by the app after a Google Play purchase and on start-up to refresh renewals.
 * The token is checked with Google before anything is unlocked.
 */
require_once __DIR__ . '/../_bootstrap.php';
require_once __DIR__ . '/../../../../NannyApp.Shared/config/subscriptions.php';

$me = require_api_auth();
if ($_SERVER['REQUEST_METHOD'] !== 'POST') json_error('POST required.', 405);
if (!plus_configured()) json_error('Subscriptions are not set up on the server yet.', 503);
if (is_rate_limited('plus-verify:' . $me['id'], 20, 3600)) json_error('Too many attempts. Please try again later.', 429);

$b = json_body();
try {
    json_response(true, plus_verify_purchase(db(), (int) $me['id'], (string) ($b['productId'] ?? ''), (string) ($b['purchaseToken'] ?? '')));
} catch (InvalidArgumentException $e) {
    json_error($e->getMessage(), 400);
} catch (Throwable $e) {
    log_exception($e, 'plus_verify_failed');
    json_error('Could not confirm the purchase with Google Play. Please try again.', 503);
}
