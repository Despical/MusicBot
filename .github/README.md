# Discord Music Bot

A Discord music bot built with JDA, LavaPlayer, Java 25, and Gradle.

## Features

- Play music with `/play <link | song name>` using YouTube links, Spotify track/album/playlist links, or search queries
- Playback controls: `/pause`, `/resume`, `/skip [count]`, `/stop`, `/leave`
- Queue and player tools: `/queue [page]`, `/nowplaying`, `/volume <1-200>`
- Replay history with `/last` and `/replaylast`
- Switch bot language between `TR` and `EN` with `/language`
- Automatically leaves an empty voice channel
- Joins the command user's voice channel when `/play` is used
- Stores the last 5 played tracks in JSON
- Supports `/play` with `repeat_count` and `loop_forever`
- Sends automatic text channel notifications when a track starts or ends

## Requirements

- Java 25
- A Discord bot token
- Spotify API credentials if you want Spotify links to be resolved

## Environment Variables

Copy `.env.example` to `.env` and fill in the values.

Example:

```env
DISCORD_TOKEN=your_discord_bot_token
SPOTIFY_CLIENT_ID=your_spotify_client_id
SPOTIFY_CLIENT_SECRET=your_spotify_client_secret
DEFAULT_LANGUAGE=TR
```

Notes:

- `DISCORD_TOKEN` is required.
- `SPOTIFY_CLIENT_ID` and `SPOTIFY_CLIENT_SECRET` are optional, but Spotify links will not work without them.
- `DEFAULT_LANGUAGE` is optional. Supported values are `TR` and `EN`.
- The bot can also read these values from system environment variables instead of `.env`.

## Setup

1. Install Java 25.
2. Copy `.env.example` to `.env`.
3. Fill in your Discord token.
4. Optionally fill in Spotify credentials.
5. Run the bot.

## Run

On Windows:

```powershell
.\gradlew.bat run
```

On Linux or macOS:

```bash
./gradlew run
```

If you already have Gradle installed globally, you can also use:

```bash
gradle run
```

## Build

```bash
./gradlew build
```

On Windows:

```powershell
.\gradlew.bat build
```

## Data Storage

Runtime guild data is stored in `data/guild-state.json`.

## Commands

- `/play`
- `/pause`
- `/resume`
- `/skip`
- `/stop`
- `/leave`
- `/queue`
- `/nowplaying`
- `/volume`
- `/last`
- `/replaylast`
- `/language`
- `/help`
