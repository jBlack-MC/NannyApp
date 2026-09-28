"""Unix PHP mail() adapter: forwards synthetic messages only to the loopback test sink."""
import email.parser
import email.utils
import os
import smtplib
import sys

body = sys.stdin.read()
message = email.parser.Parser().parsestr(body)
recipients = [address for _, address in email.utils.getaddresses(message.get_all('To', []))]
assert recipients and all(address.endswith('@example.invalid') for address in recipients)
with smtplib.SMTP('127.0.0.1', int(os.environ['NANNYAPP_TEST_SMTP_PORT'])) as smtp:
    smtp.sendmail('test@example.invalid', recipients, body)
