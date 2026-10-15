<?php
/** GET /api/subscriptions/status.php — the signed-in account's Nanny-App Plus state. */
require_once __DIR__ . '/../_bootstrap.php';
require_once __DIR__ . '/../../../../NannyApp.Shared/config/subscriptions.php';

$me = require_api_auth();
json_response(true, plus_status(db(), (int) $me['id']) + ['accountReference' => plus_account_reference((int) $me['id'])]);
