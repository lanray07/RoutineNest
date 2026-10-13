"""Execute Room's exported initial schema against real SQLite, without an emulator.

These tests verify schema constraints and file durability, not the Android Room
repository or an upgrade migration. The Android reopen test remains device-gated.
"""
import json
from pathlib import Path
import sqlite3
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
SCHEMA = ROOT / "app/schemas/com.routinenest.app.data.NestDatabase/1.json"

class SchemaTests(unittest.TestCase):
    def setUp(self):
        self.schema = json.loads(SCHEMA.read_text(encoding="utf-8"))["database"]
        target = ROOT / "core/build/schema-tests"; target.mkdir(parents=True, exist_ok=True)
        handle = tempfile.NamedTemporaryFile(dir=target, prefix="routinenest_", suffix=".db", delete=False)
        self.path = Path(handle.name).resolve(); handle.close()
        self.path.relative_to(target.resolve())
        self.db = sqlite3.connect(self.path)
        self.db.execute("PRAGMA foreign_keys=ON")
        for entity in self.schema["entities"]:
            table = entity["tableName"]
            self.db.execute(entity["createSql"].replace("${TABLE_NAME}", table))
            for index in entity.get("indices", []):
                self.db.execute(index["createSql"].replace("${TABLE_NAME}", table))
        for sql in self.schema.get("setupQueries", []): self.db.execute(sql)
        self.db.execute(f'PRAGMA user_version={int(self.schema["version"])}')
        self.db.commit()

    def tearDown(self):
        self.db.close()
        for path in [self.path, Path(str(self.path)+"-journal"), Path(str(self.path)+"-wal"), Path(str(self.path)+"-shm")]:
            path.unlink(missing_ok=True)

    def routine(self, key="r"):
        self.db.execute("INSERT INTO routines(id,name,days,start,paused,archived,reminder) VALUES(?,?,?,?,?,?,?)", (key,"Morning","1,2,3,4,5,6,7","2026-10-13",0,0,"08:00"))
    def task(self, routine="r"):
        self.db.execute("INSERT INTO tasks(id,routineId,name,position,created,removed) VALUES(?,?,?,?,?,?)", ("t",routine,"Reading",0,"2026-10-13",None))
    def completion(self, date="2026-10-13"):
        self.db.execute("INSERT OR IGNORE INTO completions(taskId,routineId,day,taskName) VALUES(?,?,?,?)", ("t","r",date,"Reading"))

    def test_file_reopen_preserves_routine_task_completion_and_identity(self):
        self.routine(); self.task(); self.completion(); self.db.commit(); self.db.close()
        self.db = sqlite3.connect(self.path)
        self.assertEqual("Morning", self.db.execute("SELECT name FROM routines").fetchone()[0])
        self.assertEqual("Reading", self.db.execute("SELECT taskName FROM completions").fetchone()[0])
        self.assertEqual(1, self.db.execute("SELECT count(*) FROM tasks").fetchone()[0])
        self.assertEqual(self.schema["identityHash"], self.db.execute("SELECT identity_hash FROM room_master_table WHERE id=42").fetchone()[0])
        self.assertEqual("ok", self.db.execute("PRAGMA integrity_check").fetchone()[0])

    def test_completion_is_idempotent_per_task_day_and_undo_is_scoped(self):
        self.routine(); self.task(); self.completion(); self.completion(); self.completion("2026-10-14")
        self.assertEqual(2, self.db.execute("SELECT count(*) FROM completions").fetchone()[0])
        self.db.execute("DELETE FROM completions WHERE taskId=? AND day=?", ("t","2026-10-13"))
        self.assertEqual("2026-10-14", self.db.execute("SELECT day FROM completions").fetchone()[0])

    def test_foreign_key_failure_rolls_back_transaction(self):
        with self.assertRaises(sqlite3.IntegrityError):
            with self.db:
                self.routine("new")
                self.task("missing")
        self.assertEqual(0, self.db.execute("SELECT count(*) FROM routines").fetchone()[0])

    def test_routine_deletion_cascades_tasks_and_schedule_states(self):
        self.routine(); self.task()
        self.db.execute("INSERT INTO routine_states(routineId,effectiveDay,days,paused,archived) VALUES(?,?,?,?,?)", ("r","2026-10-13","1",0,0))
        self.db.execute("DELETE FROM routines WHERE id=?", ("r",))
        self.assertEqual(0, self.db.execute("SELECT count(*) FROM tasks").fetchone()[0])
        self.assertEqual(0, self.db.execute("SELECT count(*) FROM routine_states").fetchone()[0])

if __name__ == "__main__":
    import sys
    from test_support import record
    program = unittest.main(exit=False)
    record("exported Room schema on host SQLite", program.result)
    sys.exit(0 if program.result.wasSuccessful() else 1)
