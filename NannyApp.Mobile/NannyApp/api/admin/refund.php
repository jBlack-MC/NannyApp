<?php
/** POST /api/admin/refund.php { payment_id } — marks a payment refunded.
 * NOTE: this flips the local DB status; actually calling Paystack's refund API
 * requires additional server-side integration (see backend_docs/API.md). */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();
require_api_role($me, 'admin');

$paymentId = (int) (json_body()['payment_id'] ?? 0);
db()->prepare("UPDATE payments SET status = 'refunded', payout_status = 'refunded' WHERE id = :id")->execute(['id' => $paymentId]);

json_response(true, null);
