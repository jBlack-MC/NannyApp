<?php
/** Synthetic-only MySQL/MariaDB suite. Never accepts an application DB name. */
declare(strict_types=1);
$port = getenv('NANNYAPP_TEST_DB_PORT') ?: '13379';
if ($port === '3306') throw new RuntimeException('Use a dedicated disposable database server port.');
$schema = $argv[2] ?? ('nanny_security_test_' . bin2hex(random_bytes(6)));
if (!preg_match('/^nanny_security_test_[a-f0-9]{12}$/', $schema)) throw new RuntimeException('Unsafe test schema');
$pdo = new PDO("mysql:host=127.0.0.1;port=$port;charset=utf8mb4", 'root', '', [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION, PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC, PDO::ATTR_EMULATE_PREPARES => false]);
function db(): PDO { global $pdo; return $pdo; }
require __DIR__ . '/../../NannyApp.Shared/config/security.php';
function check(bool $value, string $label): void { if (!$value) throw new RuntimeException($label); echo "PASS $label\n"; }
if (($argv[1] ?? '') === 'rate-worker') {
    $pdo->exec("USE `$schema`");
    echo is_rate_limited('parallel-synthetic', 5, 900) ? 'blocked' : 'allowed';
    exit;
}
if (($argv[1] ?? '') === 'reset-worker') {
    $pdo->exec("USE `$schema`");
    echo consume_password_reset(str_repeat('d',64), 'Synthetic-new-password-2') ? 'consumed' : 'rejected';
    exit;
}
if (($argv[1] ?? '') === 'change-worker') {
    $pdo->exec("USE `$schema`");
    echo change_account_password(2, 'Synthetic-old-password', 'Synthetic-concurrent-password') ? 'changed' : 'rejected';
    exit;
}
if (($argv[1] ?? '') === 'release-worker') {
    $pdo->exec("USE `$schema`");
    require __DIR__ . '/../../NannyApp.Shared/config/booking_release.php';
    echo release_stale_booking($pdo, 90, 48) ? 'released' : 'skipped';
    exit;
}
$pdo->exec("CREATE DATABASE `$schema`");
$pdo->exec("USE `$schema`");
try {
    $pdo->exec("CREATE TABLE users (id INT PRIMARY KEY, full_name VARCHAR(100), email VARCHAR(100), role VARCHAR(20), status VARCHAR(20), email_verified INT, password_hash VARCHAR(255), remember_token VARCHAR(64), verification_token VARCHAR(64), verification_sent_at DATETIME, profile_image VARCHAR(255)) ENGINE=InnoDB");
    $pdo->exec("CREATE TABLE nanny_profiles (user_id INT PRIMARY KEY, photo_url VARCHAR(255), banner_image VARCHAR(255)) ENGINE=InnoDB");
    $pdo->exec("CREATE TABLE nanny_portfolio (id INT PRIMARY KEY, nanny_id INT, file_path VARCHAR(255)) ENGINE=InnoDB");
    $pdo->exec("CREATE TABLE password_resets (id INT AUTO_INCREMENT PRIMARY KEY, user_id INT, token VARCHAR(64) UNIQUE, used INT DEFAULT 0, expires_at DATETIME) ENGINE=InnoDB");
    $pdo->exec("CREATE TABLE api_tokens (id INT AUTO_INCREMENT PRIMARY KEY, user_id INT, token_hash VARCHAR(64)) ENGINE=InnoDB");
    $pdo->exec(file_get_contents(__DIR__ . '/../../NannyApp.Shared/database/migrate_v6_security.sql'));
    $hash = password_hash('Synthetic-old-password', PASSWORD_DEFAULT);
    foreach ([1=>'nanny',2=>'parent',3=>'admin',4=>'nanny'] as $id=>$role) {
        $pdo->prepare('INSERT INTO users (id,full_name,email,role,status,email_verified,password_hash,remember_token,profile_image) VALUES (?,?,?,?,?,?,?,?,?)')->execute([$id,'Synthetic User',"user$id@example.invalid",$role,'active',$id===4?0:1,$hash,hash('sha256',"synthetic-remember-$id"),"avatars/$id.jpg"]);
    }
    $jobs=[];
    for ($i=0;$i<4;$i++) {
        $pipes=[];
        $process=proc_open([PHP_BINARY,__FILE__,'change-worker',$schema],[1=>['pipe','w'],2=>['pipe','w']],$pipes);
        $jobs[]=[$process,$pipes];
    }
    $changed=0;
    foreach ($jobs as [$process,$pipes]) {
        $out=stream_get_contents($pipes[1]); $err=stream_get_contents($pipes[2]);
        foreach ($pipes as $pipe) fclose($pipe);
        if (proc_close($process)!==0) throw new RuntimeException($err);
        if ($out==='changed') $changed++;
    }
    check($changed===1,'parallel password changes recheck old password under lock');
    $pdo->prepare('UPDATE users SET password_hash=? WHERE id=2')->execute([$hash]);
    $pdo->exec("INSERT INTO nanny_portfolio VALUES (1,1,'uploads/docs/synthetic.pdf'),(2,1,'portfolio/synthetic.jpg')");
    $users = $pdo->query('SELECT * FROM users ORDER BY id')->fetchAll();
    foreach (['uploads/docs/synthetic.pdf','portfolio/synthetic.jpg'] as $path) {
        check(!media_access_allowed($path,null),'anonymous document denied');
        check(!media_access_allowed($path,$users[1]),'unrelated document denied');
        check(media_access_allowed($path,$users[0]),'owner document allowed');
        check(media_access_allowed($path,$users[2]),'admin document allowed');
        check(!media_access_allowed($path,$users[3]),'unverified document denied');
    }
    check(media_access_allowed('avatars/1.jpg',null),'public referenced avatar');
    check(!media_access_allowed('uploads/docs/orphan.pdf',$users[2]),'unknown document denied');
    foreach (['../avatars/1.jpg','/avatars/1.jpg','avatars\\1.jpg',"avatars/1.jpg\0"] as $path) check(!media_access_allowed($path,$users[0]),'invalid path denied');
    check(!verified_active_user($users[3]),'unverified bearer policy');
    check(!verified_active_user(array_replace($users[0],['status'=>'suspended'])),'suspended policy');
    for ($i=0;$i<5;$i++) { $_SESSION=[]; check(!is_rate_limited('cookieless',5,900),'cookieless reservation'); }
    $_SESSION=[];
    check(is_rate_limited('cookieless',5,900),'session rotation cannot reset limiter');
    $pdo->exec('UPDATE security_rate_limits SET resets_at = 0');
    check(!is_rate_limited('cookieless',5,900),'expired counter permits retry');
    $jobs=[];
    for($i=0;$i<12;$i++) {
        $pipes=[];
        $proc=proc_open([PHP_BINARY,__FILE__,'rate-worker',$schema],[1=>['pipe','w'],2=>['pipe','w']],$pipes);
        $jobs[]=[$proc,$pipes];
    }
    $allowed=0;
    foreach($jobs as [$proc,$pipes]) { $out=stream_get_contents($pipes[1]); $err=stream_get_contents($pipes[2]); foreach($pipes as $pipe) fclose($pipe); if(proc_close($proc)!==0) throw new RuntimeException($err); if($out==='allowed') $allowed++; }
    check($allowed===5,'parallel independent PHP instances admit exactly five');
    $token=str_repeat('a',64);
    $pdo->prepare('INSERT INTO password_resets (user_id,token,expires_at) VALUES (1,?,DATE_ADD(NOW(),INTERVAL 1 HOUR))')->execute([$token]);
    $pdo->exec("INSERT INTO api_tokens (user_id,token_hash) VALUES (1,'synthetic-bearer')");
    check(consume_password_reset($token,'Synthetic-new-password'),'first reset accepted');
    check(!consume_password_reset($token,'Synthetic-reuse-password'),'cross-client reset reuse denied');
    check((int)$pdo->query('SELECT COUNT(*) FROM api_tokens')->fetchColumn()===0,'bearer tokens revoked');
    check($pdo->query('SELECT remember_token FROM users WHERE id=1')->fetchColumn()===null,'remember token revoked');
    $newHash=$pdo->query('SELECT password_hash FROM users WHERE id=1')->fetchColumn();
    check(password_verify('Synthetic-new-password',$newHash),'password updated');
    check(!hash_equals(hash('sha256',$newHash),hash('sha256',$hash)),'existing web session fingerprint revoked');
    $pdo->exec("INSERT INTO password_resets (user_id,token,expires_at) VALUES (1,REPEAT('b',64),DATE_SUB(NOW(),INTERVAL 1 SECOND))");
    check(!consume_password_reset(str_repeat('b',64),'Synthetic-password'),'expired reset rejected');
    $pdo->exec("INSERT INTO password_resets (user_id,token,expires_at) VALUES (1,REPEAT('d',64),DATE_ADD(NOW(),INTERVAL 1 HOUR))");
    $jobs=[];
    for($i=0;$i<6;$i++) { $pipes=[]; $proc=proc_open([PHP_BINARY,__FILE__,'reset-worker',$schema],[1=>['pipe','w'],2=>['pipe','w']],$pipes); $jobs[]=[$proc,$pipes]; }
    $consumed=0;
    foreach($jobs as [$proc,$pipes]) { $out=stream_get_contents($pipes[1]); $err=stream_get_contents($pipes[2]); foreach($pipes as $pipe) fclose($pipe); if(proc_close($proc)!==0) throw new RuntimeException($err); if($out==='consumed') $consumed++; }
    check($consumed===1,'parallel resets consume token exactly once');
    $pdo->exec("UPDATE users SET verification_token=REPEAT('c',64),verification_sent_at=DATE_SUB(NOW(),INTERVAL 25 HOUR) WHERE id=4");
    check(!consume_verification_token(str_repeat('c',64)),'expired verification denied');
    $pdo->exec('UPDATE users SET verification_sent_at=NOW() WHERE id=4');
    check(consume_verification_token(str_repeat('c',64)),'fresh verification accepted');
    check(!consume_verification_token(str_repeat('c',64)),'verification reuse denied');
    require __DIR__ . '/../../NannyApp.Shared/config/email.php';
    putenv('NANNYAPP_BASE_URL=https://synthetic.example.invalid/web');
    check(account_email_link('reset.php','abc')==='https://synthetic.example.invalid/web/auth/reset.php?token=abc','absolute configured mail link');
    putenv('NANNYAPP_MAIL_TRANSPORT=disabled');
    check(!send_password_reset_email(1,'user1@example.invalid','Synthetic'),'delivery failure reported');
    check((int)$pdo->query('SELECT COUNT(*) FROM password_resets WHERE used=0 AND expires_at>NOW()')->fetchColumn()===0,'failed reset delivery removes token');
    $pdo->exec('UPDATE users SET email_verified=0 WHERE id=4');
    check(!send_verification_email(4,'user4@example.invalid','Synthetic'),'verification delivery failure reported');
    check($pdo->query('SELECT verification_token FROM users WHERE id=4')->fetchColumn()===null,'failed verification removes token');


    if ($smtpPort=getenv('NANNYAPP_TEST_SMTP_PORT')) {
        ini_set('SMTP','127.0.0.1'); ini_set('smtp_port',$smtpPort);
        putenv('NANNYAPP_MAIL_TRANSPORT=mail'); putenv('NANNYAPP_MAIL_FROM=test@example.invalid');
        check(send_password_reset_email(1,'user1@example.invalid','Synthetic'),'reset delivery retries successfully');
        check(send_verification_email(4,'user4@example.invalid','Synthetic'),'verification delivery retries successfully');
        $deliveredToken=$pdo->query('SELECT verification_token FROM users WHERE id=4')->fetchColumn();
        check(strlen($deliveredToken)===64,'delivered verification token retained');
    }

    // Minimal booking/review fixtures for real API authorization regression.
    $pdo->exec("CREATE TABLE bookings (id INT PRIMARY KEY, parent_id INT, nanny_id INT, status VARCHAR(20)) ENGINE=InnoDB");
    $pdo->exec("CREATE TABLE reviews (id INT AUTO_INCREMENT PRIMARY KEY, booking_id INT UNIQUE, reviewer_id INT, nanny_id INT, rating INT, comment TEXT) ENGINE=InnoDB");
    $pdo->exec("ALTER TABLE nanny_profiles ADD average_rating DECIMAL(3,2) DEFAULT 0");
    $pdo->exec("INSERT INTO nanny_profiles (user_id) VALUES (1),(4)");
    $pdo->exec("INSERT INTO bookings VALUES (10,2,1,'completed'),(11,2,1,'pending'),(12,999,1,'completed')");

    $pdo->exec(file_get_contents(__DIR__ . '/../../NannyApp.Shared/database/migrate_v8_operations.sql'));
    require __DIR__ . '/../../NannyApp.Shared/config/booking_release.php';
    $pdo->exec('ALTER TABLE bookings ADD checked_out_at DATETIME, ADD parent_confirmed_at DATETIME');
    $pdo->exec('CREATE TABLE payments (booking_id INT PRIMARY KEY, status VARCHAR(20), payout_status VARCHAR(20), released_at DATETIME) ENGINE=InnoDB');
    $pdo->exec("INSERT INTO bookings (id,status,checked_out_at) VALUES (90,'in_progress',DATE_SUB(NOW(),INTERVAL 49 HOUR)),(91,'disputed',DATE_SUB(NOW(),INTERVAL 49 HOUR)),(92,'in_progress',NOW()),(93,'in_progress',DATE_SUB(NOW(),INTERVAL 49 HOUR))");
    $pdo->exec("INSERT INTO payments (booking_id,status,payout_status) VALUES (90,'paid','held'),(91,'paid','held'),(92,'paid','held'),(93,'paid','held')");
    $pdo->exec('UPDATE bookings SET nanny_id=1,parent_id=2 WHERE id>=90');
    check(!release_stale_booking($pdo,91,48),'stale candidate now disputed cannot release payment');
    check(!release_stale_booking($pdo,92,48),'release rechecks grace deadline');
    check((int)$pdo->query("SELECT COUNT(*) FROM payments WHERE booking_id IN (91,92) AND payout_status='held'")->fetchColumn()===2,'ineligible payments remain held');
    $jobs=[];
    for ($i=0;$i<4;$i++) {
        $pipes=[];
        $process=proc_open([PHP_BINARY,__FILE__,'release-worker',$schema],[1=>['pipe','w'],2=>['pipe','w']],$pipes);
        $jobs[]=[$process,$pipes];
    }
    $released=0;
    foreach ($jobs as [$process,$pipes]) {
        $out=stream_get_contents($pipes[1]); $err=stream_get_contents($pipes[2]);
        foreach ($pipes as $pipe) fclose($pipe);
        if (proc_close($process)!==0) throw new RuntimeException($err);
        if ($out==='released') $released++;
    }
    check($released===1,'parallel automatic release completes exactly once');
    check($pdo->query("SELECT payout_status FROM payments WHERE booking_id=90")->fetchColumn()==='released','successful completion releases held ledger');
    $pdo->exec("CREATE TRIGGER fail_release BEFORE UPDATE ON payments FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='synthetic write failure'");
    try { release_stale_booking($pdo,93,48); check(false,'release should fail'); } catch (PDOException $e) {}
    check(!$pdo->inTransaction(),'failed release cleans up transaction');
    check($pdo->query('SELECT status FROM bookings WHERE id=93')->fetchColumn()==='in_progress','ledger failure rolls back booking completion');
    check($pdo->query('SELECT parent_confirmed_at FROM bookings WHERE id=93')->fetchColumn()===null,'ledger failure rolls back confirmation timestamp');
    $pdo->exec('DROP TRIGGER fail_release');
    $pdo->exec('DELETE FROM bookings WHERE id>=90');
    $pdo->exec('DROP TABLE payments');

    // Exercise the real HTTP controllers, using only this generated schema/storage.
    $pdo->exec('ALTER TABLE api_tokens ADD expires_at DATETIME, ADD last_used_at DATETIME, ADD device_info VARCHAR(255)');
    $pdo->exec("ALTER TABLE users MODIFY id INT AUTO_INCREMENT, MODIFY status VARCHAR(20) DEFAULT 'active'");
    $pdo->exec('CREATE TABLE parent_profiles (user_id INT PRIMARY KEY, emergency_contact VARCHAR(100), emergency_contact_name VARCHAR(100), emergency_contact_relationship VARCHAR(100), number_of_children INT)');
    $pdo->exec('ALTER TABLE users ADD phone VARCHAR(40), ADD date_of_birth DATE, ADD address VARCHAR(255), ADD gender VARCHAR(30), ADD created_at DATETIME');
    $pdo->exec("ALTER TABLE nanny_portfolio ADD type VARCHAR(30) DEFAULT 'other', ADD title VARCHAR(100) DEFAULT 'Synthetic', ADD admin_verified INT DEFAULT 0, ADD created_at DATETIME");
    $pdo->exec('DELETE FROM security_rate_limits');
    foreach ([1,2,3,4] as $id) $pdo->prepare('INSERT INTO api_tokens (user_id,token_hash,expires_at) VALUES (?,?,DATE_ADD(NOW(),INTERVAL 1 HOUR))')->execute([$id,hash('sha256',"synthetic-token-$id")]);
    $storage=sys_get_temp_dir().'/'.$schema;
    mkdir($storage.'/uploads/docs',0700,true);
    file_put_contents($storage.'/uploads/docs/synthetic.pdf', '%PDF synthetic-only');
    putenv("NANNYAPP_DB_NAME=$schema"); putenv("NANNYAPP_DB_PORT=$port"); putenv('NANNYAPP_DB_HOST=127.0.0.1'); putenv('NANNYAPP_DB_USER=root'); putenv('NANNYAPP_DB_PASS=');
    putenv("NANNYAPP_TEST_STORAGE=$storage"); putenv('NANNYAPP_BASE_URL=http://127.0.0.1:13381/web');
    $serverPipes=[];
    $serverCommand=[PHP_BINARY,'-d','SMTP=127.0.0.1','-d','smtp_port='.(getenv('NANNYAPP_TEST_SMTP_PORT') ?: '25')];
    if (getenv('NANNYAPP_TEST_SENDMAIL')) array_push($serverCommand,'-d','sendmail_path='.getenv('NANNYAPP_TEST_SENDMAIL'));
    array_push($serverCommand,'-S','127.0.0.1:13381',__DIR__.'/router.php');
    $server=proc_open($serverCommand,[1=>['pipe','w'],2=>['pipe','w']],$serverPipes);
    function request_test(string $path, ?array $body=null, string $auth='', string $cookie='', string $remember='', bool $form=false): array {
        $headers=$form ? "Content-Type: application/x-www-form-urlencoded\r\n" : "Content-Type: application/json\r\n";
        if($auth!=='') $headers.="Authorization: Bearer $auth\r\n";
        if($cookie!=='') $headers.="Cookie: PHPSESSID=$cookie\r\n";
        if($remember!=='') $headers.="Cookie: na_remember=$remember\r\n";
        $ctx=stream_context_create(['http'=>['method'=>$body===null?'GET':'POST','header'=>$headers,'content'=>$body===null?'':($form?http_build_query($body):json_encode($body)),'ignore_errors'=>true,'follow_location'=>0,'timeout'=>5]]);
        $data=@file_get_contents('http://127.0.0.1:13381'.$path,false,$ctx);
        preg_match('/\s(\d{3})\s/',$http_response_header[0]??'',$match);
        return [(int)($match[1]??0),$data,$http_response_header??[]];
    }
    try {
        for($i=0;$i<50;$i++){ if(request_test('/ready')[0]===200) break; usleep(100000); }

        $review = ['bookingId'=>10,'nannyId'=>4,'rating'=>5,'comment'=>'Synthetic review'];
        check(request_test('/api/reviews/create.php',$review,'synthetic-token-2')[0]===409,'review rejects mismatched nanny');
        check((int)$pdo->query('SELECT COUNT(*) FROM reviews')->fetchColumn()===0,'mismatched review creates no record');
        check((float)$pdo->query('SELECT average_rating FROM nanny_profiles WHERE user_id=4')->fetchColumn()===0.0,'unrelated nanny rating unchanged');
        $review['nannyId']=1;
        foreach ([0,6,2.5,[],null] as $badRating) {
            check(request_test('/api/reviews/create.php',array_replace($review,['rating'=>$badRating]),'synthetic-token-2')[0]===400,'review rejects invalid rating');
        }
        foreach ([11,12] as $ineligibleBooking) {
            check(request_test('/api/reviews/create.php',array_replace($review,['bookingId'=>$ineligibleBooking]),'synthetic-token-2')[0]===409,'review requires own completed booking');
        }
        check(request_test('/api/reviews/create.php',$review,'synthetic-token-2')[0]===200,'review accepts assigned nanny');
        check((int)$pdo->query('SELECT nanny_id FROM reviews WHERE booking_id=10')->fetchColumn()===1,'review persists server-derived nanny');
        check((float)$pdo->query('SELECT average_rating FROM nanny_profiles WHERE user_id=1')->fetchColumn()===5.0,'assigned nanny rating updated');
        check(request_test('/api/reviews/create.php',$review,'synthetic-token-2')[0]===409,'duplicate review rejected');

        if (getenv('NANNYAPP_TEST_SMTP_PORT')) {
            $registered=request_test('/api/auth/register.php',['email'=>'signup@example.invalid','fullName'=>'Synthetic Signup','phone'=>'0000000000','password'=>'Synthetic-password','role'=>'parent']);
            check($registered[0]===200,'real registration dispatches mail');
            $registeredData=json_decode($registered[1],true)['data'];
            check(request_test('/api/user/profile.php',null,$registeredData['token'])[0]===403,'new registration token cannot access profile');
            check(request_test('/api/auth/resend-verification.php',['email'=>'signup@example.invalid'])[0]===200,'real resend dispatches mail');
            check(request_test('/api/auth/forgot.php',['email'=>'signup@example.invalid'])[0]===200,'real forgot dispatches mail');
            $verify=$pdo->query("SELECT verification_token FROM users WHERE email='signup@example.invalid'")->fetchColumn();
            check(request_test('/api/auth/verify-email.php',['token'=>$verify])[0]===200,'delivered verification token completes through API');
        }

        foreach(['/api/bookings/list.php','/api/messages/conversations.php','/api/user/profile.php'] as $route) {
            // Use only endpoints present in this checkout.
            if (!is_file(dirname(__DIR__,2).'/NannyApp.Mobile/NannyApp'.$route)) continue;
            check(request_test($route,null,'synthetic-token-4')[0]===403,'real protected endpoint rejects unverified user');
        }
        check(request_test('/api/nannies/portfolio.php?nanny_id=1',null,'synthetic-token-2')[0]===403,'portfolio endpoint denies unrelated user');
        check(request_test('/api/nannies/portfolio.php?nanny_id=1',null,'synthetic-token-1')[0]===200,'portfolio endpoint allows owner');
        foreach([''=>403,'synthetic-token-2'=>403,'synthetic-token-1'=>200,'synthetic-token-3'=>200] as $auth=>$expected) {
            [$status,$body,$headers]=request_test('/api/media.php?f=uploads/docs/synthetic.pdf',null,$auth);
            check($status===$expected,'real local media authorization '.$expected);
            check((bool)array_filter($headers,fn($h)=>stripos($h,'Cache-Control: private, no-store')===0),'media no-store header');
        }
        foreach([''=>403,'synthetic-token-2'=>403,'synthetic-token-1'=>302,'synthetic-token-3'=>302] as $auth=>$expected) {
            [$status,$body,$headers]=request_test('/api/media.php?test_driver=s3&f=uploads/docs/synthetic.pdf',null,$auth);
            check($status===$expected,'S3 authorization before signing '.$expected);
            $locations=array_values(array_filter($headers,fn($h)=>stripos($h,'Location:')===0));
            if ($expected===302) {
                check(count($locations)===1 && str_contains($locations[0],'synthetic-s3.example.invalid') && str_contains($locations[0],'X-Amz-Expires=60'),'private short-lived S3 URL ignores public CDN');
            } else check(!$locations,'denied caller receives no signed URL');
        }
        $pdo->prepare('UPDATE users SET remember_token=? WHERE id=1')->execute([hash('sha256','synthetic-remember-1')]);
        check(request_test('/web/media.php?f=uploads/docs/synthetic.pdf',null,'','',hash('sha256','synthetic-remember-1'))[0]===200,'remember cookie works before recovery');
        $session=request_test('/session?id=1')[1];
        check(request_test('/web/media.php?f=uploads/docs/synthetic.pdf',null,'',$session)[0]===200,'web owner document access');
        check(request_test('/web/media.php?f=uploads/docs/synthetic.pdf')[0]===403,'web anonymous document denied');
        $pdo->exec("INSERT INTO password_resets (user_id,token,expires_at) VALUES (1,REPEAT('f',64),DATE_ADD(NOW(),INTERVAL 1 HOUR))");
        check(request_test('/api/auth/reset.php',['token'=>str_repeat('f',64),'newPassword'=>'Synthetic-HTTP-password'])[0]===200,'API reset succeeds');
        check(request_test('/web/media.php?f=uploads/docs/synthetic.pdf',null,'',$session)[0]===403,'active web session revoked by API reset');
        check(request_test('/api/nannies/portfolio.php?nanny_id=1',null,'synthetic-token-1')[0]===401,'old bearer rejected after recovery');
        check(request_test('/web/media.php?f=uploads/docs/synthetic.pdf',null,'','',hash('sha256','synthetic-remember-1'))[0]===403,'old remember cookie rejected after recovery');
        $webReset=request_test('/web/auth/reset.php?token='.str_repeat('f',64));
        check(str_contains($webReset[1] ?: '', 'invalid or has expired'),'web rejects API-consumed reset token');
        // Ordinary password changes must revoke every prior session and recovery link.
        $oldSession=request_test('/session?id=2')[1];
        $remember=hash('sha256','synthetic-password-change-remember');
        $pdo->prepare('UPDATE users SET remember_token=? WHERE id=2')->execute([$remember]);
        $pdo->exec("INSERT INTO password_resets (user_id,token,expires_at) VALUES (2,REPEAT('8',64),DATE_ADD(NOW(),INTERVAL 1 HOUR))");
        $change=['current_password'=>'Synthetic-old-password','new_password'=>'Synthetic-API-changed-password'];
        check(request_test('/api/user/change_password.php',null,'synthetic-token-2')[0]===405,'password change rejects GET');
        foreach ([[],null,str_repeat('x',73),"valid-length\0invalid"] as $badPassword) {
            check(request_test('/api/user/change_password.php',array_replace($change,['new_password'=>$badPassword]),'synthetic-token-2')[0]===400,'password change validates new-password shape/length');
        }
        check(request_test('/api/user/change_password.php',array_replace($change,['current_password'=>'wrong']),'synthetic-token-2')[0]===400,'wrong current password rejected');
        $result=request_test('/api/user/change_password.php',$change,'synthetic-token-2');
        check($result[0]===200 && json_decode($result[1])->data instanceof stdClass,'password change returns non-null Unit payload');
        check(request_test('/api/user/profile.php',null,'synthetic-token-2')[0]===401,'password change revokes old bearer');
        check(request_test('/web/account.php',null,'',$oldSession)[0]===302,'password change revokes existing web session');
        check(request_test('/web/account.php',null,'','',$remember)[0]===302,'password change revokes remember cookie');
        check(!consume_password_reset(str_repeat('8',64),'Synthetic-replay-password'),'password change invalidates outstanding reset link');
        check(password_verify($change['new_password'],$pdo->query('SELECT password_hash FROM users WHERE id=2')->fetchColumn()),'new password stored');
        $pdo->beginTransaction();
        check(!lock_current_credentials($pdo,2,$hash),'stale login snapshot cannot issue bearer after password change');
        $pdo->rollBack();
        check(!store_remember_token_if_current(2,$hash,hash('sha256','stale-cookie')),'stale login cannot recreate remember cookie');
        check(request_test('/api/auth/login.php',['email'=>'user2@example.invalid','password'=>'Synthetic-old-password'])[0]===401,'old password cannot sign in');
        check(request_test('/api/auth/login.php',['email'=>'user2@example.invalid','password'=>$change['new_password']])[0]===200,'new password can establish a fresh login');
        $pdo->exec('DELETE FROM api_tokens WHERE user_id=2');


        // A failed profile write must roll back the web password and revocations too.
        $details=json_decode(request_test('/session?id=2&details=1')[1],true);
        $otherSession=request_test('/session?id=2')[1];
        $pdo->prepare('UPDATE users SET remember_token=? WHERE id=2')->execute([$remember]);
        $pdo->prepare('INSERT INTO api_tokens (user_id,token_hash,expires_at) VALUES (2,?,DATE_ADD(NOW(),INTERVAL 1 HOUR))')->execute([hash('sha256','synthetic-web-change-token')]);
        $form=['csrf'=>$details['csrf'],'full_name'=>'Synthetic User','email'=>'user2@example.invalid',
            'current_password'=>$change['new_password'],'password'=>'Synthetic-Web-changed-password'];
        $beforeHash=$pdo->query('SELECT password_hash FROM users WHERE id=2')->fetchColumn();
        $pdo->exec("CREATE TRIGGER synthetic_profile_failure BEFORE INSERT ON parent_profiles FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Synthetic profile failure'");
        check(request_test('/web/account.php',$form,'',$details['session'],'',true)[0]===200,'web profile failure renders controlled error');
        check($pdo->query('SELECT password_hash FROM users WHERE id=2')->fetchColumn()===$beforeHash,'web profile failure rolls back password');
        check((int)$pdo->query('SELECT COUNT(*) FROM api_tokens WHERE user_id=2')->fetchColumn()===1,'web profile failure preserves old bearer');
        $pdo->exec('DROP TRIGGER synthetic_profile_failure');
        check(request_test('/web/account.php',$form,'',$details['session'],'',true)[0]===302,'web password change redirects to sign-in');
        check(password_verify($form['password'],$pdo->query('SELECT password_hash FROM users WHERE id=2')->fetchColumn()),'web password change stores new password');
        check(request_test('/api/user/profile.php',null,'synthetic-web-change-token')[0]===401,'web password change revokes bearer');
        check(request_test('/web/account.php',null,'',$otherSession)[0]===302,'web password change revokes other browser session');
        check(request_test('/web/account.php',null,'','',$remember)[0]===302,'web password change revokes remember cookie');

        for($i=0;$i<3;$i++) check(request_test('/api/auth/forgot.php',['email'=>'missing@example.invalid'])[0]===200,'cookieless recovery request accepted');
        check(request_test('/api/auth/forgot.php',['email'=>'missing@example.invalid'])[0]===429,'real cookieless recovery throttle');
        check(request_test('/api/auth/forgot.php')[0]===405,'recovery GET rejected');
    } finally {
        proc_terminate($server); foreach($serverPipes as $pipe) fclose($pipe); proc_close($server);
        unlink($storage.'/uploads/docs/synthetic.pdf'); rmdir($storage.'/uploads/docs'); rmdir($storage.'/uploads'); rmdir($storage);
    }

    $pdo->exec("INSERT INTO password_resets (user_id,token,expires_at) VALUES (1,REPEAT('9',64),DATE_ADD(NOW(),INTERVAL 1 HOUR))");
    $beforeHash=$pdo->query('SELECT password_hash FROM users WHERE id=1')->fetchColumn();
    $pdo->exec('DROP TABLE api_tokens');
    $changeHash=$pdo->query('SELECT password_hash FROM users WHERE id=2')->fetchColumn();
    try { change_account_password(2,'Synthetic-Web-changed-password','Synthetic-must-rollback'); throw new RuntimeException('Expected change revocation failure'); }
    catch (PDOException) {}
    check($pdo->query('SELECT password_hash FROM users WHERE id=2')->fetchColumn()===$changeHash,'ordinary password change rolls back when revocation fails');

    try { consume_password_reset(str_repeat('9',64),'Synthetic-rollback-password'); throw new RuntimeException('Expected revocation failure'); }
    catch (PDOException) {}
    check($pdo->query('SELECT password_hash FROM users WHERE id=1')->fetchColumn()===$beforeHash,'revocation failure rolls back password change');
    check((int)$pdo->query("SELECT used FROM password_resets WHERE token=REPEAT('9',64)")->fetchColumn()===0,'revocation failure leaves reset unconsumed');
    $pdo->exec('DROP TABLE security_rate_limits');
    check(is_rate_limited('unavailable'),'missing limiter fails closed');
    echo "Backend synthetic regression suite passed.\n";
} finally {
    $pdo->exec("DROP DATABASE `$schema`");
}
