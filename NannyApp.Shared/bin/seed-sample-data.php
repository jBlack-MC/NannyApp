<?php
/**
 * Simulated data for demonstrations and the project brief ("at least ten records per table").
 *
 *   NANNYAPP_ALLOW_SAMPLE_SEED=true php NannyApp.Shared/bin/seed-sample-data.php
 *
 * Builds on the demo accounts that already exist (parents and nannies; see docs/DEMO_ACCOUNTS.md):
 *   - gives every parent and nanny without a photo a profile photo, and nannies a banner;
 *   - tops up the activity tables (bookings, payments, reviews, messages, notifications, saved
 *     nannies, availability slots, portfolio, support tickets, profile views, admin profiles and
 *     subscriptions) to at least ten rows each.
 * Safe to re-run: a table that already has ten or more rows is left alone, and photos are only
 * added where none is set. Rows it writes say "Sample" so they are easy to recognise.
 * Runtime-only tables (tokens, rate limits, outboxes, caches, migrations) are deliberately not faked.
 */
declare(strict_types=1);

if (PHP_SAPI !== 'cli' || getenv('NANNYAPP_ALLOW_SAMPLE_SEED') !== 'true') {
    fwrite(STDERR, "Refusing to seed: CLI only, and NANNYAPP_ALLOW_SAMPLE_SEED=true must be set.\n");
    exit(1);
}

require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../config/demo_bots.php';

$pdo = db();
$count = fn(string $table): int => (int) $pdo->query("SELECT COUNT(*) FROM $table")->fetchColumn();
$ids = fn(string $sql): array => array_map('intval', $pdo->query($sql)->fetchAll(PDO::FETCH_COLUMN));
$report = [];

$parents = $ids("SELECT id FROM users WHERE role = 'parent' AND status = 'active' AND email NOT LIKE '%@" . DEMO_BOT_EMAIL_DOMAIN . "' ORDER BY id");
$nannies = $ids("SELECT u.id FROM users u JOIN nanny_profiles p ON p.user_id = u.id WHERE u.role = 'nanny' AND p.verification_status = 'verified' AND u.email NOT LIKE '%@" . DEMO_BOT_EMAIL_DOMAIN . "' ORDER BY u.id");
$admins = $ids("SELECT id FROM users WHERE role = 'admin' ORDER BY id");
if (count($parents) < 2 || count($nannies) < 2) {
    fwrite(STDERR, "Create the demo parents and nannies first (docs/DEMO_ACCOUNTS.md).\n");
    exit(1);
}
$pick = fn(array $list, int $i): int => $list[$i % count($list)];

