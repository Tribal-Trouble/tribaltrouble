# Driving the game from a script or an LLM

Compiling proves the code builds. Running it and looking at what actually happens is what catches the
bugs. This document explains how an agent (Claude Code, a script, a CI job) can launch the game, press
its buttons, read its state, and verify results, without touching game code or the OS input system.

## How it works

`ScriptedInputProvider` wraps the normal GLFW input provider. When the system property
`com.oddlabs.tt.input.port` is set, it listens on that port on `127.0.0.1`, one text command per line,
and merges the resulting events into the normal input stream at the next poll. Everything downstream
is unchanged: the GUI, the game, and the deterministic event log all see ordinary input. A scripted
session is therefore also a normal event-log recording, replayable with `--eventload`.

Three query commands read the game back out:

- `tree` lists every live GUI widget with its class, text and pixel bounds.
- `state` lists players, the camera, and every unit and building with world and screen coordinates.
- `sites` lists legal building placement cells near the selection with screen coordinates.

All pixel coordinates, in and out, are framebuffer pixels with a top-left origin. `move 100 200` puts
the cursor exactly where `tree` or `state` reported something at `100 200`.

The provider is off unless the property is set. Nothing in shipped builds changes.

## Enabling it

Run task (the Gradle daemon and the game both pick up the variable, which is harmless):

```
JAVA_TOOL_OPTIONS="-Dcom.oddlabs.tt.input.port=7777" ./gradlew :tt:run
```

Installed distribution, which is what you want for more than one client:

```
./gradlew :tt:installDist
TT_OPTS="-Dcom.oddlabs.tt.input.port=7777" tt/build/install/tt/bin/tt
```

The game logs `Scripted input listening on localhost/127.0.0.1:7777` on startup. If the port is
taken you get a `BindException` in the log and the game runs normally without the socket, so pick
another port and relaunch.

## Talking to it

`tools/scripts/ttinput.py` is a minimal client: each argument is one command, replies are printed, and a
`sleep:N` prefix pauses before a command. The port comes from the `TT_PORT` environment variable
(default 7777) and the reply timeout from `TT_TIMEOUT` in seconds (default 300, so a long `wait`
or `warp` does not time out). It is plain Python with no dependencies; any language that can open a
TCP socket and write lines works just as well.

```
python tools/scripts/ttinput.py "ping" "tree"
python tools/scripts/ttinput.py "move 134 635" "click" "sleep:1.5 shot C:/tmp/menu.bmp"
```

Commands:

| Command | Effect |
|---|---|
| `move X Y` | Cursor to framebuffer pixel X,Y |
| `click [left\|right\|middle]` | Press now, release at the next poll |
| `down [button]`, `up [button]` | Press or release; use with `move` between them to drag |
| `key NAME [down\|up]` | Key by `Key` enum name (`RETURN`, `ESCAPE`, `BACK`, `A`); default is press then release |
| `type TEXT` | Character input, one character per poll |
| `shot [PATH]` | Screenshot to PATH (or a numbered file in the home directory); replies with the file |
| `tree` | GUI tree, one line per widget: `depth Class x y w h [text]`, ends with `END` |
| `find TEXT` | Framebuffer centre of the last widget whose label matches TEXT, e.g. `find Create new game` |
| `press TEXT` | `find` then click it in one step; menu buttons, tabs, pulldown items and list rows all work |
| `rpress TEXT` | Same with the right button, e.g. the player pulldown in the chat panel |
| `choose LABEL=ITEM` | Open the pulldown on LABEL's row and pick ITEM, e.g. `choose Game speed:=Ludicrous` |
| `edit LABEL=TEXT` | Set the edit field on LABEL's row (or the field currently showing LABEL) to TEXT |
| `state` | Game state, ends with `END`; `err not in game` from menus |
| `sites [QUARTERS\|ARMORY\|TOWER\|SHIP] [radius]` | Legal placement cells, ends with `END` |
| `tick` | Current simulation tick (high-precision ticks, 4 ms each) |
| `warp MILLIS` | Fast-forward the simulation by that much game time; answers once it has caught up |
| `wait tick N` | Answers once the simulation reaches tick N |
| `wait delegate NAME` | Answers once the game is on that delegate, e.g. `wait delegate SelectionDelegate` |
| `wait text TEXT` | Answers once a widget with that label is on screen, e.g. `wait text Ready` |
| `wait ... SECONDS` | Any wait takes an optional trailing timeout and answers `err timeout` when it passes, e.g. `wait delegate SelectionDelegate 90`; give up on the server side, never only on the client side, or the queue stays blocked |
| `ping` | `ok pong` |

