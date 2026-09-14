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

Prebuilt signed APKs are attached to the [Releases](../../releases) page.
Minimum Android version: **7.0 (API 24)**. Install with `adb install app-release.apk`
or by opening the APK on the device.

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
  rocks, or pay the Barlow Road toll instead.
- **Disease and death** — party members sicken and die; you get a gravestone
  and a full epitaph. Graves left by earlier journeys appear when you reach
  the same stretch of trail.
- **Scoring with a full breakdown** and the Oregon Top Ten, persisted between runs.
- **Difficulty and accessibility** — Easy/Normal/Hard changes how often trouble
  strikes; text size, a high-contrast palette and CRT scanlines can be toggled
  on the Management screen and are remembered.
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
| Round watch                    | Content inset to a centred square, one-row controls    |
| Multi-window / split screen    | Re-lays out live without losing the run                |

Open or close the phone mid-journey and play continues where it left off
(the activity handles configuration changes and the run is saved on pause).

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
| Map         | Tap `[ Back ]`                                        |

## Architecture

The project is deliberately split so the rules are portable and testable:

```
:engine   Pure Kotlin/JVM library — no Android dependencies.
          Rng, Model, Data (trail/items), Terminal (screen + tap targets),
          Ascii (art + block font), Hunting, Rafting, Persistence,
          Game (rules/state machine) and GameRender (all screen drawing).
:app      Android front-end.
          TerminalView  — adaptive monospace canvas that recomputes its grid
                          for any screen size/orientation and maps taps to
                          engine hotspots.
          MainActivity  — immersive fullscreen, sound, name dialog, minigame
                          ticker, SharedPreferences persistence.
```

The engine exposes just three calls to any front-end: `render(): Screen`,
`onTap(id: String)`, and `huntTick()`. That keeps it easy to add a desktop,
terminal or web front-end later.

## Building

```bash
./gradlew :engine:test          # run the unit + layout-fuzz tests
./gradlew :app:assembleDebug    # debug APK
./gradlew :app:assembleRelease  # release APK (needs keystore.properties)
```

For a signed release build, create `keystore.properties` in the project root:

```properties
storeFile=../release.keystore
storePassword=...
keyAlias=...
keyPassword=...
```

If the file is absent the release build is produced unsigned.

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
runners is slow, that job runs on demand or for a version tag and is marked
non-blocking. The fast JVM suite (`.github/workflows/android.yml`) runs on
every push and builds the debug and release APKs.

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
