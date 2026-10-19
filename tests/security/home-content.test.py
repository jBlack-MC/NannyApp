"""Exercise the public home-content endpoint with a stub database (no real database)."""
import json
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile

root = Path(__file__).resolve().parents[2]
php = sys.argv[1] if len(sys.argv) > 1 else 'php'
bootstrap = r'''<?php
function json_response($success, $data = null, $message = null, $status = 200) {
    echo json_encode(compact('success', 'data', 'message', 'status')); exit;
}
function log_exception($e, $event) {}
function web_root_url() { return 'https://example.test/'; }
function media_url($path) { return $path === null || $path === '' ? null : 'https://example.test/' . $path; }
function db() {
    return new class {
        function query($sql) {
            if ($_GET['mode'] === 'failure') throw new PDOException('down');
            $rows = str_contains($sql, 'AS parent_name')
                ? [['parent_name' => 'Thandi Mokoena', 'nanny_name' => 'Amelia Smith', 'comment' => 'Wonderful care', 'rating' => '5']]
                : [['id' => '7', 'full_name' => 'Amelia Smith', 'profile_image' => null, 'photo_url' => 'assets/img/people/nanny-1.jpg',
                    'location' => 'Durban', 'hourly_rate' => '120.00', 'average_rating' => '4.8', 'experience_years' => '6', 'review_count' => '3']];
            return new class($rows) {
                function __construct(private $rows) {}
                function fetchAll() { return $this->rows; }
            };
        }
    };
}
'''

with tempfile.TemporaryDirectory(prefix='nanny-home-content-') as temp:
    fixture = Path(temp)
    api = fixture / 'NannyApp.Mobile/NannyApp/api'
    (api / 'content').mkdir(parents=True)
    shutil.copy(root / 'NannyApp.Mobile/NannyApp/api/content/home.php', api / 'content/home.php')
    (api / '_bootstrap.php').write_text(bootstrap, encoding='utf-8')
    gallery = fixture / 'NannyApp.Web/assets/img/gallery'

    def request(mode):
        command = "$_GET=['mode'=>" + json.dumps(mode) + ']; require ' + json.dumps(str(api / 'content/home.php')) + ';'
        result = subprocess.run([php, '-r', command], capture_output=True, text=True, check=True)
        return json.loads(result.stdout)

    # Without a gallery folder the fixed home-page photos are used.
    data = request('ok')['data']
    assert data['galleryImages'][0] == 'https://example.test/assets/img/hero-nanny-SMceVYR7.jpg', data
    assert 'https://example.test/assets/img/download%20%281%29.jpg' in data['galleryImages'], data
    nanny = data['topNannies'][0]
    assert nanny == {'userId': 7, 'fullName': 'Amelia Smith', 'photoUrl': 'https://example.test/assets/img/people/nanny-1.jpg',
                     'location': 'Durban', 'hourlyRate': 120.0, 'averageRating': 4.8, 'reviewCount': 3, 'experienceYears': 6}, nanny
    # Signed-out visitors see first names only.
    assert data['testimonials'] == [{'parentName': 'Thandi', 'nannyName': 'Amelia', 'comment': 'Wonderful care', 'rating': 5}], data

    # A gallery folder supplies up to eight images, encoded, ignoring other files.
    gallery.mkdir(parents=True)
    for i in range(10):
        (gallery / f'photo {i}.jpg').write_bytes(b'x')
    (gallery / 'notes.txt').write_text('skip', encoding='utf-8')
    images = request('ok')['data']['galleryImages']
    assert len(images) == 8 and all(u.startswith('https://example.test/assets/img/gallery/photo%20') and u.endswith('.jpg') for u in images), images

    # A database failure still answers, with the gallery and empty lists.
    data = request('failure')
    assert data['success'] and data['data']['topNannies'] == [] and data['data']['testimonials'] == [] and len(data['data']['galleryImages']) == 8, data
print('Home content checks passed: default and folder galleries, nannies, first-name testimonials and database failures.')
