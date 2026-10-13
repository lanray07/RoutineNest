# RoutineNest

Native Android application for Fire tablet submission, with Compose UI and offline Room storage. This is an implemented development build, **not submission-ready**. Read `docs/RELEASE_READINESS.md` and `docs/AMAZON_REQUIREMENTS.md` before release.

## Build

Requirements: Java 17, Android SDK platform 36 and build tools 36.0.0. Point untracked `local.properties` to your SDK with `sdk.dir=...`. Gradle 8.13 wrapper is committed. No global Gradle installation is needed.

```powershell
.\gradlew.bat :core:test :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug :app:lintRelease :app:assembleRelease
```

Debug package: `com.routinenest.app.debug`; provisional release package: `com.routinenest.app`. Minimum API 26 deliberately excludes older Fire OS 6 devices. Target/compile API 36. Physical Fire OS 7/8 validation remains required. Do not infer Fire support from an Android build.

## Structure and behaviour

- `core`: plain Kotlin recurrence, local date/time rules, reminder timing, scheduled-day streaks, English command parser and entitlement/access policy, with JVM tests.
- `app/data`: Room v1 schema, task history, per-calendar-day schedule/state revisions, repository transactions and DataStore preferences. Routines have stable UUIDs; checklist completions use task/day keys and undo removes that row. Task removal retains previous history. Schedule and pause/archive edits apply from the local edit day onward. Multiple edits on the same day share that day's latest state.
- `app/ui`: onboarding, adaptive navigation rail/bottom navigation, dashboard/checklist, library/templates, editor, history, weekly/monthly insights, assistant, paywall, settings and privacy/export/deletion. Material 3 forest/linen palette and original vector illustrations.
- `app/voice`: Android SpeechRecognizer and TextToSpeech lifecycle, explicit capability/permission checks, visible recognised text, confirmation of mutations and equivalent local text commands. English UK/US command input only; unavailable/unsupported engines produce text fallback. Speech is started by a button, never background listening.
- `app/reminders`: durable WorkManager requests, cancelled for paused/archived routines; rescheduled on device clock/timezone changes. Delivery is best effort and is not an exact alarm.
- `app/billing`: Amazon SDK 3.0.9 listener/receipt/restore adapter and a fail-closed verifier seam. Real checkout is disabled until publisher configuration and server RVS are completed. The development fixture exists only in debug sources and is never selected by the app.

Free plan: three non-archived routines; pause still occupies a slot. Checklists, templates, weekly insights, reminders and assistant are free. Verified premium gates unlimited routines, duplication and monthly insights.

Version 1 is the first unreleased database schema. Room's exported schema is tracked. Do not change a released schema without a version increment, explicit migration and a real upgrade test. The instrumentation reopen test validates the initial schema and durable transactions; it does not assert any historical migration that does not exist.

## Localisation, identity and listing

```powershell
python scripts/localize.py
python scripts/test_localization.py
python scripts/test_database_schema.py
python scripts/validate_translations.py --strict
python scripts/generate_assets.py
python scripts/prepare_listing.py
python scripts/screenshots.py --locale en-GB --device tablet
python scripts/release_check.py
```

`--strict` localisation intentionally fails while translations are missing. `--translate` uses a LibreTranslate-compatible endpoint and API key from environment variables; neither is embedded in the app. Drafts are checkpointed per entry. Placeholders/product names are protected, resources are validated, and only entries both validated and marked reviewed in `localization/config.json` reach Android resources and the app language selector. Review plural grammar and visual RTL rendering separately. Two English variants are provided; ten provisional non-English locales remain pending. The earlier twelve-locale list referenced in the attachment was absent; confirm the provisional set before purchasing translation work.

English listing text is a factual **draft**, with billing limitations disclosed. Pending language directories are explicit placeholders for translation, not completed listing copy. Search intent suggestions contain no invented traffic/ranking metrics. Amazon's description/icon/screenshot rules are sourced in the requirements document. ProGuard rules keep Amazon SDK internals according to https://developer.amazon.com/docs/in-app-purchasing/iap-obfuscate-the-code.html; SDK-origin D8/R8 warnings must remain visible and purchasing needs real release-device tests.

Python tooling requires Python 3.10+ and Pillow (see `scripts/requirements.txt`). Marketing art is programmatically generated original geometry, with editable SVGs. Screenshot export requires a booted test device and actual captures; see `docs/SCREENSHOT_CAPTURE.md`. Running the capture test clears only the dedicated debug package's routine database.

## Publisher tasks

GitHub Actions runs JVM/Python tests, both lint variants and debug/release builds on pushes and pull requests, then retains candidate APKs and reports for 14 days. It compiles instrumentation tests without claiming they ran on Fire hardware. A successful workflow does not mean submission readiness: untranslated locales and publisher/device blockers remain explicit in the evidence report. The dependency-free server receipt policy and its tests live in `backend/`; endpoint authentication, Android integration and real RVS tests remain pending.

Create an Amazon app and one-time entitlement product; confirm identifier/SKU and market prices (proposed UK validation price £2.99). Obtain the app public authentication PEM from Amazon and provide it as `app/src/main/assets/AppstoreAuthenticationKey.pem`; keep the RVS shared secret only on the verifier server. Implement the `ReceiptVerifier` seam with authenticated HTTPS, cancellation/account/SKU matching and retry handling; only then enable the adapter's configuration flag and test App Tester/Live App Testing. No paid transaction or store publishing is authorised by this repository.

Release builds currently produce an unsigned APK. No publisher private key was generated. Select the publisher's secure signing/submission policy and verify with `apksigner` before distribution. Amazon can accept unsigned uploads and re-sign, but publishing identity and key handling still require publisher confirmation. Review the privacy questionnaire, host a public privacy policy, fill support details, verify Fire hardware and capture assets before submission.
