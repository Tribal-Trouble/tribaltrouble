# server/ notes for agents

Read the root [AGENTS.md](../AGENTS.md) first. This module builds the two processes behind
live multiplayer, so changes here affect players already in games.

## The two processes

- **Matchmaker** (`com.oddlabs.matchserver`): accounts, login, game listing and joining,
  rankings, the database, and the Discord bot integration under `discord/`. Run it with
  `./gradlew server:runMatchmaker`.
- **Router** (`com.oddlabs.routerserver`): relays chat and in-game messages between clients.
  Run it with `./gradlew server:runRouter`.

Both read `server/server.properties` (copy it from `server.properties.template`; it is not
committed). MySQL setup and the required properties are in the root
[README](../README.md#build--run-game-server).

## Compatibility

`Authenticator` rejects any client whose `API_VERSION` does not match exactly. That means a
deployed server and its clients are locked together: bumping `API_VERSION` requires
redeploying this module from the bumped ref and locks out everyone who has not updated. Do
not bump it on your own initiative.

The server also compares each client's reported `SIM_VERSION` when listing and joining
games. It does not need a redeploy when that value changes, but it does decide who can play
with whom.

## Database

Schema changes are numbered migrations in `../database/` with a matching rollback. Add a new
file; never edit one that has already run. Migrations are applied to the live database by
hand, so merging one does not deploy it. Say so explicitly when a change needs a migration
run.

## Operational care

- The server holds persistent connections. Avoid changes that can throw on a per-connection
  path without a guard; an unhandled exception can take down sessions for everyone.
- Do not log secrets, tokens, or player email addresses.
- Deployment is documented in [../docs/releasing.md](../docs/releasing.md) and the
  `build-deploy-server` workflow. Deploying is a maintainer action, not an agent action.
