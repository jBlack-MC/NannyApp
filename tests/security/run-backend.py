"""Run backend regressions with a loopback-only synthetic SMTP sink (no outbound mail)."""
import os
import socketserver
import subprocess
import sys
import threading
import shlex
from pathlib import Path

messages = []
class SMTP(socketserver.StreamRequestHandler):
    def handle(self):
        self.wfile.write(b"220 synthetic SMTP\r\n")
        recipients = []
        while line := self.rfile.readline():
            command = line.decode("ascii", errors="replace").strip()
            verb = command.split(" ", 1)[0].upper()
            if verb in ("EHLO", "HELO", "MAIL", "RSET"):
                self.wfile.write(b"250 OK\r\n")
            elif verb == "RCPT":
                if not command.lower().endswith("@example.invalid>"):
                    self.wfile.write(b"550 Synthetic recipients only\r\n")
                    continue
                recipients.append(command)
                self.wfile.write(b"250 OK\r\n")
            elif verb == "DATA":
                self.wfile.write(b"354 End with dot\r\n")
                body = bytearray()
                while (part := self.rfile.readline()) not in (b".\r\n", b"", b".\n"):
                    body.extend(part)
                messages.append((recipients, bytes(body)))
                self.wfile.write(b"250 Accepted\r\n")
            elif verb == "QUIT":
                self.wfile.write(b"221 Bye\r\n")
                return
            else:
                self.wfile.write(b"250 OK\r\n")

php = sys.argv[1] if len(sys.argv) > 1 else "php"
with socketserver.ThreadingTCPServer(("127.0.0.1", 0), SMTP) as server:
    threading.Thread(target=server.serve_forever, daemon=True).start()
    env = os.environ.copy()
    env["NANNYAPP_TEST_SMTP_PORT"] = str(server.server_address[1])
    command = [php]
    if os.name != 'nt':
        env['NANNYAPP_TEST_SENDMAIL'] = shlex.join([sys.executable, str(Path(__file__).with_name('sendmail-sink.py').resolve())])
        command += ['-d', 'sendmail_path=' + env['NANNYAPP_TEST_SENDMAIL']]
    result = subprocess.run(command + [str(Path(__file__).with_name("backend.php"))], env=env)
    server.shutdown()
    if result.returncode:
        sys.exit(result.returncode)
    assert len(messages) >= 5, "Expected synthetic signup, resend and reset deliveries"
    assert all(recipients for recipients, body in messages)
    assert any(b"/auth/verify-email.php?token=" in body for _, body in messages)
    assert any(b"/auth/reset.php?token=" in body for _, body in messages)
    print(f"PASS {len(messages)} synthetic messages captured in memory; no external delivery")
