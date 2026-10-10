"""Exercise the public endpoint with an isolated CMS stub (no real database)."""
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
function json_error($message, $status = 400) { json_response(false, null, $message, $status); }
class CmsError extends PDOException {
    function __construct($state) { $this->code = $state; }
}
function db() {
    return new class {
        function prepare($sql) {
            if ($_GET['mode'] === 'missing') throw new CmsError('42S02');
            if ($_GET['mode'] === 'failure') throw new CmsError('HY000');
            return new class {
                function execute($params) {}
                function fetch() {
                    return $_GET['mode'] === 'published'
                        ? ['page_key' => $_GET['key'], 'title' => 'CMS title', 'body' => 'Published content']
                        : false;
                }
            };
        }
    };
}
'''

with tempfile.TemporaryDirectory(prefix='nanny-public-content-') as temp:
    fixture = Path(temp)
    api = fixture / 'NannyApp.Mobile/NannyApp/api'
    (api / 'content').mkdir(parents=True)
    (fixture / 'NannyApp.Shared/content').mkdir(parents=True)
    shutil.copy(root / 'NannyApp.Mobile/NannyApp/api/content/page.php', api / 'content/page.php')
    shutil.copy(root / 'NannyApp.Shared/content/public-pages.json', fixture / 'NannyApp.Shared/content')
    (api / '_bootstrap.php').write_text(bootstrap, encoding='utf-8')

    def request(key, mode):
        command = '$_GET=' + "['key'=>" + json.dumps(key) + ",'mode'=>" + json.dumps(mode) + ']; require ' + json.dumps(str(api / 'content/page.php')) + ';'
        result = subprocess.run([php, '-r', command], capture_output=True, text=True, check=True)
        return json.loads(result.stdout)

    pages = json.loads((root / 'NannyApp.Shared/content/public-pages.json').read_text(encoding='utf-8'))
    for key in pages:
        for mode in ('empty', 'missing'):
            result = request(key, mode)
            assert result['success'] and result['data'] == pages[key], (key, mode)
            assert len(result['data']['body']) > 100, key
    assert request('about', 'published')['data']['body'] == 'Published content'
    assert request('about', 'failure')['status'] == 503
    assert request('../unknown', 'empty')['status'] == 404
print('Public content checks passed: nine pages, missing CMS, published overrides, failures and unknown keys.')
