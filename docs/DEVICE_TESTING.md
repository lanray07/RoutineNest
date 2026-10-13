# Device acceptance checklist

Required evidence must record model, Fire OS/API, locale, build hash, orientation and tester. Ordinary Android emulator results do not establish Fire OS compatibility.

- Fire OS 7 and 8 physical tablets, 7/8/10-inch or current equivalents: install, cold start, rotate, keyboard, scroll, tablet navigation and readable 48dp targets.
- Android phone: onboarding, create/edit, daily/weekday recurrence, complete/undo, restart persistence, duplicate with verified premium, pause/archive/restore, history and weekly/monthly totals.
- Date/time: midnight, changed device timezone, daylight saving, paused and archived reminders, reboot, notification denial and battery optimisation. Reminders are best-effort WorkManager delivery, not exact alarms.
- Speech: absent recognition service, microphone denial, denial retry, cancelled recognition, network failure, no match, ambiguity, matching multiple tasks, confirmation and real database update. No continuous/background listening.
- TTS: no engine, unsupported voice/language, disabled preference, enabled preference, navigation away and app background.
- Offline: airplane mode create/edit/complete, force stop/reopen and history, reminders and data export. Speech/purchases can depend on network; equivalent typed assistant commands operate locally.
- Amazon: App Tester then Amazon Live App Testing on Fire, store price, cancelled purchase, successful RVS verification, restore pagination, account change, revoked receipt, network outage and verified offline cache. No live purchases without explicit approval.
- Accessibility: TalkBack labels/order, large fonts, contrast, touch targets, checkbox state, keyboard focus and both themes.
- Privacy: permission prompts appear only on feature use, no analytics traffic, export integrity and deletion of database plus scheduled reminders.

Current local emulator acceleration is unavailable. Software mode was attempted; no connected/booted device was observed at the initial check. Do not mark UI, speech-engine, TTS, notifications, Fire compatibility or accessibility acceptance as passed until executed.

Follow-up: a KFTRWI tablet (API 28, Android 9, Fire OS property 7.0,
firmware PS7331.4463N) connected and accepted the debug/test APKs. The first
three-test run failed twice: a Boolean-returning persistence test was rejected by
JUnit, and the UI queried before a visible Compose hierarchy existed. Capture
reported success but visual inspection found eight uniform black frames while
the display was asleep; none were accepted as screenshots. The test fixes now
wake an unsecured display, require it to be unlocked, keep the activity awake,
return Unit from the persistence method and reject blank captures. A protected
screen requires the user's manual unlock. The corrected suite must pass before
these checks can be accepted. See `docs/device-test-evidence.json`.

The corrected persistence test and added Keystore session test subsequently ran
on this tablet and passed: **OK (2 tests)**. Room reopening and real repository
operations now have Android device evidence; encrypted token storage was reopened
and a modified ciphertext was rejected/cleared. Navigation and screenshot checks
still require the screen to be manually unlocked. This is one Fire OS 7/API 28
device, not the full Fire OS/device/voice/notification/accessibility matrix.

After manual unlock and the activity-context fix, navigation and screenshot tests
also passed (**OK (2 tests)**). Together, all four unique instrumentation tests
now have passing device runs. Eight en-GB images were pulled and visually inspected;
the assistant reports no speech-recognition service on this Fire tablet, with typed
commands available. Dark system-navigation contrast was identified in visual QA
and corrected for the final capture pass. No successful microphone recognition,
notification delivery or real purchase is claimed by these tests.

The final source (4763b4d) passed all four tests in one run: **OK (4 tests)**.
An additional en-US capture test passed. Sixteen real tablet exports are now
available across en-GB and en-US. The original USB stay-awake setting was restored
to 0 after testing. See the latest run in `device-test-evidence.json`.
