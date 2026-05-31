# Event Logs

Every run of the game records an `event.log` (plus `std.out` and `std.err`) into a
per-run directory named with the launch timestamp in milliseconds. The event log
captures the deterministic stream of input events and can be replayed to reproduce
a session frame-for-frame, which makes it the primary tool for diagnosing crashes
and desync bugs.

## Where the logs live

The log root is resolved at startup by `Renderer.setupPaths()`. The first matching
location wins.

### Steam install

When launched through Steam, save data is kept next to the game install so Steam
Cloud can sync it:

```
<steam install>/save_data/logs/<timestamp>/event.log
```

For a local Gradle run on Windows that is typically
`tt/build/classes/java/save_data/logs/<timestamp>/event.log` (the same shape — the
JAR sits next to a `save_data/` directory).

### Portable install (game dir next to the JAR or in CWD)

If a directory named `TribalTrouble` is found in the current working directory or
next to the JAR, the game runs in portable mode and writes everything under it:

```
<TribalTrouble>/logs/<timestamp>/event.log
```

### Installed, not via Steam

If neither Steam nor portable mode applies, logs go to the OS-standard log
location:

| OS      | Path                                                                      |
| ------- | ------------------------------------------------------------------------- |
| Windows | `%LOCALAPPDATA%\TribalTrouble\logs\<timestamp>\event.log`                 |
| macOS   | `~/Library/Logs/TribalTrouble/<timestamp>/event.log`                      |
| Linux   | `$XDG_STATE_HOME/tribaltrouble/logs/<timestamp>/event.log` (falls back to `~/.local/state/tribaltrouble/logs/...`) |

If the standard location is not writable, logs fall back to `<dataDir>/logs/`
under the platform's data directory (`%APPDATA%\TribalTrouble`,
`~/Library/Application Support/TribalTrouble`, or `$XDG_CONFIG_HOME/tribaltrouble`).

### Retention

On startup `deleteOldLogs()` walks every per-run directory under the log root and,
for each one that is not the previous run or the new run, deletes `std.out`,
`std.err`, and `event.log`, then tries to remove the (now empty) directory. Only
those three filenames are recognized — if a directory holds anything else
(`event.log.gz`, `--grabframes` output, ad-hoc files), the final directory
removal fails silently and the directory survives with its non-standard
contents intact.

Cleanup is also skipped entirely when the game is in developer mode or replaying
an event log, so runs accumulate in both cases.

## Replaying an event log

Pass `--eventload <mode> [path]` to the game. `<mode>` is `normal` for a raw
`event.log` or `zipped` for an `event.log.gz`. The path is optional — if omitted,
the game replays `settings.last_event_log_dir/event.log` (the most recent run).

While `--eventload` is active, the game does NOT write a new event log, so
replaying does not stomp the source.

### Example

```
./gradlew :tt:run --args="--eventload normal build/classes/java/save_data/logs/1779763503450/event.log"
```

Replays the event log from run `1779763503450` (a Steam/local-Gradle path on
Windows). Adjust the path to point at whichever run you want to reproduce; on
non-Steam installs it will look like one of the OS paths in the table above.
