<?php
/** GET /api/content/languages.php — languages offered for translation, South African ones first. */
require_once __DIR__ . '/../_bootstrap.php';
require_once __DIR__ . '/../../../../NannyApp.Shared/config/translate.php';

json_response(true, translation_languages());
