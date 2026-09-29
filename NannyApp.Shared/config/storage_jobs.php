<?php
/** Authorize and remove the reference atomically with a durable deletion request. */
function delete_portfolio_document(PDO $pdo, int $owner, int $id): bool
{
    $pdo->beginTransaction();
    try {
        $stmt=$pdo->prepare('SELECT file_path FROM nanny_portfolio WHERE id=? AND nanny_id=? FOR UPDATE');$stmt->execute([$id,$owner]);$path=$stmt->fetchColumn();
        if ($path===false) { $pdo->rollBack(); return false; }
        $pdo->prepare('INSERT INTO storage_deletions (file_path) VALUES (?)')->execute([$path]);
        $pdo->prepare('DELETE FROM nanny_portfolio WHERE id=? AND nanny_id=?')->execute([$id,$owner]);
        $pdo->commit();return true;
    } catch (Throwable $e) { if ($pdo->inTransaction()) $pdo->rollBack();throw $e; }
}

function process_storage_deletion(PDO $pdo): bool
{
    $pdo->beginTransaction();
    try {
        $job=$pdo->query('SELECT * FROM storage_deletions WHERE completed_at IS NULL AND available_at<=NOW() ORDER BY id LIMIT 1 FOR UPDATE SKIP LOCKED')->fetch(PDO::FETCH_ASSOC);
        if (!$job) { $pdo->rollBack();return false; }
        if (storage_delete($job['file_path'])) {
            $pdo->prepare('UPDATE storage_deletions SET completed_at=NOW() WHERE id=?')->execute([$job['id']]);
        } else {
            $pdo->prepare('UPDATE storage_deletions SET attempts=attempts+1,available_at=DATE_ADD(NOW(),INTERVAL 5 MINUTE) WHERE id=?')->execute([$job['id']]);
        }
        $pdo->commit();return true;
    } catch (Throwable $e) { if ($pdo->inTransaction()) $pdo->rollBack();throw $e; }
}
