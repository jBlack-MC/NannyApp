"""Bundle the public web pages for the native app. Run after editing NannyApp.Web/pages.

    python tests/build-public-content.py [path/to/php]

Each page is rendered by PHP with stubbed includes (no database, no session), so
loops and helper output match what the website shows. The result keeps two views
of every page: `blocks`, the structure the app lays out like the website (eyebrow,
headings, cards, lists, and icons by Lucide name), and `body`, the same text flattened
for older app builds.
"""
import html
from html.parser import HTMLParser
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile

root = Path(__file__).resolve().parents[1]
php = shutil.which(sys.argv[1] if len(sys.argv) > 1 else 'php')
if not php:
    raise SystemExit('PHP executable not found; pass it as the first argument')

titles = dict(about='How it works', safety='Safety', pricing='Pricing', faq='Help centre',
              community='Community', resources='Resources', contact='Contact',
              terms='Terms', privacy='Privacy')

# Stands in for config/config.php: enough helpers to render, and a database that returns nothing.
STUB_CONFIG = r'''<?php
define('APP_NAME', 'Nanny-App');
function url($path = '') { return $path; }
function e($value) { return htmlspecialchars((string) $value, ENT_QUOTES, 'UTF-8'); }
function current_user() { return null; }
function csrf_field() { return ''; }
function verify_csrf() {}
function flash(...$args) {}
function redirect(...$args) { exit; }
// Only the icon's name matters here: the app draws its own vector for each name.
function icon($name, $class = '') { return '<svg data-icon="' . e($name) . '"></svg>'; }
function db() {
    return new class {
        function query($sql) { return $this; }
        function prepare($sql) { return $this; }
        function exec($sql) { return 0; }
        function execute($params = []) { return true; }
        function fetch() { return false; }
        function fetchAll() { return []; }
        function fetchColumn() { return false; }
    };
}
'''

VOID_TAGS = {'br', 'hr', 'img', 'input', 'meta', 'link', 'source'}
# Forms and call-to-action buttons belong to the website; the app has its own navigation.
SKIP_TAGS = {'form', 'script', 'style', 'svg'}
# stat-band shows live database counts and legal-meta prints today's date; a snapshot can carry neither.
SKIP_CLASSES = {'btn', 'faq-plus', 'stat-band', 'legal-meta', 'hero-cta', 'legal-actions', 'tip-more',
                'quote-mark', 'quote-avatar'}
CARD_CLASSES = {'card', 'faq-item', 'final-cta'}
TEXT_CLASSES = {'h-eyebrow': 'eyebrow', 'faq-q': 'h3', 'faq-a-inner': 'p', 'feature-ico': 'icon',
                'tip-head': 'icon', 'final-ico': 'icon', 'tip-cat': 'tag', 'price-tag': 'tag',
                'quote-stars': 'tag', 'price-group': 'price', 'quote-text': 'quote',
                'quote-name': 'h3', 'quote-meta': 'p'}
TEXT_TAGS = {'h1': 'h1', 'h2': 'h2', 'h3': 'h3', 'h4': 'h3', 'p': 'p', 'li': 'li',
             'summary': 'h3', 'address': 'p'}
LINE_BREAK = '\x00'
CONTAINER_TAGS = {'div', 'section', 'aside', 'article', 'ul', 'ol', 'details', 'main'}


