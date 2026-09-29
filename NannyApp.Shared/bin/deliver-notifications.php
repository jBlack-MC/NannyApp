<?php
if (PHP_SAPI !== 'cli') { http_response_code(404); exit; }
require __DIR__ . '/../config/database.php';
require __DIR__ . '/../config/notification_outbox.php';
try {
    for ($i=0; $i<100 && deliver_notification(db()); $i++) {}
    echo "Delivered $i notifications.\n";
} catch (Throwable $e) {
    fwrite(STDERR, "Notification delivery failed; pending event retained for retry.\n");
    exit(1);
}
