<?php
/** POST /api/content/translate.php { text, target } — machine translation of user-written text. */
require_once __DIR__ . '/../_bootstrap.php';
require_once __DIR__ . '/../../../../NannyApp.Shared/config/translate.php';

$me = require_api_auth();
if ($_SERVER['REQUEST_METHOD'] !== 'POST') json_error('POST required.', 405);
$b = json_body();
$text = (string) ($b['text'] ?? '');
if (mb_strlen($text) > 2000) json_error('That text is too long to translate.', 400);
// The free service has a daily character budget shared by every user of this server.
if (is_rate_limited('translate:' . $me['id'], 60, 3600)) json_error('You have translated a lot recently. Please try again later.', 429);

try {
    json_response(true, ['translated' => translate_text(db(), $text, (string) ($b['target'] ?? '')), 'provider' => 'MyMemory']);
} catch (InvalidArgumentException $e) {
    json_error($e->getMessage(), 400);
} catch (Throwable $e) {
    log_exception($e, 'translate_failed');
    json_error('Translation is unavailable right now. Please try again later.', 503);
}
