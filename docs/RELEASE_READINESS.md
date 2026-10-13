# RoutineNest release-readiness report

13 October 2026 — **NOT READY FOR AMAZON SUBMISSION**

An actual native Android project, debug APK, unsigned release APK and instrumentation APK have been built. Core behaviour has executed tests. No device UI, speech engine, Fire OS or real Amazon purchasing result is claimed. No app was submitted and no live purchase was made.

## A. Implementation

Implemented source: native splash/onboarding; adaptive phone bottom navigation and tablet rail; dashboard with interactive daily checklist; routine library; creation/editing and task removal; templates; daily/weekday recurrence; completion/undo; duplication with premium access checks; pause/archive/restore; scheduled-day streaks; dated schedule/state records; history; rolling 7/30-day progress; reminders; speech/text assistant; optional TTS; paywall; settings; light/dark/system themes; English locale selection; privacy, JSON export and routine-data deletion. The dashboard contains the daily checklist. These views use Room state, not static demonstration screens. Demonstration records are confined to the debug screenshot test.

Architecture: `core` is a pure Kotlin engine with JVM tests; `app` uses Compose Material 3, a Hilt view model/repository, Room transactions, DataStore preferences, WorkManager reminders, Android SpeechRecognizer/TTS and a native Amazon SDK adapter. Task history survives renaming/removal; date-level schedule revisions preserve earlier eligibility. Multiple edits on the same local day use that day's latest state.

Versions: Java 17, Gradle 8.13, AGP 8.11.1, Kotlin/Compose compiler 2.1.20, Compose BOM 2025.04.01, Room 2.7.1, DataStore 1.1.7, Hilt 2.56.2, WorkManager 2.10.1, Amazon Appstore SDK 3.0.9. Provisional release ID `com.routinenest.app`; debug ID `com.routinenest.app.debug`. Version 0.1.0, code 1, minimum API 26, target/compile API 36. Older Fire OS 6 devices are excluded by this minimum; Fire OS 7/8 are intended targets and still require physical testing.

The repository was initially empty and had no Git history. Git was initialized; no existing source was overwritten. Source, assets and verification tooling are now committed and pushed to `https://github.com/lanray07/RoutineNest` main. No publisher signing key, RVS secret or translation credential was created or committed. Local SDK paths, private signing files, `.env`, build outputs and caches are ignored.

Major files: Gradle configuration/wrapper; `core/src/main/kotlin/com/routinenest/core/`; `app/src/main/java/com/routinenest/app/` (data, UI, voice, reminders, billing); `app/schemas/`; debug-only test support; Android/JVM tests; `scripts/`; `localization/`; `store-assets/`; and these release documents.

## B. Build and artifacts

Debug assembly, release Kotlin/Java compilation, minified release assembly and instrumentation APK assembly **passed**. Both lint variants have **0 errors and 45 warnings**. Warnings include newer dependency versions, unused resources and vector sizing; Amazon's jar also emits D8/R8 stack-map warnings. These warnings were retained. SDK warnings require particular attention during real purchasing tests.

- [app-debug.apk](<D:/App Projects/RoutineNest/app/build/outputs/apk/debug/app-debug.apk>) — 19,398,274 bytes; debug key only; verify with apksigner. SHA-256: `7be8e142f748d3def7a4a9260f41db26944181ac248280e1b07b1ba0f9e56357`.
- [app-debug-androidTest.apk](<D:/App Projects/RoutineNest/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk>) — 1,235,218 bytes; debug key only; verify with apksigner. SHA-256: `afcd18fca273bee0942b5947d18de64bff1a0ca0b4468c10c5fad87108597b1a`.
- [app-release-unsigned.apk](<D:/App Projects/RoutineNest/app/build/outputs/apk/release/app-release-unsigned.apk>) — 1,987,561 bytes; unsigned. SHA-256: `db12d0782b24a9206f22864ccb2e9c3e77d3c19ebd92d780171c627e16e0347d`.

`apksigner verify --verbose` passed for the final debug APK using v2 signing, with one debug signer. Release verification returned **DOES NOT VERIFY / Missing META-INF/MANIFEST.MF**, confirming the release candidate is unsigned. `zipalign -c -P 16 4` passed on the release APK. This is ZIP alignment evidence, not a passed 16 KB native-runtime or Fire-device test. Publisher signing/submission identity remains a manual decision; use the signed debug APK for local installation.

`aapt dump badging` confirmed release package/version and APIs. The merged manifest includes microphone, notifications, Internet and WorkManager's wake-lock/network/boot/foreground-service permissions. Microphone hardware is optional. APK native ABI folders include arm64-v8a, armeabi-v7a, x86 and x86_64. The release DEX scan found neither `TestBillingAdapter` nor `DebugDataAccess`; both appear only in debug sources/artifact. Source-set isolation is the primary guarantee, corroborated by this scan.

## C. Executed checks and limitations

