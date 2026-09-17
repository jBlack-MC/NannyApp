<?php
/** POST /api/children/create.php */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();
require_api_role($me, 'parent');

$b = json_body();
if (trim((string) ($b['name'] ?? '')) === '') json_error("Please enter the child's name.");

$stmt = db()->prepare(
    'INSERT INTO children (parent_id, name, age, gender, allergies, medical_conditions, special_needs, favourite_activities, notes_for_nannies)
     VALUES (:pid, :name, :age, :gender, :allergies, :medical, :special, :fav, :notes)'
);
$stmt->execute([
    'pid' => $me['id'], 'name' => $b['name'], 'age' => $b['age'] ?? null, 'gender' => $b['gender'] ?? null,
    'allergies' => $b['allergies'] ?? null, 'medical' => $b['medicalConditions'] ?? null,
    'special' => $b['specialNeeds'] ?? null, 'fav' => $b['favouriteActivities'] ?? null, 'notes' => $b['notesForNannies'] ?? null,
]);
$id = (int) db()->lastInsertId();

json_response(true, [
    'id' => $id, 'parentId' => (int) $me['id'], 'name' => $b['name'], 'age' => $b['age'] ?? null,
    'gender' => $b['gender'] ?? null, 'allergies' => $b['allergies'] ?? null,
    'medicalConditions' => $b['medicalConditions'] ?? null, 'specialNeeds' => $b['specialNeeds'] ?? null,
    'favouriteActivities' => $b['favouriteActivities'] ?? null, 'notesForNannies' => $b['notesForNannies'] ?? null,
]);
