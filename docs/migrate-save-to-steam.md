# Migrating Your Tribal Trouble Save to the Steam Version

If you played Tribal Trouble before it came to Steam and want to bring your campaign progress over, you can. The save format hasn't changed, only where the file lives and what it's named

## Step 1 — Find your old save file

Look in the location for the OS you used to play on:

- **Windows:** `C:\Users\<your_username>\TribalTrouble\savegames`
- **macOS:** `~/Library/Application Support/TribalTrouble/savegames`
- **Linux:** `~/.TribalTrouble/savegames` *(starts with a dot — press Ctrl+H in your file manager to see hidden folders)*

If the file isn't there, search your home folder for `savegames` (no extension). You'll need that file path for step 4.

## Step 2 — Find the Steam game folder

In Steam, right-click **Tribal Trouble** in your library → **Manage → Browse local files**. That opens the install directory. Typical paths if you'd rather navigate manually:

- **Windows:** `C:\Program Files (x86)\Steam\steamapps\common\Tribal Trouble\`
- **macOS:** `~/Library/Application Support/Steam/steamapps/common/Tribal Trouble/`
- **Linux:** `~/.steam/steam/steamapps/common/Tribal Trouble/` *(or `~/.local/share/Steam/steamapps/common/Tribal Trouble/`)*

Inside, you'll see (or need to create) a folder named `save_data/`.

## Step 3 — Find your Steam account ID

The Steam version names save files `<accountID>.savegames`. To find your ID:

1. Launch Tribal Trouble through Steam.
2. Start any campaign mission and save it (or play through to a checkpoint).
3. Quit the game.
4. Open the `save_data/` folder — you'll see a file like `123456789.savegames`. The number is your account ID.

## Step 4 — Copy and rename your old save

Copy your old `savegames` file (from step 1) into the `save_data/` folder (from step 2), and rename it to match the pattern from step 3.

**Example (Linux):**
- Old: `~/.TribalTrouble/savegames`
- New: `~/.steam/steam/steamapps/common/Tribal Trouble/save_data/123456789.savegames`

> ⚠️ If step 3 already created a `<accountID>.savegames` file, your old save will replace it — you'll lose whatever progress you made during step 3. If that matters, copy the Steam-created file to a backup name first.

## Step 5 — Launch and load

Start Tribal Trouble through Steam. Your campaign should appear under **Load Game**.

---

## Troubleshooting

- **Campaign doesn't show up:** double-check the filename is exactly `<accountID>.savegames` (no `.txt` extension added by Windows, no spaces).
- **Wrong folder:** the file must be inside `save_data/`, not the install dir root.
- **Linux permissions:** if you copied as root or via sudo, fix ownership: `chown $USER:$USER <path>` and `chmod 644 <path>`.