Prefer `warp` and `wait` to sleeping. `warp 60000` runs a minute of game time in a few frames and
returns when the ticks have actually been simulated, so the next `state` is exact. The game loop
refuses any single frame step over thirty seconds, so `warp` feeds the clock in chunks internally;
you can ask for as much as you like. `warp` only moves the local clock. In multiplayer the simulation
advances at the pace the router reports, so a warped client gets no further ahead and just waits.

Every command is answered `ok ...` or `err ...` once it has been applied on the game thread, so a
client can simply send, read one line, and move on. A `click` is answered after the release has been
applied, one frame after the press.

### Reading `state`

```
delegate SelectionDelegate
tick 18250
camera target 281.9 22.9 45.7
player 0 local Player team 0 units 20/250 buildings 0
player 1 AI1 team 1 units 20/250 buildings 1
unit 0 Peon world 279.0 59.0 10.3 screen 1321 763 hp 1 selected
building 1 Quarters world 247.0 365.0 20.8 screen 1302 -854 hp 2
```

`simtick` is the lockstep tick and each `checksum T V` line is the world checksum at checksum tick T, kept for the
last eight boundaries; two clients whose checksums match at the same T are running the same simulation, which is
the only exact comparison, since `tick` is the local real-time clock and unit positions move between frames.

`delegate` tells you what the game is doing: `SelectionDelegate` is normal play, `PlacingDelegate`
means a building is being placed, anything else is a menu. Off-screen objects have negative or
oversized screen coordinates; move the camera or pick another target.

### Reading `sites`

```
Quarters size 3 center 141 29 radius 12 legal 69
site 137 25 world 275.0 51.0 screen 1180 802
```

Click any reported screen point while in placing mode and the building lands there. The delegate
logs `placeObject: Placement illegal` if a spot was refused, which should not happen for a reported
site unless the world changed between the query and the click.

## Recipe: start a skirmish and build

Coordinates below are for a 2560x1600 framebuffer; read them from `tree` rather than trusting them.

1. `tree`, find the `MenuButton` rows. They are listed bottom-up, so Single player is the fourth.
   Click its centre.
2. `tree` again for the create-game dialog: `OKButton`, `Advanced...`, tabs, pulldowns all have bounds.
   Click OK.
3. Wait about 12 seconds, then `state` until `delegate SelectionDelegate` appears.
4. Take the bounding box of your `unit 0` screen coordinates and drag a box around it:
   `move x1 y1`, `down`, `move x2 y2`, `up`. `state` now shows them `selected`.
5. `tree` for the HUD: the peon group is a `NonFocusGroup` of `NonFocusIconButton`s on the right
   edge, listed bottom-up: gather/repair, quarters, armory, tower. Click quarters.
6. `sites QUARTERS 12`, pick a site on screen, `move` there and `click`.
7. `state` shows `building 0 Quarters` and the peons walking to it.

Right-click in game moves the camera, it does not issue orders. Orders go through the HUD buttons
and left-clicks.

## Recipe: local multiplayer with two clients

Everything runs on one machine from the terminal. Nothing needs administrator rights.

