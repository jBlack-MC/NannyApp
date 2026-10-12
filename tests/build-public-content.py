"""Bundle the public web copy for native offline reading. Run after editing pages."""
import html
from html.parser import HTMLParser
import json
from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
titles = dict(about='How it works', safety='Safety', pricing='Pricing', faq='Help centre',
              community='Community', resources='Resources', contact='Contact',
              terms='Terms', privacy='Privacy')


class PageText(HTMLParser):
    def __init__(self):
        super().__init__()
        self.parts = []
        self.hidden = 0

    def handle_starttag(self, tag, attrs):
        if tag in ('form', 'script', 'style'):
            self.hidden += 1
        if not self.hidden and tag in ('h1', 'h2', 'h3', 'p', 'li', 'br', 'div'):
            self.parts.append('\n\n')

    def handle_endtag(self, tag):
        if tag in ('form', 'script', 'style'):
            self.hidden = max(0, self.hidden - 1)
        if not self.hidden and tag in ('h1', 'h2', 'h3', 'p', 'li', 'div'):
            self.parts.append('\n\n')

    def handle_data(self, data):
        if not self.hidden:
            self.parts.append(data)


pages = {}
for key, title in titles.items():
    source = (root / 'NannyApp.Web/pages' / (key + '.php')).read_text(encoding='utf-8')
    # Live rate statistics require the database and cannot be part of a snapshot.
    if key == 'pricing':
        source = re.sub(r'<!-- Live rate.*?<!-- FAQ section -->', '<!-- FAQ section -->', source, flags=re.S | re.I)
    parser = PageText()
    parser.feed(re.sub(r'<\?(?:php|=).*?\?>', '', source, flags=re.S))
    body = '\n\n'.join(filter(None, (re.sub(r'\s+', ' ', p).strip()
                                    for p in ''.join(parser.parts).split('\n\n'))))
    if key == 'faq':
        entries = re.findall(r"'question'\s*=>\s*'((?:\\.|[^'])*)'.*?'answer'\s*=>\s*'((?:\\.|[^'])*)'", source, re.S)
        body = '\n\n'.join(q.replace("\\'", "'") + '\n' + a.replace("\\'", "'") for q, a in entries)
    assert body.strip(), key
    pages[key] = dict(pageKey=key, title=title, body=html.unescape(body))

target = root / 'NannyApp.Shared/content/public-pages.json'
target.parent.mkdir(parents=True, exist_ok=True)
target.write_text(json.dumps(pages, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print('Bundled', len(pages), 'public pages')
