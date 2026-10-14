# RoutineNest premium store asset pack

Original sculptural nest/pearl artwork, warm ivory and evergreen palettes, quiet gold accents and deterministic localized typography. The Creative Production board contains the original masters and final English promo previews.

- `source/`: original builtin ImageGen light/dark masters; source hashes in `MANIFEST.json`.
- `icons/`: transparent 114/512 PNG store marks, editable SVG and compositor source in `scripts/premium_assets.py`.
- Each of 15 locale folders: two exact 1024×500 promotional exports and six 1920×1200 tablet screenshot layouts.
- Each capture-provenance file records actual device source hashes, output hashes and review scope. All 90 UI regions were compared pixel-for-pixel with uniformly scaled real captures. No simulated UI was generated.

Captured in the dedicated debug package using three demonstration routines; raw frames are ignored. The compositor does not replace UI text, data or controls. All six concepts were visually checked across 15 locale review sheets, with full-size representative checks. Non-English native-speaker review remains pending; English-only assistant commands/replies are disclosed.

Amazon supports 15 description locale slots but this draft's media modal offers only 11: en-US, en-GB, en-AU, en-IN, de-DE, es-ES, es-MX, fr-FR, it-IT, ja-JP and pt-BR. The Dutch, Polish, European Portuguese and Swedish media sets remain ready in this pack; their stores use English US fallback because the console offers no separate media slot.

Generate deterministic exports with `python scripts/premium_assets.py` after actual raw captures exist. Source artwork is preserved. Exact capture readiness and provenance are in `docs/localized-device-evidence.json` and `MANIFEST.json`.
