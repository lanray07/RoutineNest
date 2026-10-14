"""Capture real localized screens from the dedicated debug application on USB."""
import datetime
import hashlib
import json
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ADB = Path.home() / 'AppData/Local/Android/Sdk/platform-tools/adb.exe'
REPORT = ROOT / 'docs/localized-device-evidence.json'

def adb(*args):
    result = subprocess.run([str(ADB), *args], capture_output=True, text=True, encoding='utf-8', errors='replace')
    if result.returncode: raise RuntimeError(result.stderr)
    return result.stdout

def main():
    original = adb('shell', 'settings', 'get', 'global', 'stay_on_while_plugged_in').strip()
    report = dict(version='0.2.0', appSourceCommit='9a12018b033f107b56d81224386c779bfbcc5eae',
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
        classes = ','.join('com.routinenest.app.' + x for x in ['PersistenceTest','BillingSessionTest','UiFlowTest','ScreenshotCaptureTest'])
        output = instrument(classes)
        if 'OK (4 tests)' not in output: raise RuntimeError(output[-4000:])
        report['acceptance'] = dict(tests=4, failures=0)
        print('Four device tests passed', flush=True)
        for item in json.loads((ROOT / 'localization/config.json').read_text(encoding='utf-8'))['locales']:
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
