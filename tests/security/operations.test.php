<?php
declare(strict_types=1);
require __DIR__ . '/../../NannyApp.Shared/config/migrations.php';
require __DIR__ . '/../../NannyApp.Shared/config/bookings.php';
$port=getenv('NANNYAPP_TEST_DB_PORT')?:'13379';
if ($port==='3306') throw new RuntimeException('Use disposable DB.');
$pdo=new PDO("mysql:host=127.0.0.1;port=$port;charset=utf8mb4",'root','',[PDO::ATTR_ERRMODE=>PDO::ERRMODE_EXCEPTION,PDO::ATTR_DEFAULT_FETCH_MODE=>PDO::FETCH_ASSOC]);
$schema=$argv[2]??('nanny_operations_test_'.bin2hex(random_bytes(6)));
if (!preg_match('/^nanny_operations_test_[a-f0-9]{12}$/',$schema)) throw new RuntimeException('Unsafe schema.');
function op_check(bool $ok,string $label): void { if (!$ok) throw new RuntimeException($label);echo "PASS $label\n"; }
function rejected(callable $call,int $code,string $label): void {
    try { $call();throw new LogicException('Unexpected success: '.$label); }
    catch (BookingError $e) { op_check($e->getCode()===$code,$label); }
}
function booking_input(): array { return ['nannyId'=>1,'dateTime'=>date('Y-m-d',strtotime('+2 days')).' 10:00:00','duration'=>2,'address'=>'Synthetic address','childrenIds'=>[1]]; }
if (($argv[1]??'')==='create-worker') {
    $pdo->exec("USE `$schema`");
    try { create_booking($pdo,2,booking_input());echo 'created'; } catch (BookingError $e) { if ($e->getCode()!==409) throw $e;echo 'conflict'; }exit;
}
if (in_array($argv[1]??'', ['accept-worker','cancel-worker'],true)) {
    $pdo->exec("USE `$schema`");
    $action=$argv[1]==='accept-worker'?'accept':'cancel';
    try { transition_booking($pdo,$action==='accept'?1:2,(int)$argv[3],$action);echo 'changed'; }
    catch (BookingError $e) { if ($e->getCode()!==409) throw $e;echo 'conflict'; }exit;
}
$pdo->exec("CREATE DATABASE `$schema`");$pdo->exec("USE `$schema`");
$storage=sys_get_temp_dir().'/'.$schema;mkdir($storage,0700);
define('SHARED_STORAGE_DIR',$storage);
require __DIR__ . '/../../NannyApp.Shared/config/storage.php';
require __DIR__ . '/../../NannyApp.Shared/config/storage_jobs.php';
try {
    migrate_application($pdo);
    $pdo->exec("INSERT INTO users (id,full_name,email,password_hash,role,email_verified) VALUES (1,'Synthetic Nanny','nanny@example.invalid','unused','nanny',1),(2,'Synthetic Parent','parent@example.invalid','unused','parent',1),(3,'Synthetic Stranger','stranger@example.invalid','unused','parent',1),(4,'Synthetic Admin','admin@example.invalid','unused','admin',1)");
    $pdo->exec("INSERT INTO nanny_profiles (user_id,hourly_rate,verification_status) VALUES (1,100,'verified')");
    $pdo->exec("INSERT INTO children (id,parent_id,name,age) VALUES (1,2,'Synthetic Child',5),(2,3,'Other Child',6)");
    for ($day=0;$day<7;$day++) $pdo->prepare("INSERT INTO nanny_availability (nanny_id,day_of_week,time_start,time_end) VALUES (1,?,'08:00:00','18:00:00')")->execute([$day]);
    foreach ([['dateTime'=>'2027-02-30 10:00:00'],['dateTime'=>'tomorrow'],['dateTime'=>null],['duration'=>0],['duration'=>25],['duration'=>1.234],['childrenIds'=>'1'],['childrenIds'=>[]],['childrenIds'=>[1,1]],['address'=>[]]] as $bad) rejected(fn()=>create_booking($pdo,2,array_replace(booking_input(),$bad)),400,'invalid booking payload rejected');
    rejected(fn()=>create_booking($pdo,2,array_replace(booking_input(),['childrenIds'=>[1,2]])),403,'mixed child ownership denied');
    $pdo->exec("UPDATE nanny_profiles SET verification_status='pending'");
    rejected(fn()=>create_booking($pdo,2,booking_input()),409,'unverified nanny denied');
    $pdo->exec("UPDATE nanny_profiles SET verification_status='verified'");
    rejected(fn()=>create_booking($pdo,2,array_replace(booking_input(),['dateTime'=>date('Y-m-d',strtotime('+2 days')).' 07:00:00'])),409,'outside availability denied');
    $pdo->exec("CREATE TRIGGER fail_event BEFORE INSERT ON notification_outbox FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='synthetic outbox failure'");
    try { create_booking($pdo,2,booking_input());throw new LogicException('Expected event failure'); } catch (PDOException $e) {}
    op_check((int)$pdo->query('SELECT COUNT(*) FROM bookings')->fetchColumn()===0 && (int)$pdo->query('SELECT COUNT(*) FROM payments')->fetchColumn()===0,'outbox insertion failure rolls back booking and ledger');
    $pdo->exec('DROP TRIGGER fail_event');
    $jobs=[];
    for ($i=0;$i<4;$i++) { $pipes=[];$p=proc_open([PHP_BINARY,__FILE__,'create-worker',$schema],[1=>['pipe','w'],2=>['pipe','w']],$pipes);$jobs[]=[$p,$pipes]; }
    $created=0;
    foreach ($jobs as [$p,$pipes]) { $out=stream_get_contents($pipes[1]);$err=stream_get_contents($pipes[2]);foreach ($pipes as $pipe) fclose($pipe);if(proc_close($p)!==0) throw new RuntimeException($err);if($out==='created')$created++; }
    op_check($created===1,'concurrent overlapping bookings create exactly one row');
    $id=(int)$pdo->query('SELECT id FROM bookings')->fetchColumn();
    op_check((float)$pdo->query('SELECT amount FROM payments')->fetchColumn()===200.0,'rate derived from server profile');
    rejected(fn()=>save_booking_availability($pdo,1,[['dayOfWeek'=>0,'isAvailable'=>true,'timeStart'=>'25:00','timeEnd'=>'26:00']]),400,'invalid availability times rejected');
    $adjacent=create_booking($pdo,2,array_replace(booking_input(),['dateTime'=>date('Y-m-d',strtotime('+2 days')).' 12:00:00']));
    rejected(fn()=>transition_booking($pdo,2,$adjacent,'reschedule',['dateTime'=>booking_input()['dateTime']]),409,'reschedule cannot overlap another booking');
    transition_booking($pdo,2,$adjacent,'reschedule',['dateTime'=>date('Y-m-d',strtotime('+4 days')).' 12:00']);
    $pdo->prepare('DELETE FROM payments WHERE booking_id=?')->execute([$adjacent]);$pdo->prepare('DELETE FROM bookings WHERE id=?')->execute([$adjacent]);
    transition_booking($pdo,4,$id,'record_received');
    $jobs=[];
    foreach (['accept-worker','cancel-worker'] as $worker) {
        $pipes=[];$process=proc_open([PHP_BINARY,__FILE__,$worker,$schema,(string)$id],[1=>['pipe','w'],2=>['pipe','w']],$pipes);$jobs[]=[$process,$pipes];
    }
    foreach ($jobs as [$process,$pipes]) { stream_get_contents($pipes[1]);$err=stream_get_contents($pipes[2]);foreach ($pipes as $pipe) fclose($pipe);if(proc_close($process)!==0) throw new RuntimeException($err); }
    op_check($pdo->query('SELECT status FROM bookings')->fetchColumn()==='cancelled' && $pdo->query('SELECT status FROM payments')->fetchColumn()==='refunded','accept/cancel race preserves booking-ledger consistency');
    $pdo->exec('DELETE FROM payments');$pdo->exec('DELETE FROM bookings');
    $id=create_booking($pdo,2,booking_input());
    rejected(fn()=>transition_booking($pdo,3,$id,'cancel'),404,'unrelated parent transition denied');
    rejected(fn()=>transition_booking($pdo,2,$id,'accept'),404,'parent cannot accept nanny booking');
    transition_booking($pdo,1,$id,'accept');
    for ($i=0;$i<5;$i++) rejected(fn()=>transition_booking($pdo,1,$id,'check_in',['checkInCode'=>'']),400,'invalid PIN counted');
    $pin=$pdo->query('SELECT check_in_code FROM bookings')->fetchColumn();
    rejected(fn()=>transition_booking($pdo,1,$id,'check_in',['checkInCode'=>$pin]),423,'PIN locked after five attempts');
    transition_booking($pdo,2,$id,'resend_code');$pin=$pdo->query('SELECT check_in_code FROM bookings')->fetchColumn();
    transition_booking($pdo,1,$id,'check_in',['checkInCode'=>$pin]);
    rejected(fn()=>create_booking($pdo,2,booking_input()),409,'in-progress booking blocks overlap');
    rejected(fn()=>transition_booking($pdo,2,$id,'cancel'),409,'cannot cancel in-progress booking');
    rejected(fn()=>transition_booking($pdo,2,$id,'confirm'),409,'confirmation requires checkout');
    transition_booking($pdo,4,$id,'record_received');transition_booking($pdo,1,$id,'check_out');
    $pdo->exec("CREATE TRIGGER fail_ledger BEFORE UPDATE ON payments FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='synthetic failure'");
    try { transition_booking($pdo,2,$id,'confirm');throw new LogicException('Expected failure'); } catch (PDOException $e) {}
    op_check($pdo->query('SELECT status FROM bookings')->fetchColumn()==='in_progress','ledger failure rolls back transition');
    $pdo->exec('DROP TRIGGER fail_ledger');transition_booking($pdo,2,$id,'confirm');
    transition_booking($pdo,2,$id,'dispute',['reason'=>'Synthetic dispute after release']);
    op_check($pdo->query('SELECT payout_status FROM payments')->fetchColumn()==='released','post-release dispute never silently reverses money');
    rejected(fn()=>transition_booking($pdo,4,$id,'refund'),409,'released dispute requires manual reconciliation');
    $pending=(int)$pdo->query('SELECT COUNT(*) FROM notification_outbox WHERE delivered_at IS NULL')->fetchColumn();
    $pdo->exec("CREATE TRIGGER fail_notification BEFORE INSERT ON notifications FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='synthetic delivery failure'");
    try { deliver_notification($pdo);throw new LogicException('Expected notification failure'); } catch (PDOException $e) {}
    op_check((int)$pdo->query('SELECT COUNT(*) FROM notification_outbox WHERE delivered_at IS NULL')->fetchColumn()===$pending,'failed notification stays pending');
    $pdo->exec('DROP TRIGGER fail_notification');while(deliver_notification($pdo)) {}
    op_check((int)$pdo->query('SELECT COUNT(*) FROM notifications')->fetchColumn()===$pending,'retry delivers each notification once');
    op_check(!deliver_notification($pdo),'delivered notifications not replayed');
    file_put_contents($storage.'/synthetic.pdf','synthetic');
    $pdo->exec("INSERT INTO nanny_portfolio (id,nanny_id,title,file_path) VALUES (1,1,'Synthetic','synthetic.pdf')");
    op_check(!delete_portfolio_document($pdo,3,1),'unrelated document deletion denied');
    $pdo->exec("CREATE TRIGGER fail_delete_job BEFORE INSERT ON storage_deletions FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='synthetic deletion queue failure'");
    try { delete_portfolio_document($pdo,1,1);throw new LogicException('Expected deletion queue failure'); } catch (PDOException $e) {}
    op_check((int)$pdo->query('SELECT COUNT(*) FROM nanny_portfolio')->fetchColumn()===1,'deletion queue failure retains authorized reference');
    $pdo->exec('DROP TRIGGER fail_delete_job');
    op_check(delete_portfolio_document($pdo,1,1),'authorized reference removed with deletion job');
    op_check(file_exists($storage.'/synthetic.pdf'),'file retained until durable worker runs');
    op_check(process_storage_deletion($pdo) && !file_exists($storage.'/synthetic.pdf'),'worker deletes private file');
    op_check(!process_storage_deletion($pdo),'deletion acknowledgement prevents replay');
    putenv('NANNYAPP_STORAGE_QUOTA_BYTES=10');putenv('NANNYAPP_STORAGE_RESERVE_BYTES=0');
    file_put_contents($storage.'/quota.txt','12345678');
    op_check(storage_has_capacity(2) && !storage_has_capacity(3),'total quota rejects excess bytes');
    op_check(!storage_delete('../outside.txt') && !storage_delete('/outside.txt'),'storage traversal denied');
    op_check(!storage_store_upload($storage.'/quota.txt','fake.txt'),'non-HTTP source cannot be uploaded');
    // Real HTTP controllers against the fully migrated synthetic schema.
    foreach ([1,2,3,4] as $uid) $pdo->prepare('INSERT INTO api_tokens (user_id,token_hash,expires_at) VALUES (?,?,DATE_ADD(NOW(),INTERVAL 1 HOUR))')->execute([$uid,hash('sha256','op-token-'.$uid)]);
    foreach (['NANNYAPP_DB_NAME'=>$schema,'NANNYAPP_DB_PORT'=>$port,'NANNYAPP_DB_HOST'=>'127.0.0.1','NANNYAPP_DB_USER'=>'root','NANNYAPP_DB_PASS'=>'','NANNYAPP_TEST_STORAGE'=>$storage,'NANNYAPP_STORAGE_QUOTA_BYTES'=>'1024','NANNYAPP_STORAGE_RESERVE_BYTES'=>'0'] as $key=>$value) putenv($key.'='.$value);
    function op_http(string $path, mixed $body=null, int $user=0, bool $multipart=false, string $cookie=''): array {
        $curl=curl_init('http://127.0.0.1:13382'.$path);
        $headers=$user ? ['Authorization: Bearer op-token-'.$user] : [];
        if ($cookie!=='') $headers[]='Cookie: PHPSESSID='.$cookie;
        if (!$multipart) $headers[]='Content-Type: application/json';
        curl_setopt_array($curl,[CURLOPT_RETURNTRANSFER=>true,CURLOPT_TIMEOUT=>10,CURLOPT_HTTPHEADER=>$headers]);
        if ($body!==null) curl_setopt_array($curl,[CURLOPT_POST=>true,CURLOPT_POSTFIELDS=>$multipart?$body:json_encode($body)]);
        $body=curl_exec($curl);$code=curl_getinfo($curl,CURLINFO_RESPONSE_CODE);curl_close($curl);return [$code,$body];
    }
    $server=proc_open([PHP_BINARY,'-S','127.0.0.1:13382',__DIR__.'/router.php'],[1=>['pipe','w'],2=>['pipe','w']],$pipes);
    try {
        for ($i=0;$i<50;$i++) { if(op_http('/ready')[0]===200) break;usleep(100000); }
        op_check(op_http('/api/bookings/create.php',booking_input())[0]===401,'anonymous booking API denied');
        op_check(op_http('/api/bookings/create.php',booking_input(),1)[0]===403,'nanny cannot create parent booking');
        op_check(op_http('/api/bookings/create.php',array_replace(booking_input(),['dateTime'=>'invalid']),2)[0]===400,'invalid API date returns controlled 400');
        op_check(op_http('/api/bookings/cancel.php',['bookingId'=>$id],3)[0]===404,'API unrelated cancellation denied');
        $next=array_replace(booking_input(),['dateTime'=>date('Y-m-d',strtotime('+3 days')).' 10:00:00']);
        [$code,$body]=op_http('/api/bookings/create.php',$next,2);
        op_check($code===200,'valid API booking created');
        $newId=json_decode($body,true)['data']['id'];
        op_check(op_http('/api/bookings/accept.php',['bookingId'=>$newId],1)[0]===200,'API nanny acceptance uses shared transition');
        op_check(op_http('/api/bookings/check_in.php',['bookingId'=>$newId,'checkInCode'=>''],1)[0]===400,'API empty PIN rejected');
        op_check(op_http('/api/bookings/cancel.php',['bookingId'=>$newId],2)[0]===200,'API cancellation returns successful Unit payload');
        $session=json_decode(op_http('/session?id=2&details=1')[1],true);
        op_check(op_http('/web/parent/book.php?nanny=1&step=3',null,0,false,$session['session'])[0]===200,'web wizard renders owned-child selection');
        $csrf=['csrf'=>$session['csrf']];
        $when=date('Y-m-d',strtotime('+5 days')).'T10:00';
        op_check(op_http('/web/parent/book.php?nanny=1&step=2',array_merge($csrf,['datetime'=>$when,'duration'=>'2']),0,true,$session['session'])[0]===302,'web wizard accepts exact future time');
        op_check(op_http('/web/parent/book.php?nanny=1&step=3',array_merge($csrf,['address'=>'Synthetic address','childrenIds[0]'=>'1','notes'=>'Synthetic']),0,true,$session['session'])[0]===302,'web wizard stores child selection');
        op_check(op_http('/web/parent/book.php?nanny=1&step=4',$csrf,0,true,$session['session'])[0]===302,'web wizard creates through shared service');
        op_check(op_http('/api/admin/refund.php',['payment_id'=>1],2)[0]===403,'parent cannot invoke admin refund');
        file_put_contents($storage.'/invalid.jpg','not an image');
        op_check(op_http('/api/nannies/portfolio.php',['file'=>new CURLFile($storage.'/invalid.jpg','image/jpeg','fake.jpg'),'title'=>'Synthetic'],1,true)[0]===400,'spoofed upload MIME rejected');
        file_put_contents($storage.'/valid.png',base64_decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aYJkAAAAASUVORK5CYII='));
        op_check(op_http('/api/nannies/portfolio.php',['file'=>new CURLFile($storage.'/valid.png','image/png','valid.png'),'title'=>'Synthetic','type'=>'photo'],1,true)[0]===200,'real synthetic image upload succeeds');
        file_put_contents($storage.'/fill.txt',str_repeat('x',1024));
        $before=(int)$pdo->query('SELECT COUNT(*) FROM nanny_portfolio')->fetchColumn();
        op_check(op_http('/api/nannies/portfolio.php',['file'=>new CURLFile($storage.'/valid.png','image/png','valid.png'),'title'=>'Synthetic','type'=>'photo'],1,true)[0]===400,'HTTP upload rejected at total quota');
        op_check((int)$pdo->query('SELECT COUNT(*) FROM nanny_portfolio')->fetchColumn()===$before,'quota rejection leaves no document reference');
    } finally { proc_terminate($server);foreach ($pipes as $pipe) fclose($pipe);proc_close($server); }

} finally {
    $pdo->exec("DROP DATABASE `$schema`");
    $files=new RecursiveIteratorIterator(new RecursiveDirectoryIterator($storage,FilesystemIterator::SKIP_DOTS),RecursiveIteratorIterator::CHILD_FIRST);
    foreach ($files as $file) { if ($file->isDir()) rmdir($file->getPathname()); else unlink($file->getPathname()); }
    rmdir($storage);
}
