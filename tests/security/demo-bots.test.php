<?php
// Five named sign-ins only; legacy bot users require review before cleanup.
declare(strict_types=1);

require __DIR__ . '/../../NannyApp.Shared/config/demo_bots.php';

function check(bool $ok, string $what): void
{
    if (!$ok) { fwrite(STDERR, "FAILED: $what\n"); exit(1); }
}

if (!in_array('sqlite', PDO::getAvailableDrivers(), true)) {
    echo "Demo account checks skipped: this PHP has no pdo_sqlite.\n";
    exit(0);
}

$pdo = new PDO('sqlite::memory:', null, null, [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION]);
$pdo->exec('PRAGMA foreign_keys = ON');
$pdo->exec("CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, full_name TEXT NOT NULL, email TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL, role TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'active', email_verified INT NOT NULL DEFAULT 0,
    session_version INT NOT NULL DEFAULT 0)");
$pdo->exec('CREATE TABLE api_tokens (user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE, token_hash TEXT NOT NULL)');
$pdo->exec('CREATE TABLE parent_profiles (user_id INT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE, number_of_children INT DEFAULT 0)');
$pdo->exec("CREATE TABLE nanny_profiles (user_id INT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE, bio TEXT,
    experience_years INT DEFAULT 0, hourly_rate REAL DEFAULT 0, location TEXT, verification_status TEXT NOT NULL DEFAULT 'pending')");
$pdo->exec('CREATE TABLE children (parent_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE, name TEXT NOT NULL, age INT)');
$pdo->exec('CREATE TABLE bookings (parent_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE, nanny_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE, date_time TEXT, duration REAL, location TEXT, notes TEXT, status TEXT)');
$pdo->exec('CREATE TABLE saved_nannies (parent_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE, nanny_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE)');
$pdo->exec('CREATE TABLE admin_profiles (user_id INT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE, access_level TEXT, department TEXT)');
$pdo->exec("INSERT INTO users (full_name, email, password_hash, role) VALUES ('Real Person', 'real@example.com', 'unchanged', 'parent')");
$pdo->exec("INSERT INTO users (full_name, email, password_hash, role) VALUES ('Legacy Bot', 'bot.parent.1@bots.nanny-app.invalid', 'legacy', 'parent')");

$count = fn(string $sql): int => (int) $pdo->query($sql)->fetchColumn();
check(add_demo_bots($pdo) === ['nannies' => 0, 'parents' => 0, 'reviews' => 0], 'bot add creates nothing');
check($count('SELECT COUNT(*) FROM users') === 2, 'no extra bot users');
check(count(preview_demo_bots($pdo)) === 1, 'legacy bot preview');
try { remove_demo_bots($pdo); check(false, 'remove must refuse'); } catch (LogicException) {}
check($count('SELECT COUNT(*) FROM users') === 2, 'remove does not delete users');

$created = ensure_demo_accounts($pdo, 'a-long-demo-password');
check($created === ['parent@nanny.app', 'james@nanny.app', 'amelia@nanny.app', 'grace@nanny.app', 'admin@nanny.app'], 'five demo sign-ins');
check($count("SELECT COUNT(*) FROM users WHERE email IN ('parent@nanny.app','james@nanny.app','amelia@nanny.app','grace@nanny.app','admin@nanny.app')") === 5, 'exactly five named accounts');
check($count("SELECT COUNT(*) FROM parent_profiles") === 2, 'two parent profiles');
check($count("SELECT COUNT(*) FROM nanny_profiles WHERE verification_status='verified'") === 1, 'verified nanny');
check($count("SELECT COUNT(*) FROM nanny_profiles WHERE verification_status='pending'") === 1, 'pending nanny');
check($count("SELECT COUNT(*) FROM users WHERE role='admin' AND email='admin@nanny.app'") === 1, 'admin role');
check($count("SELECT COUNT(*) FROM children WHERE parent_id=(SELECT id FROM users WHERE email='parent@nanny.app')") === 1, 'Thandi has one child');
check($count("SELECT COUNT(*) FROM children WHERE parent_id=(SELECT id FROM users WHERE email='james@nanny.app')") === 2, 'James has two children');
check($count("SELECT COUNT(*) FROM bookings WHERE status='completed'") === 1 && $count("SELECT COUNT(*) FROM bookings WHERE status='pending'") === 1, 'distinct booking states');
check($count('SELECT COUNT(*) FROM saved_nannies') === 1, 'James saved Amelia');
check($count("SELECT COUNT(*) FROM admin_profiles WHERE access_level='super_admin'") === 1, 'admin profile');
check($count("SELECT COUNT(*) FROM users WHERE email LIKE '%@bots.nanny-app.invalid'") === 1, 'legacy bot untouched');
foreach (['parent@nanny.app', 'james@nanny.app', 'amelia@nanny.app', 'grace@nanny.app', 'admin@nanny.app'] as $email) {
    $hash = $pdo->query("SELECT password_hash FROM users WHERE email=" . $pdo->quote($email))->fetchColumn();
    check(password_verify('a-long-demo-password', (string) $hash), "$email can sign in");
}
$pdo->exec("INSERT INTO api_tokens (user_id,token_hash) VALUES ((SELECT id FROM users WHERE email='parent@nanny.app'),'old-parent-token')");
ensure_demo_accounts($pdo, 'another-password!!');
check($count('SELECT COUNT(*) FROM users') === 7, 'reseed adds no users');
check($count('SELECT COUNT(*) FROM children') === 3 && $count('SELECT COUNT(*) FROM bookings') === 2, 'sample activity remains idempotent');
check(password_verify('a-long-demo-password', (string) $pdo->query("SELECT password_hash FROM users WHERE email='parent@nanny.app'")->fetchColumn()), 'normal reseed preserves password');
check($count('SELECT COUNT(*) FROM api_tokens') === 1 && $count("SELECT session_version FROM users WHERE email='parent@nanny.app'") === 0, 'normal reseed preserves sessions');
putenv('NANNYAPP_RESET_DEMO_PASSWORDS=1');
ensure_demo_accounts($pdo, 'another-password!!');
putenv('NANNYAPP_RESET_DEMO_PASSWORDS');
check(password_verify('another-password!!', (string) $pdo->query("SELECT password_hash FROM users WHERE email='parent@nanny.app'")->fetchColumn()), 'explicit rotation repairs password');
check($count('SELECT COUNT(*) FROM api_tokens') === 0 && $count("SELECT session_version FROM users WHERE email='parent@nanny.app'") === 1, 'explicit rotation revokes old sessions');
check($pdo->query("SELECT password_hash FROM users WHERE email='real@example.com'")->fetchColumn() === 'unchanged', 'real account unchanged');
foreach (['short', 'Password123!'] as $weak) {
    try { ensure_demo_accounts($pdo, $weak); check(false, "rejects $weak"); } catch (InvalidArgumentException) {}
}
echo "Demo account checks passed.\n";
