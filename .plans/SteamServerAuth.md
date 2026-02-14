# Plan: Steam Identity & Database Changes

## Goal

When connecting to `tribaltrouble.org` or `test.tribaltrouble.org`, players authenticate via Steam automatically.
When connecting to any other server, players use the existing login + profile flow unchanged.
Steam users get an auto-created profile with a human-readable nick derived from their
Steam persona name plus a hidden discriminator for uniqueness (e.g. `Viking#7432`).
The discriminator is stripped at display time so players just see `Viking`.

---

## 1. Database Migration (DONE)

File: `database/003.AddSteamIdentity.sql`

- `profiles.reg_id` made nullable (Steam users have no registration)
- `profiles.steam_id BIGINT NULL UNIQUE` added
- New `steam_to_profiles` table (steam_id → nick lookup)
- `game_players.result` and `game_players.rating_delta` columns added for per-game outcome tracking

---

## 2. Shared Protocol Changes (common/)

### 2a. Add `loginWithSteam` to `MatchmakingServerLoginInterface`

File: `common/classes/com/oddlabs/matchmaking/MatchmakingServerLoginInterface.java`

Add a new RPC method:

```java
public void loginWithSteam(long steamAccountId, String personaName, int revision);
```

- `steamAccountId` — the player's Steam account ID (from `SteamUser.getSteamID().getAccountID()`)
- `personaName` — the player's current Steam display name (from `SteamFriends.getPersonaName()`)
- `revision` — API version (same as existing login methods)

Server-side auth ticket validation is deferred (see Section 9). For now the server
trusts the client-provided Steam account ID.

### 2b. Add `NickUtils` utility class

File: `common/classes/com/oddlabs/matchmaking/NickUtils.java`

Static utility for nick discriminator handling:

```java
public final class NickUtils {
    private static final Pattern DISCRIMINATOR = Pattern.compile("#\\d+$");

    /** Strip the #XXXX discriminator for display. "Viking#7432" → "Viking" */
    public static String toDisplayName(String nick) {
        if (nick == null) return null;
        return DISCRIMINATOR.matcher(nick).replaceFirst("");
    }

    /** Generate a nick with discriminator: personaName + "#" + last 4 digits of steamAccountId */
    public static String generateSteamNick(String personaName, long steamAccountId) {
        String discriminator = String.format("%04d", Math.abs(steamAccountId % 10000));
        return personaName + "#" + discriminator;
    }
}
```

The discriminator is derived from the last 4 digits of the Steam account ID — deterministic,
stable across logins, and collision-resistant (two players need the same persona name AND
same last 4 digits to collide).

### 2c. No changes to `MatchmakingClientInterface`

The server responds with the same `loginOK(username, address)` / `loginError(code)` callbacks.
The `username` for Steam users will be the nick with discriminator (e.g. `Viking#7432`).

### 2d. No changes to `Profile`, `GameSession`, `Participant`, etc

Steam users get a real profile row. Nick = persona name + discriminator. All downstream
code sees a normal profile. The discriminator is only stripped at UI display time.

---

## 3. Server Changes (server/)

### 3a. `Authenticator.java` — handle `loginWithSteam`

File: `server/classes/com/oddlabs/matchserver/Authenticator.java`

Since `Authenticator` implements `MatchmakingServerLoginInterface`, add:

```java
public void loginWithSteam(long steamAccountId, String personaName, int revision) {
    if (!revisionOK(revision)) return;

    String nick = DBInterface.getOrCreateSteamProfile(steamAccountId, personaName);
    doLogin(nick, null, revision);
}
```

Key points:

- `personaName` is the player's Steam display name, used to build a human-readable nick
- The nick returned includes the discriminator (e.g. `Viking#7432`) for DB uniqueness
- Server trusts the client-provided Steam ID for now (see Section 8 for future validation)

### 3b. `DBInterface.java` — add `getOrCreateSteamProfile`

File: `server/classes/com/oddlabs/matchserver/DBInterface.java`

New method:

```java
public static String getOrCreateSteamProfile(long steamId, String personaName) {
    // 1. Check steam_to_profiles for existing mapping
    //    SELECT nick FROM steam_to_profiles WHERE steam_id = ?
    // 2. If found, return the existing nick (profile already exists)
    //    (persona name may have changed on Steam, but nick stays stable)
    // 3. If not found:
    //    a. Generate nick = NickUtils.generateSteamNick(personaName, steamId)
    //       e.g. "Viking#7432"
    //    b. INSERT INTO profiles (reg_id, steam_id, nick, rating, wins, losses, invalid)
    //       VALUES (NULL, ?, ?, 1000, 0, 0, 0)
    //    c. INSERT INTO steam_to_profiles (steam_id, nick) VALUES (?, ?)
    //    d. Return the generated nick
}
```

