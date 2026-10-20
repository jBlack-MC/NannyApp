<?php
// Five demo sign-ins and safe legacy cleanup behavior against the migrated schema.
declare(strict_types=1);

require __DIR__ . '/../../NannyApp.Shared/config/database.php';
require __DIR__ . '/../../NannyApp.Shared/config/migrations.php';
require __DIR__ . '/../../NannyApp.Shared/config/demo_bots.php';
require __DIR__ . '/../../NannyApp.Shared/config/session.php';

function check(bool $ok, string $what): void
{
    if (!$ok) { fwrite(STDERR, "FAILED: $what\n"); exit(1); }
}

migrate_application(db());
$count = fn(string $sql): int => (int) db()->query($sql)->fetchColumn();

check(add_demo_bots(db()) === ['nannies' => 0, 'parents' => 0, 'reviews' => 0], 'bot add is disabled');
check($count('SELECT COUNT(*) FROM users') === 0, 'no bot users created');
check(count(ensure_demo_accounts(db(), 'a-long-demo-password')) === 5, 'five accounts created');
check($count('SELECT COUNT(*) FROM users') === 5, 'exactly five users');
check($count("SELECT COUNT(*) FROM users WHERE email LIKE '%@bots.nanny-app.invalid'") === 0, 'no bot accounts');
check($count('SELECT COUNT(*) FROM parent_profiles') === 2, 'two parent profiles');
check($count("SELECT COUNT(*) FROM nanny_profiles WHERE verification_status='verified'") === 1, 'verified nanny');
check($count("SELECT COUNT(*) FROM nanny_profiles WHERE verification_status='pending'") === 1, 'pending nanny');
check($count("SELECT COUNT(*) FROM users WHERE role='admin'") === 1, 'one admin');
check($count('SELECT COUNT(*) FROM children') === 3, 'three children across the parents');
check($count("SELECT COUNT(*) FROM bookings WHERE status='completed'") === 1 && $count("SELECT COUNT(*) FROM bookings WHERE status='pending'") === 1, 'distinct booking states');
check($count('SELECT COUNT(*) FROM saved_nannies') === 1, 'James saved Amelia');
check($count("SELECT COUNT(*) FROM admin_profiles WHERE access_level='super_admin'") === 1, 'admin profile');
check($count('SELECT COUNT(*) FROM subscriptions') === 0, 'no synthetic Google purchase');
foreach (['parent@nanny.app', 'james@nanny.app', 'amelia@nanny.app', 'grace@nanny.app', 'admin@nanny.app'] as $email) {
    $hash = db()->query('SELECT password_hash FROM users WHERE email=' . db()->quote($email))->fetchColumn();
    check(password_verify('a-long-demo-password', (string) $hash), "$email can sign in");
}
db()->exec("INSERT INTO api_tokens (user_id,token_hash,expires_at) VALUES ((SELECT id FROM users WHERE email='admin@nanny.app'), REPEAT('a',64), DATE_ADD(NOW(), INTERVAL 1 DAY))");
ensure_demo_accounts(db(), 'another-password!!');
check($count('SELECT COUNT(*) FROM users') === 5, 'reseed adds no users');
check($count('SELECT COUNT(*) FROM children') === 3 && $count('SELECT COUNT(*) FROM bookings') === 2, 'sample activity remains idempotent');
check(password_verify('a-long-demo-password', (string) db()->query("SELECT password_hash FROM users WHERE email='admin@nanny.app'")->fetchColumn()), 'normal reseed preserves password');
check($count('SELECT COUNT(*) FROM api_tokens') === 1 && $count("SELECT session_version FROM users WHERE email='admin@nanny.app'") === 0, 'normal reseed preserves sessions');
putenv('NANNYAPP_RESET_DEMO_PASSWORDS=1');
ensure_demo_accounts(db(), 'another-password!!');
putenv('NANNYAPP_RESET_DEMO_PASSWORDS');
check(password_verify('another-password!!', (string) db()->query("SELECT password_hash FROM users WHERE email='admin@nanny.app'")->fetchColumn()), 'explicit rotation repairs password');
check($count('SELECT COUNT(*) FROM api_tokens') === 0 && $count("SELECT session_version FROM users WHERE email='admin@nanny.app'") === 1, 'explicit rotation revokes old sessions');

db()->exec("INSERT INTO users (full_name,email,password_hash,role,status,email_verified) VALUES ('Legacy Bot','bot.parent.1@bots.nanny-app.invalid','legacy','parent','active',1)");
check(count(preview_demo_bots(db())) === 1, 'legacy bot appears in preview');
try { remove_demo_bots(db()); check(false, 'remove must refuse'); } catch (LogicException) {}
check($count('SELECT COUNT(*) FROM users') === 6, 'legacy user was not deleted');

$handler = new NannyAppDatabaseSessionHandler(db(), 1440);
$handler->open('', 'PHPSESSID');
check($handler->write('sid1', 'csrf|s:1:"a";') && $handler->write('sid1', 'csrf|s:1:"x";'), 'session upsert');
check($handler->read('sid1') === 'csrf|s:1:"x";' && $handler->validateId('sid1'), 'session read');
check($handler->updateTimestamp('sid1', '') && $handler->destroy('sid1') && $handler->read('sid1') === '', 'session destroy');
echo "Demo database checks passed.\n";
