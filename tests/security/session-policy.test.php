<?php
declare(strict_types=1);

require_once __DIR__ . '/../../NannyApp.Shared/config/security.php';

$active = ['status' => 'active', 'email_verified' => 0];
putenv('NANNYAPP_REQUIRE_EMAIL_VERIFICATION=0');
if (!verified_active_user($active)) throw new RuntimeException('Optional verification must allow an active new account.');
putenv('NANNYAPP_REQUIRE_EMAIL_VERIFICATION=1');
if (verified_active_user($active)) throw new RuntimeException('Required verification must reject an unverified account.');
$active['email_verified'] = 1;
if (!verified_active_user($active)) throw new RuntimeException('Verified account should be allowed.');
$active['status'] = 'suspended';
if (verified_active_user($active)) throw new RuntimeException('Suspended account should be rejected.');
echo "Session policy checks passed.\n";
