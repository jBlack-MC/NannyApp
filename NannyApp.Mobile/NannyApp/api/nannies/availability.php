<?php
/** GET /api/nannies/availability.php?nanny_id=123
 *  PUT /api/nannies/availability.php  [ {dayOfWeek,isAvailable,timeStart,timeEnd,slots:[...]}, ... ] */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();
$method = $_SERVER['REQUEST_METHOD'] ?? 'GET';

if ($method === 'GET') {
    $nannyId = (int) ($_GET['nanny_id'] ?? 0);
    if ($nannyId === 0) {
        require_api_role($me, 'nanny');
        $nannyId = (int) $me['id'];
    }

    $daysStmt = db()->prepare('SELECT * FROM nanny_availability WHERE nanny_id = :id');
    $daysStmt->execute(['id' => $nannyId]);
    $days = $daysStmt->fetchAll();

    $slotsStmt = db()->prepare('SELECT day_of_week, slot FROM availability_slots WHERE nanny_id = :id');
    $slotsStmt->execute(['id' => $nannyId]);
    $slotsByDay = [];
    foreach ($slotsStmt->fetchAll() as $s) {
        $slotsByDay[(int) $s['day_of_week']][] = $s['slot'];
    }

    $byDay = [];
    foreach ($days as $d) {
        $byDay[(int) $d['day_of_week']] = $d;
    }

    $result = [];
    for ($dow = 0; $dow <= 6; $dow++) {
        $d = $byDay[$dow] ?? null;
        $result[] = [
            'dayOfWeek' => $dow,
            'isAvailable' => $d ? (bool) $d['is_available'] : false,
            'timeStart' => $d ? substr($d['time_start'], 0, 5) : '09:00',
            'timeEnd' => $d ? substr($d['time_end'], 0, 5) : '17:00',
            'slots' => $slotsByDay[$dow] ?? [],
        ];
    }
    json_response(true, $result);
}

if ($method === 'PUT') {
    require_api_role($me,'nanny');
    require_once __DIR__ . '/../../../../NannyApp.Shared/config/bookings.php';
    try { save_booking_availability(db(),(int)$me['id'],json_body()); }
    catch (BookingError $e) { json_error($e->getMessage(),$e->getCode()); }
    catch (Throwable $e) { json_error('Could not save availability.',503); }
    json_response(true,(object)[]);
}
json_error('Method not allowed.',405);
