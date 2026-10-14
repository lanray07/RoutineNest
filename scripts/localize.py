"""Extract, translate, validate and publish reviewed Android string resources.

Provider: LibreTranslate-compatible POST /translate endpoint from environment.
No credentials: produce missing/review reports without invented translations.
"""
import argparse
import collections
import csv
import json
import os
from pathlib import Path
import re
import sys
import urllib.request
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app/src/main/res"
CONFIG = ROOT / "localization/config.json"
PLACEHOLDER = re.compile(r"%(?:\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[a-zA-Z%]")

def directory(tag):
    return "values-" + tag.split("-")[0] + "-r" + tag.split("-")[1]

def entries(path):
    if not path.exists():
        return {}
    result = {}
    for element in ET.parse(path).getroot():
        if element.tag == "string":
            result[element.attrib["name"]] = element.text or ""
        elif element.tag == "plurals":
            for item in element:
                result[f'{element.attrib["name"]}#{item.attrib["quantity"]}'] = item.text or ""
    return result

def valid(source, translated, glossary):
    errors = []
    if not translated.strip(): errors.append("empty")
    if collections.Counter(PLACEHOLDER.findall(source)) != collections.Counter(PLACEHOLDER.findall(translated)): errors.append("placeholders")
    for word in glossary:
        if source.count(word) != translated.count(word): errors.append("glossary:" + word)
    return errors

def write_resources(path, values):
    root = ET.Element("resources")
    plurals = {}
    for key, value in sorted(values.items()):
        # Android needs apostrophes escaped even though XML permits them.
        value = value.replace("\\'", "'").replace("'", "\\'")
        if "#" in key:
            name, quantity = key.split("#")
            parent = plurals.get(name)
            if parent is None:
                parent = ET.SubElement(root, "plurals", name=name); plurals[name] = parent
            ET.SubElement(parent, "item", quantity=quantity).text = value
        else:
            ET.SubElement(root, "string", name=key).text = value
    path.parent.mkdir(parents=True, exist_ok=True)
    ET.indent(root)
    ET.ElementTree(root).write(path, encoding="utf-8", xml_declaration=True)

def translate(text, target, protected):
    tokens = PLACEHOLDER.findall(text) + [x for x in protected if x in text]
    replacements = {}
    for index, token in enumerate(dict.fromkeys(tokens)):
        marker = f"RNPROTECTED{index}TOKEN"
        replacements[marker] = token
        text = text.replace(token, marker)
    body = json.dumps({"q":text,"source":"en","target":target.split("-")[0],"format":"text","api_key":os.environ["TRANSLATION_API_KEY"]}).encode()
    request = urllib.request.Request(os.environ["TRANSLATION_ENDPOINT"], data=body, headers={"Content-Type":"application/json"})
    with urllib.request.urlopen(request, timeout=30) as response:
        result = json.load(response)["translatedText"]
    for marker, token in replacements.items():
        if marker not in result: raise ValueError("Provider altered a protected token")
        result = result.replace(marker, token)
    return result

