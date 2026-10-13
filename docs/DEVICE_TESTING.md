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
