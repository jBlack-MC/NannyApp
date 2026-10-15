<?php
/**
 * Machine translation of user-written text (nanny bios, reviews, chat messages) through the
 * MyMemory API (https://mymemory.translated.net): free, no key, about 5,000 characters a day
 * per server, or 50,000 when NANNYAPP_TRANSLATE_EMAIL holds a contact address MyMemory may use.
 * Results are cached, so each text is paid for once per language.
 */

declare(strict_types=1);

/**
 * Languages offered for translation. South Africa's official languages come first and are
 * marked recommended; Tshivenda is left out because the service returned nothing for it.
 * @return list<array{code:string,name:string,nativeName:string,recommended:bool}>
 */
function translation_languages(): array
{
    $rows = [
        ['en', 'English', 'English', true], ['zu', 'isiZulu', 'isiZulu', true], ['xh', 'isiXhosa', 'isiXhosa', true],
        ['af', 'Afrikaans', 'Afrikaans', true], ['nso', 'Sepedi', 'Sepedi', true], ['tn', 'Setswana', 'Setswana', true],
        ['st', 'Sesotho', 'Sesotho', true], ['ts', 'Xitsonga', 'Xitsonga', true], ['ss', 'siSwati', 'siSwati', true],
        ['nr', 'isiNdebele', 'isiNdebele', true],
        ['sn', 'Shona', 'chiShona', false], ['ny', 'Chichewa', 'Chichewa', false], ['pt', 'Portuguese', 'Português', false],
        ['fr', 'French', 'Français', false], ['sw', 'Swahili', 'Kiswahili', false], ['es', 'Spanish', 'Español', false],
        ['de', 'German', 'Deutsch', false], ['nl', 'Dutch', 'Nederlands', false], ['it', 'Italian', 'Italiano', false],
        ['ar', 'Arabic', 'العربية', false], ['hi', 'Hindi', 'हिन्दी', false], ['ur', 'Urdu', 'اردو', false],
        ['zh', 'Chinese', '中文', false], ['ja', 'Japanese', '日本語', false], ['ko', 'Korean', '한국어', false],
        ['ru', 'Russian', 'Русский', false], ['tr', 'Turkish', 'Türkçe', false], ['pl', 'Polish', 'Polski', false],
        ['am', 'Amharic', 'አማርኛ', false], ['yo', 'Yoruba', 'Yorùbá', false], ['ig', 'Igbo', 'Igbo', false],
        ['ha', 'Hausa', 'Hausa', false], ['so', 'Somali', 'Soomaali', false], ['ln', 'Lingala', 'Lingála', false],
        ['bn', 'Bengali', 'বাংলা', false], ['ta', 'Tamil', 'தமிழ்', false], ['gu', 'Gujarati', 'ગુજરાતી', false],
        ['id', 'Indonesian', 'Bahasa Indonesia', false], ['vi', 'Vietnamese', 'Tiếng Việt', false], ['el', 'Greek', 'Ελληνικά', false],
    ];
    return array_map(fn($r) => ['code' => $r[0], 'name' => $r[1], 'nativeName' => $r[2], 'recommended' => $r[3]], $rows);
}

/**
 * @throws InvalidArgumentException for an unsupported language or empty text
 * @throws RuntimeException when the translation service fails or its daily limit is used up
 */
function translate_text(PDO $pdo, string $text, string $target, ?callable $fetch = null): string
{
    $text = trim($text);
    if ($text === '') throw new InvalidArgumentException('Nothing to translate.');
    if (!in_array($target, array_column(translation_languages(), 'code'), true)) throw new InvalidArgumentException('That language is not available.');

    $hash = hash('sha256', $text);
    $stmt = $pdo->prepare('SELECT translated FROM translation_cache WHERE text_hash = ? AND target = ?');
    $stmt->execute([$hash, $target]);
    $cached = $stmt->fetchColumn();
    if ($cached !== false) return (string) $cached;

    $fetch ??= 'translate_http_get';
    $parts = [];
    foreach (translate_chunks($text) as $chunk) {
        $query = ['q' => $chunk, 'langpair' => 'autodetect|' . $target];
        if ($email = getenv('NANNYAPP_TRANSLATE_EMAIL')) $query['de'] = $email;
        $data = json_decode((string) $fetch('https://api.mymemory.translated.net/get?' . http_build_query($query)), true);
        $out = $data['responseData']['translatedText'] ?? null;
        if ((int) ($data['responseStatus'] ?? 0) !== 200 || !is_string($out) || !empty($data['quotaFinished'])) {
            throw new RuntimeException('Translation unavailable: ' . substr((string) ($data['responseDetails'] ?? 'no response'), 0, 120));
        }
        $parts[] = html_entity_decode($out, ENT_QUOTES | ENT_HTML5, 'UTF-8');
    }
    $translated = implode(' ', $parts);
    $pdo->prepare('INSERT IGNORE INTO translation_cache (text_hash, target, translated) VALUES (?, ?, ?)')->execute([$hash, $target, $translated]);
    return $translated;
}

/** The service accepts at most 500 characters per request; split on sentence ends, then words. */
function translate_chunks(string $text, int $limit = 450): array
{
    $chunks = [];
    $current = '';
    foreach (preg_split('/(?<=[.!?])\s+/u', $text) ?: [$text] as $sentence) {
        foreach (mb_strlen($sentence) > $limit ? preg_split('/\s+/u', $sentence) : [$sentence] as $piece) {
            if ($current !== '' && mb_strlen($current . ' ' . $piece) > $limit) {
                $chunks[] = $current;
                $current = '';
            }
            $current = $current === '' ? mb_substr($piece, 0, $limit) : $current . ' ' . $piece;
        }
    }
    if ($current !== '') $chunks[] = $current;
    return $chunks;
}

function translate_http_get(string $url): ?string
{
    $curl = curl_init($url);
    curl_setopt_array($curl, [CURLOPT_RETURNTRANSFER => true, CURLOPT_TIMEOUT => 10, CURLOPT_CONNECTTIMEOUT => 4]);
    $body = curl_exec($curl);
    curl_close($curl);
    return is_string($body) ? $body : null;
}
