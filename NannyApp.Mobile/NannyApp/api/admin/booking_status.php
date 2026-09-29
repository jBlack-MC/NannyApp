<?php
require_once __DIR__ . '/../_bootstrap.php';
require_once __DIR__ . '/../../../../NannyApp.Shared/config/bookings.php';
if ($_SERVER['REQUEST_METHOD']!=='POST') json_error('POST required.',405);
$me=require_api_auth(); require_api_role($me,'admin'); $b=json_body();
$map=['completed'=>'release','cancelled'=>'refund'];
$status=$b['status']??null;
if (!is_string($status) || !isset($map[$status])) json_error('Use a supported ledger resolution: completed or cancelled.',400);
try { transition_booking(db(),(int)$me['id'],(int)($b['bookingId']??0),$map[$status]); }
catch (BookingError $e) { json_error($e->getMessage(),$e->getCode()); }
catch (Throwable $e) { json_error('Could not update ledger.',503); }
json_response(true,(object)[]);
