<?php
/**
 * POST /api/reports/create.php { reportedUserId, bookingId?, reason, details }
 * Reports another parent or nanny to the safety team. GET returns the reasons to offer.
 */
require_once __DIR__ . '/../_bootstrap.php';
require_once __DIR__ . '/../../../../NannyApp.Shared/config/reports.php';

$me = require_api_auth();
if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    json_response(true, array_map(fn($code, $label) => ['code' => $code, 'label' => $label], array_keys(report_reasons()), report_reasons()));
}
if ($_SERVER['REQUEST_METHOD'] !== 'POST') json_error('POST required.', 405);
if (is_rate_limited('report:' . $me['id'], 5, 3600)) json_error('You have sent several reports recently. For an emergency call 10111.', 429);

$b = json_body();
try {
    $id = create_user_report(
        db(), $me, (int) ($b['reportedUserId'] ?? 0),
        isset($b['bookingId']) && $b['bookingId'] !== null ? (int) $b['bookingId'] : null,
        (string) ($b['reason'] ?? ''), (string) ($b['details'] ?? '')
    );
} catch (InvalidArgumentException $e) {
    json_error($e->getMessage(), 400);
} catch (Throwable $e) {
    log_exception($e, 'report_create_failed');
    json_error('Could not send your report. Please try again.', 503);
}
json_response(true, ['id' => $id], 'Thank you. Our safety team will review your report.');
