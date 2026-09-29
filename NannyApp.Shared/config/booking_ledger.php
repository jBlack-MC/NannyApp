<?php
/** Caller holds the booking lock. All monetary writes use this helper in the caller's transaction. */
function update_booking_ledger(PDO $pdo, int $id, string $operation, bool $completed=false): void
{
    if (!$pdo->inTransaction()) throw new LogicException('Ledger write requires a transaction.');
    $stmt=$pdo->prepare('SELECT * FROM payments WHERE booking_id=? FOR UPDATE');$stmt->execute([$id]);$pay=$stmt->fetch(PDO::FETCH_ASSOC);
    if (!$pay) throw new RuntimeException('Booking ledger missing.');
    if ($operation==='release') {
        $pdo->prepare("UPDATE payments SET payout_status='released',released_at=NOW() WHERE booking_id=? AND status='paid' AND payout_status='held'")->execute([$id]);
    } elseif ($operation==='reverse') {
        if ($pay['payout_status']==='released') throw new RuntimeException('Released ledger requires manual reconciliation.');
        $pdo->prepare("UPDATE payments SET payout_status=IF(status='paid','refunded',payout_status),status=CASE WHEN status='paid' THEN 'refunded' WHEN status='pending' THEN 'failed' ELSE status END WHERE booking_id=?")->execute([$id]);
    } elseif ($operation==='receive') {
        if ($pay['method']!=='manual' || $pay['status']!=='pending') throw new RuntimeException('No pending manual payment.');
        $state=$completed?'released':'held';
        $pdo->prepare("UPDATE payments SET status='paid',payout_status=?,released_at=IF(?='released',NOW(),NULL) WHERE booking_id=?")->execute([$state,$state,$id]);
    } else throw new LogicException('Unknown ledger operation.');
}
