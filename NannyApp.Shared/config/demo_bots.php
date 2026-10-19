<?php
/**
 * Demo "bot" accounts that make an empty site look lived-in: verified nannies with photos,
 * parents, completed bookings and reviews.
 *
 * Every bot is obviously a bot and easy to remove:
 *   - its name ends in "(Bot)" and its bio says it is not a real person;
 *   - its e-mail is on the reserved domain bots.nanny-app.invalid, so no e-mail can ever reach anyone;
 *   - its password is random and never shown, so nobody can sign in as it.
 * Removing the bot users removes their profiles, bookings, payments and reviews (ON DELETE CASCADE).
 *
 * Run through api/bots.php (the "Demo bots" GitHub workflow) or NannyApp.Shared/bin/demo-bots.php.
 */

declare(strict_types=1);

const DEMO_BOT_EMAIL_DOMAIN = 'bots.nanny-app.invalid';

/** @return array{nannies: list<array>, parents: list<array>, reviews: list<array>} */
function demo_bot_data(): array
{
    return [
        'nannies' => [
            ['Amelia', 'Johannesburg', 180, 6, 'Calm, structured routines for toddlers and school-age children.', 'First aid, Homework help, Meal prep', 'English, isiZulu'],
            ['Jasmine', 'Cape Town', 165, 4, 'Playful learning through reading, music and outdoor time.', 'Early learning, Swimming, Arts and crafts', 'English, Afrikaans'],
            ['Margaret', 'Durban', 200, 12, 'Experienced newborn and twins care, including overnight support.', 'Newborn care, Sleep routines, First aid', 'English, isiZulu'],
            ['Naledi', 'Pretoria', 150, 3, 'After-school pick-ups, homework support and healthy snacks.', 'School runs, Homework help, Driving', 'English, Sesotho, Setswana'],
            ['Grace', 'Sandton', 220, 9, 'Special-needs experience and gentle, patient support.', 'Special needs, First aid, Speech play', 'English'],
            ['Zinhle', 'Centurion', 140, 2, 'Energetic weekend and date-night babysitting.', 'Babysitting, Games, Bedtime routines', 'English, isiZulu, isiXhosa'],
        ],
        'parents' => [
            ['Thandi', 'Johannesburg'], ['James', 'Cape Town'], ['Priya', 'Durban'], ['Sipho', 'Pretoria'],
        ],
        // [parent index, nanny index, rating, comment]
        'reviews' => [
            [0, 0, 5, 'Amelia kept our routine on track and the kids adored her.'],
            [1, 1, 5, 'Jasmine turned every afternoon into a little adventure.'],
            [2, 2, 5, 'Margaret helped us through the newborn weeks. We finally slept!'],
            [3, 3, 4, 'Reliable school pick-ups and homework always done.'],
            [0, 4, 5, 'Grace is patient, kind and incredibly professional.'],
            [1, 5, 4, 'Great for date nights: the kids were asleep when we got home.'],
            [2, 0, 5, 'Punctual, warm and great with our toddler.'],
            [3, 2, 5, 'Calm and experienced. Highly recommended.'],
        ],
    ];
}

function demo_bot_email(string $role, int $n): string
{
    return "bot.$role.$n@" . DEMO_BOT_EMAIL_DOMAIN;
}

/** Deletes every bot user and, through the foreign keys, everything that belongs to them. Returns users removed. */
function remove_demo_bots(PDO $pdo): int
{
    $stmt = $pdo->prepare('DELETE FROM users WHERE email LIKE ?');
    $stmt->execute(['%@' . DEMO_BOT_EMAIL_DOMAIN]);
    return $stmt->rowCount();
}

/**
 * The named demo sign-in accounts used for demonstrations (parent@nanny.app, james@nanny.app,
 * amelia@nanny.app). Creates only missing ones; existing accounts and their passwords are left alone.
 * No admin is created here: see docs/DEMO_ACCOUNTS.md.
 */
function ensure_demo_accounts(PDO $pdo, string $password): array
{
    if (strlen($password) < 12 || $password === 'Password123!') {
        throw new InvalidArgumentException('Choose a demo password of at least 12 characters (not the public Password123!).');
    }
    $hash = password_hash($password, PASSWORD_DEFAULT);
    $exists = $pdo->prepare('SELECT id FROM users WHERE email = ?');
    $insert = $pdo->prepare("INSERT INTO users (full_name, email, password_hash, role, status, email_verified) VALUES (?, ?, ?, ?, 'active', ?)");
    $created = [];
    foreach ([['Thandi Mokoena', 'parent@nanny.app', 'parent'], ['James Naidoo', 'james@nanny.app', 'parent'], ['Amelia Dlamini', 'amelia@nanny.app', 'nanny']] as [$name, $email, $role]) {
        $exists->execute([$email]);
        if ($exists->fetchColumn() !== false) continue;
        $insert->execute([$name, $email, $hash, $role, '1']);
        $exists->execute([$email]);
        $id = (int) $exists->fetchColumn();
        if ($role === 'nanny') {
            $pdo->prepare("INSERT INTO nanny_profiles (user_id, bio, experience_years, hourly_rate, location, verification_status) VALUES (?, ?, 5, 170, 'Johannesburg', 'verified')")
                ->execute([$id, 'Demo nanny account for trying Nanny-App.']);
        } else {
            $pdo->prepare('INSERT INTO parent_profiles (user_id, number_of_children) VALUES (?, 1)')->execute([$id]);
        }
        $created[] = $email;
    }
    return $created;
}

