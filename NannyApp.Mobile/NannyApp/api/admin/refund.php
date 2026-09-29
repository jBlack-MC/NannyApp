<?php
require_once __DIR__ . '/../_bootstrap.php';
require_once __DIR__ . '/../../../../NannyApp.Shared/config/bookings.php';
if ($_SERVER['REQUEST_METHOD']!=='POST') json_error('POST required.',405);
$me=require_api_auth();require_api_role($me,'admin');
$id=filter_var(json_body()['payment_id']??null,FILTER_VALIDATE_INT,['options'=>['min_range'=>1]]);
if (!$id) json_error('Invalid payment ID.',400);
$stmt=db()->prepare('SELECT booking_id FROM payments WHERE id=?');$stmt->execute([$id]);$booking=$stmt->fetchColumn();
if (!$booking) json_error('Payment not found.',404);
try { transition_booking(db(),(int)$me['id'],(int)$booking,'refund'); }
catch (BookingError $e) { json_error($e->getMessage(),$e->getCode()); }
catch (Throwable $e) { json_error('Could not update ledger.',503); }
json_response(true,(object)[],'Manual ledger updated; no bank transfer was made.');
