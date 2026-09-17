<?php
/** PUT /api/children/update.php */
require_once __DIR__ . '/../_bootstrap.php';
$me = require_api_auth();
require_api_role($me, 'parent');

$b = json_body();
$id = (int) ($b['id'] ?? 0);

$stmt = db()->prepare('SELECT id FROM children WHERE id = :id AND parent_id = :pid');
$stmt->execute(['id' => $id, 'pid' => $me['id']]);
if (!$stmt->fetch()) json_error('Child not found.', 404);

db()->prepare(
    'UPDATE children SET name = :name, age = :age, gender = :gender, allergies = :allergies,
        medical_conditions = :medical, special_needs = :special, favourite_activities = :fav, notes_for_nannies = :notes
     WHERE id = :id'
)->execute([
    'name' => $b['name'], 'age' => $b['age'] ?? null, 'gender' => $b['gender'] ?? null,
    'allergies' => $b['allergies'] ?? null, 'medical' => $b['medicalConditions'] ?? null,
    'special' => $b['specialNeeds'] ?? null, 'fav' => $b['favouriteActivities'] ?? null,
    'notes' => $b['notesForNannies'] ?? null, 'id' => $id,
]);

json_response(true, [
    'id' => $id, 'parentId' => (int) $me['id'], 'name' => $b['name'], 'age' => $b['age'] ?? null,
    'gender' => $b['gender'] ?? null, 'allergies' => $b['allergies'] ?? null,
    'medicalConditions' => $b['medicalConditions'] ?? null, 'specialNeeds' => $b['specialNeeds'] ?? null,
    'favouriteActivities' => $b['favouriteActivities'] ?? null, 'notesForNannies' => $b['notesForNannies'] ?? null,
]);
