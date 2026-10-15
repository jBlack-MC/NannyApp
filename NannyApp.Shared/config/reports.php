<?php
/**
 * Safety reports about another user, shared by the website and the API.
 *
 * A report is a support ticket in the 'safety' category (admins see those first) with the
 * reported account and, optionally, the booking attached. Every admin also gets an in-app
 * notification. Callers own authentication and rate limiting.
 */

declare(strict_types=1);

require_once __DIR__ . '/notification_outbox.php';

/** @return array<string,string> reason code => label shown to the person reporting */
function report_reasons(): array
{
    return [
        'child_safety' => 'A child may be at risk',
        'unsafe_behaviour' => 'Unsafe or inappropriate behaviour',
        'harassment' => 'Harassment, threats or abuse',
        'fake_profile' => 'Fake profile or false information',
        'payment' => 'Payment fraud or off-platform payment request',
        'no_show' => 'No-show or serious reliability problem',
        'other' => 'Something else',
    ];
}

/**
 * @throws InvalidArgumentException with a message safe to show the reporter
 * @return int the ticket id
 */
function create_user_report(PDO $pdo, array $reporter, int $reportedUserId, ?int $bookingId, string $reason, string $details): int
{
    $reasons = report_reasons();
    if (!isset($reasons[$reason])) throw new InvalidArgumentException('Please choose what the report is about.');
    $details = trim($details);
    if (mb_strlen($details) < 10) throw new InvalidArgumentException('Please describe what happened (at least 10 characters).');
    if (mb_strlen($details) > 2000) throw new InvalidArgumentException('Please keep the description under 2000 characters.');
    if ($reportedUserId === (int) $reporter['id']) throw new InvalidArgumentException('You cannot report your own account.');

    $stmt = $pdo->prepare("SELECT id, full_name, role FROM users WHERE id = ? AND role IN ('parent','nanny')");
    $stmt->execute([$reportedUserId]);
    $reported = $stmt->fetch(PDO::FETCH_ASSOC);
    if (!$reported) throw new InvalidArgumentException('That account could not be found.');

    if ($bookingId !== null) {
        // A linked booking must be one the reporter and the reported person share.
        $stmt = $pdo->prepare('SELECT id FROM bookings WHERE id = ?
            AND ((parent_id = ? AND nanny_id = ?) OR (parent_id = ? AND nanny_id = ?))');
        $stmt->execute([$bookingId, $reporter['id'], $reportedUserId, $reportedUserId, $reporter['id']]);
        if (!$stmt->fetchColumn()) throw new InvalidArgumentException('That booking is not one you share with this person.');
    }

    $subject = 'Safety report: ' . $reasons[$reason] . ' (' . $reported['full_name'] . ')';
    $pdo->beginTransaction();
    try {
        $pdo->prepare("INSERT INTO support_tickets (user_id, name, email, category, subject, message, reported_user_id, booking_id, report_reason)
            VALUES (?, ?, ?, 'safety', ?, ?, ?, ?, ?)")
            ->execute([$reporter['id'], $reporter['full_name'], $reporter['email'], mb_substr($subject, 0, 200), $details,
                $reportedUserId, $bookingId, $reason]);
        $ticketId = (int) $pdo->lastInsertId();
        foreach ($pdo->query("SELECT id FROM users WHERE role = 'admin' AND status = 'active'")->fetchAll(PDO::FETCH_COLUMN) as $adminId) {
            queue_notification($pdo, (int) $adminId, 'New safety report', $reasons[$reason] . ' — ticket #' . $ticketId, 'admin/support.php?category=safety');
        }
        $pdo->commit();
    } catch (Throwable $e) {
        if ($pdo->inTransaction()) $pdo->rollBack();
        throw $e;
    }
    return $ticketId;
}
