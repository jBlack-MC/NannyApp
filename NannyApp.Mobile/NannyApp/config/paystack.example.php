<?php
/**
 * Copy this file to config/paystack.php (which is .gitignored) and fill in
 * your real Paystack secret key. NEVER commit the real key, and NEVER send
 * it to the Android app — only the public authorization_url is returned to
 * the client (see api/payments/initialize.php).
 */

define('PAYSTACK_SECRET_KEY', 'sk_test_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx');
define('PAYSTACK_PUBLIC_KEY', 'pk_test_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx');