**Hosts file.** The client resolves `matchmaking.<domain>` and `router.<domain>` from the
`domain_name` setting. With `domain_name=local`, map both names to `127.0.0.1` in
`C:\Windows\System32\drivers\etc\hosts`:

```
127.0.0.1    matchmaking.local
127.0.0.1    router.local
```

**Database.** The servers need MySQL. If the installed service is not running and you cannot start
it, run a throwaway server from the same install in a scratch directory. It needs no privileges:

```
mysqld --no-defaults --initialize-insecure --datadir=<scratch>/mysql/data --console
mysqld --no-defaults --datadir=<scratch>/mysql/data --port=3307 --bind-address=127.0.0.1 --console --mysqlx=OFF &
mysql -h 127.0.0.1 -P 3307 -u root < initmysql.sql
for f in database/00*.sql; do mysql -h 127.0.0.1 -P 3307 -u root oddlabs < $f; done
mysql -h 127.0.0.1 -P 3307 -u root -e "CREATE USER 'matchmaker'@'localhost' IDENTIFIED BY 'matchmaker';
  GRANT ALL PRIVILEGES ON oddlabs.* TO 'matchmaker'@'localhost'; FLUSH PRIVILEGES;"
```

**Servers.** `./gradlew :server:installDist`, then put a `server.properties` in
`server/build/install/server/` (the start scripts change into that directory):

```
DB_CONNECTION=jdbc:mysql://127.0.0.1:3307/oddlabs
DB_USER=matchmaker
SQL_PASS=matchmaker
WEBSITE_DOMAIN=local
STEAM_ONLY_AUTH=false
```

Start `bin/matchmaker` and `bin/router` in the background. The matchmaker logs
`Matchmaking server started: v...-104.1001`; the API and sim versions must match the clients.

**Clients.** Portable mode: if a `TribalTrouble` directory exists in the working directory, the game
uses it for settings and logs. Make one directory per client with a `settings` file:

```
domain_name=local
fullscreen=false
view_width=1600
view_height=1000
new_view_width=1600
new_view_height=1000
play_music=false
play_sfx=false
remember_login=true
username=llmtest1
first_run=false
warning_no_sound=false
```

Launch each client from its own directory with its own port:

```
cd client1 && TT_OPTS="-Dcom.oddlabs.tt.input.port=7777" <repo>/tt/build/install/tt/bin/tt
cd client2 && TT_OPTS="-Dcom.oddlabs.tt.input.port=7790" <repo>/tt/build/install/tt/bin/tt
```

**Flow, all through `tree` and clicks.** On each client: Multiplayer, New account, fill username,
email, password twice, Create User. Then Create new profile, type a nick, Create profile, select
the row, Join. That lands in the lobby on the Chat tab; click the Games tab. On the host: Create new
game, OK, then Ready. On the joiner: Update game list if needed, select the row, Join game, Ready.
Back on the host: Start. Start stays disabled until every filled slot, host included, is ready.

**Verification.** After both report `delegate SelectionDelegate`, compare `state` from both ports
with the `screen` columns stripped. Every unit and building should match exactly; they are the same
lockstep simulation. Then act on one client and confirm the other sees the result.

Two snapshots taken seconds apart will differ by a fraction of a metre on anything that is moving,
which is timing, not desync. For an exact comparison read the `tick` line of both snapshots and
only compare when they match, or `wait tick N` on both clients for the same N before reading. A real
desync also shows up as `Checksum mismatch` in the client log.

## What to verify, and how

- A menu change: `tree` before and after, check the widget exists, has the right text, and sits where
  it should relative to its neighbours.
- A gameplay rule: drive to the situation, then assert on `state` (counts, positions, hit points,
  ownership) rather than on pixels.
- A desync: two clients, `state` on both, diff the world coordinates. Any difference is a bug.
- A visual: `shot`, then look at the image. The game's BMP writer currently drops the red channel,
  so convert with a channel remap before viewing. Use screenshots for layout questions only.

