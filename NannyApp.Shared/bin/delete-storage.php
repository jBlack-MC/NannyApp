<?php
if (PHP_SAPI!=='cli') { http_response_code(404);exit; }
require __DIR__ . '/../config/database.php';
require __DIR__ . '/../config/storage_jobs.php';
try {
    for ($i=0;$i<100 && process_storage_deletion(db());$i++) {}
    echo "Processed $i storage jobs.\n";
} catch (Throwable $e) { fwrite(STDERR,"Storage worker failed; retry pending jobs.\n");exit(1); }