**30 JVM tests passed, 0 failures/errors:** engine 20; voice availability/TTS policy 6; schedule-history/task-removal 3; development billing fixture 1.

**21 Python tests passed, 0 failures/errors:** translation guards 5; real host SQLite against Room's exported schema 4; synthetic Amazon RVS contracts 12. SQLite tests cover file reopen/identity/integrity, task/day completion uniqueness and scoped undo, transaction rollback on foreign-key failure, and cascading task/schedule-state deletion. They do not execute Android Room repository calls and do not replace the Android reopen test. RVS tests use synthetic responses, not a real Amazon service or purchase.

Both Android lint variants passed with zero errors. Icon transparency/dimensions and English metadata byte/character limits passed. Python tools executed; translation-provider mode explicitly reported configuration pending. No test is reported passed merely because its source compiled.

**Failed or blocked checks:** strict localisation validation exited 1 for **1040 missing translation entries**; capture exited 1 for **no booted device**; connected Android tests failed with **No connected devices**; unsigned release signature validation exited 1 as expected. Early missing build-tools, stale-core compilation, invalid resource documentation, Windows encoding/property escaping and Compose state-read errors were repaired; subsequent build/lint runs passed. A PowerShell Gradle-property quoting issue was corrected before the connected-device failure was reached.

**Not executed:** Android Room repository/reopen integration, navigation/touch UI tests, permission prompts and recognition failure on a real service, TTS engine behaviour, notification delivery, airplane-mode device persistence, TalkBack/font scaling/contrast review, natural/forced RTL layouts, phone/tablet rotations, physical Fire compatibility, Amazon App Tester/Live App Testing, store checkout and RVS. There is no prior released database version: historical upgrade migration tests are not applicable yet; an exported initial v1 schema and Android reopen test are provided. Future schema changes after release require versioned migrations and genuine upgrade tests.

The Android emulator and two API 36 AVDs exist. Native launch failed because the emulator acceleration driver is unavailable. Software mode was attempted but no booted device appeared. Android UI tests compiled; they did not run. Their debug entry point is now in the app's debug source set, and generated Hilt code was inspected to confirm the component implements it.

Commands executed include:

```powershell
.\gradlew.bat :core:test :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug :app:lintRelease :app:assembleRelease
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.routinenest.app.PersistenceTest,com.routinenest.app.UiFlowTest'
python scripts/localize.py --translate
python scripts/validate_translations.py --strict
python scripts/test_localization.py
python scripts/test_database_schema.py
python scripts/generate_assets.py
python scripts/prepare_listing.py
python scripts/screenshots.py --capture --locale en-GB --device tablet
python scripts/release_check.py
```

Full local build outputs remain in `verification-final.log`, `verification-harness.log` and `device-tests.log` (ignored). Durable summaries are `docs/release-evidence.json`, `docs/python-test-evidence.json` and `docs/artifact-audit.json`. Lint HTML/XML reports are in `app/build/reports/`.

## D. Voice

Implemented commands: create a morning routine; add reading to an exactly named evening routine; mark a uniquely matching reading task complete; query remaining steps; show weekly progress; remind at eight tomorrow; read the daily routine. Mutations are confirmed, ambiguous matches are rejected with guidance, recognised text is shown, and the executor reads/writes actual application data. Typed commands use the same parser/executor and operate locally. Reminders resolve to a displayed device-local time and are best-effort WorkManager jobs.

Command parsing supports **English**, with en-GB/en-US recognition requests. Additional command grammars are not implemented or claimed. Recognition/TTS engines tested: **none**. Some Fire tablets lack a recognition service; the UI detects availability and offers text commands. Denial/error/no-match and missing/unsupported TTS paths are implemented, with pure policy/parser tests; actual service and permission behaviour still needs device execution. TTS and recognition stop when leaving/backgrounding the assistant.

## E. Localisation

Validated, selectable resource variants: **en-GB and en-US**. No machine translation is claimed. Pending provisional locales: fr-FR, de-DE, es-ES, it-IT, pt-BR, ja-JP, ko-KR, zh-CN, ar-SA, hi-IN. The earlier twelve-locale list referenced by the attachment was not included; this set needs publisher confirmation.

Delivered: `scripts/localize.py`, validation/tests, translation configuration, protected glossary, checkpointed drafts, pending locale resource directories outside production, generated locale selector/config, `localization/reports/missing-strings.csv` and `translation-review.json`. The pipeline protects printf/product tokens, validates entries/plural categories, detects missing/untranslated content and publishes only validated/reviewed resources. Missing translations remain visible in strict reports, rather than being labelled complete. Native review and real layout/RTL validation are still required before approving additional languages.

## F. Identity, screenshots and metadata

