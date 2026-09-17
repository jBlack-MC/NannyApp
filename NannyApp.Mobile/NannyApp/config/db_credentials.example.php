<?php
/**
 * Copy this file to config/db_credentials.php (gitignored) and fill in your
 * real MySQL credentials. These constants are consumed by config/database.php's
 * db() PDO singleton, which is shared by both the original web app and the
 * /api/ layer used by the Android app.
 */

define('DB_HOST', '127.0.0.1');
define('DB_PORT', '3306');
define('DB_NAME', 'nanny_app');
define('DB_USER', 'root');
define('DB_PASS', '');
