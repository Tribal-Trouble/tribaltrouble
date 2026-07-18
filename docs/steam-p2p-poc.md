# Steam P2P Private Match (Proof of Concept)

Serverless private multiplayer over Steam: no matchmaking server, no router server. The host runs
the game's existing in-process listen server and embedded router; remote players connect over
Steam's legacy P2P networking (NAT punch-through with Valve relay fallback), and a friends-only
Steam lobby handles discovery and invites.

## Player flow

Host: main menu -> "Steam Game" (visible only when Steam is running) -> map setup screen
(terrain, size, gamespeed) -> OK creates a friends-only Steam lobby, starts the in-process game
server, and opens the usual multiplayer lobby screen with an "Invite friends" button (the Steam
overlay invite dialog also opens automatically). AI slots are configured in the lobby screen.

Joiner: accept the invite in Steam (or join via friends list) -> the game joins the Steam lobby,
reads the host's SteamID and world parameters from lobby data, connects to the host over Steam
P2P, and lands in the same lobby screen. Ready up; host starts.

## Architecture

The transport plugs in beneath the existing `AbstractConnection` / ARMI event layer, alongside
the TCP and matchmaking-tunnel transports:

- `steam/SteamP2P` — singleton registry and per-frame packet pump (called next to
  `NetworkSelector.tick()` in `AnimationManager`). One reliable P2P packet per ARMI event.
  Channels: 0 = lobby negotiation, 1 = in-game router traffic. Packet types:
  HELLO / ACCEPT / REJECT / EVENT / CLOSE. HELLOs that arrive before a listener registers are
  parked, which covers the joiner racing ahead of the host's world load.
- `steam/SteamP2PConnection` — `AbstractConnection` over a (SteamID, channel) pair.
- `steam/SteamP2PConnectionListener` — accept/reject listener, same contract as
  `ConnectionListener` and `TunnelledConnectionListener`.
- `steam/SteamLobbySession` — the Steam lobby lifecycle (create, invite, join), host SteamID and
  world parameters carried as lobby data. Lives until the player returns to the main menu.
- Host wiring: `Server.incomingConnection` registers a Steam listener instead of a matchmaking
  tunnel; `PeerHub` runs the single-player embedded `Router` and adds a Steam listener on the
  game channel. Joiner wiring: `Client` and `RouterClient` connect over `SteamP2PConnection`
  (`Client.STEAM_HOST_ID` sentinel).

## Untested / known limitations (PoC)

- **Compiles, not yet run.** Real verification needs two machines with Steam running and the
  game launched under the app ID. The whole handshake sequence (lobby data timing, P2P session
  acceptance, HELLO buffering) is untested.
- Replays of Steam matches will not play back: Steam-delivered events bypass the
  `Deterministic` log that records socket traffic. Live play is unaffected (lockstep
  determinism is between peers, not the log).
- Spectators and web spectator streaming are not wired for Steam matches.
- No rated games, rankings, or persistent stats (by design, there is no server).
- UI strings ("Steam Game", "Invite friends") are hardcoded English, not in resource bundles.
- Non-friend joiners may show an empty name until Steam resolves their persona.
- Steam lobby chat is not used; lobby-screen chat rides the game's own connection once joined,
  so there is no chat during the brief connect window.
- Uses Valve's deprecated-but-supported `SteamNetworking` API (all steamworks4j wraps). The
  modern `SteamNetworkingSockets` path would need custom bindings (Java FFM over the flat API).
