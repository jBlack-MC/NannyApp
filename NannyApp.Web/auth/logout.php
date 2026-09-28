<?php
require_once __DIR__ . '/../config/config.php';

/* ── Clear remember-me token from DB before destroying session ───────── */
if (isset($_SESSION['user_id'])) {
    try {
        db()->prepare('UPDATE users SET remember_token = NULL WHERE id = ?')
           ->execute([$_SESSION['user_id']]);
    } catch (Throwable) {}
}

/* ── Expire the remember-me cookie ──────────────────────────────────── */
if (isset($_COOKIE['na_remember'])) {
    setcookie('na_remember', '', time() - 3600, '/', '', false, true);
}

/* ── Destroy the session ─────────────────────────────────────────────── */
$_SESSION = [];
if (ini_get('session.use_cookies')) {
    $p = session_get_cookie_params();
    setcookie(session_name(), '', time() - 42000, $p['path'], $p['domain'], $p['secure'], $p['httponly']);
}
session_destroy();

/* ── Fresh session to carry the goodbye flash ────────────────────────── */
session_start();
flash('You have been logged out successfully.');
header('Clear-Site-Data: "cache", "storage"');
?><!doctype html><meta charset="utf-8"><title>Logged out</title>
<p>You have been logged out. <a href="<?= e(url('index.php')) ?>">Continue</a></p>
<script>
(async () => {
  if ('serviceWorker' in navigator) navigator.serviceWorker.controller?.postMessage({type: 'LOGOUT'});
  if ('caches' in window) await Promise.all((await caches.keys()).filter(k => k.startsWith('nannyapp-')).map(k => caches.delete(k)));
  localStorage.clear(); sessionStorage.clear();
  location.replace(<?= json_encode(url('index.php'), JSON_HEX_TAG | JSON_HEX_AMP) ?>);
})();
</script>
