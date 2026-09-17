<?php
/** GET /api/children/list.php */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();
require_api_role($me, 'parent');

$stmt = db()->prepare('SELECT * FROM children WHERE parent_id = :id ORDER BY created_at DESC');
$stmt->execute(['id' => $me['id']]);

json_response(true, array_map('serialize_child', $stmt->fetchAll()));

function serialize_child(array $c): array {
    return [
        'id' => (int) $c['id'], 'parentId' => (int) $c['parent_id'], 'name' => $c['name'],
        'age' => $c['age'] !== null ? (int) $c['age'] : null, 'gender' => $c['gender'],
        'allergies' => $c['allergies'], 'medicalConditions' => $c['medical_conditions'],
        'specialNeeds' => $c['special_needs'], 'favouriteActivities' => $c['favourite_activities'],
        'notesForNannies' => $c['notes_for_nannies'],
    ];
}
