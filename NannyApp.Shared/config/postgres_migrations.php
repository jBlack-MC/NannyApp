<?php
declare(strict_types=1);

/** Called under the shared migration lock and transaction, after portable DDL. */
function apply_postgres_native_migrations(PDO $pdo): void
{
    $files = glob(__DIR__ . '/../database/postgres/*.sql');
    sort($files);
    foreach ($files as $path) {
        $name = 'postgres/' . basename($path);
        $raw = str_replace("\r\n", "\n", file_get_contents($path));
        $hash = hash('sha256', $raw);
        $query = $pdo->prepare('SELECT checksum FROM schema_migrations WHERE name=?');
        $query->execute([$name]);
        $existing = $query->fetchColumn();
        if ($existing !== false) {
            if (!hash_equals($existing, $hash)) throw new RuntimeException('Applied migration changed: ' . $name);
            continue;
        }
        $recorded = false;
        if ($pdo->query("SELECT to_regclass('supabase_migrations.schema_migrations')")->fetchColumn()) {
            $query = $pdo->prepare("SELECT array_to_string(statements, E'\\n') FROM supabase_migrations.schema_migrations WHERE name=?");
            $query->execute([preg_replace('/^[0-9]+_/', '', pathinfo($path, PATHINFO_FILENAME))]);
            $recorded = $query->fetchColumn();
        }
        if ($recorded !== false) {
            if (trim(str_replace("\r\n", "\n", $recorded)) !== trim($raw)) {
                throw new RuntimeException('Supabase migration source differs: ' . $name);
            }
        } else {
            // Native SQL may contain DO blocks: never split it on semicolons.
            $pdo->exec($raw);
        }
        $pdo->prepare('INSERT INTO schema_migrations (name,checksum) VALUES (?,?)')->execute([$name,$hash]);
    }
}
