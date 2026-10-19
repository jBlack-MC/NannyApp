<?php
// Demo bots: added, refreshed without duplicates, removed with everything they own, real accounts untouched.
// SQLite with the production tables' shape and cascading foreign keys stands in for the database.
declare(strict_types=1);

require __DIR__ . '/../../NannyApp.Shared/config/demo_bots.php';

function check(bool $ok, string $what): void
{
    if (!$ok) { fwrite(STDERR, "FAILED: $what\n"); exit(1); }
}

if (!in_array('sqlite', PDO::getAvailableDrivers(), true)) {
    echo "Demo bot checks skipped: this PHP has no pdo_sqlite.\n";
    exit(0);
}

$pdo = new PDO('sqlite::memory:', null, null, [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION]);
$pdo->exec('PRAGMA foreign_keys = ON');
$pdo->exec("CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, full_name TEXT NOT NULL, email TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL, role TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'active', email_verified INT NOT NULL DEFAULT 0, verification_token TEXT, profile_image TEXT)");
$pdo->exec('CREATE TABLE parent_profiles (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE, number_of_children INT DEFAULT 0)');
$pdo->exec("CREATE TABLE nanny_profiles (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE, bio TEXT,
    experience_years INT DEFAULT 0, hourly_rate REAL DEFAULT 0, location TEXT, skills TEXT, languages TEXT, verification_status TEXT NOT NULL DEFAULT 'pending',
    photo_url TEXT, average_rating REAL DEFAULT 0)");
$pdo->exec("CREATE TABLE bookings (id INTEGER PRIMARY KEY AUTOINCREMENT, parent_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    nanny_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE, date_time TEXT NOT NULL, duration REAL NOT NULL, location TEXT, notes TEXT, status TEXT NOT NULL)");
$pdo->exec('CREATE TABLE reviews (id INTEGER PRIMARY KEY AUTOINCREMENT, booking_id INT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    reviewer_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE, nanny_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5), comment TEXT, UNIQUE (booking_id, reviewer_id))');
$pdo->exec("INSERT INTO users (full_name, email, password_hash, role) VALUES ('Real Person', 'real@example.com', 'x', 'parent')");

$count = fn(string $sql) => (int) $pdo->query($sql)->fetchColumn();
$result = add_demo_bots($pdo);
check($result === ['nannies' => 6, 'parents' => 4, 'reviews' => 8], 'add reports what it created');
check($count("SELECT COUNT(*) FROM users WHERE email LIKE '%@bots.nanny-app.invalid' AND full_name LIKE '% (Bot)'") === 10, 'bots are named and addressed as bots');
check($count("SELECT COUNT(*) FROM nanny_profiles WHERE verification_status = 'verified' AND photo_url LIKE 'assets/img/people/%'") === 6, 'verified nannies with photos');
check($count('SELECT COUNT(*) FROM reviews') === 8 && $count("SELECT COUNT(*) FROM bookings WHERE status = 'completed'") === 8, 'completed bookings and reviews');
check($count('SELECT COUNT(*) FROM nanny_profiles WHERE average_rating > 0') === 6, 'ratings recomputed');
check(!password_verify('Password123!', (string) $pdo->query("SELECT password_hash FROM users WHERE email = 'bot.nanny.1@bots.nanny-app.invalid'")->fetchColumn()), 'bots have no known password');

add_demo_bots($pdo);
check($count('SELECT COUNT(*) FROM users') === 11 && $count('SELECT COUNT(*) FROM reviews') === 8, 're-adding refreshes instead of duplicating');

check(ensure_demo_accounts($pdo, 'a-long-demo-password') === ['parent@nanny.app', 'james@nanny.app', 'amelia@nanny.app'], 'demo accounts created');
$pdo->exec("UPDATE users SET email_verified = 0, verification_token = 'stale' WHERE email = 'parent@nanny.app'");
check(ensure_demo_accounts($pdo, 'another-password!!') === ['parent@nanny.app (repaired)', 'james@nanny.app (repaired)', 'amelia@nanny.app (repaired)'], 'existing demo accounts are repaired');
check(password_verify('another-password!!', (string) $pdo->query("SELECT password_hash FROM users WHERE email = 'parent@nanny.app'")->fetchColumn()), 'repair sets the new password');
check($pdo->query("SELECT email_verified || COALESCE(verification_token, '-') FROM users WHERE email = 'parent@nanny.app'")->fetchColumn() === '1-', 'repair verifies the account');
foreach (['short', 'Password123!'] as $weak) {
    try { ensure_demo_accounts($pdo, $weak); check(false, "rejects $weak"); } catch (InvalidArgumentException) {}
}

check(remove_demo_bots($pdo) === 10, 'remove reports bots removed');
check($count('SELECT COUNT(*) FROM bookings') === 0 && $count('SELECT COUNT(*) FROM reviews') === 0 && $count('SELECT COUNT(*) FROM parent_profiles') === 2, 'their data goes with them');
check($count('SELECT COUNT(*) FROM users') === 4, 'the real account and demo accounts stay');
check(remove_demo_bots($pdo) === 0, 'removing again is harmless');
echo "Demo bot checks passed: marked bots, photos, reviews, refresh, demo accounts, weak passwords and cascading removal.\n";
