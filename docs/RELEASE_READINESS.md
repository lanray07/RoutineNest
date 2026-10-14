# RoutineNest release readiness

14 October 2026 — **Not approved for Amazon submission.**

Version 0.2.0/code 2 is implemented, built and publisher signed. Amazon accepted this APK into its existing unsubmitted draft. The app remains free with purchasing disabled. Store form completeness is separate from production acceptance.

## Implementation and languages

Native Compose/Room/DataStore/Hilt app: onboarding, adaptive tablet/phone navigation, editable routines/tasks, templates, daily/weekday schedules, completion/undo, historical schedule records, pause/archive/restore, scheduled-day streaks, weekly insights, reminders, typed assistant, optional device speech/TTS, JSON export and local deletion. Demonstration records and capture helpers are confined to the debug package.

The interface has 19 locale variants: en-GB, en-US, en-AU, en-IN, fr-FR, de-DE, es-ES, es-MX, it-IT, pt-BR, pt-PT, ja-JP, nl-NL, pl-PL, sv-SE, ko-KR, zh-CN, ar-SA and hi-IN. All 104 source keys have structurally valid translations, protected tokens and complete resource coverage. Non-English translations were authored and reviewed by the assistant; **native-speaker review remains pending**. There is no claim of professional human translation.

Assistant command parsing and replies remain **English-only**, and recognition requests use English. Localized help explains this limitation. The tested Fire tablet has no speech-recognition service; typed fallback works. Actual speech/TTS engine acceptance remains pending.

## Build and verification

Runtime source: `9a12018b033f107b56d81224386c779bfbcc5eae`. [GitHub Actions run 38543978373](https://github.com/lanray07/RoutineNest/actions/runs/38543978373) passed release/debug assembly, instrumentation assembly, 31 JVM tests, 21 Python tests, 12 synthetic Edge Function contract tests and both lint variants (zero errors, 52 warnings each). Synthetic receipt tests are not real Amazon purchases.

Java 17, Gradle 8.13, AGP 8.11.1, Kotlin 2.1.20, minimum API 26 and target/compile API 36. Package `com.routinenest.app`; debug `com.routinenest.app.debug`. CI's unsigned release APK was signed locally with the dedicated publisher key, and v2/v3 signatures plus ZIP alignment passed. The public Amazon authentication PEM is packaged. No private signing material or server secret is in Git. `docs/artifact-origins.json` records exact hashes and origin.

Four acceptance tests passed on the physical KFTRWI/API 28 tablet: persistence, Keystore session storage, core UI flow and seeded screenshot capture. A test assertion initially failed because its summary was offscreen after scrolling to a checkbox; the assertion now scrolls to the summary and the complete suite passes. The capture harness also waits for the requested locale preference before rendering.

Nineteen additional locale capture runs passed, saving eight actual frames each (152 raw captures). The original USB stay-awake setting was restored to 0. Raw frames are ignored; durable results are in `docs/localized-device-evidence.json`. The instrumentation tests and locale capture runs execute the debug build. A subsequent check on the exact publisher-signed minified APK passed installation, startup/onboarding, routine/task creation, task completion and force-stop/restart persistence. Its assistant-created test routine was archived reversibly, as verified visually. See `docs/signed-release-smoke-evidence.json`; this is partial release acceptance, not a completed notification/voice/accessibility/billing matrix. Genuine billing transactions, broader models, notifications/reboot/offline/date-time and accessibility coverage remain pending.

## Store artwork and copy

Original AI-generated sculptural nest/pearl artwork has light and dark variants. Exact typography, transparent 114/512 PNG marks, editable icon SVGs, 30 localized 1024×500 promotional exports and 90 localized 1920×1200 screenshot layouts are in `store-assets/premium/`. Screenshot layouts preserve genuine captured UI through uniform scaling; no interface text, controls or data were painted into fake screenshots. Demonstration routines are clearly test data.

All six screenshot concepts were visually inspected across the 15 store locale sets using review sheets, with full-size representative checks. Arabic RTL and Hindi shaping were inspected on actual captures. This is layout/rendering review, not native-language certification or exhaustive accessibility acceptance. Source hashes and visual review scope are recorded with the exports.

Search-oriented descriptions, five feature bullets and relevant keyword hypotheses are prepared for all 15 Amazon description locales. No search-volume or ranking claims are invented. Amazon's image modal offers only 11 locale slots; Dutch, Polish, Portuguese (Portugal) and Swedish image exports are retained locally and those marketplaces use the English US media fallback. See `docs/AMAZON_DRAFT_STATUS.md` for verified saved contents.

## Remaining release gates

1. Complete native-speaker translation review and broader Fire/model/accessibility acceptance, including actual speech/TTS and notification behavior.
2. Complete the remaining signed-APK notification, voice, accessibility and broader-device acceptance, plus publisher review of store copy, artwork and privacy/provider disclosures. Basic signed-release CRUD/restart smoke checks now pass.
3. Confirm billing SKU, server-side RVS credentials, authentication and abuse/rate controls, then test genuine purchase/restore/revocation/account/offline/fulfillment flows before enabling checkout. The selected free Supabase endpoint remains isolated; no shared database or global Auth settings were changed.
4. Securely back up the dedicated signing key and its password outside this machine. The current DPAPI password storage is not a portable backup; see `docs/SIGNING.md`.
5. Obtain explicit publication authorization before Submit App. No store submission or live purchase has been performed.

The publisher authorized submission on 14 October only if all is ready. That condition is not satisfied. The execution workflow explicitly prohibits a release-ready claim while critical billing or compatibility requirements remain unresolved; Submit App was not pressed.
