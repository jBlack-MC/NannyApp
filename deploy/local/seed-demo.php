<?php
/**
 * Local development only: fills an empty database with demo data so the website and the
 * Android app have something to show. Run through deploy/local/compose.yml (`run --rm seed`).
 *
 * Creates five sign-in accounts, all with the password Password123!, matching the demo
 * workflow: parent@nanny.app, james@nanny.app, amelia@nanny.app and grace@nanny.app
 * (nannies), plus admin@nanny.app. Then runs NannyApp.Web/seed_demo.php for nannies, bookings,
 * payments, reviews and messages. Never run this against a real database.
 */
declare(strict_types=1);

if (PHP_SAPI !== 'cli' || getenv('NANNYAPP_ALLOW_DEMO_SEED') !== 'true') {
    fwrite(STDERR, "Refusing to seed: CLI only, and NANNYAPP_ALLOW_DEMO_SEED=true must be set.\n");
    exit(1);
}

// config.php expects a web request; present one from localhost so it does not redirect to HTTPS.
$_SERVER += ['SERVER_NAME' => 'localhost', 'HTTP_HOST' => 'localhost', 'REQUEST_URI' => '/', 'REQUEST_METHOD' => 'GET'];
$root = dirname(__DIR__, 2);
require_once $root . '/NannyApp.Web/config/config.php';

$demoPassword = (string) (getenv('NANNYAPP_DEMO_PASSWORD') ?: 'Password123!');
if (strlen($demoPassword) < 8) {
    fwrite(STDERR, "NANNYAPP_DEMO_PASSWORD must be at least 8 characters.\n");
    exit(1);
}
$hash = password_hash($demoPassword, PASSWORD_DEFAULT);
$insert = db()->prepare("INSERT INTO users (full_name, email, password_hash, role, phone, email_verified, status)
    VALUES (?, ?, ?, ?, '+27 82 000 0000', 1, 'active')
    ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash), role = VALUES(role),
      email_verified = 1, status = 'active'");
foreach ([
    ['Thandi Mokoena', 'parent@nanny.app', 'parent'],
    ['James Naidoo', 'james@nanny.app', 'parent'],
    ['Amelia Dlamini', 'amelia@nanny.app', 'nanny'],
    ['Grace Dlamini', 'grace@nanny.app', 'nanny'],
    ['Site Admin', 'admin@nanny.app', 'admin'],
] as [$name, $email, $role]) {
    $insert->execute([$name, $email, $hash, $role]);
}

// Keep the local accounts usable even when the database already existed with
// incomplete profile rows.
$parentProfile = db()->prepare("INSERT INTO parent_profiles (user_id, number_of_children)
    SELECT id, 1 FROM users WHERE email = ?
    ON DUPLICATE KEY UPDATE number_of_children = GREATEST(number_of_children, 1)");
$nannyProfile = db()->prepare("INSERT INTO nanny_profiles
    (user_id, bio, experience_years, hourly_rate, location, verification_status)
    SELECT id, ?, ?, ?, ?, ?
    FROM users WHERE email = ?
    ON DUPLICATE KEY UPDATE verification_status = VALUES(verification_status)");
$parentProfile->execute(['parent@nanny.app']);
$parentProfile->execute(['james@nanny.app']);
$nannyProfile->execute(['Demo nanny account for local testing.', 5, 170, 'Johannesburg', 'verified', 'amelia@nanny.app']);
$nannyProfile->execute(['Pending demo nanny account for local testing.', 3, 150, 'Sandton', 'pending', 'grace@nanny.app']);

// seed_demo.php is an admin page; sign the script in as the demo admin.
$admin = db()->query("SELECT id, password_hash FROM users WHERE email = 'admin@nanny.app'")->fetch();
$_SESSION['user_id'] = (int) $admin['id'];
$_SESSION['auth_password'] = hash('sha256', $admin['password_hash']);

ob_start();
require $root . '/NannyApp.Web/seed_demo.php';
ob_end_clean();

$counts = db()->query("SELECT
    (SELECT COUNT(*) FROM users) AS users,
    (SELECT COUNT(*) FROM nanny_profiles WHERE verification_status = 'verified') AS verified_nannies,
    (SELECT COUNT(*) FROM bookings) AS bookings")->fetch();
echo "Demo data ready: {$counts['users']} users, {$counts['verified_nannies']} verified nannies, {$counts['bookings']} bookings.\n";
echo "Sign in with parent@nanny.app and the configured demo password.\n";
