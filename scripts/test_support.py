import datetime
import json
from pathlib import Path

def record(suite, result):
    root = Path(__file__).resolve().parents[1]
    path = root / "docs/python-test-evidence.json"
    reports = json.loads(path.read_text(encoding="utf-8")) if path.exists() else []
    reports = [value for value in reports if value["suite"] != suite]
    reports.append({"suite":suite,"tests":result.testsRun,"failures":len(result.failures),"errors":len(result.errors),"skipped":len(result.skipped),"executedAtUtc":datetime.datetime.now(datetime.timezone.utc).isoformat(),"failedCases":[str(test) for test,_ in result.failures + result.errors]})
    path.parent.mkdir(exist_ok=True)
    path.write_text(json.dumps(reports,indent=2),encoding="utf-8")
