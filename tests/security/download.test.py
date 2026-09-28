"""Serve only a synthetic APK from a disposable loopback PHP server."""
import os, pathlib, subprocess, sys, tempfile, time, urllib.request, urllib.error
root = pathlib.Path(__file__).resolve().parents[2]
with tempfile.TemporaryDirectory(prefix='nanny-download-') as folder:
    apk = pathlib.Path(folder) / 'synthetic.apk'
    payload = b'SYNTHETIC-NOT-AN-INSTALLABLE-APK'
    apk.write_bytes(payload)
    env = dict(os.environ, NANNYAPP_APK_PATH=str(apk), NANNYAPP_BASE_URL='http://127.0.0.1:13386')
    process = subprocess.Popen([sys.argv[1], '-S', '127.0.0.1:13386', '-t', str(root / 'NannyApp.Web')], env=env, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    try:
        url = 'http://127.0.0.1:13386/download-app.php'
        for _ in range(60):
            if process.poll() is not None: raise RuntimeError('Test PHP server failed to start')
            try:
                with urllib.request.urlopen(url, timeout=1) as r:
                    assert r.status == 200
                    assert b'Download Android app' in r.read()
                break
            except urllib.error.URLError: time.sleep(.1)
        else: raise RuntimeError('Test PHP server did not become ready')
        with urllib.request.urlopen(url+'?platform=android', timeout=3) as r:
            assert r.headers['Content-Type'] == 'application/vnd.android.package-archive'
            assert r.headers['X-Content-Type-Options'] == 'nosniff'
            assert 'attachment' in r.headers['Content-Disposition']
            assert r.read() == payload
        apk.unlink()
        with urllib.request.urlopen(url, timeout=3) as r:
            assert r.status == 200
            assert b'not published yet' in r.read()
        try: urllib.request.urlopen(url+'?platform=android', timeout=3)
        except urllib.error.HTTPError as e: assert e.code == 503
        else: raise AssertionError('Missing APK must not return a fake download')
        print('PASS install page, exact synthetic download, headers, missing-release behavior')
    finally:
        process.terminate()
        process.wait(timeout=10)
