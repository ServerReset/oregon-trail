# The Oregon Trail — ASCII Edition (Android)

A faithful, full-featured recreation of the classic **Oregon Trail**, rendered
entirely in monospace ASCII art like the teleprinter and Apple II games of the
1970s and 80s. It runs on any Android phone or tablet, in portrait or landscape,
and scales its character grid to every screen size.

The game plays on a retro green-phosphor terminal with tap controls, sound
cues, saved games, and a persistent Oregon Top Ten.

```
                  _.---._
                .'  ___  '.
               /   /   \   \
              |   |     |   |
              |   |     |   |
               \   \___/   /
                '._______.'
          _______|_______|_______
         |_______________________|
              (O)       (O)
       ###  ####  #####  ###   ###  #   #
      #   # #   # #     #   # #   # ##  #
      #   # ####  ###   # ### #   # # # #
      #   # #  #  #     #   # #   # #  ##
       ###  #   # #####  ###   ###  #   #
       ##### ####   ###  ##### #
         #   #   # #   #   #   #
         #   ####  #####   #   #
         #   #  #  #   #   #   #
         #   #   # #   # ##### #####
```

## Download

Every push to `master` builds, tests and **publishes the signed APK to GitHub
Releases** automatically (see `.github/workflows/android.yml`). Grab
`oregon-trail.apk` from the newest release. Minimum Android version:
**7.0 (API 24)**. Install with `adb install oregon-trail.apk` or by opening the
APK on the device.

Debug and release APKs are also attached to each CI run as artifacts.

## Features

- **Full trail** — 18 landmarks from Independence, Missouri to the Willamette
  Valley, including the Kansas, Big Blue, Green and Snake river crossings and
  the historic Lander and Sublette cutoffs.
- **Real setup** — choose Banker, Carpenter or Farmer; pick a departure month;
  name all five members of the party. Each occupation has a perk: bankers get
  a fort discount, carpenters can repair wagons without spare parts, farmers
  bring home more meat.
- **Matt's General Store** — buy oxen, food, clothing, ammunition and spare
  wheels/axles/tongues with a touch stepper and a running cash total. Fort
  prices are higher, as they were on the real trail.
- **Trail journal** — every arrival, illness, hunt, river crossing and mishap
  is written to a dated diary you can page back through.
- **Day-by-day travel** — pace, food rations, resting, trading, weather,
  disease, injury, thieves, bandits, wild animals, blizzards and helpful
  Shoshone.
- **River crossings** — ford, caulk and float, take a ferry, hire a guide, or
  wait for conditions to improve.
- **ASCII hunting minigame** — a roguelike hunt with wandering animals, trees,
  rocks, a moving hunter and travelling bullets. Carry back up to 100 lb of meat.
- **Columbia River rafting finale** — steer a raft through the rapids, dodging
  rocks; or take the **Barlow Road** over the Cascades (the second ending
  minigame the original designer never had room to build), or portage slowly
  and safely.
- **Bargaining** — traders, mountain men and soldiers offer deals; accept,
  haggle for more, or decline. Bankers bargain best.
- **Oxen health** — hard driving and harsh weather wear down the team, slowing
  travel and risking a collapse if you don't rest.
- **Talk to people** — sixteen trail monologues plus useful rumors, and a
  "Learn the history" option at every landmark.
- **Epilogue and postcards** — read what became of each traveler, then send a
  shareable ASCII **postcard** (a PNG drawn from the game's art plus a text
  summary) from the pause menu or the arrival/death screens.
- **Landmark postcards** — each landmark shows a one-line quote, and river
  ferries and tolls are flagged when you cannot afford them.
- **Campfire stories and trail wisdom** — resting heals the party and settles
  in for a story told by one of your travelers.
- **Layered ASCII scenery** — plains, snow-capped peaks, pine forest, rivers,
  towns and forts are drawn in several colours with depth, and the sun crosses
  the sky as the day passes (stars and shooting stars after dark). A soft
  phosphor glow gives the whole display a CRT feel, and the default Classic
  theme renders the art in full colour.
- **Disease and death** — party members sicken and die; you get a gravestone
  and a full epitaph. Graves left by earlier journeys appear when you reach
  the same stretch of trail.
- **Scoring with a full breakdown** and the Oregon Top Ten, persisted between runs.
- **Difficulty and accessibility** — Easy/Normal/Hard changes how often trouble
  strikes; text size, a high-contrast palette and CRT scanlines can be toggled
  on the Management screen and are remembered.
