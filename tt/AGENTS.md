# tt/ notes for agents

Read the root [AGENTS.md](../AGENTS.md) first. This is the game client. Entry point is
`com.oddlabs.tt.Main`; run it with `./gradlew tt:run`.

## The line that matters

Everything here falls on one side of the determinism boundary described in the root file.

**Simulation** (`model/`, `model/behaviour/`, `pathfinder/`, `landscape/`, `player/`,
`trigger/`, `gamemode/`): runs identically on every client in a lockstep match and during
`--eventload` replay. No wall-clock time, no unseeded randomness, no hash-order iteration,
no branching on local settings or hardware. Time comes from `LocalEventQueue` and
`AnimationManager` ticks. A behavior change here means `SIM_VERSION` needs a bump, which is
a maintainer's call.

**Presentation** (`render/`, `gui/`, `form/`, `guievent/`, `camera/`, `audio/`, `particle/`,
`font/`, `vbo/`, `input/`, `window/`): free of those constraints, and the right place for
anything that should look or feel different per player.

When a feature needs both, keep the simulation half as small as possible.

## Where things live

- `delegate/` is the screen and mode state machine: menus, in-game, placing, first person,
  countdown, stats. A `Delegate` owns input handling and what is drawn.
- `form/` holds the menu and dialog forms; `gui/` holds the widget toolkit they are built
  from.
- `render/` is the OpenGL layer. `Renderer` and `DefaultRenderer` drive it, `PostProcessor`
  runs screen-space passes, `render/state/` wraps GL state. Shaders live in
  `render/shader/` as GLSL text blocks inside the Java source, not separate files, and
  target `#version 410 core`, so a GL 4.1 capable driver is the floor. Rendering uses
  LWJGL 3 with GLFW.
- `net/` is the client side of matchmaking and the in-game lockstep network.
- `steam/` wraps steamworks4j. Steam is optional at runtime: every path through it must work
  with Steam absent, and nothing in the simulation may depend on it.
- `player/campaign/` and `trigger/campaign/` are the single-player campaign islands.
- `resource/` and `landscape/` load the binary assets produced by the `assets` module.

## Strings

User-visible text belongs in the `.properties` bundle next to the class under
`src/main/resources/com/oddlabs/tt/`. Add new keys to the English base file only and leave
the locale files to the translation sync. Never write a translation yourself, and never
generate art or sound for a feature: see the creative work policy in the root
[AGENTS.md](../AGENTS.md#policy-creative-work-stays-human).

## Practical notes

- `module-info.java` is real here. New dependencies may need a `requires`, and steamworks4j
  stays on the classpath as the unnamed module via an `--add-reads` compiler flag.
- Assertions are on in the run configuration (`-ea -esa`), so an `assert` that fires in
  development is a real failure, not a warning.
- `./gradlew :tt:compileJava` is the fast feedback loop and `./gradlew :tt:test` runs the headless
  simulation scenarios in a few seconds. Compiling is not the same as working; say which one you did.
- `headless/` holds `HeadlessWorld` and `HeadlessPeers`, the no-window entry points the tests use.
  `input/scripted/ScriptedInputProvider` is the loopback driver for play-testing the windowed client.
