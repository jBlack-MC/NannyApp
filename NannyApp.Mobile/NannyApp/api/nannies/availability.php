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
    require_api_role($me, 'nanny');
    $days = json_body();
    $nannyId = (int) $me['id'];

    db()->beginTransaction();
    try {
        foreach ($days as $d) {
            db()->prepare(
                'INSERT INTO nanny_availability (nanny_id, day_of_week, is_available, time_start, time_end)
                 VALUES (:nid, :dow, :avail, :start, :end)
                 ON DUPLICATE KEY UPDATE is_available = VALUES(is_available), time_start = VALUES(time_start), time_end = VALUES(time_end)'
            )->execute([
                'nid' => $nannyId, 'dow' => $d['dayOfWeek'], 'avail' => !empty($d['isAvailable']) ? 1 : 0,
                'start' => $d['timeStart'] . ':00', 'end' => $d['timeEnd'] . ':00',
            ]);

            db()->prepare('DELETE FROM availability_slots WHERE nanny_id = :nid AND day_of_week = :dow')
                ->execute(['nid' => $nannyId, 'dow' => $d['dayOfWeek']]);
            foreach (($d['slots'] ?? []) as $slot) {
                db()->prepare('INSERT IGNORE INTO availability_slots (nanny_id, day_of_week, slot) VALUES (:nid, :dow, :slot)')
                    ->execute(['nid' => $nannyId, 'dow' => $d['dayOfWeek'], 'slot' => $slot]);
            }
        }
        db()->commit();
    } catch (Throwable $e) {
        db()->rollBack();
        json_error('Could not save availability.', 500);
    }

    json_response(true, null, 'Availability updated.');
}

json_error('Method not allowed.', 405);
