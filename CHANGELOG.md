# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/) and the project uses semantic
versioning.

## [1.1.0] - 2026-09-14

### Added
- **Trail journal** — a dated diary of arrivals, illness, hunts, river
  crossings, cutoffs, encounters, rests and the final arrival, with paging.
- **Occupation perks** — Bankers get a 10% fort discount, Carpenters can
  repair a breakdown without a spare part half the time, Farmers bring home
  50% more meat.
- **Difficulty** (Easy/Normal/Hard) scaling event and illness frequency.
- **Accessibility settings** — text size, high-contrast palette and CRT
  scanlines, persisted and applied live to the adaptive terminal.
- **Persistent graves** — graves left by earlier journeys appear when you
  reach the same stretch of trail.
- **Score breakdown** on the arrival screen.
- `tools/ot_smoke.py` — a committed end-to-end on-device smoke test.
- An instrumented GitHub Actions workflow that boots an emulator and runs it.

### Changed
- Rendering split out of `Game.kt` into `GameRender.kt`; `Game.kt` keeps the
  rules and a small `render()` dispatcher.
- Cutoffs now use an explicit target so the miles/landmark invariant holds.
- Miles can never fall behind the last landmark reached.
- Debug screen dumping is keyed off `BuildConfig.DEBUG`.
- Bumped version to 1.1.0 (versionCode 2).

### Fixed
- The calendar skipped the first day of each month, corrupting arrival dates
  and the days-on-trail count.
- The Columbia River rafting choice fell through to immediate arrival.
- A Kotlin string interpolation bug printed the `Inventory` object in the
  score breakdown.

### Removed
- Dead code: `Screen.toAnsi`, `Screen.hotspotOnText`, unused ASCII art,
  `Sound.HIT` and unused fields.

## [1.0.0] - 2026-09-13

### Added
- Full Oregon Trail game: 18 landmarks, four river crossings, the Lander and
  Sublette cutoffs, Banker/Carpenter/Farmer, month selection, five named party
  members, Matt's General Store, pace/rations/rest/trade, weather, disease,
  injury, bandits, wild animals, blizzards, the ASCII hunting minigame, the
  Columbia River rafting finale, death with a gravestone and the Oregon Top Ten.
- Pure-Kotlin `:engine` module with no Android dependencies, and an adaptive
  `:app` front-end with a monospace terminal canvas and touch controls.
- Save/resume across rotation and process death.
- Unit and layout-fuzz tests, and a signed release APK.
