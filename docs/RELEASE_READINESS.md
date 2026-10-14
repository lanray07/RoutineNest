# RoutineNest release-readiness report

13 October 2026 â€” **NOT READY FOR AMAZON SUBMISSION**

An actual native Android project, signed debug APK, unsigned release APK and instrumentation APK have been built. All four instrumentation tests pass on a KFTRWI Fire tablet, and sixteen real English tablet screenshot exports are captured. Billing, additional locales and broader device acceptance remain blocked. No app was submitted and no live purchase was made.

## A. Implementation

Implemented source: native splash/onboarding; adaptive phone bottom navigation and tablet rail; dashboard with interactive daily checklist; routine library; creation/editing and task removal; templates; daily/weekday recurrence; completion/undo; duplication with premium access checks; pause/archive/restore; scheduled-day streaks; dated schedule/state records; history; rolling 7/30-day progress; reminders; speech/text assistant; optional TTS; paywall; settings; light/dark/system themes; English locale selection; privacy, JSON export and routine-data deletion. The dashboard contains the daily checklist. These views use Room state, not static demonstration screens. Demonstration records are confined to the debug screenshot test.

Architecture: `core` is a pure Kotlin engine with JVM tests; `app` uses Compose Material 3, a Hilt view model/repository, Room transactions, DataStore preferences, WorkManager reminders, Android SpeechRecognizer/TTS and a native Amazon SDK adapter. Task history survives renaming/removal; date-level schedule revisions preserve earlier eligibility. Multiple edits on the same local day use that day's latest state.

Versions: Java 17, Gradle 8.13, AGP 8.11.1, Kotlin/Compose compiler 2.1.20, Compose BOM 2025.04.01, Room 2.7.1, DataStore 1.1.7, Hilt 2.56.2, WorkManager 2.10.1, Amazon Appstore SDK 3.0.9. Provisional release ID `com.routinenest.app`; debug ID `com.routinenest.app.debug`. Version 0.1.0, code 1, minimum API 26, target/compile API 36. Older Fire OS 6 devices are excluded by this minimum; One Fire OS 7/API 28 tablet has passed the checks below; additional Fire OS 7/8 models still require physical testing.

The repository was initially empty and had no Git history. Git was initialized; no existing source was overwritten. Source, assets and verification tooling are now committed and pushed to `https://github.com/lanray07/RoutineNest` main. A dedicated publisher signing key was created on 14 October at the publisher's request and stored outside Git. No private signing key, RVS secret or translation credential was committed. Local SDK paths, private signing files, `.env`, build outputs and caches are ignored.

Major files: Gradle configuration/wrapper; `core/src/main/kotlin/com/routinenest/core/`; `app/src/main/java/com/routinenest/app/` (data, UI, voice, reminders, billing); `app/schemas/`; debug-only test support; Android/JVM tests; `scripts/`; `localization/`; `store-assets/`; and these release documents.

## B. Build and artifacts

