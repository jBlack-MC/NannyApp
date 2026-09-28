"""Execute the actual Room DAO query text against synthetic in-memory SQLite rows."""
from pathlib import Path
import re
import sqlite3

root = Path(__file__).resolve().parents[2]
source = (root / 'NannyApp.Mobile/NannyApp/app/src/main/java/com/nannyapp/data/db/dao/Daos.kt').read_text()
db = sqlite3.connect(':memory:')
cases = [
    ('BookingDao', 'cached_booking', 'parentId INT, nannyId INT, dateTime TEXT',
     [(11, 101, 301, '2026-09-01'), (22, 202, 302, '2026-09-02')]),
    ('PaymentDao', 'cached_payment', 'accountId INT, createdAt TEXT',
     [(11, 101, '2026-09-01'), (22, 202, '2026-09-02')]),
    ('ChildDao', 'cached_child', 'parentId INT', [(11, 101), (22, 202)]),
    ('SavedNannyDao', 'cached_saved_nanny', 'accountId INT', [(11, 101), (22, 202)]),
    ('NotificationDao', 'cached_notification', 'accountId INT, createdAt TEXT, isRead INT',
     [(11, 101, '2026-09-01', 0), (22, 202, '2026-09-02', 0)]),
]
for dao, table, columns, rows in cases:
    db.execute(f'CREATE TABLE {table} (id INT PRIMARY KEY, {columns})')
    db.executemany(f'INSERT INTO {table} VALUES ({",".join("?" for _ in rows[0])})', rows)
    section = source.split(f'interface {dao} {{', 1)[1].split('\n}', 1)[0]
    query = re.search(r'@Query\("([^"]+)"\)\s+fun observe(?:ForAccount|ForParent)', section).group(1)
    assert [row[0] for row in db.execute(query, {'accountId': 202, 'parentId': 202})] == [22], dao
    assert not list(db.execute(query, {'accountId': 0, 'parentId': 0})), dao
    print(f'PASS {dao}: account B cannot read retained account A rows')
db.close()
