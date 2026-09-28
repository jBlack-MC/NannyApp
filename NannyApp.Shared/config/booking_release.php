<?php
/** One atomic automatic completion; caller must not already own a transaction. */
function release_stale_booking(PDO $pdo, int $id, int $graceHours): bool
{
    if ($graceHours < 1) throw new InvalidArgumentException('Grace period must be positive.');
    if ($pdo->inTransaction()) throw new LogicException('Release requires its own transaction.');
    $pdo->beginTransaction();
    try {
        // UPDATE takes a row lock and rechecks eligibility, even after an earlier candidate scan.
        $stmt = $pdo->prepare("UPDATE bookings SET status='completed', parent_confirmed_at=NOW()
            WHERE id=? AND status='in_progress' AND checked_out_at IS NOT NULL
            AND parent_confirmed_at IS NULL
            AND checked_out_at < DATE_SUB(NOW(), INTERVAL ? HOUR)");
        $stmt->execute([$id, $graceHours]);
        if ($stmt->rowCount() !== 1) {
            $pdo->rollBack();
            return false;
        }
        $pdo->prepare("UPDATE payments SET payout_status='released', released_at=NOW()
            WHERE booking_id=? AND status='paid' AND payout_status='held'")->execute([$id]);
        $pdo->commit();
        return true;
    } catch (Throwable $e) {
        if ($pdo->inTransaction()) $pdo->rollBack();
        throw $e;
    }
}