The current public-key source is `3dc65a9`. [GitHub run 38535671144](https://github.com/lanray07/RoutineNest/actions/runs/38535671144) passed the full verification workflow, including minified release assembly. The release APK from that run contains the exact downloaded Amazon authentication PEM. It was signed locally with the publisher-requested dedicated RoutineNest key. Local debug assembly passed; the slow local release attempt was interrupted after the successful CI candidate was obtained.

Current output files:

- [app-debug.apk](<D:/App Projects/RoutineNest/app/build/outputs/apk/debug/app-debug.apk>) — 19,105,372 bytes; SHA-256 `60f14ad1506b9ab75ecd95f3fa608654621381adf1f7c87c978ddbc6ea8d6803`.
- [app-release-unsigned.apk](<D:/App Projects/RoutineNest/app/build/outputs/apk/release/app-release-unsigned.apk>) — 2,004,455 bytes; SHA-256 `e2ff1aa4e4e9a58591fb1b2c2351a333039e9849056f03ca0dbc0f45636515da`.
- [app-release.apk](<D:/App Projects/RoutineNest/app/build/outputs/apk/release/app-release.apk>) — 2,028,526 bytes; SHA-256 `3affc15a0e9e0746f61ec726e8c866f3c5343288df132445888e9e1dc375c213`.

The signed release passed `apksigner verify --verbose --print-certs` with one dedicated publisher signer and v2/v3 signatures. Certificate SHA-256: `c9438d60c3f444da9d7d8765b0635925ae89f08c6ec419ed64a91d4d0f801f3c`. `zipalign -c -P 16 4` passed. This is signature/ZIP alignment evidence, not a new device acceptance run or a 16 KB native-runtime claim. Key-backup instructions are in `docs/SIGNING.md`; APK signing does not approve publication.

The previous four-test physical acceptance run and sixteen screenshots are from `4763b4d`. The newly packaged authentication key activates Amazon SDK registration, so SDK initialization and product discovery require new device testing. No new billing/device result is inferred from compilation. The previous instrumentation APK remains separate from the current debug/release artifacts. `docs/artifact-origins.json` records provenance.


## C. Executed checks and limitations

**31 JVM tests passed, 0 failures/errors:** engine 21 (including lease expiry); voice availability/TTS policy 6; schedule-history/task-removal 3; development billing fixture 1.

**21 Python tests passed, 0 failures/errors:** translation guards 5; real host SQLite against Room's exported schema 4; synthetic Amazon RVS contracts 12. SQLite tests cover file reopen/identity/integrity, task/day completion uniqueness and scoped undo, transaction rollback on foreign-key failure, and cascading task/schedule-state deletion. They do not execute Android Room repository calls and do not replace the Android reopen test. RVS tests use synthetic responses, not a real Amazon service or purchase.

Both Android lint variants passed with zero errors. Icon transparency/dimensions and English metadata byte/character limits passed. Python tools executed; translation-provider mode explicitly reported configuration pending. No test is reported passed merely because its source compiled.

**Failed or blocked checks:** strict localisation validation exited 1 for **1040 missing translation entries**; capture exited 1 for **no booted device**; connected Android tests failed with **No connected devices**; unsigned release signature validation exited 1 as expected. Early missing build-tools, stale-core compilation, invalid resource documentation, Windows encoding/property escaping and Compose state-read errors were repaired; subsequent build/lint runs passed. A PowerShell Gradle-property quoting issue was corrected before the connected-device failure was reached.

**Executed on device:** Room repository/reopen, Keystore session persistence/tamper rejection, template creation, checklist completion, typed assistant submission, settings/themes/privacy navigation and real screenshot capture. **Not executed:** successful microphone recognition/TTS, notification delivery, airplane-mode/reboot/midnight behavior, comprehensive TalkBack/font scaling/RTL/orientation checks, additional Fire OS/models, Amazon App Tester/Live App Testing and real RVS/checkout. This tablet has no available speech-recognition service. There is no earlier released database version; future schema releases require versioned migrations and genuine upgrade tests.

The Android emulator and two API 36 AVDs exist, but emulator launch remains blocked by unavailable acceleration. Physical Fire tablet testing subsequently passed all four instrumentation tests, including the UI flow and genuine screenshot capture. The instrumentation entry point is confined to the app's debug source set and excluded from the release artifact.

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

Original editable vectors and programmatic raster exports: `store-assets/identity/`. Icons: `icon-light-114.png`, `icon-light-512.png`, corresponding dark variants and SVG sources. Android adaptive icon: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`; foreground: `drawable/nest_mark.xml`. Original onboarding, empty-state and premium-growth vector sources have light/dark variants. Category SVGs, 1024Ã—500 promotional art, screenshot backgrounds and typography specifications are included. The light promotional PNG was visually inspected. These are simple original geometric assets; no complex commissioned illustration is claimed.

**Actual device exports: 16** â€” eight each for en-GB and en-US at 1920x1200, captured on the physical Fire tablet. Raw frames are private/ignored; store overlays use those actual captures. Blank frames were rejected. Light/dark navigation contrast and representative screen/overlay readability were visually checked. The export manifest marks the English tablet set captured/review-pending; phone and untranslated locale exports remain pending. No app screenshot was fabricated. Captures contain deterministic debug example routines and 46 seeded completion records, not a real customer history. Final publisher/store-copy approval remains required.

Listing drafts: `store-assets/metadata/en-GB/` and `en-US/` contain title, short/full descriptions, features, release notes and screenshot copy. Equivalent ten locale folders are explicitly pending. `ASO_KEYWORD_MATRIX.csv` contains natural search-intent hypotheses, with no invented volume/ranking data or Google Play keyword fields. Official current Amazon requirements and sources are recorded in `docs/AMAZON_REQUIREMENTS.md`; the submission checklist is `store-assets/AMAZON_LISTING_CHECKLIST.md`.

## G. Monetisation

**BLOCKED â€” not production-ready.** The native Amazon listener/price/purchase/restore/pagination/cancellation adapter and verification interface compile. The real Supabase verifier is selected, and checkout stays disabled pending configuration and real integration checks. Verified account/receipt caching and premium access checks are implemented, but no actual verified purchase has been obtained. One debug-fixture purchase/restore test and pure entitlement guards passed; no Amazon transaction or RVS test ran. Fake purchases are not enabled in the release artifact.

The proposed UK Â£2.99 one-time unlock is documented for validation, not hardcoded as a universal store price. The UI uses Amazon's returned price when available. Free access allows three non-archived routines plus checklist, reminders, weekly insights and assistant; premium gates unlimited routines, duplication and monthly insights.

The authenticated Supabase function is now deployed in the selected existing free project, and the Android HTTPS client is implemented. Tokens and six-hour cached entitlement leases use Android Keystore authenticated encryption, with account/receipt guards. Checkout stays disabled. Required work includes publisher SKU confirmation, server secrets, Auth and abuse-control setup, callback/offline-clock integration checks and genuine App Tester/Live App Testing. The RVS secret stays server-side. See `docs/BILLING_INTEGRATION.md`.

## H. Submission decision and required actions

**Not ready for submission.** The selected Supabase organization remains on its existing free plan. The isolated billing function is ACTIVE with JWT authentication; missing/invalid-token live requests returned 401. Its Android HTTPS client, encrypted session/lease storage and account guards are implemented. No database/global Auth setting was changed and no paid project was created.

The locale-context crash after onboarding was reproduced on Fire and fixed by retaining the activity registry and a stable activity-backed context. JUnit return type, cold-start readiness and asleep-display capture errors were fixed; the final four-test run passes. Dark system-navigation contrast was corrected and checked in the final captures. Temporary USB stay-awake was restored to its original value (0).

Evidence: 31 JVM tests, 21 Python tests, 12 synthetic Edge Function contract tests and 4 unique physical-device tests pass. The additional en-US capture test passes. Both lint variants have zero errors. GitHub run 38431357950 passes for the final app source. See `docs/device-test-evidence.json`, `docs/supabase-verification.json`, `docs/release-evidence.json` and `store-assets/SCREENSHOT_MANIFEST.csv`.

Remaining work:

1. The publisher has created the draft app and supplied its public authentication PEM. Confirm Amazon package/SKU and supply the server-side RVS secret; finish Auth, signup abuse protection and verification rate limits. Keep checkout disabled until real purchase/restore/revocation/account/offline/fulfillment tests pass.
2. Complete the wider Fire OS/model, phone, speech/TTS, notifications, reboot/offline, date/time and accessibility matrix. Speech recognition is unavailable on this tested tablet; typed fallback works. Define/test any additional Fire voice provider before claiming voice support there.
3. Confirm the provisional twelve locales, configure translation, translate/review the ten pending languages and their listing content, and verify grammar/fonts/RTL. English assistant grammar remains the only implemented command language.
4. Approve English store copy/screens and capture the remaining approved locale/device sets. Current Fire tablet distribution requirements govern submission; phone images are development previews.
5. Supply publisher support details, a reviewed public privacy policy and completed Amazon disclosures, including Supabase billing-session/provider processing.
6. The dedicated publisher key and signed candidate are prepared. Back up the key/password and confirm submission policy and version; rebuild and verify the final reviewed source. Obtain explicit authorization before any store submission or live paid transaction.

Source, working development candidates and English tablet captures are delivered. Critical release blockers remain explicit; this is not represented as a fully tested multilingual premium release.
