<?php
declare(strict_types=1);
require __DIR__ . '/../../NannyApp.Shared/config/migrations.php';
$port = getenv('NANNYAPP_TEST_DB_PORT') ?: '13379';
if ($port === '3306') throw new RuntimeException('Disposable database port required.');
$pdo = new PDO("mysql:host=127.0.0.1;port=$port;charset=utf8mb4", 'root', '', [PDO::ATTR_ERRMODE=>PDO::ERRMODE_EXCEPTION]);
$schema = 'nanny_migration_test_' . bin2hex(random_bytes(6));
function expect_migration(bool $ok, string $label): void {
    if (!$ok) throw new RuntimeException($label);
    echo "PASS $label\n";
}
$pdo->exec("CREATE DATABASE `$schema`");
$pdo->exec("USE `$schema`");
try {
    migrate_application($pdo);
    expect_migration((int)$pdo->query('SELECT COUNT(*) FROM users')->fetchColumn()===0,'fresh bootstrap has no demo users');
    $pdo->exec("INSERT INTO users (full_name,email,password_hash) VALUES ('Synthetic','synthetic@example.invalid','sentinel')");
    migrate_application($pdo);
    expect_migration($pdo->query('SELECT password_hash FROM users')->fetchColumn()==='sentinel','repeated migration preserves populated data');
    // Simulate a pre-runner installation: adopt existing DDL without replacing data.
    $pdo->exec('DELETE FROM schema_migrations');
    migrate_application($pdo);
    expect_migration($pdo->query('SELECT password_hash FROM users')->fetchColumn()==='sentinel','legacy populated schema adopted without overwriting');
    // Conflicting legacy duplicates must cause a failure without ALTER IGNORE deleting rows.
    $pdo->exec('ALTER TABLE payments ADD INDEX synthetic_payment_booking (booking_id)');
    $pdo->exec('ALTER TABLE payments DROP INDEX uq_pay_booking, DROP INDEX uk_payment_booking_id');
    $pdo->exec("INSERT INTO bookings (parent_id,nanny_id,date_time) VALUES (1,1,'2030-01-01 10:00:00')");
    $pdo->exec("INSERT INTO payments (booking_id,amount) VALUES (1,10),(1,20)");
    $pdo->exec("DELETE FROM schema_migrations WHERE name='migrate_v2.sql'");
    try { migrate_application($pdo); throw new LogicException('Duplicate ledger accepted'); }
    catch (PDOException $e) { expect_migration((int)$pdo->query('SELECT COUNT(*) FROM payments')->fetchColumn()===2,'conflicting existing rows preserved on constraint failure'); }
    // Resolve only synthetic fixtures to finish testing the runner.
    $pdo->exec('DELETE FROM payments WHERE amount=20');
    migrate_application($pdo);
    $pdo->exec("UPDATE schema_migrations SET checksum=REPEAT('0',64) WHERE name='schema.sql'");
    try { migrate_application($pdo); throw new LogicException('Checksum mismatch accepted'); }
    catch (RuntimeException $e) { expect_migration(str_contains($e->getMessage(),'Applied migration changed'),'changed migration refused'); }
} finally {
    $pdo->exec("DROP DATABASE `$schema`");
}
