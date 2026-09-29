<?php
require_once __DIR__ . '/notification_outbox.php';
require_once __DIR__ . '/booking_ledger.php';
class BookingError extends RuntimeException {}

function save_booking_availability(PDO $pdo,int $nannyId,array $days): void
{
    if (!array_is_list($days) || count($days)>7) throw new BookingError('Invalid availability days.',400);
    $seen=[];
    foreach ($days as $day) {
        if (!is_array($day) || !is_int($day['dayOfWeek']??null) || $day['dayOfWeek']<0 || $day['dayOfWeek']>6 || isset($seen[$day['dayOfWeek']])) throw new BookingError('Invalid or duplicate day.',400);
        $seen[$day['dayOfWeek']]=true;
        foreach (['timeStart','timeEnd'] as $field) if (!is_string($day[$field]??null) || !preg_match('/^(?:[01]\d|2[0-3]):[0-5]\d$/',$day[$field])) throw new BookingError('Use HH:MM times.',400);
        if (!is_bool($day['isAvailable']??null) || $day['timeStart']>=$day['timeEnd']) throw new BookingError('Availability must end after it starts.',400);
        if (!is_array($day['slots']??[]) || count(array_filter($day['slots']??[],fn($slot)=>!is_string($slot) || !in_array($slot,['morning','afternoon','evening'],true)))) throw new BookingError('Invalid slots.',400);
    }
    $pdo->beginTransaction();
    try {
        $nanny=lock_booking_nanny($pdo,$nannyId);
        if ($nanny['role']!=='nanny' || $nanny['status']!=='active') throw new BookingError('Nanny account required.',403);
        foreach ($days as $day) {
            $pdo->prepare('INSERT INTO nanny_availability (nanny_id,day_of_week,is_available,time_start,time_end) VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE is_available=VALUES(is_available),time_start=VALUES(time_start),time_end=VALUES(time_end)')->execute([$nannyId,$day['dayOfWeek'],(int)$day['isAvailable'],$day['timeStart'].':00',$day['timeEnd'].':00']);
            $pdo->prepare('DELETE FROM availability_slots WHERE nanny_id=? AND day_of_week=?')->execute([$nannyId,$day['dayOfWeek']]);
            foreach (array_unique($day['slots']??[]) as $slot) $pdo->prepare('INSERT INTO availability_slots (nanny_id,day_of_week,slot) VALUES (?,?,?)')->execute([$nannyId,$day['dayOfWeek'],$slot]);
        }
        $pdo->commit();
    } catch (Throwable $e) { if ($pdo->inTransaction()) $pdo->rollBack();throw $e; }
}

function booking_time(mixed $raw): DateTimeImmutable
{
    if (!is_string($raw)) throw new BookingError('Choose a valid date and time.',400);
    foreach (['Y-m-d H:i','Y-m-d H:i:s','Y-m-d\\TH:i:s','Y-m-d\\TH:i'] as $format) {
        $date = DateTimeImmutable::createFromFormat('!' . $format, $raw);
        if ($date && $date->format($format) === $raw && $date->getTimestamp()>time()) return $date;
    }
    throw new BookingError('Choose an exact future date and time.',400);
}

function booking_duration(mixed $value): float
{
    if ((!is_int($value) && !is_float($value) && !is_string($value)) || !is_numeric($value)) throw new BookingError('Invalid duration.',400);
    $hours = (float)$value;
    if (!is_finite($hours) || $hours<1 || $hours>24 || abs($hours*10-round($hours*10))>0.00001) throw new BookingError('Duration must be 1 to 24 hours in tenths of an hour.',400);
    return $hours;
}

/** Always acquire the stable nanny row before booking rows, across create/reschedule/transitions. */
function lock_booking_nanny(PDO $pdo, int $id): array
{
    $stmt=$pdo->prepare('SELECT * FROM users WHERE id=? FOR UPDATE'); $stmt->execute([$id]);
    $user=$stmt->fetch(PDO::FETCH_ASSOC);
    if (!$user) throw new BookingError('Nanny not found.',404);
    return $user;
}

