<?php
/** MariaDB schema upgrades. DDL auto-commits; each statement must be safe to retry. */
function migrate_application(PDO $pdo): void
{
    $name = $pdo->query('SELECT DATABASE()')->fetchColumn();
    if (!$name) throw new RuntimeException('Select a database before migration.');
    $lock = 'nanny-migrate-' . substr(hash('sha256', $name), 0, 40);
    $stmt = $pdo->prepare('SELECT GET_LOCK(?, 0)');
    $stmt->execute([$lock]);
    if ((int) $stmt->fetchColumn() !== 1) throw new RuntimeException('Another migration is running.');
    try {
        $pdo->exec("SET SESSION sql_mode='STRICT_ALL_TABLES,NO_ZERO_DATE,NO_ZERO_IN_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION'");
        $pdo->exec('CREATE TABLE IF NOT EXISTS schema_migrations (name VARCHAR(100) PRIMARY KEY, checksum CHAR(64) NOT NULL, applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB');
        $files = ['schema.sql','migrate_v2.sql','migrate_v3.sql','migrate_v4.sql','migrate_v5_api.sql','phase1_constraints.sql','phase2_authentication.sql','migrate_v6_security.sql','migrate_v7_email_outbox.sql','migrate_v8_operations.sql'];
        foreach ($files as $file) {
            $sql = file_get_contents(__DIR__ . '/../database/' . $file);
            if ($sql === false) throw new RuntimeException('Missing migration: ' . $file);
            $hash = hash('sha256', str_replace("\r\n", "\n", $sql));
            $stmt = $pdo->prepare('SELECT checksum FROM schema_migrations WHERE name=?');
            $stmt->execute([$file]);
            $existing = $stmt->fetchColumn();
            if ($existing !== false) {
                if (!hash_equals($existing, $hash)) throw new RuntimeException('Applied migration changed: ' . $file);
                continue;
            }
            // These reviewed files contain DDL only, no procedures or semicolons in strings.
            $sql = preg_replace('/--[^\r\n]*/', '', $sql);
            foreach (explode(';', $sql) as $statement) {
                $statement = trim($statement);
                if ($statement === '') continue;
                if (!preg_match('/^(CREATE TABLE IF NOT EXISTS|CREATE INDEX IF NOT EXISTS|ALTER TABLE)\b/i', $statement)) {
                    throw new RuntimeException('Non-schema statement refused in ' . $file);
                }
                $pdo->exec($statement);
            }
            $pdo->prepare('INSERT INTO schema_migrations (name,checksum) VALUES (?,?)')->execute([$file,$hash]);
        }
    } finally {
        $pdo->prepare('SELECT RELEASE_LOCK(?)')->execute([$lock]);
    }
}
