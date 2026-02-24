# Tribal Trouble Server

## Setup

1. Edit `server.properties` with your settings (at minimum `SQL_PASS`)
2. Run the start-all script from this directory

## Running

**Start both servers:**

```bash
# Windows
start-all.bat

# Linux/Mac
./start-all
```

**Or run individually:**

```bash
# Matchmaking server (the main game server)
bin/matchmaker      # or bin\matchmaker.bat on Windows

# Router server (relays game traffic between players)
bin/router          # or bin\router.bat on Windows
```

Logs are written to the `logs/` directory.

## Configuration

See `server.properties` for all available settings:

| Key | Required | Description |
|-----|----------|-------------|
| `SQL_PASS` | Yes | MySQL database password |
| `DISCORD_BOT_TOKEN` | No | Discord bot token for integration |
| `DISCORD_SERVER_ID` | No | Discord server ID |
| `WEBSITE_DOMAIN` | No | Website domain for links |
| `NATIVE_CHIEF_EMOJI` | No | Custom emoji ID |
| `VIKING_CHIEF_EMOJI` | No | Custom emoji ID |
| `EMOJI_ROLE_MAPPINGS` | No | Emoji-to-role mapping JSON |
| `REACTION_ROLE_MESSAGE_ID` | No | Message ID for reaction roles |
