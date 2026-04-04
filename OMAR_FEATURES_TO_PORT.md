# Omar's Features to Port onto Bondolo's Base

Features from OmarAMokhtar/tribaltrouble merged PRs + open PRs + contributor PRs.
Starting from Bondolo's `bondolo_formatted` branch, porting features on top.

---

## Porting Order

### Phase 1 — Foundation
1. **CI/CD + Build** — GitHub Actions workflows, gradle setup, Steam deploy steps, Mac/Linux packaging, auto-formatter CI (#57, #87, #104, #124, #125, #150, #151, #152, #17, #73, #33)

### Phase 2 — Small Fixes
2. **Remove registration** (#45) — remove registration file requirement
3. **API version** (#52) — client+server API version check
4. **Pitch angle fix** (#38) — camera snap on middle mouse. Verify if still needed on Bondolo's base
5. **Remove Oddlabs loading screen** (#126) — remove loading screen reference
6. **Fix close button** (#146) — small UI fix
7. **DeploySpinner fix** (#159) — fix constructor mismatch

### Phase 3 — UI / Input Features
8. **Discord/GitHub buttons** (#32) — buttons on main menu, needs GUIImage clickable
9. **Cursor changes** (#41, #78) — bind cursor to window + hardware cursor setting
10. **EditLine text selection** (#46) — keyboard selection, copy/paste/cut in text fields

### Phase 4 — Gameplay Features
11. **Enormous map size** (from #93) — add SIZE_ENORMOUS to TerrainMenu, Landscape, WordsEncoding. Separate from player count changes
12. **Gatherer count** (#153) — show gatherer count per resource (Maxenor)
13. **Countdown before game** (#135) — countdown before multiplayer start (Maxenor)
14. **Compass indicator** (#147) — shared directional reference

### Phase 5 — Assets
15. **Seasonal assets** (#130) — Christmas hats
16. **Updated models** (#102) — updated 3D models/geometry

### Phase 6 — Server / Discord
17. **Discord bot + server DB** (#82, #90, #106, #118, #120, #142) — Discord4j integration, bot commands, emoji roles, GitHub notifications, DB connection fixes. Modernize DBInterface with try-with-resources

### Phase 7 — Big Features (defer until base is solid)
18. **Steam integration** (#119, #127, #128, #143, #131) — Steamworks4j, achievements, stats, login, arm64 support
19. **Spectator mode** (#55, #154) — live game spectating, event streaming, catch-up logic
20. **More players** (#79, #93, #84) — dynamic player count, ScrollableGroup, UI for >6 players. LAST — most invasive

---

## Dropped / Abandoned

| PR | Feature | Reason |
|----|---------|--------|
| #49 | Escape closes chat | Didn't work reliably |
| #53 | Graphics settings panel | Bondolo rewrote options menu — verify his covers this |
| #156 | Display changes (first boot) | Bondolo handles first-run resolution better — verify on his base |
| #36 | Player colors | Bondolo has Settings.team_colours |
| — | Our keybinds/options work | Bondolo already implemented equivalent functionality |

---

## Reference: All PRs by Category

### Gameplay
| PR | Branch | Feature |
|----|--------|---------|
| #79 | `newmain/more_players` | Allow N players (set by constant) |
| #93 (open) | `more_players_with_ui` | Enormous maps + more players UI |
| #84 | `newmain/crash_fix` | Fix crash after increasing player count |
| #55 | `spectator` | Watch/stream feature |
| #154 (open) | `spectator_mode` | Spectating running games |
| #153 | `gatherer_count_display` | Show gatherer count per resource |
| #135 | `countdown_before_game_start` | Countdown before multiplayer start |
| #147 | `compass-indicator` | Compass indicator |
| #130 | `seasonal_assets` | Christmas hats |
| #102 | `updated_models` | Updated 3D models |

### Steam
| PR | Branch | Feature |
|----|--------|---------|
| #119 | `steam` | Steamworks4j + achievements + stats |
| #143 (open) | `steam_login_process` | Steam server authentication |
| #128 | `steam` | Update Steam4j for arm64 |
| #127 | `fix/steam_crash` | Guard check before Steam APIs |
| #131 | `fix_hard_achievements` | AI difficulty for achievement unlocks |

### UI / Input
| PR | Branch | Feature |
|----|--------|---------|
| #46 | `new_main_keyboard_changes` | Keyboard text selection in EditLine |
| #41 | `cursor_changes` | Bind cursor to game window |
| #78 | `newmain/fix_cursor` | Hardware cursor support + setting |
| #38 | `pitch_angle_fix` | Fix pitch angle snapping |
| #32 | `discord_button` | Discord + GitHub buttons on main menu |
| #126 | `remove_oddlabs_loading_screen` | Remove Oddlabs loading screen |
| #146 | `fix_close_button` | Fix close button |
| #159 | `fix/deploy-spinner-constructor-mismatch` | Fix DeploySpinner constructor |

### Server / Networking
| PR | Branch | Feature |
|----|--------|---------|
| #52 | `newmain/apiversion` | API version for client+server |
| #45 | `newmain/remove_registration` | Remove registration file requirement |
| #48 | `newmain/include_ai_to_game_players` | Include AI in game table data |

### Discord Bot / Server DB
| PR | Branch | Feature |
|----|--------|---------|
| #82 | `db_update_discord_4j` | Discord4j integration |
| #106 | `Discord-Bot-Update` | Discord bot embed + /rank |
| #90 | `discord_bot_threading_issues` | Discord bot threading fixes |
| #118 | `Issue-DB-Unclosed-Connections` | Close DB connections properly |
| #120 | `gamer_command` | Emoji reaction roles |
| #142 | `notify_github_activity` | Discord notifications for GitHub |

### Build / CI / Infra
| PR | Branch | Feature |
|----|--------|---------|
| #57 | `auto_format` | Auto formatter + PR format checks |
| #152 (open) | `work/gradle` | Gradle build |
| #150 | `jar_cleanup_and_upgrade` | Jar cleanup |
| #17 | `mac_arm_build_and_release` | ARM Mac builds |
| #73 | `work/fix_x86_mac` | x86 Mac DMG builds |
| #151 | `fix_mac_runner` | Fix Mac runner for Intel |
| #33 | `server_ant` | Build XML + DB password env var |
| #87, #104 | `steam` | Steam deploy steps in workflow |
| #124, #125 | `steamworks4j_linux` | Linux Steam packaging |

### Website
| PR | Branch | Feature |
|----|--------|---------|
| #81 | `website_updates` | Website updates |
| #72 | `work/timestamps` | Site adjustments |
| #123 | `website/touchevents` | Touch events for watch page |
| #58, #59 | — | README updates |

### Code Cleanup (verify not lost)
| PR | Branch | Feature |
|----|--------|---------|
| #47 | `newmain/remove_systemouts` | Remove System.out calls (Bondolo did this too) |
| #43 | `new_main_classpath` | Classpath cleanup |

---

## Notable Unmerged Branches (no PR)
| Branch | Feature | Notes |
|--------|---------|-------|
| `newman/enormous_with_ships` | Ships feature | On bondolo's fork too |
| `rewriting_opengl` | OpenGL rewrite | May overlap with Bondolo's shader work |
| `MapEditor` | Map editor | |
| `HeightBrush` | Height brush tool | |
| `Game_mode_Options` | Game mode options | |
| `DApAITesting`, `DapDebugTools` | AI testing / debug tools | |
| `ArmoryonyAI` | AI improvements | |
| `feature/archipelago-10x-islands` | Archipelago map type | |
