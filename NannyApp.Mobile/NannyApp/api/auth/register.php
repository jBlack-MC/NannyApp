<?php
/** POST /api/auth/register.php -> { token, user }. Creates a users row plus the
 * matching parent_profiles/nanny_profiles row, mirroring auth/register.php. */
require_once __DIR__ . '/../_bootstrap.php';

if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') {
    json_error('Method not allowed.', 405);
}

$b = json_body();
$role = in_array($b['role'] ?? '', ['parent', 'nanny'], true) ? $b['role'] : 'parent';
$fullName = trim((string) ($b['fullName'] ?? ''));
$email = strtolower(trim((string) ($b['email'] ?? '')));
$phone = trim((string) ($b['phone'] ?? ''));
$password = (string) ($b['password'] ?? '');

if ($fullName === '' || !filter_var($email, FILTER_VALIDATE_EMAIL) || strlen($password) < 8) {
    json_error('Please enter your name, a valid email and a password of at least 8 characters.');
}

if (auth_rate_limited('register', $email, 3)) json_error('Too many requests. Try again later.', 429);

$existing = db()->prepare('SELECT id FROM users WHERE email = :email');
$existing->execute(['email' => $email]);
if ($existing->fetch()) {
    json_error('An account with that email already exists.', 409);
}

$hash = password_hash($password, PASSWORD_DEFAULT);
$verificationRequired = nannyapp_email_verification_required();

db()->beginTransaction();
try {
    $stmt = db()->prepare(
        'INSERT INTO users (full_name, email, phone, password_hash, role, date_of_birth, address, gender,
                             email_verified, verification_token, verification_sent_at)
         VALUES (:full_name, :email, :phone, :hash, :role, :dob, :address, :gender, :verified, NULL, NULL)'
    );
    $stmt->execute([
        'full_name' => $fullName, 'email' => $email, 'phone' => $phone ?: null, 'hash' => $hash, 'role' => $role,
        'dob' => ($b['dateOfBirth'] ?? null) ?: null, 'address' => ($b['address'] ?? null) ?: null,
        'gender' => ($b['gender'] ?? null) ?: null, 'verified' => $verificationRequired ? 0 : 1,
    ]);
    $userId = (int) db()->lastInsertId();

    if ($role === 'parent') {
        db()->prepare(
            'INSERT INTO parent_profiles (user_id, emergency_contact, emergency_contact_name, emergency_contact_relationship, number_of_children)
             VALUES (:uid, :ec, :ecn, :ecr, :n)'
        )->execute([
            'uid' => $userId, 'ec' => $b['emergencyContact'] ?? null, 'ecn' => $b['emergencyContactName'] ?? null,
            'ecr' => $b['emergencyContactRelationship'] ?? null, 'n' => $b['numberOfChildren'] ?? 0,
        ]);
    } else {
        db()->prepare(
            'INSERT INTO nanny_profiles (user_id, bio, experience_years, hourly_rate, location, skills, languages, qualifications, specialisations, verification_status)
             VALUES (:uid, :bio, :exp, :rate, :loc, :skills, :lang, :qual, :spec, \'pending\')'
        )->execute([
            'uid' => $userId, 'bio' => $b['bio'] ?? null, 'exp' => $b['experienceYears'] ?? 0,
            'rate' => $b['hourlyRate'] ?? 0, 'loc' => $b['location'] ?? null, 'skills' => $b['skills'] ?? null,
            'lang' => $b['languages'] ?? 'English', 'qual' => $b['qualifications'] ?? null, 'spec' => $b['specialisations'] ?? null,
        ]);
    }

    db()->commit();
} catch (Throwable $e) {
    if (db()->inTransaction()) db()->rollBack();
    // A second request may have won the race after the pre-check. Return a
    // stable conflict instead of exposing a generic server error to mobile.
    $duplicate = db()->prepare('SELECT id FROM users WHERE email = :email');
    $duplicate->execute(['email' => $email]);
    if ($duplicate->fetch()) {
        json_error('An account with that email already exists.', 409);
    }
    log_exception($e, 'registration_failed');
    json_error('Could not create your account. Please try again.', 500);
}

$mailSent = !$verificationRequired || send_verification_email($userId, $email, $fullName);

$token = issue_api_token($userId);
$stmt = db()->prepare('SELECT * FROM users WHERE id = :id');
$stmt->execute(['id' => $userId]);
$user = $stmt->fetch();

json_response(true, [
    'token' => $token,
    'user' => [
        'id' => $userId, 'fullName' => $user['full_name'], 'email' => $user['email'], 'phone' => $user['phone'],
        'role' => $user['role'], 'status' => $user['status'], 'emailVerified' => (bool) $user['email_verified'],
        'profileImage' => media_url($user['profile_image']), 'dateOfBirth' => $user['date_of_birth'],
        'address' => $user['address'], 'gender' => $user['gender'], 'createdAt' => $user['created_at'],
    ],
], !$verificationRequired ? 'Account created. You can use your account now.' : ($mailSent ? 'Account created. Please check your email to verify your account.' : 'Account created, but email delivery failed. Please request a new verification email.'));
