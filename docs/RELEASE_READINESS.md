# RoutineNest release readiness

14 October 2026 — **The free version 0.2.0/code 2 is submitted to Amazon.** The publisher subsequently instructed submission of the current release. Amazon confirms SUBMITTED; approval is pending. Premium purchasing remains disabled. Full production Premium billing and broader acceptance remain future work. See `amazon-submission-evidence.json`.

## Implementation and build

RoutineNest is a native Compose/Room/DataStore/Hilt app with onboarding, adaptive navigation, routine/task editing, templates, daily/weekday schedules, completion/undo, history, pause/archive/restore, scheduled-day streaks, weekly insights, reminders, typed commands, optional device speech/TTS, JSON export and local deletion. Demonstration data and capture helpers are confined to the debug package.

Runtime source: `a238d3de8b52b3c44944a353082446b0a7b7a7d1`. The later `5a8e30b` commit adds the tested App Tester confirmation helper and evidence; it does not change the runtime. [CI run 38587862747](https://github.com/lanray07/RoutineNest/actions/runs/38587862747) passed assembly, 31 JVM tests, 21 Python tests, 17 synthetic Edge Function tests, both lint variants and seven Android 15 emulator tests. Lint has zero errors and 39 warnings per variant.

Java 17, Gradle 8.13, AGP 8.11.1, Kotlin 2.1.20; minimum API 26 and target/compile API 36. Release package `com.routinenest.app`, version 0.2.0/code 2. The current local Gradle release APK is publisher signed with verified v2/v3 signatures and ZIP alignment. Its SHA-256 is `deac306265d62dafb5ac02c648cfc8f9ced42fdebd39e366586975a3087df307`. Amazon's downloaded draft APK matches these bytes exactly. No private signing key or production server credential is in Git.

Artifacts: `app/build/outputs/apk/release/app-release.apk`, `app/build/outputs/apk/debug/app-debug.apk`, `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`. Exact origin and test hashes are in `artifact-origins.json`.

## Executed checks and limits

Eight tests passed on the current debug APK on the physical KFTRWI/API 28 Fire tablet: Room persistence/archive, Keystore sessions, onboarding/navigation, genuine capture, checkbox accessibility/persisted toggle, Android plural selection, actual scheduled notification delivery and English TTS audio synthesis. The checkbox exposes one control through Compose semantics with a 56dp row. English singular completion/streak and French completed-count selection are checked using actual Android resources. See `acceptance-device-evidence.json`.

One additional native Amazon component test passed through real Appstore SDK 3.0.9, installed App Tester, real Supabase Auth/Edge Function and Amazon RVS Cloud Sandbox. It checks a fictitious account before the simulated purchase, verifies the resulting receipt, writes and reads an encrypted fixture lease before calling fulfillment, then restores and verifies the receipt again. There was no live charge. This does not certify the main checkout adapter's full reconciliation/expiry/retry matrix or production purchases. See `native-sandbox-receipt-evidence.json`.

The exact publisher-signed minified APK passed update/startup, existing routine persistence, task-label toggle, force-stop/restart persistence, corrected singular text, a reminder delivered while the app was in the background, offline checklist access and undo. This is partial signed acceptance on one Fire model. The own test reminder was removed and its routine archived reversibly. Wi-Fi, USB stay-awake (0), App Tester catalog and sandbox mode (`none`) were restored. See `current-candidate-evidence.json`.

Seven tests also passed on an Android 15 phone emulator. This adds phone layout, Android plural selection, checkbox accessibility, persistence, Keystore and granted-permission notification coverage. It does not certify notification permission denial, other Fire models, reboot/Doze delivery or speech recognition. Earlier failed attempts (a checkbox assertion race, CI AVD discovery and App Tester confirmation timeout) were corrected; their earlier reports remain historical evidence.

## Languages, voice and marketing

The interface has 19 variants across 14 languages: en-GB, en-US, en-AU, en-IN, fr-FR, de-DE, es-ES, es-MX, it-IT, pt-BR, pt-PT, ja-JP, nl-NL, pl-PL, sv-SE, ko-KR, zh-CN, ar-SA and hi-IN. All 107 expanded base resource entries have structural coverage and protected placeholders. Human/native-speaker review remains pending; these are assistant-authored translations.

Assistant commands/replies and recognition requests remain English-only; localized help explains this. Commands cover creating a routine, adding/checking a task, showing routines and setting a reminder. The tested Fire tablet has no recognition service, so typed fallback is available. Its actual English TTS engine generated audio successfully. Recognition on a capable device and audible-response acceptance remain untested.

Nineteen earlier locale capture runs saved eight genuine frames each. Four current runs refreshed French, Italian, Polish and Arabic after plural fixes. Arabic RTL and shaping were inspected. Eighteen regenerated French/Italian/Polish layouts were visually inspected and their uniformly scaled UI pixels compared exactly against the raw captures. This is visual/rendering review, not native-language certification.

Original generated sculptural nest/pearl masters, light/dark artwork, transparent 114/512 PNG marks and editable SVGs are in `store-assets/premium/`. The pack contains 30 localized 1024x500 promotional exports and 90 genuine 1920x1200 screenshot layouts. The refreshed archive is `build/RoutineNest-premium-localized-assets.zip`. Fifteen search-oriented description sets, feature bullets, keyword hypotheses and screenshot captions are in `store-assets/metadata/`; no ranking or search-volume claim is invented.

Amazon's draft has all 15 description locales and all 11 offered image locale slots complete. The current signed APK and twelve new French/Italian screenshot IDs are saved and verified after reload. Dutch, Polish, Portuguese (Portugal) and Swedish have no separate image slot; their local exports remain available and those storefronts use English US media. The 13 matching language checkboxes are selected; Amazon has no Swedish checkbox. Export certification remains checked with the publisher's explicit authorization. Form completeness is separate from release acceptance. See `AMAZON_DRAFT_STATUS.md`.

## Billing, privacy and remaining decisions

The Premium ENTITLED SKU is an unpublished Amazon draft with 12 description locales and a proposed GBP 2.99 base price. The app uses the SDK price; checkout remains disabled. Supabase function v6 has JWT/principal checks and distributed limits using a private table and service-role-only RPC. Real Auth creation/refresh and concurrent HTTPS validation passed: 60 requests reached validation and request 61 received HTTP 429. A public dummy sandbox secret is configured; production RVS credentials were deliberately not transferred. Temporary anonymous sign-ins are disabled again; all four own Auth test users were removed. Existing booking data was not changed.

Before enabling checkout, finish the main adapter's purchase/restore/cancellation/revocation/account/offline/fulfillment/retry tests and the production/LAT policy. Choose permanent Auth/signup-abuse controls and update account/provider privacy, retention and deletion disclosures. The existing GitHub policy describes the current account-free disabled-billing release.

Also complete the remaining device/voice/permission acceptance and publisher review of translations, copy, artwork and disclosures. Securely back up the signing key and its password outside this machine; the external-drive or secure-vault destination is still awaiting the publisher. The Windows DPAPI password file is not a portable backup. See `SIGNING.md`.

The current free release was submitted with the publisher's later explicit instruction. This does not claim that production Premium billing, human translation review or the wider device matrix are complete. Amazon estimates availability by 19 October 2026 at 17:30 London time, subject to review.


The final CI run exposed a checklist assertion race; the test now waits for the persisted summary. The corrected test passed on the Fire tablet and all CI checks, including seven Android 15 tests, passed in [run 38593657669](https://github.com/lanray07/RoutineNest/actions/runs/38593657669). The production APK was unchanged. A verified local encrypted-keystore/DPAPI backup was also created; off-machine backup and portable password recovery still need an external destination.