/** Replaces any existing bots with a fresh set. Real accounts are never touched. */
function add_demo_bots(PDO $pdo): array
{
    $data = demo_bot_data();
    $pdo->beginTransaction();
    try {
        remove_demo_bots($pdo);
        $insertUser = $pdo->prepare(
            "INSERT INTO users (full_name, email, password_hash, role, status, email_verified, profile_image)
             VALUES (?, ?, ?, ?, 'active', ?, ?)"
        );
        $userId = $pdo->prepare('SELECT id FROM users WHERE email = ?');
        $create = function (string $name, string $email, string $role, string $photo) use ($insertUser, $userId): int {
            // A random password nobody knows: bots exist to be looked at, not signed in to.
            $insertUser->execute([$name, $email, password_hash(bin2hex(random_bytes(24)), PASSWORD_DEFAULT), $role, '1', $photo]);
            $userId->execute([$email]);
            return (int) $userId->fetchColumn();
        };

        $nannyIds = [];
        $insertNanny = $pdo->prepare(
            "INSERT INTO nanny_profiles (user_id, bio, experience_years, hourly_rate, location, skills, languages, verification_status, photo_url)
             VALUES (?, ?, ?, ?, ?, ?, ?, 'verified', ?)"
        );
        foreach ($data['nannies'] as $i => [$first, $city, $rate, $years, $bio, $skills, $languages]) {
            $photo = 'assets/img/people/nanny-' . ($i + 1) . '.jpg';
            $id = $create("$first (Bot)", demo_bot_email('nanny', $i + 1), 'nanny', $photo);
            $insertNanny->execute([$id, "$bio Demo bot profile, not a real person.", $years, $rate, $city, $skills, $languages, $photo]);
            $nannyIds[] = $id;
        }

        $parentIds = [];
        $insertParent = $pdo->prepare('INSERT INTO parent_profiles (user_id, number_of_children) VALUES (?, ?)');
        foreach ($data['parents'] as $i => [$first]) {
            $id = $create("$first (Bot)", demo_bot_email('parent', $i + 1), 'parent', 'assets/img/people/parent-' . ($i + 1) . '.jpg');
            $insertParent->execute([$id, 1 + $i % 3]);
            $parentIds[] = $id;
        }

        $insertBooking = $pdo->prepare(
            "INSERT INTO bookings (parent_id, nanny_id, date_time, duration, location, notes, status)
             VALUES (?, ?, ?, ?, ?, 'Demo bot booking.', 'completed')"
        );
        $bookingId = $pdo->prepare('SELECT MAX(id) FROM bookings WHERE parent_id = ? AND nanny_id = ?');
        $insertReview = $pdo->prepare('INSERT INTO reviews (booking_id, reviewer_id, nanny_id, rating, comment) VALUES (?, ?, ?, ?, ?)');
        foreach ($data['reviews'] as $i => [$p, $n, $rating, $comment]) {
            $when = date('Y-m-d H:i:s', strtotime('-' . (7 + 5 * $i) . ' days 09:00'));
            $insertBooking->execute([$parentIds[$p], $nannyIds[$n], $when, 4, $data['parents'][$p][1]]);
            $bookingId->execute([$parentIds[$p], $nannyIds[$n]]);
            $insertReview->execute([(int) $bookingId->fetchColumn(), $parentIds[$p], $nannyIds[$n], $rating, $comment]);
        }

        $pdo->prepare(
            'UPDATE nanny_profiles SET average_rating = COALESCE((SELECT ROUND(AVG(r.rating), 2) FROM reviews r WHERE r.nanny_id = nanny_profiles.user_id), 0)
             WHERE user_id IN (SELECT id FROM users WHERE email LIKE ?)'
        )->execute(['%@' . DEMO_BOT_EMAIL_DOMAIN]);
        $pdo->commit();
    } catch (Throwable $e) {
        $pdo->rollBack();
        throw $e;
    }
    return ['nannies' => count($nannyIds), 'parents' => count($parentIds), 'reviews' => count($data['reviews'])];
}
