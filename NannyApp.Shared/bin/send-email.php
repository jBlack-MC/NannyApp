<?php
declare(strict_types=1);
if (PHP_SAPI !== 'cli') { http_response_code(404); exit; }
require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../config/email_outbox.php';
if ((getenv('NANNYAPP_RESEND_API_KEY') ?: '') === '' || !extension_loaded('curl')) {
    fwrite(STDERR, "mail_worker_configuration_missing\n");
    exit(1);
}
$limit = min(20, max(1, (int) ($argv[1] ?? 5)));
try {
    for ($i = 0; $i < $limit; $i++) {
        $job = claim_account_email(db());
        if (!$job) break;
        $status = deliver_account_email(db(), $job);
        echo json_encode(['event'=>'mail_job','id'=>$job['id'],'status'=>$status]) . "\n";
    }
} catch (Throwable) {
    // A provider acceptance followed by DB failure leaves the leased job retryable.
    fwrite(STDERR, "mail_worker_failed\n");
    exit(1);
}
