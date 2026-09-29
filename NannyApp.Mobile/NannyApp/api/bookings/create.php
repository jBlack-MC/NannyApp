<?php
require_once __DIR__ . '/../_bootstrap.php';
require_once __DIR__ . '/_serialize.php';
require_once __DIR__ . '/../../../../NannyApp.Shared/config/bookings.php';
if ($_SERVER['REQUEST_METHOD'] !== 'POST') json_error('POST required.',405);
$me=require_api_auth();
require_api_role($me,'parent');
try { $id=create_booking(db(),(int)$me['id'],json_body()); }
catch (BookingError $e) { json_error($e->getMessage(),$e->getCode()); }
catch (Throwable $e) { json_error('Could not create booking. Please retry.',503); }
$stmt=db()->prepare(BOOKING_SELECT_SQL . ' WHERE b.id=?');
$stmt->execute([$id]);
json_response(true,serialize_booking($stmt->fetch(),$me));
