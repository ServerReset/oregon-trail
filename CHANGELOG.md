# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/) and the project uses semantic
versioning.

## [1.3.0] - 2026-09-14

### Added
- **Watch input**: the rotary bezel/crown and D-pad move a highlighted
  selection cursor; the centre/side button or a tap anywhere activates it.
  Enabled automatically on watches and round displays (a debug override lets
  it be tested on a phone).
- **CRT vignette** for a more authentic phosphor look.
- **Map wagon**: the current position is drawn as a little wagon on the map.
- An in-progress **hunt or rafting run now survives a form-factor change**:
  folding, unfolding or resizing rebuilds the field at the new size while
  keeping meat, shots, kills, progress and hits.
- Compact management menu for watch-sized screens.

### Fixed
- Density and configuration changes now recompute the character grid even
  when the view size itself does not change (display settings, accessibility,
  emulator overrides).
- Tapping a hotspot also moves the selection cursor to it.

## [1.2.0] - 2026-09-14

### Added
- **Runs on any screen** — ultra-compact layouts for watches and phone cover
  screens (short labels, tappable store rows, one-row hunting/rafting controls)
  and round-watch safe-area insetting. Verified from a 16×10 watch grid up to a
  120-column unfolded foldable, including folding and unfolding mid-journey.
- **Rest for a chosen number of days** (1, 2, 3 or 5), as in the original.
- **Write your own epitaph** on the death screen; it is saved as the gravestone.
- Manifest declares optional touchscreen/watch features and resizeable screens.
- Tests now cover watch/cover viewports and exercise the rest, trade and riders
  choice screens across all fuzzed sizes.

### Fixed
- Choice options (`rest:`, `riders:`, `trade:`) were registered as tap targets
  but never routed to the choice handler, so tapping them directly did nothing.
- Hunting/rafting fields now size themselves for very short screens.

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
