<?php
/**
 * Database-backed PHP sessions for hosts without a shared disk.
 *
 * Vercel runs each request on whichever short-lived instance is free, and each instance has its
 * own /tmp, so file sessions vanish between requests: the login form's CSRF token is gone by
 * the time the form is posted ("Invalid or expired form token"). Storing sessions in the
 * application database keeps them across instances.
 *
 * Used automatically on Vercel and with PostgreSQL (the Supabase production setup, whether as
 * serverless PHP or a container that Vercel may run several copies of), or anywhere with
 * NANNYAPP_SESSION_DRIVER=database.
 * NANNYAPP_SESSION_DRIVER=files forces PHP's normal file sessions.
 */

declare(strict_types=1);

require_once __DIR__ . '/database.php';

function nannyapp_database_sessions_wanted(): bool
{
    $driver = strtolower((string) (getenv('NANNYAPP_SESSION_DRIVER') ?: ''));
    if ($driver !== '') return $driver === 'database';
    return (string) (getenv('VERCEL') ?: '') !== '' || (defined('DB_DRIVER') && DB_DRIVER === 'pgsql');
}

final class NannyAppDatabaseSessionHandler implements SessionHandlerInterface, SessionUpdateTimestampHandlerInterface
{
    private static bool $tableReady = false;

    public function __construct(private PDO $pdo, private int $lifetime)
    {
    }

    public function open(string $path, string $name): bool
    {
        if (!self::$tableReady) {
            // Created on first use so existing deployments need no migration run.
            $this->pdo->exec(
                'CREATE TABLE IF NOT EXISTS web_sessions (
                    id VARCHAR(128) NOT NULL PRIMARY KEY,
                    data TEXT NOT NULL,
                    updated_at BIGINT NOT NULL
                )'
            );
            self::$tableReady = true;
        }
        return true;
    }

    public function close(): bool
    {
        return true;
    }

    public function read(string $id): string|false
    {
        $stmt = $this->pdo->prepare('SELECT data FROM web_sessions WHERE id = ? AND updated_at >= ?');
        $stmt->execute([$id, time() - $this->lifetime]);
        $data = $stmt->fetchColumn();
        // Stored as base64 because serialized sessions may contain bytes a TEXT column rejects.
        return $data === false ? '' : (string) base64_decode((string) $data, true);
    }

    public function write(string $id, string $data): bool
    {
        $sql = DB_DRIVER === 'pgsql'
            ? 'INSERT INTO web_sessions (id, data, updated_at) VALUES (?, ?, ?)
               ON CONFLICT (id) DO UPDATE SET data = EXCLUDED.data, updated_at = EXCLUDED.updated_at'
            : 'INSERT INTO web_sessions (id, data, updated_at) VALUES (?, ?, ?)
               ON DUPLICATE KEY UPDATE data = VALUES(data), updated_at = VALUES(updated_at)';
        return $this->pdo->prepare($sql)->execute([$id, base64_encode($data), time()]);
    }

    public function destroy(string $id): bool
    {
        return $this->pdo->prepare('DELETE FROM web_sessions WHERE id = ?')->execute([$id]);
    }

    public function gc(int $max_lifetime): int|false
    {
        $stmt = $this->pdo->prepare('DELETE FROM web_sessions WHERE updated_at < ?');
        $stmt->execute([time() - $max_lifetime]);
        return $stmt->rowCount();
    }

    /** With session.use_strict_mode, only IDs this server issued are accepted. */
    public function validateId(string $id): bool
    {
        $stmt = $this->pdo->prepare('SELECT 1 FROM web_sessions WHERE id = ? AND updated_at >= ?');
        $stmt->execute([$id, time() - $this->lifetime]);
        return $stmt->fetchColumn() !== false;
    }

    public function updateTimestamp(string $id, string $data): bool
    {
        return $this->pdo->prepare('UPDATE web_sessions SET updated_at = ? WHERE id = ?')->execute([time(), $id]);
    }
}

/** Call before session_start(). Does nothing unless database sessions are wanted. */
function nannyapp_configure_sessions(): void
{
    if (!nannyapp_database_sessions_wanted()) return;
    session_set_save_handler(new NannyAppDatabaseSessionHandler(db(), (int) ini_get('session.gc_maxlifetime')), true);
}
