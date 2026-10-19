<?php
/**
 * Which database to connect to, shared by the website and the native-app API so both always
 * use the same one.
 *
 * A full connection URL wins and is used as a whole: NANNYAPP_DB_URL, else the Supabase/Vercel
 * integration's POSTGRES_URL_NON_POOLING. Mixing fields from different sources (for example a
 * host left over from an old project with the integration's password) silently connects to the
 * wrong database, so separate NANNYAPP_DB_* / POSTGRES_* fields are used only when no URL is set.
 */

declare(strict_types=1);

/** @return array{driver: string, host: string, port: string, name: string, user: string, pass: string, sslmode: string, source: string} */
function nannyapp_db_settings(): array
{
    $env = static fn(string $name): string => trim((string) (getenv($name) ?: ''));
    foreach (['NANNYAPP_DB_URL', 'POSTGRES_URL_NON_POOLING'] as $source) {
        $url = $env($source);
        $parts = $url !== '' ? parse_url($url) : false;
        if (!is_array($parts) || empty($parts['host'])) continue;
        parse_str((string) ($parts['query'] ?? ''), $query);
        $scheme = strtolower((string) ($parts['scheme'] ?? ''));
        $driver = in_array($scheme, ['mysql', 'mariadb'], true) ? 'mysql' : 'pgsql';
        return [
            'driver' => $driver,
            'host' => (string) $parts['host'],
            'port' => (string) ($parts['port'] ?? ($driver === 'pgsql' ? '5432' : '3306')),
            'name' => rawurldecode(ltrim((string) ($parts['path'] ?? ''), '/')) ?: ($driver === 'pgsql' ? 'postgres' : 'nanny_app'),
            'user' => rawurldecode((string) ($parts['user'] ?? '')),
            'pass' => rawurldecode((string) ($parts['pass'] ?? '')),
            'sslmode' => $env('NANNYAPP_DB_SSLMODE') ?: (string) ($query['sslmode'] ?? 'require'),
            'source' => $source,
        ];
    }
    $driver = strtolower($env('NANNYAPP_DB_DRIVER') ?: ($env('POSTGRES_HOST') !== '' ? 'pgsql' : 'mysql'));
    return [
        'driver' => $driver,
        'host' => $env('NANNYAPP_DB_HOST') ?: $env('POSTGRES_HOST') ?: '127.0.0.1',
        'port' => $env('NANNYAPP_DB_PORT') ?: $env('POSTGRES_PORT') ?: ($driver === 'pgsql' ? '5432' : '3306'),
        'name' => $env('NANNYAPP_DB_NAME') ?: $env('POSTGRES_DATABASE') ?: 'nanny_app',
        'user' => $env('NANNYAPP_DB_USER') ?: $env('POSTGRES_USER') ?: 'root',
        'pass' => (string) (getenv('NANNYAPP_DB_PASS') ?: getenv('POSTGRES_PASSWORD') ?: ''),
        'sslmode' => $env('NANNYAPP_DB_SSLMODE') ?: 'require',
        'source' => 'fields',
    ];
}

/** Defines the DB_* constants from nannyapp_db_settings(), keeping any already defined (tests, CLI). */
function nannyapp_define_db_constants(): void
{
    $s = nannyapp_db_settings();
    foreach (['DB_DRIVER' => 'driver', 'DB_HOST' => 'host', 'DB_PORT' => 'port', 'DB_NAME' => 'name', 'DB_USER' => 'user', 'DB_PASS' => 'pass', 'DB_SSLMODE' => 'sslmode'] as $constant => $key) {
        if (!defined($constant)) define($constant, $s[$key]);
    }
}
