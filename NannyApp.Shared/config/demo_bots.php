<?php
/**
 * The five named demo sign-ins. Legacy bot rows are not removed automatically.
 * Preview: SELECT id, email, role, status FROM users
 *          WHERE email LIKE '%@bots.nanny-app.invalid'
 *             OR email LIKE 'demo.parent%@nanny.app'
 *             OR email LIKE 'demo.nanny%@nanny.app';
 * Review bookings, reviews, payments, messages and profiles for these IDs too.
 */
declare(strict_types=1);

const DEMO_BOT_EMAIL_DOMAIN = 'bots.nanny-app.invalid';

function demo_bot_email(string $role, int $n): string
{
    return "bot.$role.$n@" . DEMO_BOT_EMAIL_DOMAIN;
}

/** Read-only preview of legacy bot users; deletion is a manual operation. */
function preview_demo_bots(PDO $pdo): array
{
    $stmt = $pdo->prepare('SELECT id, email, role, status FROM users WHERE email LIKE ? ORDER BY id');
    $stmt->execute(['%@' . DEMO_BOT_EMAIL_DOMAIN]);
    return $stmt->fetchAll(PDO::FETCH_ASSOC);
}

function remove_demo_bots(PDO $pdo): int
{
    throw new LogicException('Automatic bot deletion is disabled. Preview legacy users and their dependent records first.');
}

/** Existing bot endpoint may still call this; it no longer creates bot users. */
function add_demo_bots(PDO $pdo): array
{
    return ['nannies' => 0, 'parents' => 0, 'reviews' => 0];
}

/**
 * Create the five named sign-ins. Existing passwords are preserved by default.
 * Set NANNYAPP_RESET_DEMO_PASSWORDS=1 on the application for intentional
 * rotation/repair, and tell demo users the new password.
 */
function ensure_demo_accounts(PDO $pdo, string $password): array
{
    if (strlen($password) < 12 || $password === 'Password123!') {
        throw new InvalidArgumentException('Choose a demo password of at least 12 characters (not the public Password123!).');
    }
    $accounts = [
        ['Thandi Mokoena', 'parent@nanny.app', 'parent', null],
        ['James Naidoo', 'james@nanny.app', 'parent', null],
        ['Amelia Dlamini', 'amelia@nanny.app', 'nanny', 'verified'],
        ['Grace Dlamini', 'grace@nanny.app', 'nanny', 'pending'],
        ['Demo Admin', 'admin@nanny.app', 'admin', null],
    ];
    $exists = $pdo->prepare('SELECT id, role FROM users WHERE email = ?');
    $insert = $pdo->prepare("INSERT INTO users (full_name, email, password_hash, role, status, email_verified) VALUES (?, ?, ?, ?, 'active', 1)");
    $repair = $pdo->prepare("UPDATE users SET password_hash = ?, status = 'active', email_verified = 1,
        session_version = session_version + 1 WHERE email = ?");
    $revokeTokens = $pdo->prepare('DELETE FROM api_tokens WHERE user_id = ?');
    $parentProfile = $pdo->prepare('INSERT INTO parent_profiles (user_id, number_of_children) SELECT ?, ? WHERE NOT EXISTS (SELECT 1 FROM parent_profiles WHERE user_id = ?)');
    $nannyProfile = $pdo->prepare("INSERT INTO nanny_profiles (user_id, bio, experience_years, hourly_rate, location, verification_status)
        SELECT ?, ?, ?, ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM nanny_profiles WHERE user_id = ?)");
    $setNannyStatus = $pdo->prepare('UPDATE nanny_profiles SET verification_status = ? WHERE user_id = ?');
    $result = [];
    $ids = [];
    foreach ($accounts as [$name, $email, $role, $verification]) {
        $exists->execute([$email]);
        $row = $exists->fetch(PDO::FETCH_ASSOC);
        if ($row && $row['role'] !== $role) {
            throw new RuntimeException("Demo email $email already belongs to a different role; inspect it manually.");
        }
        if (!$row) {
            $insert->execute([$name, $email, password_hash($password, PASSWORD_DEFAULT), $role]);
            $exists->execute([$email]);
            $row = $exists->fetch(PDO::FETCH_ASSOC);
            $result[] = $email;
        } else {
            if (getenv('NANNYAPP_RESET_DEMO_PASSWORDS') === '1') {
                $repair->execute([password_hash($password, PASSWORD_DEFAULT), $email]);
                $revokeTokens->execute([(int) $row['id']]);
                $result[] = $email . ' (repaired)';
            } else {
                $result[] = $email . ' (existing; unchanged)';
            }
        }
        $id = (int) $row['id'];
        $ids[$email] = $id;
        if ($role === 'parent') $parentProfile->execute([$id, $email === 'james@nanny.app' ? 2 : 1, $id]);
        if ($role === 'nanny') {
            $pending = $email === 'grace@nanny.app';
            $nannyProfile->execute([$id, $pending ? 'Pending verification demo nanny.' : 'Verified demo nanny with first-aid experience.',
                $pending ? 3 : 5, $pending ? 150 : 170, $pending ? 'Sandton' : 'Johannesburg', $verification, $id]);
            $setNannyStatus->execute([$verification, $id]);
        }
    }
    // Distinct parent journeys and visible sample activity; no purchase records.
    $child = $pdo->prepare('INSERT INTO children (parent_id, name, age) SELECT ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM children WHERE parent_id = ? AND name = ?)');
    foreach ([['parent@nanny.app', 'Lebo', 4], ['james@nanny.app', 'Asha', 7], ['james@nanny.app', 'Ravi', 3]] as [$email, $name, $age]) {
        $child->execute([$ids[$email], $name, $age, $ids[$email], $name]);
    }
    $pdo->prepare('UPDATE parent_profiles SET number_of_children = 2 WHERE user_id = ?')->execute([$ids['james@nanny.app']]);
    $booking = $pdo->prepare("INSERT INTO bookings (parent_id, nanny_id, date_time, duration, location, notes, status)
        SELECT ?, ?, ?, 4, ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM bookings WHERE parent_id = ? AND nanny_id = ? AND notes = ?)");
    foreach ([['parent@nanny.app', 'completed', 'Johannesburg', 'Demo sample: Thandi completed booking.'],
              ['james@nanny.app', 'pending', 'Cape Town', 'Demo sample: James pending booking.']] as [$email, $status, $city, $note]) {
        $when = date('Y-m-d H:i:s', strtotime($status === 'completed' ? '-14 days 09:00' : '+7 days 09:00'));
        $booking->execute([$ids[$email], $ids['amelia@nanny.app'], $when, $city, $note, $status,
            $ids[$email], $ids['amelia@nanny.app'], $note]);
    }
    $saved = $pdo->prepare('INSERT INTO saved_nannies (parent_id, nanny_id) SELECT ?, ? WHERE NOT EXISTS (SELECT 1 FROM saved_nannies WHERE parent_id = ? AND nanny_id = ?)');
    $saved->execute([$ids['james@nanny.app'], $ids['amelia@nanny.app'], $ids['james@nanny.app'], $ids['amelia@nanny.app']]);
    $admin = $pdo->prepare("INSERT INTO admin_profiles (user_id, access_level, department) SELECT ?, 'super_admin', 'Operations'
        WHERE NOT EXISTS (SELECT 1 FROM admin_profiles WHERE user_id = ?)");
    $admin->execute([$ids['admin@nanny.app'], $ids['admin@nanny.app']]);
    return $result;
}