- **Pause menu** — press Back or tap `[||]` for camp-at-night pause with save,
  quick save/load, options and quit.
- **Trail of the Day** — a date-seeded challenge, the same trail for everyone.
- **Themes** — **Classic Green**, **Material Dark** or **Material Light**
  (Material schemes borrow the system wallpaper's dynamic colours on Android
  12+); switch it on the Management screen. Dialogs are Material 3.
- **Coloured animations** — snow, hail, rain and storms each bring their own
  colour.
- **Animated ASCII** — drifting rain and snow, flowing rivers, a sun with
  turning rays, drifting clouds and birds, curling campfire smoke, twinkling
  stars, a blinking prompt, an animated progress bar, lightning flashes, a
  rolling tumbleweed and a screen-change reveal.
- **Event illustrations** — ASCII scenes for breakdowns, bandits, snakes,
  wolves, teepees, fruit, fire, riders and stranded wagons.
- **Weather ambience** — the background tint shifts with weather and terrain.
- **Save states** — name and keep multiple saved games (paged list with
  rename, **save-over** and delete), a **Quick save** entry under the
  autosave, a Continue option on the title, and **export/import** to a file.
- **Obvious controls** — menu rows show a `>` marker with generous tap
  targets, low supplies blink a warning, and status lines are colour-coded.
- **Achievements and statistics** — twelve milestones to unlock and a
  lifetime stats screen, both persisted between runs.
- **Save / resume** — the journey survives rotation, folding and process death.
- **Runs anywhere** — phones, tablets, foldables and flippable cover screens,
  and even small round watches. The terminal recomputes its character grid on
  every size change, drops to ultra-compact layouts with short labels on tiny
  screens, and insets itself inside the inscribed square on round displays.

## Device support

| Device                         | Behaviour                                              |
|--------------------------------|--------------------------------------------------------|
| Phone / tablet portrait        | Wide terminal, artwork, full menus                     |
| Landscape / unfolded foldable  | Extra columns for extra rows, everything stays on screen |
| Folded cover screen (e.g. 260×512) | Ultra-compact layout, short labels, tappable store rows |
| Round watch                    | Content inset to a centred square, one-row controls, rotary/crown selection cursor |
| Multi-window / split screen    | Re-lays out live without losing the run                |

Open or close the phone mid-journey and play continues where it left off
(the activity handles configuration changes and the run is saved on pause).
Even a hunt or river-rafting run in progress is resized and preserved.

On round watches the game uses the rotary bezel or crown: rotating moves a
highlighted target and pressing the centre button (or tapping the screen)
activates it.

## Controls

Everything is touch. Menu entries and buttons are tappable on the terminal.

| Screen      | Controls                                              |
|-------------|-------------------------------------------------------|
| Menus       | Tap the menu line or button                           |
| Store       | Tap `[-]` / `[+]` on an item row                      |
| Names       | Tap a name to open the keyboard dialog                |
| Hunting     | On-screen `^ < > v` D-pad to move/aim, `SHOOT`, `Return to trail` |
| Rafting     | `<< LEFT` / `RIGHT >>` to steer                            |
| Journal     | `[< Prev ]` / `[ Next >]` / `[ Back ]`                 |
| Rest        | Choose 1, 2, 3 or 5 days                               |
| Death       | Write your own epitaph                                 |
| Title       | Continue / Load saved games, Awards, Statistics        |
| Travel      | `10. Save game` writes a named save state             |
| Map         | Tap `[ Back ]`                                        |

## Architecture

The project is deliberately split so the rules are portable and testable. No
source file is longer than ~150 lines; each one holds a single screen, rule or
piece of the model:

```
:engine   Pure Kotlin/JVM library — no Android dependencies.
          Phase/Sound           small enums used by the front-end.
          GameState/GameSession the mutable state of a journey and session.
          Game                  the class that ties them together.
          GameInput/GameTravel/GameSimulation/GameWeather/GameDay/GameIllness
                                the day loop, weather and illness rules.
          GameStore/GameEvents/GameEventHazards/GameEventFinds/GameEncounters
          GameLandmarks/GameRiver/GameEnding/GameDeath/GameHunting
          GameTrade/GameJournal/GameScore/GameNotices  the rule modules.
          GameSave/GameViewport/GameRequests/GameDebug/GameTicks/GamePause
                                plumbing and small helpers.
          Render*.kt            one file per screen (title, travel, store…).
          Data/LandmarkData*/NameData/IllnessData/Meta/TrailLore/Items
                                the trail's content.
          Ascii/AsciiScenery/AsciiLandmarks/AsciiSky/AsciiEvents  the art.
          Screen/ScreenExtra/Cell/Palette/Hotspot  the terminal contract.
          Hunting/HuntingTick/HuntView/Rafting/Barlow/Persistence/SaveBundle
:app      Android front-end (no file over ~150 lines).
          TerminalView/TerminalGrid/TerminalPainter/TerminalInput
                                the adaptive monospace canvas.
          MainActivity/GameRenderer/GameInputHost/SystemUi/SaveFileHost
                                the activity and its hosts.
          ThemeColors/TerminalThemes/MaterialYouTheme  the colour schemes.
          SoundPlayer, dialogs (TextDialogs/SaveDialogs/LoadDialogs), settings.
```