Also add a lookup method for other code that may need it:

```java
public static String getProfileNickBySteamId(long steamId) {
    // SELECT nick FROM steam_to_profiles WHERE steam_id = ?
}
```

---

## 4. Client Changes (tt/)

### 4a. `Settings.java` — expose official domain check

File: `tt/classes/com/oddlabs/tt/global/Settings.java`

Add a helper:

```java
public static final String OFFICIAL_DOMAIN = "tribaltrouble.org";

public boolean isOfficialServer() {
    return OFFICIAL_DOMAIN.equals(domain_name);
}
```

### 4b. `MainMenu.java` — branch on server address

File: `tt/delegate/MainMenu.java`

Current multiplayer button logic (line ~88):

```java
if (Network.getMatchmakingClient().isConnected()) {
    new SelectGameMenu(...);
} else {
    new LoginForm(...);  // always shows login form
}
```

Change to:

```java
if (Network.getMatchmakingClient().isConnected()) {
    new SelectGameMenu(...);
} else if (Settings.getSettings().isOfficialServer() && SteamAPI.isSteamRunning()) {
    // Steam auto-login — skip LoginForm entirely
    new MatchmakingConnectingForm(..., /* steamLogin = true */);
} else {
    new LoginForm(...);  // existing flow for community servers
}
```

### 4c. `MatchmakingClient.java` — add Steam login path

File: `tt/classes/com/oddlabs/tt/net/MatchmakingClient.java`

Add a new login method alongside the existing `login()`:

```java
public void loginWithSteam(NetworkSelector network) {
    this.login = null;
    this.login_details = null;
    open(network);
    state = STATE_AWAITING_OK;
}
```

In the `connected()` callback (~line 400), add a branch:

```java
public void connected(AbstractConnection connection) {
    // ... existing setup ...
    if (steamLogin) {
        long accountId = SteamAchievementManager.getAchievementManager().getAccountID();
        String personaName = SteamAchievementManager.getAchievementManager().getPersonaName();
        matchmaking_login_interface.loginWithSteam(accountId, personaName, revision);
    } else if (!Renderer.isRegistered()) {
        matchmaking_login_interface.loginAsGuest(revision);
    } else if (login_details != null) {
        matchmaking_login_interface.createUser(login, login_details, null, revision);
    } else {
        matchmaking_login_interface.login(login, null, revision);
    }
}
```

Note: `getPersonaName()` needs to be exposed via `SteamAchievementManager` (wraps
`SteamFriends.getPersonaName()`).

### 4d. `SelectGameMenu.java` — skip profile selection for Steam users

File: `tt/form/SelectGameMenu.java`

Current logic (~line 254):

```java
if (Network.getMatchmakingClient().getProfile() == null && Renderer.isRegistered()) {
    main_menu.setMenuCentered(profiles_form);  // show profile picker
    Network.getMatchmakingClient().requestProfiles();
}
```

For Steam users, the server auto-creates and selects the profile, so the client
should call `setProfile(nick)` automatically instead of showing the ProfilesForm.
Add a check:

```java
if (Settings.getSettings().isOfficialServer() && SteamAPI.isSteamRunning()) {
    // Profile was auto-created on login; set it directly
    String nick = Network.getMatchmakingClient().getUsername();
    Network.getMatchmakingClient().setProfile(nick);
    main_menu.setMenuCentered(this);  // go straight to game menu
} else if (Network.getMatchmakingClient().getProfile() == null && Renderer.isRegistered()) {
    main_menu.setMenuCentered(profiles_form);
    Network.getMatchmakingClient().requestProfiles();
} else {
    main_menu.setMenuCentered(this);
}
```

---

## 5. Display Name Stripping (client-side)

All nicks flow through a small number of accessor methods before reaching the UI.
Apply `NickUtils.toDisplayName()` at these choke points so the discriminator never
appears on screen:

| Choke point | Covers |
|-------------|--------|
| `ChatMessage.formatShort()` / `formatLong()` | All chat (lobby, in-game, team, private) |
| `ChatRoomHistory` join/leave messages | "Viking joined" not "Viking#7432 joined" |
| `ChatPanel` user list labels | Player list in chat room |
| `ProfilesForm` / `InfoForm` nick labels | Profile picker and profile detail view |
| `SelectGameMenu` ranking list | Leaderboard names |
| `DefaultInGameInfo` player name labels | In-game HUD player panel |
| `GameStatsDelegate` column headers | Post-game stats screen |
| `GameMenu` player slot labels and join/leave | Game lobby slots |

