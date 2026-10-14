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

Strict localization now passes for 19 interface locale variants. UTF-8 translations were authored and reviewed by the assistant, with native-speaker review explicitly pending. Protected tokens, missing keys and allowed brand-identical text are checked before publishing resources. The config records structural/assistant review separately from native review. Assistant commands and replies remain English-only in every interface language.

Factual, search-oriented descriptions for all 15 Amazon storefront locales are saved in the unsubmitted draft, with billing and English-assistant limitations disclosed. Premium artwork, transparent icons, 30 light/dark promos and 90 real localized screenshot layouts are in `store-assets/premium/`; all 11 console-supported media locale slots are updated. Four locale image sets remain local because Amazon offers no separate slot. Keyword hypotheses contain no invented traffic/ranking metrics. Amazon requirements and source links are in `docs/AMAZON_REQUIREMENTS.md`.

Python tooling requires Python 3.10+ and Pillow (see `scripts/requirements.txt`). Premium marketing art combines original builtin ImageGen sculpture with deterministic typography and editable icon vectors. Screenshot export requires a booted test device and actual captures; see `docs/SCREENSHOT_CAPTURE.md`. Running the capture test clears only the dedicated debug package's routine database.

## Publisher tasks

GitHub Actions runs JVM/Python/Edge Function tests, both lint variants and debug/release builds on pushes and pull requests, then retains candidate APKs and reports for 14 days. CI compiles instrumentation tests; physical-device execution is recorded separately. All four acceptance tests and 19 localized capture runs pass on one Fire tablet. A successful workflow does not mean submission readiness: native-speaker review, publisher-signed APK acceptance, broader devices and genuine billing integration remain pending. The Python reference policy lives in `backend/`, the deployed authenticated function in `supabase/`, and its Android client in the billing package. Server secrets, Auth/abuse-control setup and real purchasing/RVS tests remain pending.

The Amazon app draft is created. Before enabling billing, create/confirm the one-time entitlement product, identifier/SKU and market prices (proposed UK validation price £2.99). The downloaded Amazon public authentication PEM is included as `app/src/main/assets/AppstoreAuthenticationKey.pem`; keep the RVS shared secret only on the verifier server. The Supabase authenticated endpoint and Android client are implemented, with purchasing still disabled. Finish server secrets, Auth/abuse-control configuration and real App Tester/Live App Testing before enabling checkout. See `docs/BILLING_INTEGRATION.md`. No paid transaction or store publishing is authorised by this repository.

CI produces an unsigned APK, then `scripts/sign-release.ps1` signs it locally with the publisher-authorized dedicated RoutineNest key stored outside Git. The version 0.2.0/code 2 candidate passed v2/v3 signature and ZIP-alignment checks and is uploaded to the unsubmitted Amazon draft. Secure portable key/password backup and final publication approval remain required. See `docs/SIGNING.md`, `docs/AMAZON_DRAFT_STATUS.md` and `docs/RELEASE_READINESS.md`.
