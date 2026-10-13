"""Report release evidence without treating outstanding checks as successful."""
import hashlib
import json
from pathlib import Path
import sys
import xml.etree.ElementTree as ET
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
def main():
    checks=[]
    for size in [114,512]:
        path=ROOT/f"store-assets/identity/icon-light-{size}.png"
        ok=False
        if path.exists():
            with Image.open(path) as icon:
                ok=icon.size==(size,size) and icon.mode=="RGBA" and icon.getchannel("A").getextrema()[0]<255
        checks.append({"check":f"transparent icon {size}","passed":ok})
    for locale in ["en-GB","en-US"]:
        short=(ROOT/f"store-assets/metadata/{locale}/short-description.txt").read_text(encoding="utf-8").strip()
        full=(ROOT/f"store-assets/metadata/{locale}/full-description.txt").read_text(encoding="utf-8").strip()
        checks.append({"check":f"metadata limits {locale}","passed":len(short.encode())<=2000 and len(full)<=4000})
    tests=[]
    for path in ROOT.glob("**/build/test-results/**/TEST-*.xml"):
        root=ET.parse(path).getroot(); tests.append({"suite":root.get("name"),"tests":int(root.get("tests","0")),"failures":int(root.get("failures","0")),"errors":int(root.get("errors","0")),"path":str(path.relative_to(ROOT))})
    artifacts=[]
    for path in ROOT.glob("app/build/outputs/apk/**/*.apk"):
        artifacts.append({"path":str(path.relative_to(ROOT)),"bytes":path.stat().st_size,"sha256":hashlib.sha256(path.read_bytes()).hexdigest(),"signing":"unsigned" if "unsigned" in path.name else "debug key only; verify with apksigner"})
    lint=[]
    for variant in ["debug","release"]:
        path=ROOT/f"app/build/reports/lint-results-{variant}.xml"
        if path.exists():
            issues=list(ET.parse(path).getroot())
            lint.append({"variant":variant,"errors":sum(x.get("severity") in ("Error","Fatal") for x in issues),"warnings":sum(x.get("severity")=="Warning" for x in issues),"report":str(path.relative_to(ROOT))})
    translation=json.loads((ROOT/"localization/reports/translation-review.json").read_text(encoding="utf-8"))
    python_path=ROOT/"docs/python-test-evidence.json"
    python_tests=json.loads(python_path.read_text(encoding="utf-8")) if python_path.exists() else []
    report={"submission_ready":False,"checks":checks,"executed_unit_tests":tests,"executed_python_tests":python_tests,"lint":lint,"localization":{"published":[x["locale"] for x in translation if x["published"]],"pending_issues":sum(x["issues"] for x in translation)},"instrumentation":{"compiled":(ROOT/"app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk").exists(),"executed":False,"reason":"No connected booted device; emulator acceleration driver unavailable"},"artifacts":artifacts,"blockers":["Amazon product/app key absent; RVS server and verification client require implementation and integration tests","Release publisher signing/identity approval pending","Ten locales and listing translations pending; twelve-locale selection provisional","Physical Fire device testing pending","Real screenshots and visual/accessibility device verification pending","Privacy disclosures need publisher review and public policy URL"]}
    out=ROOT/"docs/release-evidence.json"; out.write_text(json.dumps(report,indent=2),encoding="utf-8")
    print(json.dumps(report,indent=2))
    return 1 if "--strict" in sys.argv or any(not x["passed"] for x in checks) else 0

if __name__=="__main__": sys.exit(main())