function require_booking_slot(PDO $pdo, array $nanny, DateTimeImmutable $start, float $hours, int $exclude=0): array
{
    if ($nanny['role']!=='nanny' || $nanny['status']!=='active' || !(int)$nanny['email_verified']) throw new BookingError('Nanny is unavailable.',409);
    $stmt=$pdo->prepare('SELECT * FROM nanny_profiles WHERE user_id=? FOR UPDATE'); $stmt->execute([$nanny['id']]); $profile=$stmt->fetch(PDO::FETCH_ASSOC);
    if (!$profile || $profile['verification_status']!=='verified' || (float)$profile['hourly_rate']<=0) throw new BookingError('Nanny is unavailable.',409);
    $end=$start->modify('+' . (int)round($hours*3600) . ' seconds');
    // Availability is explicit: missing day rows are unavailable. Overnight bookings are not supported.
    $stmt=$pdo->prepare('SELECT * FROM nanny_availability WHERE nanny_id=? AND day_of_week=? FOR UPDATE');
    $stmt->execute([$nanny['id'],(int)$start->format('w')]); $day=$stmt->fetch(PDO::FETCH_ASSOC);
    if (!$day || !(int)$day['is_available'] || $start->format('Y-m-d')!==$end->format('Y-m-d') || $start->format('H:i:s')<$day['time_start'] || $end->format('H:i:s')>$day['time_end']) throw new BookingError('Requested time is outside nanny availability.',409);
    $stmt=$pdo->prepare("SELECT id FROM bookings WHERE nanny_id=? AND id<>? AND status IN ('pending','confirmed','in_progress','disputed') AND date_time<? AND TIMESTAMPADD(SECOND,ROUND(duration*3600),date_time)>? FOR UPDATE");
    $stmt->execute([$nanny['id'],$exclude,$end->format('Y-m-d H:i:s'),$start->format('Y-m-d H:i:s')]);
    if ($stmt->fetch()) throw new BookingError('This time overlaps another booking.',409);
    return $profile;
}

function create_booking(PDO $pdo, int $parentId, array $input): int
{
    $start=booking_time($input['dateTime']??null); $hours=booking_duration($input['duration']??null);
    $nannyId=$input['nannyId']??null;
    if (!is_int($nannyId) || $nannyId<1) throw new BookingError('Invalid nanny ID.',400);
    $address=$input['address']??null; $notes=$input['notes']??''; $ids=$input['childrenIds']??null;
    if (!$nannyId || !is_string($address) || trim($address)==='' || strlen($address)>200 || !is_string($notes) || strlen($notes)>500 || !is_array($ids) || !array_is_list($ids) || count($ids)<1 || count($ids)>20) throw new BookingError('Invalid address, notes or children selection.',400);
    foreach ($ids as $id) if (!is_int($id) || $id<1) throw new BookingError('Invalid child ID.',400);
    if (count(array_unique($ids))!==count($ids)) throw new BookingError('Duplicate child selection.',400);
    $pdo->beginTransaction();
    try {
        $nanny=lock_booking_nanny($pdo,$nannyId);
        $stmt=$pdo->prepare("SELECT id FROM users WHERE id=? AND role='parent' AND status='active' AND email_verified=1 FOR UPDATE");$stmt->execute([$parentId]);
        if (!$stmt->fetch()) throw new BookingError('Parent account is not eligible.',403);
        $profile=require_booking_slot($pdo,$nanny,$start,$hours);
        $stmt=$pdo->prepare('SELECT id,name,age FROM children WHERE parent_id=? AND id IN (' . implode(',',array_fill(0,count($ids),'?')) . ') FOR UPDATE');
        $stmt->execute([$parentId,...$ids]);$children=$stmt->fetchAll(PDO::FETCH_ASSOC);
        if (count($children)!==count($ids)) throw new BookingError('Every child must belong to your account.',403);
        $details=implode(', ',array_map(fn($c)=>$c['name'].' ('.$c['age'].')',$children));
        $pdo->prepare("INSERT INTO bookings (parent_id,nanny_id,date_time,duration,location,notes,children_details,booking_address,status) VALUES (?,?,?,?,?,?,?,?,'pending')")->execute([$parentId,$nannyId,$start->format('Y-m-d H:i:s'),$hours,trim($address),$notes,$details,trim($address)]);
        $id=(int)$pdo->lastInsertId();
        $pdo->prepare('UPDATE bookings SET booking_ref=? WHERE id=?')->execute(['BK'.str_pad((string)$id,6,'0',STR_PAD_LEFT),$id]);
        $pdo->prepare("INSERT INTO payments (booking_id,amount,method,status) VALUES (?,?,'manual','pending')")->execute([$id,round($hours*(float)$profile['hourly_rate'],2)]);
        queue_notification($pdo,$nannyId,'New booking request','Review booking #'.$id.'.','nanny/bookings.php');
        $pdo->commit();return $id;
    } catch (Throwable $e) { if ($pdo->inTransaction()) $pdo->rollBack(); throw $e; }
}

