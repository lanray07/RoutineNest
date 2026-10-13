# Real-screen capture workflow

No device screen is fabricated. Run against a dedicated emulator or test device; the capture test replaces **debug package** routine data with deterministic demonstration records. Release user data is inaccessible to it. The seed uses stable IDs and a captured reference local date, stored in seed-date.txt.

1. Enable Windows emulator acceleration following https://developer.android.com/studio/run/emulator-acceleration or connect an authorised test tablet by USB.
2. Boot the device and confirm `adb devices` reports `device`.
3. Use an Amazon-supported tablet size, e.g. 1280×800. Keep emulator scaling outside the capture path; instrumentation takes original pixels.
4. Run `python scripts/screenshots.py --capture --locale en-GB --device tablet --serial emulator-5554`.
5. Repeat en-US, phone layouts, tablet portrait and landscape. Phone images are development previews; Amazon tablet requirements take precedence for listing.
6. Read the exported SCREENSHOT_MANIFEST.csv. Inspect each actual capture and overlay for readable text, no clipping, accurate feature claims and no platform-specific UI leaks.
7. For each additional locale, first translate and review resources, run the localisation pipeline and prepare native-language screenshot-text.json. Never reuse English copy as a completed translation.
8. Run `python scripts/screenshots.py --locale en-GB --device tablet` to regenerate overlays from existing actual raw captures. Missing captures remain `capture_pending`.

Eight concepts map to implemented screens; speech availability is stated in the caption. Ensure a screenshot headed “Habit Tracker & Streaks” actually displays the dashboard streak. Reminder screenshots must visibly show the configured reminder. Do not show premium unlocked while billing is blocked. Approve each final image manually before upload; programmatic dimensions alone are insufficient.

RTL: use a reviewed Arabic build, enable force RTL in Android developer settings, exercise navigation and editor fields in both orientations, test long text/font scaling and undo. An RTL manifest declaration is not a passed RTL layout test.

Also verify the app-selected Arabic locale without force RTL, checking natural layout direction and keyboard behaviour. Overlay rendering selects installed locale fonts; Arabic/Hindi exports require Pillow RAQM shaping and Arabic copy is right aligned. Missing fonts/shaping stop export instead of producing unreadable text. Font coverage, typography and native copy still need human review for each language.
