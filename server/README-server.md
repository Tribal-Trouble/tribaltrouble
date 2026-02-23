# Tribal Trouble Server

## Setup

1. Copy `server.properties.template` to `server.properties`
2. Fill in the required values (at minimum `SQL_PASS`)
3. Run one of the server scripts from this directory

## Running

**Matchmaking server** (the main game server):

```bash
./bin/server
```

**Router server** (relays game traffic between players):

```bash
./bin/router
```

Both servers expect `server.properties` in the working directory.
Logs are written to the `logs/` directory.

## Configuration

See `server.properties.template` for all available settings:

| Key | Required | Description |
|-----|----------|-------------|
| `SQL_PASS` | Yes | MySQL database password |
| `DISCORD_BOT_TOKEN` | No | Discord bot token for integration |
| `DISCORD_SERVER_ID` | No | Discord server ID |
| `WEBSITE_DOMAIN` | No | Website domain for links |
| `NATIVE_CHIEF_EMOJI` | No | Custom emoji ID |
| `VIKING_CHIEF_EMOJI` | No | Custom emoji ID |
| `EMOJI_ROLE_MAPPINGS` | No | Emoji-to-role mapping |
| `REACTION_ROLE_MESSAGE_ID` | No | Message ID for reaction roles |
