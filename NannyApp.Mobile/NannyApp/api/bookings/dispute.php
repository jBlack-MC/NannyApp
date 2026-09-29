<?php
require_once __DIR__ . '/../_bootstrap.php';
require_once __DIR__ . '/_serialize.php';
require_once __DIR__ . '/../../../../NannyApp.Shared/config/bookings.php';
if ($_SERVER['REQUEST_METHOD'] !== 'POST') json_error('POST required.',405);
$me=require_api_auth();
$b=json_body();
$id=filter_var($b['bookingId']??null,FILTER_VALIDATE_INT,['options'=>['min_range'=>1]]);
if (!$id) json_error('Invalid booking ID.',400);
try {
    transition_booking(db(),(int)$me['id'],$id,'dispute',$b);
} catch (BookingError $e) { json_error($e->getMessage(),$e->getCode()); }
catch (Throwable $e) { json_error('Could not update booking. Please retry.',503); }
json_response(true,(object)[]);
