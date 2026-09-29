<?php
function queue_notification(PDO $pdo, int $userId, string $title, string $message, ?string $url = null): void
{
    if (!$pdo->inTransaction()) throw new LogicException('Notification events require a transaction.');
    $pdo->prepare('INSERT INTO notification_outbox (user_id,title,message,url) VALUES (?,?,?,?)')->execute([$userId,$title,$message,$url]);
}

/** Delivery is a local DB insert. Insert and acknowledgement commit together, so retries cannot duplicate it. */
function deliver_notification(PDO $pdo): bool
{
    $pdo->beginTransaction();
    try {
        $row = $pdo->query('SELECT * FROM notification_outbox WHERE delivered_at IS NULL ORDER BY id LIMIT 1 FOR UPDATE SKIP LOCKED')->fetch(PDO::FETCH_ASSOC);
        if (!$row) { $pdo->rollBack(); return false; }
        $pdo->prepare('INSERT INTO notifications (user_id,title,message,url) VALUES (?,?,?,?)')->execute([$row['user_id'],$row['title'],$row['message'],$row['url']]);
        $pdo->prepare('UPDATE notification_outbox SET delivered_at=NOW() WHERE id=?')->execute([$row['id']]);
        $pdo->commit();
        return true;
    } catch (Throwable $e) {
        if ($pdo->inTransaction()) $pdo->rollBack();
        throw $e;
    }
}