const BOOKING_TRANSITIONS = [
 'accept'=>['nanny',['pending'],'confirmed'], 'reject'=>['nanny',['pending'],'rejected'],
 'cancel'=>['participant',['pending','confirmed'],'cancelled'],
 'check_in'=>['nanny',['confirmed'],'in_progress'], 'check_out'=>['nanny',['in_progress'],'in_progress'],
 'resend_code'=>['parent',['confirmed'],'confirmed'], 'confirm'=>['parent',['in_progress'],'completed'],
 'dispute'=>['parent',['confirmed','in_progress','completed'],'disputed'],
 'record_received'=>['admin',['pending','confirmed','in_progress','completed'],''],
 'release'=>['admin',['in_progress','disputed','completed'],'completed'],
 'refund'=>['admin',['pending','confirmed','in_progress','disputed'],'cancelled'],
 'reschedule'=>['parent',['pending','confirmed'],'pending'],
];

function transition_booking(PDO $pdo, int $actorId, int $id, string $action, array $input=[]): void
{
    if (!isset(BOOKING_TRANSITIONS[$action])) throw new BookingError('Unsupported booking action.',400);
    $pdo->beginTransaction();
    try {
        $stmt=$pdo->prepare('SELECT nanny_id FROM bookings WHERE id=?');$stmt->execute([$id]);$nannyId=$stmt->fetchColumn();
        if (!$nannyId) throw new BookingError('Booking not found.',404);
        $nanny=lock_booking_nanny($pdo,(int)$nannyId);
        $stmt=$pdo->prepare('SELECT * FROM bookings WHERE id=? FOR UPDATE');$stmt->execute([$id]);$b=$stmt->fetch(PDO::FETCH_ASSOC);
        $stmt=$pdo->prepare('SELECT * FROM users WHERE id=?');$stmt->execute([$actorId]);$actor=$stmt->fetch(PDO::FETCH_ASSOC);
        [$role,$from,$to]=BOOKING_TRANSITIONS[$action];
        $owned=$role==='admin' ? ($actor && $actor['role']==='admin') : ($role==='participant' ? in_array($actorId,[(int)$b['parent_id'],(int)$b['nanny_id']],true) : $actorId===(int)$b[$role.'_id']);
        if (!$actor || $actor['status']!=='active' || !(int)$actor['email_verified'] || !$owned) throw new BookingError('Booking not found.',404);
        if (!in_array($b['status'],$from,true)) throw new BookingError('Action is not allowed in this booking state.',409);
        $stmt=$pdo->prepare('SELECT * FROM payments WHERE booking_id=? FOR UPDATE');$stmt->execute([$id]);$pay=$stmt->fetch(PDO::FETCH_ASSOC);
        if (!$pay) throw new BookingError('Missing booking ledger; contact support.',409);
        if ($action==='check_in') {
            $code=$input['checkInCode']??null;
            if ((int)$b['check_in_attempts']>=5) throw new BookingError('PIN locked. Ask the parent to regenerate it.',423);
            if (!is_string($code) || !preg_match('/^\d{6}$/',$code) || !$b['check_in_code'] || !hash_equals($b['check_in_code'],$code)) {
                $pdo->prepare('UPDATE bookings SET check_in_attempts=check_in_attempts+1 WHERE id=?')->execute([$id]);
                $pdo->commit();throw new BookingError('Incorrect PIN.',400);
            }
            $pdo->prepare('UPDATE bookings SET checked_in_at=NOW(),check_in_code=NULL WHERE id=?')->execute([$id]);
        }
        if (in_array($action,['accept','resend_code'],true)) $pdo->prepare('UPDATE bookings SET check_in_code=?,check_in_attempts=0 WHERE id=?')->execute([str_pad((string)random_int(0,999999),6,'0',STR_PAD_LEFT),$id]);
        if ($action==='check_out') {
            if ($b['checked_out_at']!==null) throw new BookingError('Already checked out.',409);
            $pdo->prepare('UPDATE bookings SET checked_out_at=NOW() WHERE id=?')->execute([$id]);
        }
        if ($action==='confirm' && !$b['checked_out_at']) throw new BookingError('Nanny must check out first.',409);
        if ($action==='dispute') {
            $reason=$input['reason']??null;
            if (!is_string($reason) || trim($reason)==='' || strlen($reason)>500) throw new BookingError('Enter a reason up to 500 bytes.',400);
            // Released funds are never silently reversed. Admin reconciles them outside this ledger.
            $pdo->prepare('UPDATE bookings SET dispute_reason=?,disputed_at=NOW() WHERE id=?')->execute([$reason,$id]);
            foreach ($pdo->query("SELECT id FROM users WHERE role='admin' AND status='active'")->fetchAll(PDO::FETCH_COLUMN) as $admin) queue_notification($pdo,(int)$admin,'Dispute requires manual review','Review booking #'.$id.'. Released funds have not been reversed.','admin/payments.php');
        }
        if ($action==='reschedule') {
            $date=booking_time($input['dateTime']??null);
            require_booking_slot($pdo,$nanny,$date,(float)$b['duration'],$id);
            $pdo->prepare('UPDATE bookings SET date_time=?,check_in_code=NULL,check_in_attempts=0 WHERE id=?')->execute([$date->format('Y-m-d H:i:s'),$id]);
        }
        if ($action==='record_received') {
            if ($pay['method']!=='manual' || $pay['status']!=='pending') throw new BookingError('No pending manual payment.',409);
            update_booking_ledger($pdo,$id,'receive',$b['status']==='completed');
        }
        if (in_array($action,['cancel','reject','refund'],true)) {
            if ($pay['payout_status']==='released') throw new BookingError('Released funds require manual reconciliation; no automatic refund.',409);
            update_booking_ledger($pdo,$id,'reverse');
        }
        if (in_array($action,['confirm','release'],true)) {
            $pdo->prepare('UPDATE bookings SET parent_confirmed_at=NOW() WHERE id=?')->execute([$id]);
            update_booking_ledger($pdo,$id,'release');
        }
        if ($to!=='') $pdo->prepare('UPDATE bookings SET status=? WHERE id=?')->execute([$to,$id]);
        foreach (['parent','nanny'] as $participant) queue_notification($pdo,(int)$b[$participant.'_id'],'Booking updated','Booking #'.$id.': '.str_replace('_',' ',$action).'.',$participant.'/bookings.php');
        $pdo->commit();
    } catch (Throwable $e) { if ($pdo->inTransaction()) $pdo->rollBack(); throw $e; }
}