Original editable vectors and programmatic raster exports: `store-assets/identity/`. Icons: `icon-light-114.png`, `icon-light-512.png`, corresponding dark variants and SVG sources. Android adaptive icon: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`; foreground: `drawable/nest_mark.xml`. Original onboarding, empty-state and premium-growth vector sources have light/dark variants. Category SVGs, 1024×500 promotional art, screenshot backgrounds and typography specifications are included. The light promotional PNG was visually inspected. These are simple original geometric assets; no complex commissioned illustration is claimed.

**Actual device screenshots: 0.** Eight concepts, deterministic localized debug seeding, automated screen routing/capture, marketing overlays, locale-font/complex-script guards, dimension validation and export manifest are implemented. Morning data sorts first and capture waits for all 46 seeded completion records; reminder configuration is represented in its library screen. `store-assets/SCREENSHOT_MANIFEST.csv` marks all current English phone/tablet exports capture-pending. No device screenshot was fabricated. See `docs/SCREENSHOT_CAPTURE.md` for capture and approval steps.

Listing drafts: `store-assets/metadata/en-GB/` and `en-US/` contain title, short/full descriptions, features, release notes and screenshot copy. Equivalent ten locale folders are explicitly pending. `ASO_KEYWORD_MATRIX.csv` contains natural search-intent hypotheses, with no invented volume/ranking data or Google Play keyword fields. Official current Amazon requirements and sources are recorded in `docs/AMAZON_REQUIREMENTS.md`; the submission checklist is `store-assets/AMAZON_LISTING_CHECKLIST.md`.

## G. Monetisation

**BLOCKED — not production-ready.** The native Amazon listener/price/purchase/restore/pagination/cancellation adapter and verification interface compile. The unconfigured verifier denies entitlement and checkout stays disabled. Verified account/receipt caching and premium access checks are implemented, but no actual verified purchase has been obtained. One debug-fixture purchase/restore test and pure entitlement guards passed; no Amazon transaction or RVS test ran. Fake purchases are not enabled in the release artifact.

The proposed UK £2.99 one-time unlock is documented for validation, not hardcoded as a universal store price. The UI uses Amazon's returned price when available. Free access allows three non-archived routines plus checklist, reminders, weekly insights and assistant; premium gates unlimited routines, duplication and monthly insights.

Required work includes publisher app/SKU/public authentication PEM, an RVS server and authenticated Android verification client, account/restore/revocation reconciliation, offline-cache policy and genuine App Tester/Live App Testing. This is more than filling in a key. The RVS secret must remain server-side. See `docs/BILLING_INTEGRATION.md` for exact integration steps.

## H. Submission decision and required actions

Follow-up after GitHub push: source is on `lanray07/RoutineNest` main. Added a server-side Amazon RVS verification component and 12 passing synthetic contract tests (21 Python tests total). The component rejects mismatched/revoked/test receipts in production and distinguishes retryable outages. An authenticated HTTP service, Android client and real Amazon integration remain pending. GitHub Actions now builds candidates, runs JVM/Python checks and both lint variants; device tests and store readiness are separate gates.

GitHub verification **passed** for source commit `ff68b3bb998e8d8df94f09283c31c61ccaeefd99`: [run 38408173323](https://github.com/lanray07/RoutineNest/actions/runs/38408173323). Both APK variants and the instrumentation APK compiled, JVM/Python checks passed, and both lint variants completed. Candidate APKs and reports are retained on that run for 14 days. The follow-up source also respects the selected locale for template/weekday text and layout direction, handles keyboard insets, indexes progress completions and uses the current date when writing completions. Visual/RTL and midnight device interaction checks still need execution.

Submission is blocked. Next actions, in order:

1. Enable emulator acceleration or provide a connected test device; execute the compiled integration/UI tests and resolve their findings. Complete the Fire OS 7/8 and 7/8/10-inch physical-device checklist, speech/TTS, notifications, offline persistence, orientation and accessibility checks in `docs/DEVICE_TESTING.md`.
2. Confirm publisher identity, package/SKU and target devices; implement and test the RVS server/client and real Amazon purchasing. Review SDK R8 warnings on the actual release purchase path before enabling payment.
3. Confirm the twelve locales, configure translation endpoint/key, translate/review app and listing content, verify fonts/grammar/layout/RTL, and approve only complete languages. Non-English assistant grammar support remains a separate feature if required; current listing claims English commands only.
4. Capture the eight real screens on suitable devices for each approved listing locale, render/review overlays, validate dimensions and approve the export manifest. Phone outputs are development previews; Amazon's current Fire tablet distribution requirements govern submission.
5. Supply publisher support details and a public, reviewed privacy policy; complete Amazon's privacy/content disclosures against the final SDK/binary. `docs/PRIVACY_DRAFT.md` is an engineering draft, not legal approval.
6. Confirm publisher signing/submission policy and version; rebuild from the final reviewed source, verify artifact/signing/device targets and complete the Amazon checklist. Obtain explicit authorization before submission or live purchases.

The source and candidate artifacts are delivered for development and integration review. They are not represented as a finished, fully tested, multilingual premium app ready to publish.