def run(do_translate=False, strict=False):
    config = json.loads(CONFIG.read_text(encoding="utf-8"))
    glossary = json.loads((ROOT / "localization/glossary.json").read_text())["protected"]
    source = entries(RES / "values/strings.xml")
    output = ROOT / "localization/reports"; output.mkdir(parents=True, exist_ok=True)
    drafts = ROOT / "localization/drafts"; drafts.mkdir(parents=True, exist_ok=True)
    rows, reviews, approved = [], [], []
    for locale in config["locales"]:
        tag = locale["tag"]
        path = RES / directory(tag) / "strings.xml"
        path.parent.mkdir(parents=True, exist_ok=True)
        draft = drafts / f"{tag}.json"
        values = json.loads(draft.read_text(encoding="utf-8")) if draft.exists() else entries(path)
        if tag.startswith("en-"): values = {k: (v.replace("personalise", "personalize") if tag == "en-US" else v) for k,v in source.items()}
        elif do_translate:
            if not os.getenv("TRANSLATION_API_KEY") or not os.getenv("TRANSLATION_ENDPOINT"):
                print(f"{tag}: provider configuration pending", file=sys.stderr)
            else:
                for key, text in source.items():
                    if key not in values:
                        try:
                            translated = translate(text, tag, glossary)
                            if valid(text, translated, glossary): raise ValueError("Translation validation failed")
                            values[key] = translated
                            draft.write_text(json.dumps(values, ensure_ascii=False, indent=2), encoding="utf-8")
                        except Exception as error:
                            rows.append([tag,key,"provider failure:" + str(error)])
        issues = []
        for key, text in source.items():
            if key not in values: issues.append([tag,key,"missing"]); continue
            errors = valid(text, values[key], glossary)
            if not tag.startswith("en-") and values[key] == text and text not in glossary and key not in locale.get("allowed_identical_keys", []): errors.append("untranslated")
            issues += [[tag,key,error] for error in errors]
        # Plural forms must include other and match locale CLDR quantities before publication.
        if any("#" in key for key in source):
            required = {"ar":{"zero","one","two","few","many","other"},"fr":{"one","many","other"},"es":{"one","many","other"},"it":{"one","many","other"},"pt":{"one","many","other"},"pl":{"one","few","many","other"},"en":{"one","other"},"ja":{"other"},"ko":{"other"},"zh":{"other"}}.get(tag.split("-")[0], {"one","other"})
            for name in {k.split("#")[0] for k in source if "#" in k}:
                if not required.issubset({k.split("#")[1] for k in values if k.startswith(name + "#")}): issues.append([tag,name,"plural categories pending CLDR review"])
        rows.extend(issues)
        publish = not issues and locale["reviewed"]
        if publish:
            write_resources(path, values); approved.append(locale)
            pending_note = output / f"{tag}-PENDING.md"
            if pending_note.exists(): pending_note.unlink()
        else:
            # Pending translations never enter the production resource set.
            if path.exists(): path.unlink()
            write_resources(ROOT / "localization/resources" / directory(tag) / "strings.xml", {})
            if path.parent.exists() and not any(path.parent.iterdir()): path.parent.rmdir()
            (output / f"{tag}-PENDING.md").write_text("Translations pending validation and human review. English fallback is not a completed translation.\n", encoding="utf-8")
        draft.write_text(json.dumps(values, ensure_ascii=False, indent=2), encoding="utf-8")
        reviews.append({"locale":tag,"published":publish,"reviewed":locale["reviewed"],"review_method":locale.get("review_method","existing review"),"native_review":locale.get("native_review","unspecified"),"issues":len(issues),"entries":len(values),"rtl":tag.startswith("ar"),"visual_review":"pending on real device"})
    with (output / "missing-strings.csv").open("w", newline="", encoding="utf-8") as file:
        writer = csv.writer(file); writer.writerow(["locale","key","issue"]); writer.writerows(rows)
    (output / "translation-review.json").write_text(json.dumps(reviews, indent=2), encoding="utf-8")
    generated = ROOT / "app/src/main/java/com/routinenest/app/data/ValidatedLocales.kt"
    generated.write_text('package com.routinenest.app.data\n\n// Generated by scripts/localize.py; reviewed, validated locales only.\nval validatedLocales = listOf(\n' + ',\n'.join('    ' + json.dumps(x["tag"]) + ' to ' + json.dumps(x["label"], ensure_ascii=False) for x in approved) + '\n)\n', encoding="utf-8")
    locale_xml = ET.Element("locale-config", {"xmlns:android":"http://schemas.android.com/apk/res/android"})
    for item in approved: ET.SubElement(locale_xml,"locale", {"android:name":item["tag"]})
    (RES / "xml").mkdir(exist_ok=True); ET.ElementTree(locale_xml).write(RES / "xml/locales_config.xml", encoding="utf-8", xml_declaration=True)
    print(f"{len(approved)} structurally validated locales; {len(rows)} pending translation issues. Native/device review status is recorded separately in localization/reports.")
    return 1 if strict and rows else 0

if __name__ == "__main__":
    parser = argparse.ArgumentParser(); parser.add_argument("--translate",action="store_true"); parser.add_argument("--strict",action="store_true")
    args = parser.parse_args(); sys.exit(run(args.translate,args.strict))
