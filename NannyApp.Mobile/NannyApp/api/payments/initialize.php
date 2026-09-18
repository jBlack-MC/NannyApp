<?php
/** Online checkout is deliberately disabled during the manual-payment launch phase. */
require_once __DIR__ . '/../_bootstrap.php';
require_api_auth();

json_error('Online payments are not available yet. Payment is arranged manually after booking confirmation.', 410);
