<?php
/** Online payment verification is disabled during the manual-payment launch phase. */
require_once __DIR__ . '/../_bootstrap.php';
require_api_auth();

json_error('Online payment verification is not available yet.', 410);
