<?php
/** POST /api/support/create.php { category, subject, message, name, email } */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();

$b = json_body();
$category = in_array($b['category'] ?? '', ['booking', 'payment', 'technical', 'safety', 'general'], true) ? $b['category'] : 'general';
$subject = trim((string) ($b['subject'] ?? ''));
$message = trim((string) ($b['message'] ?? ''));
$name = trim((string) ($b['name'] ?? $me['full_name']));
$email = trim((string) ($b['email'] ?? $me['email']));

if ($subject === '' || $message === '') json_error('Please fill in the subject and message.');

$id = create_support_ticket($name ?: $me['full_name'], $email ?: $me['email'], $subject, $message, $category, (int) $me['id']);

json_response(true, [
    'id' => $id, 'userId' => (int) $me['id'], 'name' => $name, 'email' => $email, 'category' => $category,
    'subject' => $subject, 'message' => $message, 'status' => 'open', 'adminNotes' => null,
    'createdAt' => date('Y-m-d H:i:s'), 'updatedAt' => null,
]);
