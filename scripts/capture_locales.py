"""Capture real localized screens from the dedicated debug application on USB."""
import datetime
import hashlib
import json
import subprocess
import argparse
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ADB = Path.home() / 'AppData/Local/Android/Sdk/platform-tools/adb.exe'
REPORT = ROOT / 'docs/localized-device-evidence.json'
SERIAL = None

def adb(*args):
    result = subprocess.run([str(ADB), *(['-s', SERIAL] if SERIAL else []), *args], capture_output=True, text=True, encoding='utf-8', errors='replace')
    if result.returncode: raise RuntimeError(result.stderr)
    return result.stdout

def main():
    global SERIAL, REPORT
    parser=argparse.ArgumentParser()
    parser.add_argument('--serial')
    parser.add_argument('--locales', nargs='+')
    parser.add_argument('--report', default=str(REPORT))
    parser.add_argument('--skip-acceptance', action='store_true', help='Capture only; does not record the acceptance suite as passed')
    parser.add_argument('--app-source-commit', required=True, help='Commit used to build the installed debug APK')
    options=parser.parse_args()
    SERIAL=options.serial
    REPORT=Path(options.report)
    configured=json.loads((ROOT/'localization/config.json').read_text(encoding='utf-8'))['locales']
    selected=[x for x in configured if not options.locales or x['tag'] in options.locales]
    if options.locales and set(options.locales) != {x['tag'] for x in selected}: raise ValueError('Unknown capture locale')
    original = adb('shell', 'settings', 'get', 'global', 'stay_on_while_plugged_in').strip()
    report = dict(version='0.2.0', appSourceCommit=options.app_source_commit,
                  appSha256=hashlib.sha256((ROOT / 'app/build/outputs/apk/debug/app-debug.apk').read_bytes()).hexdigest(),
                  captureGuard='locale preference must match requested locale before screenshot', locales=[])
    def instrument(classes, locale=None):
        args = ['shell','am','instrument','-w','-e','class',classes]
        if locale: args += ['-e','locale',locale]
        output = adb(*args, 'com.routinenest.app.debug.test/androidx.test.runner.AndroidJUnitRunner')
        (ROOT / 'build' / f'localized-{locale or "device-suite"}.log').write_text(output, encoding='utf-8')
        return output
    try:
        adb('shell','settings','put','global','stay_on_while_plugged_in','3')
        if not options.skip_acceptance:
            classes = ','.join('com.routinenest.app.' + x for x in ['PersistenceTest','BillingSessionTest','UiFlowTest','ScreenshotCaptureTest','AccessibilityTest','DeviceServicesTest'])
            output = instrument(classes)
            if 'OK (8 tests)' not in output: raise RuntimeError(output[-4000:])
            report['acceptance'] = dict(tests=8, failures=0)
            print('Eight device tests passed', flush=True)
        else:
            report['acceptance'] = dict(executed=False,reason='Capture-only run; separate acceptance evidence applies')
        for item in selected:
            locale = item['tag']
            output = instrument('com.routinenest.app.ScreenshotCaptureTest', locale)
            if 'OK (1 test)' not in output: raise RuntimeError(output[-4000:])
            destination = ROOT / 'store-assets/screenshots/raw' / locale / 'tablet'
            destination.mkdir(parents=True, exist_ok=True)
            for name in [f'{i:02}.png' for i in range(1,9)] + ['seed-date.txt']:
                adb('pull',f'/sdcard/Android/data/com.routinenest.app.debug/files/screenshots/{locale}/{name}',str(destination/name))
            report['locales'].append(dict(locale=locale, tests=1, failures=0, captures=8, device='KFTRWI API 28'))
            print(f'{locale}: eight genuine captures saved', flush=True)
    except Exception as error:
        report['failure'] = str(error)
        raise
    finally:
        adb('shell','settings','put','global','stay_on_while_plugged_in',original)
        report['originalStayAwakeRestored'] = adb('shell','settings','get','global','stay_on_while_plugged_in').strip() == original
        report['recordedAtUtc'] = datetime.datetime.now(datetime.timezone.utc).isoformat()
        REPORT.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

if __name__ == '__main__': main()