class PageBlocks(HTMLParser):
    """Turns rendered page HTML into a flat list of blocks, with cards as one level of nesting."""

    def __init__(self):
        super().__init__()
        self.blocks = []
        self.card = None      # blocks of the card being filled, if any
        self.card_has_form = False
        self.stack = []       # (tag, kind) for every open element
        self.skipping = 0
        self.collecting = None  # block type whose text is being gathered
        self.text = []
        self.loose = []       # text that sits directly inside a container

    def emit(self, kind, text):
        # Source line wrapping is not content; only an explicit <br> starts a new line.
        lines = (re.sub(r'\s+', ' ', line).strip() for line in text.split(LINE_BREAK))
        text = '\n'.join(filter(None, lines))
        if kind == 'price':
            text = text.replace(' ', '')
        if text:
            (self.card if self.card is not None else self.blocks).append({'type': kind, 'text': text})

    def flush_loose(self):
        self.emit('p', ''.join(self.loose))
        self.loose = []

    def handle_starttag(self, tag, attrs):
        if tag in VOID_TAGS:
            if tag == 'br' and not self.skipping:
                (self.text if self.collecting else self.loose).append(LINE_BREAK)
            return
        # An icon tile carries the icon's name, which the app maps to its own vector.
        if tag == 'svg' and self.collecting == 'icon' and not self.skipping:
            self.text.append(dict(attrs).get('data-icon') or '')
        classes = set((dict(attrs).get('class') or '').split())
        kind = 'plain'
        # A link that opens a container's text is navigation ("All resources →"), not prose.
        lone_link = (tag == 'a' and not classes & CARD_CLASSES and not self.collecting
                     and not ''.join(self.loose).strip())
        if self.skipping or tag in SKIP_TAGS or classes & SKIP_CLASSES or lone_link:
            kind = 'skip'
            self.skipping += 1
            if tag == 'form':
                self.card_has_form = True
        elif not self.collecting:
            text_kind = next((TEXT_CLASSES[c] for c in TEXT_CLASSES if c in classes), None) or TEXT_TAGS.get(tag)
            if tag == 'li' and 'off' in classes:
                text_kind = 'li_off'
            if classes & CARD_CLASSES and self.card is None:
                self.flush_loose()
                kind, self.card, self.card_has_form = 'card', [], False
            elif text_kind:
                self.flush_loose()
                kind, self.collecting, self.text = 'text', text_kind, []
            elif tag in CONTAINER_TAGS:
                self.flush_loose()
        self.stack.append((tag, kind))

    def handle_endtag(self, tag):
        if tag in VOID_TAGS or not any(open_tag == tag for open_tag, _ in self.stack):
            return
        while self.stack:
            open_tag, kind = self.stack.pop()
            if kind == 'skip':
                self.skipping -= 1
            elif kind == 'text':
                self.emit(self.collecting, ''.join(self.text))
                self.collecting = None
            elif kind == 'card':
                self.flush_loose()
                card, self.card = self.card, None
                # A card built around a website form has nothing left to show in the app.
                if card and not self.card_has_form:
                    self.blocks.append({'type': 'card', 'blocks': card})
            elif open_tag in CONTAINER_TAGS and not self.collecting and not self.skipping:
                self.flush_loose()
            if open_tag == tag:
                break

    def handle_data(self, data):
        if not self.skipping:
            (self.text if self.collecting else self.loose).append(data)


def flatten(blocks):
    for block in blocks:
        if block['type'] == 'card':
            yield from flatten(block['blocks'])
        elif block['type'] != 'icon':  # icon names are not prose
            yield block['text']


pages = {}
with tempfile.TemporaryDirectory(prefix='nanny-public-pages-') as temp:
    fixture = Path(temp)
    for folder in ('pages', 'config', 'includes'):
        (fixture / folder).mkdir()
    (fixture / 'config/config.php').write_text(STUB_CONFIG, encoding='utf-8')
    for name in ('header.php', 'footer.php'):
        (fixture / 'includes' / name).write_text('', encoding='utf-8')

    for key, title in titles.items():
        source = (root / 'NannyApp.Web/pages' / (key + '.php')).read_text(encoding='utf-8')
        # Live rate statistics require the database and cannot be part of a snapshot.
        if key == 'pricing':
            source = re.sub(r'<!-- Live rate.*?<!-- FAQ section -->', '<!-- FAQ section -->', source, flags=re.S | re.I)
        page = fixture / 'pages' / (key + '.php')
        page.write_text(source, encoding='utf-8')
        command = "$_SERVER['REQUEST_METHOD']='GET'; require " + json.dumps(page.as_posix()) + ';'
        rendered = subprocess.run([php, '-d', 'display_errors=0', '-r', command],
                                  capture_output=True, encoding='utf-8', check=True).stdout
        parser = PageBlocks()
        parser.feed(rendered)
        parser.close()
        parser.flush_loose()
        body = '\n\n'.join(flatten(parser.blocks))
        assert len(body) > 100, key
        pages[key] = dict(pageKey=key, title=title, body=html.unescape(body), blocks=parser.blocks)

target = root / 'NannyApp.Shared/content/public-pages.json'
target.parent.mkdir(parents=True, exist_ok=True)
target.write_text(json.dumps(pages, ensure_ascii=False, indent=2) + '\n', encoding='utf-8', newline='\n')
print('Bundled', len(pages), 'public pages')
