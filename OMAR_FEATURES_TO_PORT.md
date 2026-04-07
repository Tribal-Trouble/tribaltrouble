# Omar's Features to Port onto Bondolo's Base

Features from OmarAMokhtar/tribaltrouble merged PRs + open PRs + contributor PRs.
Starting from Bondolo's `bondolo/master` branch (`port/bondolo-clean`), porting features on top.
Formatter will be added AFTER all porting is complete — keeps diffs clean during port.

---

## Attribution

Before merging, do an interactive rebase to add proper co-author attribution to commits
based on who contributed the original work being ported. Use `Co-Authored-By` trailers.

| Contributor | GitHub | Work |
|-------------|--------|------|
| OmarAMokhtar | @OmarAMokhtar | Original fork maintainer, most features |
| Maxenor | @Maxenor | Countdown before game (#135), gatherer count (#153) |
| Bondolo | @bondolo | Base branch, LWJGL3 migration, rendering modernization |
| RcubDev | @RcubDev | Porting, rework, server setup, bug fixes |

---

## Porting Order

### Phase 1 — Foundation (DONE)

1. ~~**CI/CD + Build**~~ — GitHub Actions workflows, gradle setup, Steam deploy steps, Mac/Linux packaging. Server
   compile fixed (DBUtils, Authenticator, Client imports). Windows package tested and working. Formatter deferred to
   after porting.

### Phase 2 — Small Fixes (DONE)

2. ~~**Remove registration**~~ (#45) — removed reg key validation from server, pass null for reg keys
3. ~~**API version**~~ (#52) — added Compatibility.API_VERSION, replaced DB revision check
4. ~~**Pitch angle fix**~~ (#38) — fixed camera snap by centering cursor on first-person entry
5. ~~**Remove Oddlabs loading screen**~~ (#126) — replaced oddlabs logo texture
6. ~~**Fix close button**~~ (#146) — reset GLFW close flag after reading

- **Bondolo bug fix: high-DPI cursor offset** — fixed by Bondolo (commit a7a66a9e), included via rebase. Added
  getLogicalWidth/getLogicalHeight to Window interface for proper screen-coord to framebuffer-pixel mapping.

### Phase 3 — UI / Input Features (DONE)

7. ~~**Discord/GitHub buttons**~~ (#32) — added clickable GUIImage with target cursor, Discord/GitHub buttons on main
   menu, URLs in Settings.java
8. ~~**Cursor changes**~~ (#41, #78) — skipped, Bondolo already uses hardware cursor. Software cursor not needed for
   now. Revisit if we need to confine the cursor to the screen for windowed mode or something.
9. ~~**EditLine text selection**~~ (#46) — added selection, clipboard, word jump. Separated into
   handleClipboardInput/handleSelectionNavigation/handleSelectionReplacement to keep Bondolo's base nav untouched.
   Also fixed: Space key submitting form instead of typing; global keybinds (Ctrl+A etc.) firing while typing in text fields.

### Phase 4 — Gameplay Features

10. ~~**Enormous map size**~~ (from #93) — added SIZE_ENORMOUS (2048m) to Game, TerrainMenu, Landscape. Fixed
    SIZE_CARDINALITY (Omar's was broken at 3, now 4) so map codes round-trip correctly. Bondolo's instanced sprite
    rendering eliminates Omar's ShortVBO tree overflow — no tree rendering hack needed. Colormap texture chunking
    already handled by existing code.
14. ~~**Compass indicator**~~ (#147) — fully reworked from Omar's immediate-mode GL11 implementation. New
    CompassRenderer uses Bondolo's batched GUIRenderer pipeline (drawColoredQuad + font texture rendering). Hooked
    into SelectionDelegate.render2D(). Settings toggle + options menu checkbox with Omar's original i18n strings.

### Phase 5 — Server / Discord (DONE)

13. ~~**Countdown before game**~~ (#135) — countdown before multiplayer start (Maxenor). Requires working server.
11. ~~**Gatherer count + DeploySpinner fix**~~ (#153, #159) — show gatherer count per resource (Maxenor) + fix
    DeploySpinner constructor. Added `getGathererCount()` and `recallGatherers()` to Player, `getDisplayCount()` hook
    in IconSpinner, constructor params on DeploySpinner. Adapted to Bondolo's DeployType enum, Action enum, generics,
    and pattern matching. Skipped `Building.deployTypeToGatherSupply()` since supply class passed directly via constructor.
15. ~~**Discord bot + server DB**~~ (#82, #90, #106, #118, #120, #142) — Discord4j integration, bot commands, emoji roles,
    GitHub notifications, DB connection fixes. Modernize DBInterface with try-with-resources

### Phase 6 — Big Features (defer until base is solid)

16. **Steam integration** (#119, #127, #128, #143, #131) — Steamworks4j, achievements, stats, login, arm64 support.
    Also includes Omar's save/settings file relocation: `<install>/save_data/` for Steam cloud sync,
    per-user file prefixing (`<steamAccountId>.settings`, `<steamAccountId>.savegames`). Bondolo already has
    sophisticated path resolution in `Renderer.setupPaths()` (portable mode, XDG, %APPDATA%) — Omar's Steam-specific
    paths need to be integrated into that system rather than replacing it.
17. **Spectator mode** (#55, #154) — live game spectating, event streaming, catch-up logic
18. **More players** (#79, #93, #84) — dynamic player count, ScrollableGroup, UI for >6 players. LAST — most invasive

### Phase 7 — Final Polish

19. **Formatter + CI guard** — add google-java-format to gradle, format entire codebase, add formatCheck to CI workflow
    as PR gate
20. **Modernize server DB layer** — refactor DBInterface.java to use try-with-resources for all PreparedStatement/
    Connection/ResultSet handling. Replace manual finally blocks with auto-closeable patterns.

### Phase 8 — Assets (non-code)

20. **Seasonal assets** (#130) — Christmas hats
21. **Updated models** (#102) — updated 3D models/geometry

---

## Dropped / Abandoned

| PR   | Feature                      | Reason                                                           |
|------|------------------------------|------------------------------------------------------------------|
| #49  | Escape closes chat           | Didn't work reliably                                             |
| #53  | Graphics settings panel      | Bondolo rewrote options menu — verify his covers this            |
| #156 | Display changes (first boot) | Bondolo handles first-run resolution better — verify on his base |
| #36  | Player colors                | Bondolo has Settings.team_colours                                |
| —    | Our keybinds/options work    | Bondolo already implemented equivalent functionality             |

---

## Bugs Found During Porting

| Issue | Description | Trigger |
|-------|-------------|---------|
| FBO zero-size crash | `PostProcessor.resize()` passes 0 width/height to `Texture`/`FBO.resize()` | Minimizing the game window |
| Lobby user list | `ChatRoom.join()` had `sendUsers()` side effect that fired before client was ready | Fixed (ca0ebc2f) |
| Camera snap on map zoom | Camera does a fast scroll instead of teleporting when clicking map mode location | Clicking a location on the overhead map |
| Game loop pauses when unfocused | Game loop only runs when window is active — freezes multiplayer for all players on alt-tab | Alt-tabbing or losing window focus |

---

## Reference: All PRs by Category

### Gameplay

| PR          | Branch                        | Feature                                 |
|-------------|-------------------------------|-----------------------------------------|
| #79         | `newmain/more_players`        | Allow N players (set by constant)       |
| #93 (open)  | `more_players_with_ui`        | Enormous maps + more players UI         |
| #84         | `newmain/crash_fix`           | Fix crash after increasing player count |
| #55         | `spectator`                   | Watch/stream feature                    |
| #154 (open) | `spectator_mode`              | Spectating running games                |
| #153        | `gatherer_count_display`      | Show gatherer count per resource        |
| #135        | `countdown_before_game_start` | Countdown before multiplayer start      |
| #147        | `compass-indicator`           | Compass indicator                       |
| #130        | `seasonal_assets`             | Christmas hats                          |
| #102        | `updated_models`              | Updated 3D models                       |

### Steam

| PR          | Branch                  | Feature                               |
|-------------|-------------------------|---------------------------------------|
| #119        | `steam`                 | Steamworks4j + achievements + stats   |
| #143 (open) | `steam_login_process`   | Steam server authentication           |
| #128        | `steam`                 | Update Steam4j for arm64              |
| #127        | `fix/steam_crash`       | Guard check before Steam APIs         |
| #131        | `fix_hard_achievements` | AI difficulty for achievement unlocks |

### UI / Input

| PR   | Branch                                    | Feature                               |
|------|-------------------------------------------|---------------------------------------|
| #46  | `new_main_keyboard_changes`               | Keyboard text selection in EditLine   |
| #41  | `cursor_changes`                          | Bind cursor to game window            |
| #78  | `newmain/fix_cursor`                      | Hardware cursor support + setting     |
| #38  | `pitch_angle_fix`                         | Fix pitch angle snapping              |
| #32  | `discord_button`                          | Discord + GitHub buttons on main menu |
| #126 | `remove_oddlabs_loading_screen`           | Remove Oddlabs loading screen         |
| #146 | `fix_close_button`                        | Fix close button                      |
| #159 | `fix/deploy-spinner-constructor-mismatch` | Fix DeploySpinner constructor         |

### Server / Networking

| PR  | Branch                               | Feature                              |
|-----|--------------------------------------|--------------------------------------|
| #52 | `newmain/apiversion`                 | API version for client+server        |
| #45 | `newmain/remove_registration`        | Remove registration file requirement |
| #48 | `newmain/include_ai_to_game_players` | Include AI in game table data        |

### Discord Bot / Server DB

| PR   | Branch                          | Feature                          |
|------|---------------------------------|----------------------------------|
| #82  | `db_update_discord_4j`          | Discord4j integration            |
| #106 | `Discord-Bot-Update`            | Discord bot embed + /rank        |
| #90  | `discord_bot_threading_issues`  | Discord bot threading fixes      |
| #118 | `Issue-DB-Unclosed-Connections` | Close DB connections properly    |
| #120 | `gamer_command`                 | Emoji reaction roles             |
| #142 | `notify_github_activity`        | Discord notifications for GitHub |

### Build / CI / Infra

| PR          | Branch                      | Feature                           |
|-------------|-----------------------------|-----------------------------------|
| #57         | `auto_format`               | Auto formatter + PR format checks |
| #152 (open) | `work/gradle`               | Gradle build                      |
| #150        | `jar_cleanup_and_upgrade`   | Jar cleanup                       |
| #17         | `mac_arm_build_and_release` | ARM Mac builds                    |
| #73         | `work/fix_x86_mac`          | x86 Mac DMG builds                |
| #151        | `fix_mac_runner`            | Fix Mac runner for Intel          |
| #33         | `server_ant`                | Build XML + DB password env var   |
| #87, #104   | `steam`                     | Steam deploy steps in workflow    |
| #124, #125  | `steamworks4j_linux`        | Linux Steam packaging             |

### Website

| PR       | Branch                | Feature                     |
|----------|-----------------------|-----------------------------|
| #81      | `website_updates`     | Website updates             |
| #72      | `work/timestamps`     | Site adjustments            |
| #123     | `website/touchevents` | Touch events for watch page |
| #58, #59 | —                     | README updates              |

### Code Cleanup (verify not lost)

| PR  | Branch                      | Feature                                        |
|-----|-----------------------------|------------------------------------------------|
| #47 | `newmain/remove_systemouts` | Remove System.out calls (Bondolo did this too) |
| #43 | `new_main_classpath`        | Classpath cleanup                              |

---

## Notable Unmerged Branches (no PR)

| Branch                            | Feature                  | Notes                                  |
|-----------------------------------|--------------------------|----------------------------------------|
| `newman/enormous_with_ships`      | Ships feature            | On bondolo's fork too                  |
| `rewriting_opengl`                | OpenGL rewrite           | May overlap with Bondolo's shader work |
| `MapEditor`                       | Map editor               |                                        |
| `HeightBrush`                     | Height brush tool        |                                        |
| `Game_mode_Options`               | Game mode options        |                                        |
| `DApAITesting`, `DapDebugTools`   | AI testing / debug tools |                                        |
| `ArmoryonyAI`                     | AI improvements          |                                        |
| `feature/archipelago-10x-islands` | Archipelago map type     |                                        |