Legacy (non-Steam) nicks won't contain `#\d+` so `toDisplayName()` is a no-op for them.

---

## 6. Files Changed Summary

| File | Change |
|------|--------|
| `database/003.AddSteamIdentity.sql` | New migration (DONE) |
| `common/.../MatchmakingServerLoginInterface.java` | Add `loginWithSteam(long, String, int)` |
| `common/.../NickUtils.java` | **New** — `toDisplayName()`, `generateSteamNick()` |
| `server/.../Authenticator.java` | Implement `loginWithSteam()` |
| `server/.../DBInterface.java` | Add `getOrCreateSteamProfile()`, `getProfileNickBySteamId()` |
| `tt/.../global/Settings.java` | Add `OFFICIAL_DOMAIN`, `isOfficialServer()` |
| `tt/.../delegate/MainMenu.java` | Branch: Steam auto-login vs existing LoginForm |
| `tt/.../net/MatchmakingClient.java` | Add `loginWithSteam()`, send persona name in `connected()` |
| `tt/.../form/SelectGameMenu.java` | Skip ProfilesForm for Steam users |
| `tt/.../net/ChatMessage.java` | Strip discriminator in `formatShort()`/`formatLong()` |
| `tt/.../net/ChatRoomHistory.java` | Strip discriminator in join/leave messages |
| `tt/.../gui/ChatPanel.java` | Strip discriminator in user list labels |
| `tt/.../form/ProfilesForm.java` | Strip discriminator in profile list |
| `tt/.../form/InfoForm.java` | Strip discriminator in profile detail |
| `tt/.../form/SelectGameMenu.java` | Strip discriminator in ranking list |
| `tt/.../viewer/DefaultInGameInfo.java` | Strip discriminator in HUD player names |
| `tt/.../delegate/GameStatsDelegate.java` | Strip discriminator in stats columns |
| `tt/.../form/GameMenu.java` | Strip discriminator in lobby slots + join/leave |
| `tt/.../steam/SteamAchievementManager.java` | Expose `getPersonaName()` |

---

## 7. What's NOT Changing

- `LoginForm`, `NewUserForm`, `ProfilesForm`, `NewProfileForm` — all stay as-is for community servers
- `Profile.java`, `GameSession.java`, `Participant.java` — unchanged (nick with discriminator flows through)
- Existing win/loss/rating/ranking DB queries — unchanged (they query by full nick including discriminator)
- Steam achievement/stats code in `SteamUtils.java` — unchanged (client-side only)
- `discord_to_profiles` — unchanged

---

## 8. Follow-up Tasks (not in this PR)

- **Steam auth ticket validation** — see Section 9 below
- **Persona name sync** — if a player changes their Steam name, optionally update the nick (requires generating a new discriminator and migrating DB references)
- **Profile migration** — let existing profile-based players link their Steam account to preserve stats

---

## 9. Future: Server-Side Steam Auth Ticket Validation

Currently the server trusts the client-provided Steam account ID. This is acceptable
for a small community — an attacker would need to reverse-engineer the ARMI protocol
and craft raw TCP packets to spoof someone else's ID, all to manipulate Tribal Trouble
stats. If abuse becomes a problem, add server-side validation:

### How it would work

1. **Protocol change** — re-add `byte[] authTicket` parameter:

   ```java
   public void loginWithSteam(long steamAccountId, byte[] authTicket, int revision);
   ```

2. **Client** — obtain an auth session ticket via Steamworks:

   ```java
   SteamUser.getAuthSessionTicket(buffer);
   ```

   Send the ticket bytes alongside the Steam account ID.

3. **Server** — validate the ticket by calling the Steam Web API:

   ```
   GET https://api.steampowered.com/ISteamUserAuth/AuthenticateUserTicket/v1/
       ?key=<STEAM_WEB_API_KEY>
       &appid=<APP_ID>
       &ticket=<hex-encoded ticket>
   ```

   The response includes the authenticated `steamid`. Verify it matches the
   client-provided `steamAccountId`. Reject with `loginError()` on mismatch.

4. **Server config** — store the Steam Web API key in server config (not in code).

### When to add this

- If players report impersonation or stat manipulation
- Before any competitive ranked season or tournament
- Before adding any Steam-linked rewards or purchases
