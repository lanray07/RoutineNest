"""Capture real debug app screens and compose locale-approved marketing overlays.

Requires a booted device. Never synthesises an app screen when capture is absent.
"""
import argparse
import csv
import json
import os
from pathlib import Path
import subprocess
from PIL import Image, ImageDraw, features
from generate_assets import font

ROOT = Path(__file__).resolve().parents[1]
CONCEPTS = json.loads((ROOT / "store-assets/screenshot-concepts.json").read_text(encoding="utf-8"))
SIZES = {(800,480),(1024,600),(1280,800),(1920,1200),(1920,1080),(2560,1600),(2160,1440)}

def command(args, **kwargs): return subprocess.run(args, check=True, **kwargs)

def main():
    parser=argparse.ArgumentParser(); parser.add_argument("--locale",default="en-GB"); parser.add_argument("--device", choices=["phone","tablet"],default="tablet"); parser.add_argument("--serial"); parser.add_argument("--capture",action="store_true"); args=parser.parse_args()
    cfg=json.loads((ROOT/"localization/config.json").read_text(encoding="utf-8")); assert any(x["tag"]==args.locale and x["reviewed"] for x in cfg["locales"]),"Locale not approved"
    sdk=os.getenv("ANDROID_HOME",str(Path.home()/"AppData/Local/Android/Sdk")); adb=str(Path(sdk)/"platform-tools/adb.exe")
    prefix=[adb]+(["-s",args.serial] if args.serial else [])
    raw=ROOT/"store-assets/screenshots/raw"/args.locale/args.device; raw.mkdir(parents=True,exist_ok=True)
    if args.capture:
        devices=subprocess.check_output([adb,"devices"],text=True)
        if not any(line.endswith("\tdevice") for line in devices.splitlines()): raise SystemExit("No booted device. See docs/SCREENSHOT_CAPTURE.md. No screenshots fabricated.")
        command([str(ROOT/"gradlew.bat"),":app:installDebug",":app:installDebugAndroidTest"],cwd=ROOT)
        result=subprocess.check_output(prefix+["shell","am","instrument","-w","-e","class","com.routinenest.app.ScreenshotCaptureTest","-e","locale",args.locale,"com.routinenest.app.debug.test/androidx.test.runner.AndroidJUnitRunner"],text=True)
        print(result)
        if "OK (1 test)" not in result: raise SystemExit("Capture instrumentation failed")
        for concept in CONCEPTS: command(prefix+["pull",f'/sdcard/Android/data/com.routinenest.app.debug/files/screenshots/{args.locale}/{concept["id"]}.png',str(raw/f'{concept["id"]}.png')])
        command(prefix+["pull",f'/sdcard/Android/data/com.routinenest.app.debug/files/screenshots/{args.locale}/seed-date.txt',str(raw/"seed-date.txt")])
    metadata=ROOT/"store-assets/metadata"/args.locale/"screenshot-text.json"
    localized=json.loads(metadata.read_text(encoding="utf-8")) if metadata.exists() else CONCEPTS
    if args.locale not in ("en-GB","en-US") and not metadata.exists(): raise SystemExit("Approved locale-specific screenshot copy missing")
    output=ROOT/"store-assets/screenshots"/args.locale/args.device; output.mkdir(parents=True,exist_ok=True)
    rows=[]
    for concept in localized:
        source=raw/f'{concept["id"]}.png'; dest=output/f'{concept["id"]}.png'
        if not source.exists(): rows.append([args.locale,args.device,concept["id"],concept["headline"],"capture_pending","", ""]); continue
        screen=Image.open(source).convert("RGB"); width,height=screen.size
        supported=(width,height) in SIZES or (height,width) in SIZES
        if args.device=="tablet" and not supported: raise SystemExit(f"Unsupported Amazon tablet dimensions: {screen.size}")
        image=Image.new("RGB",screen.size,"#F6F4ED"); draw=ImageDraw.Draw(image)
        margin=round(width*.04); header=round(height*.22)
        language=args.locale.split("-")[0]
        rtl=language=="ar"
        if language in ("ar","hi") and not features.check_feature("raqm"): raise SystemExit("Complex-script shaping requires Pillow with RAQM; no unshaped export was generated.")
        options={"direction":"rtl" if rtl else "ltr","language":language} if features.check_feature("raqm") else {}
        def fitted(text,size,bold=False):
            while size>=14:
                selected=font(size,bold,args.locale)
                if selected.getlength(text,**options)<=width-2*margin: return selected
                size-=1
            raise ValueError("Overlay text exceeds the available width. Review the locale copy.")
        heading_font=fitted(concept["headline"],max(20,round(width*.032)),True); caption_font=fitted(concept["caption"],max(14,round(width*.019)))
        x=width-margin if rtl else margin
        draw.text((x,round(header*.15)),concept["headline"],font=heading_font,fill="#205B47",anchor="ra" if rtl else "la",**options)
        draw.text((x,round(header*.55)),concept["caption"],font=caption_font,fill="#526558",anchor="ra" if rtl else "la",**options)
        screen.thumbnail((width-2*margin,height-header-margin),Image.Resampling.LANCZOS)
        image.paste(screen,((width-screen.width)//2,header)); image.save(dest)
        rows.append([args.locale,args.device,concept["id"],concept["headline"],"captured_review_pending",str(dest.relative_to(ROOT)),f"{width}x{height}"])
    manifest=ROOT/"store-assets/SCREENSHOT_MANIFEST.csv"
    previous=[]
    if manifest.exists():
        with manifest.open(encoding="utf-8",newline="") as file: previous=[r for r in list(csv.reader(file))[1:] if r[:2]!=[args.locale,args.device]]
    with manifest.open("w",encoding="utf-8",newline="") as file:
        writer=csv.writer(file); writer.writerow(["locale","device","concept","headline","status","path","dimensions"]); writer.writerows(previous+rows)
    print(f"{sum(r[4].startswith('captured') for r in rows)} real screen exports; {sum(r[4]=='capture_pending' for r in rows)} pending.")

if __name__=="__main__": main()
