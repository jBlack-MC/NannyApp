<?php
/**
 * South African public holidays from the Nager.Date public API (https://date.nager.at,
 * listed in github.com/public-apis/public-apis; code MIT-licensed, no key required).
 *
 * Each year is fetched once and cached in public_holidays_cache. If the API cannot be
 * reached, callers get an empty list: a missing holiday notice must never block a booking.
 */

declare(strict_types=1);

const HOLIDAYS_COUNTRY = 'ZA';
const HOLIDAYS_REFRESH_DAYS = 30;

/** @return list<array{date:string,name:string}> */
function public_holidays(PDO $pdo, int $year, ?callable $fetch = null): array
{
    if ($year < 2000 || $year > 2100) return [];
    $stmt = $pdo->prepare('SELECT payload, fetched_at > DATE_SUB(NOW(), INTERVAL ' . HOLIDAYS_REFRESH_DAYS . ' DAY) AS fresh
        FROM public_holidays_cache WHERE country = ? AND year = ?');
    $stmt->execute([HOLIDAYS_COUNTRY, $year]);
    $cached = $stmt->fetch(PDO::FETCH_ASSOC);
    if ($cached && (int) $cached['fresh'] === 1) return json_decode($cached['payload'], true) ?: [];

    $fetch ??= 'holidays_http_get';
    $body = $fetch('https://date.nager.at/api/v3/PublicHolidays/' . $year . '/' . HOLIDAYS_COUNTRY);
    $rows = is_string($body) ? json_decode($body, true) : null;
    if (!is_array($rows)) {
        log_event('warning', 'holidays_fetch_failed', ['year' => $year]);
        return $cached ? (json_decode($cached['payload'], true) ?: []) : [];
    }
    $holidays = [];
    foreach ($rows as $row) {
        if (is_array($row) && preg_match('/^\d{4}-\d{2}-\d{2}$/', (string) ($row['date'] ?? '')) && is_string($row['name'] ?? null)) {
            $holidays[] = ['date' => $row['date'], 'name' => mb_substr($row['name'], 0, 80)];
        }
    }
    $pdo->prepare('REPLACE INTO public_holidays_cache (country, year, payload, fetched_at) VALUES (?, ?, ?, NOW())')
        ->execute([HOLIDAYS_COUNTRY, $year, json_encode($holidays)]);
    return $holidays;
}

function holidays_http_get(string $url): ?string
{
    if (!function_exists('curl_init')) return null;
    $curl = curl_init($url);
    curl_setopt_array($curl, [CURLOPT_RETURNTRANSFER => true, CURLOPT_TIMEOUT => 5, CURLOPT_CONNECTTIMEOUT => 3,
        CURLOPT_HTTPHEADER => ['Accept: application/json'], CURLOPT_USERAGENT => 'Nanny-App holidays']);
    $body = curl_exec($curl);
    $status = curl_getinfo($curl, CURLINFO_RESPONSE_CODE);
    curl_close($curl);
    return $status === 200 && is_string($body) ? $body : null;
}