Prefer `state` and `tree` over screenshots. They are exact, cheap to parse, and do not depend on
rendering.

## Using cheaper models

The driving loop is mechanical once the recipe is known: send commands, read replies, compare
numbers. Hand that loop to a smaller model with this document, the ports, and a precise pass/fail
question, and keep the expensive model for deciding what to test and interpreting failures. A
subagent given "on port 7790 select all peons and build quarters via `sites`, then confirm on port
7777 that player 1 has one building" needs nothing else.

## Towards a headless simulation

The socket driver still needs a window because the simulation only runs inside the rendering
client. A survey of where the simulation packages touch OpenGL shows the split is smaller than it
looks:

| Package | Files touching GL or the renderer | Notes |
|---|---|---|
| `player`, `pathfinder`, `event` | 0 of 46 | Already renderer-free |
| `model` | 4 of 67 | `Unit`, `LandBuilding`, `Ship` only in their render methods; `RacesResources` loads textures and effects next to the unit and building templates |
| `landscape` | 2 of 22 | `HeightMap` uploads a height texture in its constructor; `LandscapeBaker` is texture baking |
| `procedural` | 13 of 35 | Texture generators, plus `Landscape` which mixes heightmap generation with texture work |
| `resource` | 9 of 19 | `IslandGenerator.generate` builds terrain then bakes textures in the same call |
| `net` | 5 of 43 | `WorldStarter`, `PeerHub`, `MatchmakingClient` reach for the renderer or GUI |
| `animation` | 1 of 5 | `AnimationManager.runGameLoop` is the game loop and it is welded to `Renderer` and the window |

What it would take, in order, each step verifiable with an event-log replay because none of them
may change a single simulation value:

1. **Split terrain generation from texture baking.** `IslandGenerator.generate` and `Landscape`
   produce the heightmap, trees, supplies, start positions and the access and build grids on the
   CPU, then bake colour maps on the GPU. Return the CPU part as `WorldInfo` first and bake
   afterwards from the renderer. Same for `HeightMap`, whose height texture belongs to the renderer
   that reads it, not to the map.
2. **Split `RacesResources` into templates and art.** Unit and building templates, abilities, costs
   and build times are simulation data; textures, sprites, halos and smoke are not. Two classes,
   or one class with the art loader optional.
3. **A headless world owner.** `WorldViewer` today owns the world, the camera, the GUI panels and
   the peer hub. The simulation needs only the peer hub, the local player interface and a
   notification sink. A small `HeadlessWorld` that constructs `World` with a no-op audio
   implementation and no GUI covers it.
4. **A headless loop.** `AnimationManager.runGameLoop` polls the renderer's input and window every
   tick. A second loop that ticks `LocalEventQueue` and the network at a fixed step, with no
   renderer, is a few dozen lines next to the existing one.
5. **Leave the render methods where they are.** The GL references in `Unit`, `LandBuilding` and
   `Ship` are constants inside methods only the renderer calls. Without a renderer nothing calls
   them, and LWJGL constants load without a GL context, so they cost nothing.

Loopback networking already runs in-process for single player, so a headless test is
`Server` plus `Client` plus the headless owner in one JVM, feeding orders through the same
`PlayerInterface` the GUI uses. Rough size: ten to fifteen files, a few hundred lines moved rather
than rewritten, and one to two weeks including the replay verification after each step. The risk
sits in steps 1 and 2, which is exactly what the replay checksums guard.

## Cleanup

Kill the client processes, then the servers, then `mysqld`. Portable clients leave nothing behind
outside their directories. If you muted a real installation by editing its settings file, restore it
after the game exits, because the game rewrites settings on shutdown.

## Known limitations

- Injected input never sets OS-level key state, so modifier combinations only work through `key ...
  down` and `key ... up` around the other key.
- `sites` reports the local player's race templates only.
- `state` does not yet include resources carried by buildings or unit orders.
- Screenshots go through the game's own BMP writer with its red-channel bug.
