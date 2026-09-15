# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/) and the project uses semantic
versioning.

## [2.4.0] - 2026-09-15

### Added
- **Refreshed theme model**: the three schemes are now **Terminal** (green
  phosphor on black), **Classic** (full-colour ASCII art on black) and
  **Material You** (wallpaper-driven colours). Material You now follows the
  system light/dark mode automatically instead of having separate entries.
- **Haptics toggle**: Management → Haptics turns the tap vibration on or off,
  and the choice is remembered between launches.
- **Shooting stars**: on fair nights a comet now streaks across the sky.

### Changed
- Theme, text size, contrast, scanlines and haptics settings all persist
  together and are driven from the engine's `UiSettings`, so headless tests
  can exercise them too.
- Tests now cover the three-way theme cycle and the haptics toggle (102 total).

## [2.3.0] - 2026-09-14

### Added
- **Three themes**: Classic Green, Material Dark and Material Light (all
  Material schemes use the wallpaper's dynamic colours on Android 12+).
  Scanlines and the CRT vignette only apply to dark themes.
- **Save overwrite UX**: saving under an existing name offers to overwrite,
  and each slot has a `[sv]` action on the saved-games screen to save the
  current journey over it.
- **Export / import**: Management → Export/Import writes and reads all save
  slots (and the autosave) through the system file picker, using a portable
  text bundle (`SaveBundle`).
- **More animation**: an animated trail progress bar with a bobbing wagon
  marker, lightning flashes during thunderstorms, and a tumbleweed rolling
  across fair-weather plains.
- 5 new tests (101 total), including a `SaveBundle` round-trip with tricky
  characters.

## [2.2.0] - 2026-09-14

### Added
- **Material 3 dialogs** — the app now uses Material Components with a
  Material 3 dark theme, so the name/epitaph/save/rename/delete dialogs are
  proper Material surfaces. When Material You is selected (and the device
  supports it) the dialogs also pick up the wallpaper's dynamic colours via
  `DynamicColors`.

## [2.1.0] - 2026-09-14

### Added
- **Material You theme toggle** (Management → Theme). On Android 12+ the whole
  terminal is recoloured from the system wallpaper's dynamic colours; older
  devices get a Material 3 dark baseline. The default remains the dark green
  retro terminal, and the choice is remembered.
- **Coloured animations** — weather particles now carry colour: snow is bright
  white, blizzards white, hail/rain cyan, heavy rain blue and thunderstorms
  yellow, and they map onto the active theme.
- 2 new tests (96 total).

## [2.0.0] - 2026-09-14

### Added
- **Tappable menus everywhere** — every menu row now shows a bright `>`
  marker and has a forgiving tap target that extends past the label. Stock
  warnings: low food or tired oxen blink a red alert on the travel screen,
  and food/ammo/oxen lines are colour-coded when they run low.
- **Full save-slot management** — the saved-games screen now paginates
  unlimited slots, labels each slot with date/miles/profession, and offers
  per-slot **rename** (`[ren]`) and **delete** (`[del]`). The autosave appears
  as a **Quick save (auto)** entry you can load or delete from the list.
- 5 new polish tests (94 total).

### Changed
- The pause menu's Load and Management screens return to the pause menu.

## [1.9.0] - 2026-09-14

### Added
- **Living skies** — a sun with turning rays, drifting clouds and flitting
  birds over the travel, landmark and river scenes (weather-aware: more
  clouds and no sun in rain, snow and storms).
- **Event illustrations** — breakdowns show a wheel, bandits a highwayman,
  snakebites a snake, wild animals a wolf, helpful Shoshone a teepee, wild
  fruit a bush, fire, riders and a broken wagon for the stranded family.
- **Campfire smoke** curls above the pause-screen fire.
- **Sunrise** sun on the arrival screen.
- **Screen-change transitions** — an opt-in top-to-bottom reveal when the
  screen changes.
- 2 new animation tests (89 total).

## [1.8.0] - 2026-09-14

### Added
- **Animated visuals.** The engine now has an animation clock that the app
  advances a few times a second, so the whole game moves:
  - drifting **rain, snow and hail** across the travel, landmark and river
    scenes (matched to the current weather),
  - **flowing river** waves on crossings and travel,
  - a **flickering campfire** on the pause screen,
  - **twinkling stars** over the title wagon,
  - a **blinking prompt** on continue screens.
- The app animates continuously (not only during minigames) and de-duplicates
  debug output so animation does not flood the log.
- 8 animation tests (87 total).

## [1.7.0] - 2026-09-14

### Added
- **Pause menu** — press Back (or tap the new `[||]` button) for a camp-at-night
  pause screen: Resume, Save game, **Quick save**, **Quick load**, Load a saved
  game, Management options, Save and return to title, or Quit. Load and
  Management return to the pause menu when opened from it.
- **More ASCII art** — a night camp for the pause screen, and new landmark
  scenes: a town at Independence, a tall spire for Chimney Rock, a gap for
  South Pass, a gorge for The Dalles, plus a Willamette Valley cabin scene on
  the arrival screen and stars over the title wagon.
- 8 new pause/quick-save tests (79 total).

## [1.6.0] - 2026-09-14

### Added
- **Weather ambience** — the background tint now shifts with the weather and
  terrain (cold blues for snow and rain, warm browns for heat, green on the
  open plains), on top of the CRT scanlines and vignette.
- **Trail of the Day** — a date-seeded daily challenge: everyone gets the same
  trail for the day.
- A much larger test suite: **71 tests** including save/load round-trips at
  every resumable phase, coverage of every event and every river/landmark/
  ending option, a determinism replay test, a balance test over 30 seeded
  games, ambience checks and the layout fuzz across 120 viewports.
- The on-device smoke test now opens the journal, switches rations, and must
  finish the trail (arrival or death), and it is robust to wrapped text.

### Changed
- The RNG can be re-seeded (used by the Trail of the Day).

## [1.5.0] - 2026-09-14

Features the original designer, Philip Bouchard, said on The Verge's
*Version History* (13 Sep 2026) that he wished he'd had the space to add.

### Added
- **Barlow Road minigame** — the toll-road ending over the Cascades that was
  cut for disc space: drive a winding, rutted mountain track, dodging rocks,
  and arrive with only the damage you take. Plus a **portage** option.
- **Bargaining** — traders, mountain men and soldiers offer deals you can
  accept, haggle over (bankers get the best odds), or decline. New
  "Wheeler-Dealer" achievement.
- **Oxen health** — hard pace, heat, cold and hunger wear the team down,
  slowing travel and risking a collapsed ox; rest restores them.
- **Richer talk to people** — sixteen monologues plus useful rumors, and a
  historical note ("Learn the history") at every landmark, with a
  "Student of the Trail" achievement.
- **Moral encounters** — a stranded family asks for help; sharing earns the
  "Good Samaritan" achievement.
- **Epilogue** on arrival describing each traveler's fate, and a **share**
  action for your journey summary.
- River crossings now report an estimated depth in feet.

### Fixed
- The Dalles options no longer skip straight to arrival; the Barlow and
  rafting endings run the actual minigames.

## [1.4.0] - 2026-09-14

### Added
- **Save states**: name and keep multiple saved games, load or delete them from
  a "Saved games" screen, and a separate autosave offered as **Continue** on the
  title screen. The title is now always shown at launch, with Continue/Load
  entries appearing when saves exist.
- **Achievements**: twelve milestones (Oregon or Bust, Everyone Made It, Trail
  Legend, Meat on the Wagon, Sharpshooter, Frugal, Well Off, Handy, Ferryman,
  Forced a Crossing, You Have Died of Dysentery, Sole Survivor) with an
  Awards screen and a toast when one unlocks.
- **Statistics**: games played, arrivals, deaths, best score, total miles and
  achievement count, kept across runs.
- Journal records achievement unlocks.

### Fixed
- Selling goods could drive food/ammo/oxen negative when only a partial step
  remained; selling now returns whole steps only and can never go below zero.
- Career statistics were not counted when starting a game; they now increment
  when the journey actually begins.

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
