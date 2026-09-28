<?php
declare(strict_types=1);
$port = getenv('NANNYAPP_TEST_DB_PORT') ?: '13379';
if ($port === '3306') throw new RuntimeException('Use a disposable test database port.');
$schema = 'nanny_mail_test_' . bin2hex(random_bytes(6));
$pdo = new PDO("mysql:host=127.0.0.1;port=$port;charset=utf8mb4", 'root', '', [PDO::ATTR_ERRMODE=>PDO::ERRMODE_EXCEPTION,PDO::ATTR_DEFAULT_FETCH_MODE=>PDO::FETCH_ASSOC,PDO::ATTR_EMULATE_PREPARES=>false]);
function db(): PDO { global $pdo; return $pdo; }
function expect(bool $ok, string $message): void { if (!$ok) throw new RuntimeException($message); echo "PASS $message\n"; }
require __DIR__ . '/../../NannyApp.Shared/config/email.php';
require __DIR__ . '/../../NannyApp.Shared/config/email_outbox.php';
putenv('NANNYAPP_MAIL_TRANSPORT=resend');
putenv('NANNYAPP_RESEND_API_KEY=synthetic-only');
putenv('NANNYAPP_MAIL_FROM=sender@example.invalid');
putenv('NANNYAPP_BASE_URL=https://synthetic.example.invalid');
$pdo->exec("CREATE DATABASE `$schema`");
$pdo->exec("USE `$schema`");
try {
    $pdo->exec('CREATE TABLE users (id INT PRIMARY KEY,email VARCHAR(255),email_verified INT DEFAULT 0,verification_token VARCHAR(64),verification_sent_at DATETIME) ENGINE=InnoDB');
    $pdo->exec('CREATE TABLE password_resets (id INT AUTO_INCREMENT PRIMARY KEY,user_id INT,token VARCHAR(64),used INT DEFAULT 0,expires_at DATETIME) ENGINE=InnoDB');
    $pdo->exec("INSERT INTO users(id,email) VALUES (1,'recipient@example.invalid')");
    $pdo->exec(file_get_contents(__DIR__.'/../../NannyApp.Shared/database/migrate_v7_email_outbox.sql'));
    $accepted = ['status'=>200,'body'=>'{"id":"49a3999c-0ce1-4ea6-ab68-afcd6dc2e794"}'];
    $fake = ['id'=>str_repeat('a',32),'payload'=>'{"text":"synthetic"}'];
    foreach ([0,408,409,429,500,503] as $status) {
        $result=resend_deliver($fake,fn()=>['status'=>$status,'body'=>'{"message":"secret must not escape"}','retry_after'=>'120']);
        expect($result['state']==='retry' && $result['delay']===120,'retryable HTTP '.$status);
        expect(!str_contains(json_encode($result),'secret'),'provider error body excluded');
    }
    foreach ([400,401,403,422,302] as $status) expect(resend_deliver($fake,fn()=>['status'=>$status,'body'=>'{}'])['state']==='failed','permanent HTTP '.$status);
    foreach (['null','{}','{"id":42}','{"id":"bad"}','not JSON',str_repeat('x',65537)] as $body) expect(resend_deliver($fake,fn()=>['status'=>200,'body'=>$body])['state']==='retry','malformed/oversized success is not accepted');
    expect(resend_deliver($fake,static function(){throw new RuntimeException('timeout');})['state']==='retry','transport exception retries safely');
    putenv('NANNYAPP_RESEND_API_KEY');
    expect(resend_deliver($fake,static function(){throw new RuntimeException('must not run');})['code']==='configuration','missing key never sends');
    putenv('NANNYAPP_RESEND_API_KEY=synthetic-only');
    expect(send_verification_email(1,'recipient@example.invalid','Synthetic'),'verification token and job committed');
    $job=claim_account_email($pdo);
    expect($job!==null && claim_account_email($pdo)===null,'second worker cannot claim a live lease');
    $requests=[];
    $provider=static function($body,$key) use (&$requests,$accepted) { $requests[]=[$body,$key]; return $accepted; };
    // Simulate provider acceptance followed by local process death: no acknowledgement write.
    expect(resend_deliver($job,$provider)['state']==='sent','provider accepted before simulated local crash');
    $pdo->prepare('UPDATE email_outbox SET lease_until=DATE_SUB(NOW(),INTERVAL 1 SECOND) WHERE id=?')->execute([$job['id']]);
    $retry=claim_account_email($pdo);
    expect(deliver_account_email($pdo,$retry,$provider)==='sent','abandoned lease recovers');
    expect($requests[0]===$requests[1],'partial success retry preserves exact payload and idempotency key');
    expect(deliver_account_email($pdo,$job,$provider)==='stale','stale worker cannot resend or acknowledge');
    expect($pdo->query('SELECT payload FROM email_outbox')->fetchColumn()===null,'accepted job scrubs recipient and link payload');
    expect(send_password_reset_email(1,'recipient@example.invalid','Synthetic'),'reset queued');
    $job=claim_account_email($pdo);
    expect(deliver_account_email($pdo,$job,fn()=>['status'=>429,'body'=>'{}','retry_after'=>'120'])==='pending','rate limited job rescheduled');
    expect(claim_account_email($pdo)===null,'retry waits instead of tight loop');
    $wait=(int)$pdo->query("SELECT TIMESTAMPDIFF(SECOND,NOW(),available_at) FROM email_outbox WHERE status='pending'")->fetchColumn();
    expect($wait>=119,'provider retry delay honored');
    $pdo->exec("UPDATE email_outbox SET available_at=NOW(),expires_at=DATE_SUB(NOW(),INTERVAL 1 SECOND) WHERE status='pending'");
    expect(claim_account_email($pdo)===null,'expired mail never sent');
    expect(send_verification_email(1,'recipient@example.invalid','Synthetic'),'first resend queued');
    $old=claim_account_email($pdo);
    expect(send_verification_email(1,'recipient@example.invalid','Synthetic'),'new resend replaces token');
    expect(deliver_account_email($pdo,$old,static function(){throw new RuntimeException('must not send obsolete token');})==='expired','superseded verification skipped');
    $new=claim_account_email($pdo);
    $pdo->prepare('UPDATE email_outbox SET attempts=8,lease_until=DATE_SUB(NOW(),INTERVAL 1 SECOND) WHERE id=?')->execute([$new['id']]);
    expect(claim_account_email($pdo)===null,'retry budget caps abandoned jobs');
    expect(!send_password_reset_email(1,'wrong@example.invalid','Synthetic'),'recipient must match server account');
    $before=$pdo->query('SELECT verification_token FROM users WHERE id=1')->fetchColumn();
    $pdo->exec('DROP TABLE email_outbox');
    expect(!send_verification_email(1,'recipient@example.invalid','Synthetic'),'queue database failure reported');
    expect($pdo->query('SELECT verification_token FROM users WHERE id=1')->fetchColumn()===$before,'enqueue failure rolls back token replacement');
    echo "Resend synthetic integration suite passed; no external requests.\n";
} finally { $pdo->exec("DROP DATABASE `$schema`"); }
