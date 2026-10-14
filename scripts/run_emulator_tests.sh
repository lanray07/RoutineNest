#!/usr/bin/env bash
# CI uses an isolated Google Android image; this does not certify other Fire models.
set -euo pipefail
sdk="${ANDROID_HOME:?Android SDK required}"
image='system-images;android-35;google_apis;x86_64'
mkdir -p build
sudo chmod a+rw /dev/kvm
"$sdk/cmdline-tools/latest/bin/sdkmanager" "$image" 'emulator'
printf 'no\n' | "$sdk/cmdline-tools/latest/bin/avdmanager" create avd -n RoutineNestCI -k "$image" --device pixel_2
"$sdk/emulator/emulator" -avd RoutineNestCI -no-window -no-audio -no-snapshot -no-boot-anim -gpu swiftshader -accel on > build/emulator-boot.log 2>&1 &
emulator_pid=$!
trap 'kill "$emulator_pid" 2>/dev/null || true' EXIT
adb="$sdk/platform-tools/adb"
booted=false
for attempt in $(seq 1 180); do
  if [[ "$("$adb" -s emulator-5554 shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == '1' ]]; then booted=true; break; fi
  sleep 2
done
[[ "$booted" == true ]] || { echo 'Emulator boot timed out'; exit 1; }
"$adb" -s emulator-5554 shell input keyevent KEYCODE_WAKEUP
"$adb" -s emulator-5554 shell wm dismiss-keyguard
"$adb" -s emulator-5554 shell settings put global window_animation_scale 0
"$adb" -s emulator-5554 shell settings put global transition_animation_scale 0
"$adb" -s emulator-5554 shell settings put global animator_duration_scale 0
"$adb" -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
"$adb" -s emulator-5554 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
"$adb" -s emulator-5554 shell pm grant com.routinenest.app.debug android.permission.POST_NOTIFICATIONS
classes='com.routinenest.app.AccessibilityTest,com.routinenest.app.BillingSessionTest,com.routinenest.app.PersistenceTest,com.routinenest.app.ScreenshotCaptureTest,com.routinenest.app.UiFlowTest,com.routinenest.app.DeviceServicesTest#scheduledWorkPostsAnActualNotification'
"$adb" -s emulator-5554 shell am instrument -w -e class "$classes" com.routinenest.app.debug.test/androidx.test.runner.AndroidJUnitRunner | tee build/emulator-tests.log
python - <<'PY'
import json, pathlib, re, os
log=pathlib.Path('build/emulator-tests.log').read_text()
match=re.search(r'OK \((\d+) tests\)',log)
assert match and int(match[1]) == 7, 'All seven selected emulator tests must pass'
pathlib.Path('docs/emulator-test-evidence.json').write_text(json.dumps({
    'sourceCommit':os.environ['GITHUB_SHA'], 'api':35, 'image':'google_apis/x86_64',
    'deviceProfile':'pixel_2', 'tests':7, 'failed':0,
    'scope':'Core UI, persistence, Keystore, checkbox accessibility, Android plurals, real notification delivery with permission granted',
    'notCovered':['Other Fire models','Amazon checkout','TTS engine','Notification permission denial','Reboot/Doze delivery']
},indent=2)+'\n')
PY
