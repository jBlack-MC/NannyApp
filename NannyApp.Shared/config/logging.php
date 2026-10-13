<?php
/**
 * Shared diagnostics for the web portal, the /api/ layer and the CLI workers.
 *
 * Every request gets a short reference (request_ref()). The same reference is
 * written on each log line and shown to the person who hit the error, so
 * "it failed, reference 4F2A9C1B" can be matched to one line in the server log.
 *
 * Log lines are single-line JSON prefixed with "nannyapp" and go to PHP's
 * error log (container stderr in production). They must never carry names,
 * emails, tokens, message bodies or request payloads — see docs/TROUBLESHOOTING.md.
 */

declare(strict_types=1);

function request_ref(): string
{
    static $ref = null;
    return $ref ??= strtoupper(bin2hex(random_bytes(4)));
}

/** Exception text can echo user input (e.g. a duplicate email); strip it before logging. */
function log_scrub(string $text): string
{
    $text = preg_replace('/[A-Z0-9._%+\-]+@[A-Z0-9.\-]+\.[A-Z]{2,}/i', '[email]', $text) ?? '';
    $text = preg_replace('/\b[a-f0-9]{32,}\b/i', '[token]', $text) ?? '';
    return substr(str_replace(["\r", "\n"], ' ', $text), 0, 500);
}

function log_event(string $level, string $event, array $context = []): void
{
    $line = ['level' => $level, 'event' => $event, 'ref' => request_ref()];
    if (PHP_SAPI === 'cli') {
        $line['script'] = basename((string) ($_SERVER['argv'][0] ?? 'cli'));
    } else {
        $line['method'] = (string) ($_SERVER['REQUEST_METHOD'] ?? '');
        // Path only: query strings can hold verification and reset tokens.
        $line['path'] = (string) parse_url((string) ($_SERVER['REQUEST_URI'] ?? ''), PHP_URL_PATH);
    }
    error_log('nannyapp ' . json_encode($line + $context, JSON_UNESCAPED_SLASHES | JSON_PARTIAL_OUTPUT_ON_ERROR));
}

function log_exception(Throwable $e, string $event = 'unhandled_exception', array $context = []): void
{
    $trace = [];
    // Frames without arguments: arguments may contain personal data.
    foreach (array_slice($e->getTrace(), 0, 6) as $frame) {
        $trace[] = basename((string) ($frame['file'] ?? '?')) . ':' . ($frame['line'] ?? '?') . ' ' . ($frame['function'] ?? '');
    }
    log_event('error', $event, $context + [
        'type' => get_class($e),
        'code' => $e->getCode(),
        'message' => log_scrub($e->getMessage()),
        'at' => basename($e->getFile()) . ':' . $e->getLine(),
        'trace' => $trace,
    ]);
}

/** Replaces a blank 500 with a response that carries the reference. $surface is 'api' or 'web'. */
function render_failure(string $surface): void
{
    $ref = request_ref();
    if (!headers_sent()) {
        http_response_code(500);
        header('X-Request-Ref: ' . $ref);
    }
    // Drop any half-rendered page so the person sees one clear message.
    while (ob_get_level() > 0) ob_end_clean();
    if ($surface === 'api') {
        echo json_encode([
            'success' => false,
            'message' => 'Something went wrong on our side. Please try again.',
            'data' => null,
            'ref' => $ref,
        ]);
        return;
    }
    echo '<!doctype html><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">'
        . '<title>Something went wrong</title>'
        . '<div style="font-family:system-ui,sans-serif;max-width:32rem;margin:15vh auto;padding:0 1.5rem;text-align:center">'
        . '<h1>Something went wrong</h1>'
        . '<p>We hit an unexpected problem. Please try again in a moment.</p>'
        . '<p>If it keeps happening, contact support and quote reference <strong>' . $ref . '</strong>.</p></div>';
}

function install_error_handlers(string $surface): void
{
    if (!headers_sent()) header('X-Request-Ref: ' . request_ref());

    set_exception_handler(static function (Throwable $e) use ($surface): void {
        log_exception($e);
        render_failure($surface);
    });

    // Fatal errors (out of memory, missing include, parse error) never reach the exception handler.
    register_shutdown_function(static function () use ($surface): void {
        $error = error_get_last();
        if ($error === null || !in_array($error['type'], [E_ERROR, E_PARSE, E_CORE_ERROR, E_COMPILE_ERROR], true)) return;
        log_event('error', 'fatal_error', [
            'message' => log_scrub($error['message']),
            'at' => basename($error['file']) . ':' . $error['line'],
        ]);
        render_failure($surface);
    });
}
