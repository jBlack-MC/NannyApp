<?php
/** POST /api/user/upload_avatar.php (multipart, field "avatar") -> { url } */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();

if (empty($_FILES['avatar'])) {
    json_error('No file uploaded.');
}

// Reuses the existing save_uploaded_image() helper from includes/functions.php,
// which validates mime type/size and returns a public-relative path.
$result = save_uploaded_image($_FILES['avatar'], 'avatars');
if (!($result['ok'] ?? false)) {
    json_error($result['error'] ?? 'Could not upload image.', 400);
}

$pdo = db();
$pdo->beginTransaction();
try {
    $pdo->prepare('UPDATE users SET profile_image = :img WHERE id = :id')
        ->execute(['img' => $result['path'], 'id' => $me['id']]);
    // Discovery cards on both clients read nanny_profiles.photo_url.
    if ($me['role'] === 'nanny') {
        $pdo->prepare('UPDATE nanny_profiles SET photo_url = :img WHERE user_id = :id')
            ->execute(['img' => $result['path'], 'id' => $me['id']]);
    }
    $pdo->commit();
} catch (Throwable $e) {
    if ($pdo->inTransaction()) $pdo->rollBack();
    throw $e;
}

json_response(true, ['url' => media_url($result['path'])]);
