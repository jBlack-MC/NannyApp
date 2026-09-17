<?php
/** Shared booking row -> API shape serializer, included by every bookings/*.php endpoint. */

function serialize_booking(array $b, array $viewer): array {
    // The one-time check-in PIN must only ever be visible to the parent who owns
    // the booking — never to the nanny (who must be told it in person) or admin.
    $showCode = $viewer['role'] === 'parent' && (int) $viewer['id'] === (int) $b['parent_id'];

    return [
        'id' => (int) $b['id'],
        'bookingRef' => $b['booking_ref'],
        'parentId' => (int) $b['parent_id'],
        'parentName' => $b['parent_name'] ?? null,
        'nannyId' => (int) $b['nanny_id'],
        'nannyName' => $b['nanny_name'] ?? null,
        'nannyPhotoUrl' => media_url($b['nanny_photo'] ?? null),
        'dateTime' => $b['date_time'],
        'duration' => (float) $b['duration'],
        'location' => $b['location'],
        'notes' => $b['notes'],
        'childrenDetails' => $b['children_details'],
        'bookingAddress' => $b['booking_address'],
        'status' => $b['status'],
        'hourlyRate' => (float) ($b['hourly_rate'] ?? 0),
        'amount' => (float) ($b['amount'] ?? 0),
        'checkInCode' => $showCode ? $b['check_in_code'] : null,
        'checkInAttempts' => (int) ($b['check_in_attempts'] ?? 0),
        'checkedInAt' => $b['checked_in_at'] ?? null,
        'checkedOutAt' => $b['checked_out_at'] ?? null,
        'parentConfirmedAt' => $b['parent_confirmed_at'] ?? null,
        'disputeReason' => $b['dispute_reason'] ?? null,
        'disputedAt' => $b['disputed_at'] ?? null,
        'paymentStatus' => $b['payment_status'] ?? null,
        'payoutStatus' => $b['payout_status'] ?? null,
        'createdAt' => $b['created_at'] ?? null,
    ];
}

/** Common JOIN used by every booking read query in this directory. */
const BOOKING_SELECT_SQL = "
    SELECT b.*, pu.full_name AS parent_name, nu.full_name AS nanny_name, np.photo_url AS nanny_photo,
           np.hourly_rate, (b.duration * np.hourly_rate) AS amount,
           pay.status AS payment_status, pay.payout_status AS payout_status
    FROM bookings b
    JOIN users pu ON pu.id = b.parent_id
    JOIN users nu ON nu.id = b.nanny_id
    LEFT JOIN nanny_profiles np ON np.user_id = b.nanny_id
    LEFT JOIN payments pay ON pay.booking_id = b.id
";
