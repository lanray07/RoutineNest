# RoutineNest release readiness

14 October 2026 — **Not approved for Amazon submission.**

Version 0.2.0/code 2 is implemented, built and publisher signed. Amazon accepted this APK into its existing unsubmitted draft. The app remains free with purchasing disabled. Store form completeness is separate from production acceptance.

## Implementation and languages

Native Compose/Room/DataStore/Hilt app: onboarding, adaptive tablet/phone navigation, editable routines/tasks, templates, daily/weekday schedules, completion/undo, historical schedule records, pause/archive/restore, scheduled-day streaks, weekly insights, reminders, typed assistant, optional device speech/TTS, JSON export and local deletion. Demonstration records and capture helpers are confined to the debug package.

The interface has 19 locale variants: en-GB, en-US, en-AU, en-IN, fr-FR, de-DE, es-ES, es-MX, it-IT, pt-BR, pt-PT, ja-JP, nl-NL, pl-PL, sv-SE, ko-KR, zh-CN, ar-SA and hi-IN. All 107 expanded base resource entries have structurally valid translations, protected tokens and complete resource coverage. Non-English translations were authored and reviewed by the assistant; **native-speaker review remains pending**. There is no claim of professional human translation.

Assistant command parsing and replies remain **English-only**, and recognition requests use English. Localized help explains this limitation. The tested Fire tablet has no speech-recognition service; typed fallback works. Its actual English TTS engine now passes audio synthesis; microphone recognition on a capable device and audible response acceptance remain pending.

## Build and verification

Current runtime source: `a238d3de8b52b3c44944a353082446b0a7b7a7d1`. [GitHub Actions run 38578540498](https://github.com/lanray07/RoutineNest/actions/runs/38578540498) passed assembly, 31 JVM tests, 21 Python tests, 17 synthetic Edge Function tests, both lint variants and seven Android 15 phone emulator tests. These include actual Android plural selection, checkbox accessibility, persistence, Keystore sessions and notification delivery with permission granted. The current local publisher-signed candidate passes v2/v3 signature and ZIP alignment verification. USB disconnected before its final physical installation, so the Amazon draft still contains the earlier tested `4184918` APK; exact hashes and separate provenance are recorded in `artifact-origins.json` and `current-candidate-evidence.json`.

Java 17, Gradle 8.13, AGP 8.11.1, Kotlin 2.1.20, minimum API 26 and target/compile API 36. Package `com.routinenest.app`; debug `com.routinenest.app.debug`. CI's unsigned release APK was signed locally with the dedicated publisher key, and v2/v3 signatures plus ZIP alignment passed. The public Amazon authentication PEM is packaged. No private signing material or server secret is in Git. `docs/artifact-origins.json` records exact hashes and origin.

Four acceptance tests passed on the physical KFTRWI/API 28 tablet: persistence, Keystore session storage, core UI flow and seeded screenshot capture. A test assertion initially failed because its summary was offscreen after scrolling to a checkbox; the assertion now scrolls to the summary and the complete suite passes. The capture harness also waits for the requested locale preference before rendering.

Nineteen additional locale capture runs passed, saving eight actual frames each (152 raw captures). The original USB stay-awake setting was restored to 0. Raw frames are ignored; durable results are in `docs/localized-device-evidence.json`. The instrumentation tests and locale capture runs execute the debug build. A subsequent check on the exact publisher-signed minified APK passed installation, startup/onboarding, routine/task creation, task completion and force-stop/restart persistence. Its assistant-created test routine was archived reversibly, as verified visually. See `docs/signed-release-smoke-evidence.json`; this is partial release acceptance, not a completed notification/voice/accessibility/billing matrix. Genuine billing transactions, broader models, notifications/reboot/offline/date-time and accessibility coverage remain pending.

## Store artwork and copy

The 14 October follow-up fixes checkbox accessibility (one named 56dp row control), singular/plural routine summaries across the locale resources, and distributed receipt-service limits. Eight tests passed on the connected Fire tablet with this updated debug APK, including real scheduled notification delivery and English TTS synthesis. The asynchronous checkbox assertion now waits for the persisted state. See `acceptance-device-evidence.json` for exact APK hashes and scope. JVM tests, both lint variants and release assembly also passed locally. Seven Android 15 phone tests passed on the current runtime. Eight earlier physical Fire tests remain pinned to their earlier APK; their rerun and corrected French/Italian/Polish/Arabic captures await stable USB. These checks do not certify twelve Fire models or real payments.

Original AI-generated sculptural nest/pearl artwork has light and dark variants. Exact typography, transparent 114/512 PNG marks, editable icon SVGs, 30 localized 1024×500 promotional exports and 90 localized 1920×1200 screenshot layouts are in `store-assets/premium/`. Screenshot layouts preserve genuine captured UI through uniform scaling; no interface text, controls or data were painted into fake screenshots. Demonstration routines are clearly test data.

All six screenshot concepts were visually inspected across the 15 store locale sets using review sheets, with full-size representative checks. Arabic RTL and Hindi shaping were inspected on actual captures. This is layout/rendering review, not native-language certification or exhaustive accessibility acceptance. Source hashes and visual review scope are recorded with the exports.

Search-oriented descriptions, five feature bullets and relevant keyword hypotheses are prepared for all 15 Amazon description locales. No search-volume or ranking claims are invented. Amazon's image modal offers only 11 locale slots; Dutch, Polish, Portuguese (Portugal) and Swedish image exports are retained locally and those marketplaces use the English US media fallback. See `docs/AMAZON_DRAFT_STATUS.md` for verified saved contents.

## Remaining release gates

1. Complete native-speaker translation review and broader Fire/model/accessibility acceptance, including actual speech/TTS and notification behavior.
2. Complete the remaining signed-APK notification, voice, accessibility and broader-device acceptance, plus publisher review of store copy, artwork and privacy/provider disclosures. Basic signed-release CRUD/restart smoke checks now pass.
3. The ENTITLED SKU is now saved in an unsubmitted localized Amazon product draft. Supabase function v6 uses a private rate-limit table and service-role-only RPC. Real temporary authenticated requests and Android Auth refresh passed; a concurrent HTTPS check admitted 60 validation requests and denied request 61. Anonymous sign-ins were temporarily enabled with permission, then restored disabled and verified after reload; the three own test users were removed. Public dummy sandbox RVS configuration is saved, but positive SDK purchase/receipt verification remains uncompleted after USB disconnection. Production credentials, permanent Auth/abuse controls and purchase/restore/revocation/account/offline/fulfillment flows remain pending. Existing booking data was not changed.
4. Securely back up the dedicated signing key and its password outside this machine. The current DPAPI password storage is not a portable backup; see `docs/SIGNING.md`.
5. Execute the publisher's conditional publication authorization only after the required checks pass. No store submission or live purchase has been performed.

The publisher authorized submission on 14 October only if all is ready. That condition is not satisfied. The execution workflow explicitly prohibits a release-ready claim while critical billing or compatibility requirements remain unresolved; Submit App was not pressed.


Current USB handoff: restore tablet USB stay-awake from temporary 3 to original 0, finish the sandbox receipt rerun, refresh four affected locale capture sets, test/install the current signed candidate and then replace the draft APK and French/Italian media. Original App Tester catalog and sandbox mode were restored after the earlier test; later failed installations did not change them. The signing-backup destination is still awaiting the publisher.