$pdo->beginTransaction();
try {
    // --- Photos: public website images, so the app and website both show them -------------
    $nannyPhotos = array_map(fn($n) => "assets/img/people/nanny-$n.jpg", range(1, 6));
    $parentPhotos = array_map(fn($n) => "assets/img/people/parent-$n.jpg", range(1, 4));
    $banners = ['assets/img/hero-nanny-SMceVYR7.jpg', 'assets/img/download.jpg'];
    $setUserPhoto = $pdo->prepare("UPDATE users SET profile_image = ? WHERE id = ? AND COALESCE(profile_image, '') = ''");
    $setNannyPhoto = $pdo->prepare("UPDATE nanny_profiles SET photo_url = ? WHERE user_id = ? AND (COALESCE(photo_url, '') = '' OR photo_url LIKE '%.svg')");
    $setBanner = $pdo->prepare("UPDATE nanny_profiles SET banner_image = ? WHERE user_id = ? AND COALESCE(banner_image, '') = ''");
    $photos = 0;
    foreach ($nannies as $i => $id) {
        $setUserPhoto->execute([$nannyPhotos[$i % 6], $id]); $photos += $setUserPhoto->rowCount();
        $setNannyPhoto->execute([$nannyPhotos[$i % 6], $id]); $photos += $setNannyPhoto->rowCount();
        $setBanner->execute([$banners[$i % 2], $id]); $photos += $setBanner->rowCount();
    }
    foreach ($parents as $i => $id) {
        $setUserPhoto->execute([$parentPhotos[$i % 4], $id]); $photos += $setUserPhoto->rowCount();
    }
    $report['photos set'] = $photos;

    // --- Bookings, payments and reviews ------------------------------------------------------
    if ($count('bookings') < 10) {
        $insertBooking = $pdo->prepare(
            'INSERT INTO bookings (parent_id, nanny_id, date_time, duration, location, notes, status, booking_ref, children_details, parent_confirmed_at, checked_in_at, checked_out_at)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)'
        );
        $statuses = ['completed', 'completed', 'completed', 'completed', 'completed', 'completed', 'confirmed', 'confirmed', 'pending', 'pending', 'cancelled', 'completed'];
        $suburbs = ['Sandton', 'Rosebank', 'Randburg', 'Midrand', 'Centurion', 'Fourways'];
        foreach ($statuses as $i => $status) {
            $days = $status === 'completed' || $status === 'cancelled' ? -(3 + 4 * $i) : 2 + 2 * $i;
            $start = date('Y-m-d H:i:s', strtotime("$days days 08:00"));
            $done = $status === 'completed';
            $insertBooking->execute([
                $pick($parents, $i), $pick($nannies, $i * 3 + 1), $start, 3 + $i % 4, $suburbs[$i % 6] . ', Johannesburg',
                'Sample booking.', $status, sprintf('NA-S%04d', $i + 1), 'Sample: 2 children, ages 3 and 6',
                $done ? date('Y-m-d H:i:s', strtotime("$days days 13:00")) : null,
                $done ? $start : null, $done ? date('Y-m-d H:i:s', strtotime("$days days 12:00")) : null,
            ]);
        }
        $report['bookings'] = count($statuses);
    }
    if ($count('payments') < 10) {
        $insertPayment = $pdo->prepare('INSERT INTO payments (booking_id, amount, method, transaction_id, status, payout_status, released_at) VALUES (?, ?, ?, ?, ?, ?, ?)');
        $rows = $pdo->query("SELECT b.id, b.status, b.duration, p.hourly_rate FROM bookings b JOIN nanny_profiles p ON p.user_id = b.nanny_id
            WHERE b.status IN ('completed', 'confirmed', 'cancelled') AND NOT EXISTS (SELECT 1 FROM payments x WHERE x.booking_id = b.id) ORDER BY b.id")->fetchAll();
        foreach ($rows as $i => $b) {
            [$status, $payout] = match ($b['status']) { 'completed' => ['paid', 'released'], 'confirmed' => ['paid', 'held'], default => ['refunded', 'refunded'] };
            $insertPayment->execute([$b['id'], round((float) $b['duration'] * (float) $b['hourly_rate'], 2), $i % 3 === 0 ? 'eft' : 'card',
                sprintf('SAMPLE-TXN-%05d', $b['id']), $status, $payout, $payout === 'released' ? date('Y-m-d H:i:s') : null]);
        }
        $report['payments'] = count($rows);
    }
    if ($count('reviews') < 10) {
        $comments = ['Wonderful with our toddler and always on time.', 'Our kids ask for her every week!', 'Calm, kind and very professional.',
            'Homework done and dinner ready. Brilliant.', 'Great communication throughout the booking.', 'Very patient with our shy son.',
            'Reliable and caring. Highly recommended.', 'Fun activities and a tidy house afterwards.'];
        $insertReview = $pdo->prepare('INSERT INTO reviews (booking_id, reviewer_id, nanny_id, rating, comment) VALUES (?, ?, ?, ?, ?)');
        $rows = $pdo->query("SELECT id, parent_id, nanny_id FROM bookings b WHERE status = 'completed' AND NOT EXISTS (SELECT 1 FROM reviews r WHERE r.booking_id = b.id) ORDER BY id")->fetchAll();
        foreach ($rows as $i => $b) {
            $insertReview->execute([$b['id'], $b['parent_id'], $b['nanny_id'], $i % 4 === 3 ? 4 : 5, $comments[$i % count($comments)]]);
        }
        // Not enough completed bookings for ten reviews: add past completed bookings to review.
        for ($i = count($rows); $count('reviews') < 10; $i++) {
            $start = date('Y-m-d H:i:s', strtotime('-' . (60 + 3 * $i) . ' days 09:00'));
            $parent = $pick($parents, $i + 5); $nanny = $pick($nannies, $i + 2);
            $pdo->prepare("INSERT INTO bookings (parent_id, nanny_id, date_time, duration, location, notes, status, booking_ref) VALUES (?, ?, ?, 3, 'Johannesburg', 'Sample booking.', 'completed', ?)")
                ->execute([$parent, $nanny, $start, sprintf('NA-R%04d', $i + 1)]);
            $booking = (int) $pdo->query('SELECT MAX(id) FROM bookings')->fetchColumn();
            $insertReview->execute([$booking, $parent, $nanny, 5, $comments[$i % count($comments)]]);
        }
        $pdo->exec('UPDATE nanny_profiles SET average_rating = COALESCE((SELECT ROUND(AVG(r.rating), 2) FROM reviews r WHERE r.nanny_id = nanny_profiles.user_id), average_rating)');
        $report['reviews'] = $count('reviews');
    }

    // --- Messages, notifications, saved nannies ----------------------------------------------
    if ($count('chat_messages') < 10) {
        $lines = ['Hi! Are you free on Saturday morning?', 'Yes, I am available from 8am.', 'Great, I will send a booking request.',
            'Received, thank you. See you then!', 'Do the children have any allergies?', 'Only peanuts for the youngest.'];
        $insert = $pdo->prepare('INSERT INTO chat_messages (sender_id, receiver_id, content, is_read, created_at) VALUES (?, ?, ?, ?, ?)');
        for ($i = 0; $i < 12; $i++) {
            $parent = $pick($parents, intdiv($i, 6)); $nanny = $pick($nannies, intdiv($i, 6) + 1);
            [$from, $to] = $i % 2 === 0 ? [$parent, $nanny] : [$nanny, $parent];
            $insert->execute([$from, $to, $lines[$i % 6], $i < 10 ? 1 : 0, date('Y-m-d H:i:s', strtotime('-2 days') + 600 * $i)]);
        }
        $report['chat_messages'] = 12;
    }
    if ($count('notifications') < 10) {
        $insert = $pdo->prepare('INSERT INTO notifications (user_id, title, message, url, is_read) VALUES (?, ?, ?, ?, ?)');
        $items = [['Booking confirmed', 'Your nanny confirmed your booking.', 'parent/bookings.php'], ['New booking request', 'A family sent you a booking request.', 'nanny/bookings.php'],
            ['Payment recorded', 'A payment was recorded for your booking.', 'parent/payments.php'], ['New review', 'A parent left you a review.', 'nanny/reviews.php'],
            ['New message', 'You have a new message.', 'messages.php']];
        for ($i = 0; $i < 12; $i++) {
            [$title, $message, $url] = $items[$i % 5];
            $user = in_array($i % 5, [1, 3], true) ? $pick($nannies, $i) : $pick($parents, $i);
            $insert->execute([$user, $title, "Sample: $message", $url, $i % 3 === 0 ? 1 : 0]);
        }
        $report['notifications'] = 12;
    }
    if ($count('saved_nannies') < 10) {
        $insert = $pdo->prepare('INSERT INTO saved_nannies (parent_id, nanny_id) SELECT ?, ? WHERE NOT EXISTS (SELECT 1 FROM saved_nannies WHERE parent_id = ? AND nanny_id = ?)');
        for ($i = 0; $i < 12; $i++) {
            $parent = $pick($parents, $i); $nanny = $pick($nannies, $i + 2);
            $insert->execute([$parent, $nanny, $parent, $nanny]);
        }
        $report['saved_nannies'] = $count('saved_nannies');
    }

    // --- Nanny profiles: availability slots, portfolio, profile views -------------------------
    if ($count('availability_slots') < 10) {
        $insert = $pdo->prepare('INSERT INTO availability_slots (nanny_id, day_of_week, slot) VALUES (?, ?, ?)');
        $n = 0;
        foreach (array_slice($nannies, 0, 6) as $i => $nanny) {
            foreach ([1, 3, 5] as $day) { $insert->execute([$nanny, $day, ['morning', 'afternoon', 'evening'][($i + $day) % 3]]); $n++; }
        }
        $report['availability_slots'] = $n;
    }
    if ($count('nanny_portfolio') < 10) {
        $insert = $pdo->prepare('INSERT INTO nanny_portfolio (nanny_id, type, title, file_path, admin_verified) VALUES (?, ?, ?, ?, ?)');
        // Public website photos whose names need no URL encoding.
        $items = [['photo', 'Reading time', 'assets/img/download.jpg'], ['photo', 'Outdoor play', 'assets/img/hero-nanny-SMceVYR7.jpg'],
            ['photo', 'Arts and crafts', 'assets/img/gallery/download.jpg'], ['photo', 'Story hour', 'assets/img/gallery/hero-nanny-SMceVYR7.jpg']];
        for ($i = 0; $i < 12; $i++) {
            [$type, $title, $path] = $items[$i % 4];
            $insert->execute([$pick($nannies, $i), $type, "Sample: $title", $path, 1]);
        }
        $report['nanny_portfolio'] = 12;
    }
    if ($count('profile_views') < 10) {
        $insert = $pdo->prepare('INSERT INTO profile_views (nanny_id, viewer_id, view_date, viewed_at) VALUES (?, ?, ?, ?)');
        for ($i = 0; $i < 15; $i++) {
            $at = strtotime('-' . $i . ' days 10:00');
            $insert->execute([$pick($nannies, $i % 5), $pick($parents, $i), date('Y-m-d', $at), date('Y-m-d H:i:s', $at)]);
        }
        $pdo->exec('UPDATE nanny_profiles SET profile_views = (SELECT COUNT(*) FROM profile_views v WHERE v.nanny_id = nanny_profiles.user_id)');
        $report['profile_views'] = 15;
    }

    // --- Support tickets -----------------------------------------------------------------------
    if ($count('support_tickets') < 10) {
        $insert = $pdo->prepare('INSERT INTO support_tickets (user_id, name, email, category, subject, message, status) VALUES (?, ?, ?, ?, ?, ?, ?)');
        $topics = [['booking', 'Change booking time', 'Can I move my booking to the afternoon?'], ['payment', 'Payment not showing', 'I paid by EFT yesterday.'],
            ['technical', 'Update my email', 'Please help me change my e-mail address.'], ['general', 'How do reviews work?', 'When can I leave a review?'],
            ['safety', 'Verification question', 'Which documents do nannies upload?']];
        $who = $pdo->prepare('SELECT full_name, email FROM users WHERE id = ?');
        for ($i = 0; $i < 12; $i++) {
            $user = $i % 3 === 2 ? $pick($nannies, $i) : $pick($parents, $i);
            $who->execute([$user]); $u = $who->fetch();
            [$category, $subject, $message] = $topics[$i % 5];
            $insert->execute([$user, $u['full_name'], $u['email'], $category, "Sample: $subject", $message, ['open', 'in_progress', 'resolved'][$i % 3]]);
        }
        $report['support_tickets'] = 12;
    }

    // --- Admin profiles: the real admins plus "(Bot)" support staff nobody can sign in as ------
    if ($count('admin_profiles') < 10) {
        $insertUser = $pdo->prepare("INSERT INTO users (full_name, email, password_hash, role, status, email_verified) VALUES (?, ?, ?, 'admin', 'active', '1')");
        for ($i = 1; count($admins) < 10; $i++) {
            $email = demo_bot_email('admin', $i);
            $exists = $pdo->prepare('SELECT id FROM users WHERE email = ?'); $exists->execute([$email]);
            if ($exists->fetchColumn() !== false) continue;
            $insertUser->execute(["Support Agent $i (Bot)", $email, password_hash(bin2hex(random_bytes(24)), PASSWORD_DEFAULT)]);
            $exists->execute([$email]); $admins[] = (int) $exists->fetchColumn();
        }
        $insert = $pdo->prepare('INSERT INTO admin_profiles (user_id, access_level, department, notes) SELECT ?, ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM admin_profiles WHERE user_id = ?)');
        foreach ($admins as $i => $id) {
            $insert->execute([$id, $i === 0 ? 'super_admin' : 'support', ['Operations', 'Support', 'Verification', 'Payments'][$i % 4], 'Sample admin profile.', $id]);
        }
        $report['admin_profiles'] = $count('admin_profiles');
    }

    // --- Subscriptions: expired sample records, so nobody gets Plus for free -------------------
    if ($count('subscriptions') < 10) {
        $insert = $pdo->prepare("INSERT INTO subscriptions (user_id, platform, product_id, purchase_token, token_hash, status, expires_at, auto_renewing, last_verified_at)
            VALUES (?, 'google_play', 'nannyapp_plus_monthly', ?, ?, 'expired', ?, 0, ?)");
        for ($i = 0; $i < 10; $i++) {
            $token = 'sample-expired-' . bin2hex(random_bytes(8));
            $insert->execute([$pick(array_merge($parents, $nannies), $i), $token, hash('sha256', $token),
                date('Y-m-d H:i:s', strtotime('-' . (30 + $i) . ' days')), date('Y-m-d H:i:s', strtotime('-' . (30 + $i) . ' days'))]);
        }
        $report['subscriptions'] = 10;
    }

    $pdo->commit();
} catch (Throwable $e) {
    $pdo->rollBack();
    fwrite(STDERR, 'Sample data failed, nothing was changed: ' . $e->getMessage() . "\n");
    exit(1);
}

foreach ($report as $what => $n) echo str_pad($what, 20) . $n . "\n";
echo "\nRows per table:\n";
foreach ($pdo->query(DB_DRIVER === 'pgsql'
    ? "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE' ORDER BY table_name"
    : 'SHOW TABLES')->fetchAll(PDO::FETCH_COLUMN) as $table) {
    echo '  ' . str_pad($table, 26) . $count($table) . "\n";
}
