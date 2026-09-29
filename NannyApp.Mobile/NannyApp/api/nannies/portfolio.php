<?php
/** GET /api/nannies/portfolio.php?nanny_id=123
 *  POST /api/nannies/portfolio.php (multipart: type, title, file)
 *  DELETE /api/nannies/portfolio.php?id=456 */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();
$method = $_SERVER['REQUEST_METHOD'] ?? 'GET';

if ($method === 'GET') {
    $nannyId = (int) ($_GET['nanny_id'] ?? 0);
    if ($nannyId === 0) {
        require_api_role($me, 'nanny');
        $nannyId = (int) $me['id'];
    }
    if ((int) $me['id'] !== $nannyId && $me['role'] !== 'admin') json_error('Access denied.', 403);
    $stmt = db()->prepare('SELECT * FROM nanny_portfolio WHERE nanny_id = :id ORDER BY created_at DESC');
    $stmt->execute(['id' => $nannyId]);
    json_response(true, array_map(fn($r) => [
        'id' => (int) $r['id'], 'type' => $r['type'], 'title' => $r['title'], 'filePath' => media_url($r['file_path']),
        'adminVerified' => (bool) $r['admin_verified'], 'createdAt' => $r['created_at'],
    ], $stmt->fetchAll()));
}

if ($method === 'POST') {
    require_api_role($me, 'nanny');
    if (empty($_FILES['file'])) {
        json_error('No file uploaded.');
    }
    $type = $_POST['type'] ?? 'other';
    $title = $_POST['title'] ?? 'Document';
    if (!is_string($title) || trim($title)==='' || strlen($title)>150 || !in_array($type,['certificate','id','photo','reference','other'],true)) json_error('Invalid document title or type.',400);

    $result = save_uploaded_image($_FILES['file'], 'portfolio');
    if (!($result['ok'] ?? false)) {
        json_error($result['error'] ?? 'Could not upload file.', 400);
    }

    $stmt = db()->prepare(
        'INSERT INTO nanny_portfolio (nanny_id, type, title, file_path, admin_verified) VALUES (:nid, :type, :title, :path, 0)'
    );
    try { $stmt->execute(['nid' => $me['id'], 'type' => $type, 'title' => $title, 'path' => $result['path']]); }
    catch (Throwable $e) { storage_delete($result['path']); json_error('Could not save document.',503); }
    $id = (int) db()->lastInsertId();

    json_response(true, [
        'id' => $id, 'type' => $type, 'title' => $title, 'filePath' => $result['path'],
        'adminVerified' => false, 'createdAt' => date('Y-m-d H:i:s'),
    ]);
}

if ($method === 'DELETE') {
    require_api_role($me, 'nanny');
    $id = (int) ($_GET['id'] ?? 0);
    require_once __DIR__ . '/../../../../NannyApp.Shared/config/storage_jobs.php';
    try {
        if (!delete_portfolio_document(db(),(int)$me['id'],$id)) json_error('Document not found.',404);
    } catch (Throwable $e) { json_error('Could not delete document.',503); }
    json_response(true,(object)[]);
}

json_error('Method not allowed.', 405);
