"""Prepare honest listing drafts; untranslated markets receive explicit pending files."""
import csv
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]

def main():
    config=json.loads((ROOT/"localization/config.json").read_text(encoding="utf-8"))
    concepts=json.loads((ROOT/"store-assets/screenshot-concepts.json").read_text(encoding="utf-8"))
    description="""Make room for a better day with RoutineNest, a daily routine planner and habit tracker designed for a calmer rhythm.

Create morning, evening and focus routines with your own steps. Choose daily or selected weekday schedules, tick off your checklist and undo a completion when plans change. Your routines and completion history stay on your device and work offline.

Start with a template or build a routine from scratch. See weekly progress and scheduled-day streaks. Pause or archive routines whenever you need space. Choose light, dark or system appearance and a layout that adapts to your tablet.

Use the assistant to create routines, add tasks, complete a step or read your checklist. Speech input requires a compatible speech service and microphone permission; equivalent text commands are available when recognition is unavailable. Commands currently support English. Optional spoken responses require a compatible text-to-speech engine.

Set reminders to help you return to your routine. Notification permission and device battery settings affect delivery, so reminders are not guaranteed exact alarms.

The free plan includes up to three active routines, checklists, templates, weekly insights, reminders and the assistant. Premium is intended to unlock unlimited routines, duplication and monthly insights through a one-time Amazon purchase. Premium purchasing is not yet configured in this development build; the store price will be shown when available.

Export routine data to a file you control or delete it from the app. RoutineNest includes no advertising or analytics trackers. Your speech service and Amazon may process audio or purchase information respectively.

This listing is a pre-submission draft. Premium billing and Fire hardware validation must be completed before publication.
"""
    short="Daily routines, habit checklists, streaks and an English voice or text assistant. Plan morning and evening rhythms with on-device data, weekly progress and reminders."
    assert len(description)<=4000 and len(short.encode("utf-8"))<=2000
    keyword_rows=[]
    for locale in config["locales"]:
        tag=locale["tag"]; out=ROOT/"store-assets/metadata"/tag; out.mkdir(parents=True,exist_ok=True)
        if tag.startswith("en-"):
            files={"title.txt":"RoutineNest: Daily Routine Planner", "short-description.txt":short,"full-description.txt":description,"release-notes.txt":"Initial development version: routines, checklists, history, English assistant and tablet layouts. Billing verification and Fire testing pending.","features.txt":"Daily and weekly routines\nOn-device checklist and history\nWeekly progress and streaks\nVoice recognition where available; equivalent text commands\nLight and dark appearance\nConfigurable reminders\nData export and deletion"}
            for name,text in files.items(): (out/name).write_text(text+"\n",encoding="utf-8")
            (out/"screenshot-text.json").write_text(json.dumps(concepts,indent=2),encoding="utf-8")
            (out/"REVIEW.md").write_text("Pre-submission draft. Review factual feature claims after final device tests. Remove the development billing disclaimer only once genuine purchasing, restore and RVS have passed. Validate title in the live console; official docs give no numerical title cap. Proposed £2.99 one-time price needs publisher/market validation, not a hardcoded app price. Screenshots require real captures. No search-volume claims have been measured.\n",encoding="utf-8")
            for keyword in ["daily routine planner","habit tracker","morning routine","evening routine","daily checklist","offline habit tracker","routine reminders","voice routine planner"]:
                keyword_rows.append([tag,keyword,"title/description or screenshot copy where relevant","intent hypothesis; unmeasured","human review pending"])
        else:
            (out/"PENDING.md").write_text(f"# {tag} listing pending\n\nNo translation provider credentials or reviewed listing translation supplied. Translate English source title, short/full descriptions, features, release notes and screenshot-text.json, validate store limits and review natural search intent. Do not submit English files as completed localisation. Locale set is provisional.\n",encoding="utf-8")
            for filename in ["title.txt","short-description.txt","full-description.txt","features.txt","release-notes.txt"]:
                (out/(filename+".pending")).write_text("Translation and native-language review required.\n",encoding="utf-8")
            keyword_rows.append([tag,"pending native-language research","pending","no research metrics claimed","translation pending"])
    with (ROOT/"store-assets/ASO_KEYWORD_MATRIX.csv").open("w",encoding="utf-8",newline="") as file:
        writer=csv.writer(file); writer.writerow(["locale","search_intent","placement","evidence","review"]); writer.writerows(keyword_rows)
    print("English listing drafts validated; ten locale listing packages explicitly pending.")

if __name__=="__main__": main()