The engine exposes just three calls to any front-end: `render(): Screen`,
`onTap(id: String)`, and `huntTick()`. That keeps it easy to add a desktop,
terminal or web front-end later.

## Balance

The economy and difficulty are tuned with a headless bot that plays hundreds
of full games through the real engine, and the results are locked in by
`BalanceTest`. Generate the report with:

```bash
./gradlew balanceReport      # writes docs/BALANCE.md
```

`docs/BALANCE.md` shows arrival rate, average survivors, journey length, final
score and end-of-trail supplies for every occupation and difficulty, plus the
most common causes of death. Use it to sanity-check any rules change.

## Building

```bash
./gradlew :engine:test          # run the unit + layout-fuzz tests
./gradlew :app:assembleDebug    # debug APK (signed with the release key)
./gradlew :app:assembleRelease  # release APK
python3 tools/gen_icons.py      # regenerate the ASCII launcher icons
```

The debug and release APKs are signed with the **same key**, so a newer build
installs as an update over an older one without an uninstall. Locally the key
comes from `keystore.properties` in the project root:

```properties
storeFile=../release.keystore
storePassword=...
keyAlias=...
keyPassword=...
```

If `keystore.properties` is absent, both variants fall back to the debug key.
CI decodes the release key from these repository secrets *before* building, so
the published APKs and your local builds share one signature:

| Secret | Purpose |
|--------|---------|
| `RELEASE_KEYSTORE_BASE64` | `base64 -w0 release.keystore` |
| `RELEASE_STORE_PASSWORD` | keystore password |
| `RELEASE_KEY_ALIAS` | key alias |
| `RELEASE_KEY_PASSWORD` | key password |

Without them, CI still builds and publishes an installable (debug-signed) APK.

## Testing

`./gradlew :engine:test` runs:

- game-rule tests (store accounting, travel, illness, death, scoring),
- a deterministic full playthrough test that must reach Oregon,
- a save/restore round-trip test,
- a **layout fuzz test** that drives a complete game through *every* phase at
  70 different viewport sizes (28–82 columns, 16–60 rows) and asserts that
  every tap target stays on screen.

The Android front-end is additionally exercised end to end by
`tools/ot_smoke.py`, which drives a real device/emulator with computed taps
(read from a debug-only logcat screen dump) through setup, the store, a river
crossing, landmarks, the rafting finale and on to Oregon:

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
python3 tools/ot_smoke.py --serial emulator-5554
```

A GitHub Actions job (`.github/workflows/instrumented.yml`) can boot an
Android emulator and run this smoke test; because emulator boot on hosted
runners is slow, that job is run on demand and is marked non-blocking. The
fast JVM suite (`.github/workflows/android.yml`) runs on every push and builds
the debug and release APKs.

The front-end was also manually verified across phone, tablet and landscape
metrics using computed taps and the logcat screen dump.

## Extending the game

- **Landmarks** live in `Data.landmarks`. Add an entry and it automatically
  appears on the map and becomes reachable. `cutoffId` marks a cutoff.
- **Events** are selected in `Game.eventPool()` and resolved in
  `Game.applyEvent()`.
- **Store goods** are the `Item` enum; add a value and the store and
  inventory adapt automatically.
- **Animals** are the `AnimalKind` enum; the hunting minigame pools them by
  region in `Game.huntPool()`.
- **Occupations, months, pace and rations** are enums in `Data.kt`.

## Credits

This is an independent fan recreation. *The Oregon Trail* was created by Don
Rawitsch, Bill Heinemann and Paul Dillenberger in 1971 and reimagined by MECC
(R. Philip Bouchard, John Krenz and colleagues) in 1985. Historical trail data
and event probabilities come from public accounts of the overland migrations.
All code in this repository is original.

## License

MIT — see [LICENSE](LICENSE).
