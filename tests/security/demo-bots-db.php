<?php
// Runs inside a disposable database container (see tests/security/demo-bots-db.sh): bots, demo accounts,
// database sessions and the home-page queries against the real migrated schema.
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
$count = fn(string $sql) => (int) db()->query($sql)->fetchColumn();

check(add_demo_bots(db()) === ['nannies' => 6, 'parents' => 4, 'reviews' => 8], 'add');
add_demo_bots(db());
check($count("SELECT COUNT(*) FROM users WHERE email LIKE '%@bots.nanny-app.invalid'") === 10, 'refresh without duplicates');
check($count("SELECT COUNT(*) FROM nanny_profiles WHERE verification_status = 'verified' AND average_rating > 0") === 6, 'verified, rated nannies');
check(count(ensure_demo_accounts(db(), 'a-long-demo-password')) === 3, 'demo accounts');

// The website's home-page queries see the bots.
$featured = db()->query("SELECT u.full_name FROM nanny_profiles p JOIN users u ON u.id = p.user_id
    WHERE p.verification_status = 'verified' ORDER BY p.average_rating DESC, p.experience_years DESC LIMIT 6")->fetchAll();
check(count($featured) === 6, 'home page lists nannies');
check($count("SELECT COUNT(*) FROM reviews r JOIN users p ON p.id = r.reviewer_id JOIN users n ON n.id = r.nanny_id WHERE COALESCE(r.comment, '') <> ''") === 8, 'testimonials');

// Sessions.
$handler = new NannyAppDatabaseSessionHandler(db(), 1440);
$handler->open('', 'PHPSESSID');
check($handler->write('sid1', "csrf|s:3:\"a\0b\";") && $handler->write('sid1', 'csrf|s:1:"x";'), 'session upsert');
check($handler->read('sid1') === 'csrf|s:1:"x";' && $handler->validateId('sid1'), 'session read');
check($handler->updateTimestamp('sid1', '') && $handler->destroy('sid1') && $handler->read('sid1') === '', 'session destroy');

check(remove_demo_bots(db()) === 10, 'remove');
check($count('SELECT COUNT(*) FROM bookings') === 0 && $count('SELECT COUNT(*) FROM reviews') === 0, 'cascade');
check($count('SELECT COUNT(*) FROM users') === 3, 'demo accounts stay');
echo 'Database checks passed on ' . DB_DRIVER . ".\n";
