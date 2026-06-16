# MusicBot

[![](https://github.com/Despical/MusicBot/actions/workflows/build.yaml/badge.svg)](https://github.com/Despical/MusicBot/actions/workflows/build.yaml)
[![](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![Java 25](https://img.shields.io/badge/Java-25-007396.svg)](https://www.java.com/)
[![JDA](https://img.shields.io/badge/JDA-6.4.1-5865F2.svg)](https://github.com/discord-jda/JDA)
[![LavaPlayer](https://img.shields.io/badge/LavaPlayer-2.2.2-F59E0B.svg)](https://github.com/lavalink-devs/lavaplayer)

MusicBot is a clean, self-hostable Discord music bot built with JDA, LavaPlayer, Java 25, and Gradle. It focuses on fast slash-command playback, modern Discord embeds, queue controls, replay history, and lightweight JSON persistence.

Paste a YouTube link, Spotify link, or search query, and MusicBot joins your voice channel, resolves the track, and keeps the text channel updated with polished playback cards and button controls.

---

## Features

* **Slash-command playback:** Play songs with `/play <query>` using YouTube URLs, Spotify track/album/playlist links, or plain search terms.
* **Search selection:** Pick from interactive search results before queueing a track.
* **Playback controls:** Pause, resume, skip, stop, leave, and control playback with Discord buttons.
* **Queue tools:** View current playback, queue pages, volume, requested-by data, live streams, and loop status.
* **Replay history:** Replay one of the last 5 tracks with `/last` or instantly restart the latest track with `/replaylast`.
* **Server language:** Switch each guild between Turkish and English with `/language`.
* **Modern embeds:** Uses compact, readable Discord embeds for playback notifications, command replies, search results, and queue views.
* **Self-hostable storage:** Stores guild language and playback history locally in `data/guild-state.json`.

---

## Requirements

* Java 25
* A Discord bot token
* Spotify API credentials if you want Spotify links to be resolved
* A Discord voice channel with an appropriate bitrate for the quality you expect

---

## Building From Source

### 1. Clone the Repository

```bash
git clone https://github.com/Despical/MusicBot.git
cd MusicBot
```

### 2. Environment Configuration

Create your local environment file for the Discord token, optional Spotify credentials, and default language.

Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

Linux / macOS:

```bash
cp .env.example .env
```

Open `.env` and fill in the values:

```env
DISCORD_TOKEN=your_discord_bot_token
SPOTIFY_CLIENT_ID=your_spotify_client_id
SPOTIFY_CLIENT_SECRET=your_spotify_client_secret
DEFAULT_LANGUAGE=EN
```

Notes:

* `DISCORD_TOKEN` is required.
* `SPOTIFY_CLIENT_ID` and `SPOTIFY_CLIENT_SECRET` are optional, but Spotify links will not work without them.
* `DEFAULT_LANGUAGE` is optional. Supported values are `TR` and `EN`.
* The bot can also read these values from system environment variables instead of `.env`.

### 3. Build & Test

Windows:

```bat
gradlew.bat build
```

Linux / macOS:

```bash
./gradlew build
```

### 4. Run Locally

Windows:

```bat
gradlew.bat run
```

Linux / macOS:

```bash
./gradlew run
```

---

## Docker

Docker is the recommended way to keep the bot running on a server.

1. Create `.env` from `.env.example` and fill in your Discord token.
2. Start the container:

```bash
docker compose up -d --build
```

3. Check logs:

```bash
docker compose logs -f musicbot
```

4. Stop the bot:

```bash
docker compose down
```

The compose file mounts `./data` into the container, so guild language settings
and playback history survive rebuilds and restarts.

---

## Discord Setup

1. Create an application and bot in the [Discord Developer Portal](https://discord.com/developers/applications).
2. Enable the bot token and copy it into `.env` as `DISCORD_TOKEN`.
3. Invite the bot with slash command and voice permissions.
4. Run the application and wait for slash commands to register.
5. Join a voice channel and use `/play`.

Recommended permissions:

* View Channels
* Send Messages
* Embed Links
* Use Slash Commands
* Connect
* Speak
* Use Voice Activity

---

## Spotify Links

MusicBot does not stream audio directly from Spotify. Spotify URLs are resolved into metadata, then matched against playable sources through LavaPlayer.

Set these variables if you want Spotify support:

```env
SPOTIFY_CLIENT_ID=your_spotify_client_id
SPOTIFY_CLIENT_SECRET=your_spotify_client_secret
```

---

## Audio Notes

MusicBot sends Opus frames through JDA and LavaPlayer. If audio sounds distorted or muddy, keep the bot volume at or below `100` first:

```text
/volume 100
```

Values above `100` amplify the decoded audio and can cause clipping on some tracks.

---

## Data Storage

Runtime guild data is stored in:

```text
data/guild-state.json
```

This file contains per-guild language settings and recent playback history. It is intentionally ignored by Git.

---

## Commands

* `/play`
* `/pause`
* `/resume`
* `/skip`
* `/stop`
* `/leave`
* `/queue`
* `/nowplaying`
* `/volume`
* `/last`
* `/replaylast`
* `/language`
* `/help`

---

## Security

Please do not open public issues for discovered vulnerabilities.

Read [SECURITY.md](SECURITY.md) for responsible disclosure reporting.

---

## Contributing

Pull Requests are welcome. To keep the project clean and easy to review, please follow the contribution guidelines:

* **No tabs:** Use spaces exclusively for indentation.
* **Style consistency:** Respect the established code architecture and style.
* **Version control cleanliness:** Do not increment project version numbers in example configurations within your PR.
* **Minimal diffs:** Disable automated reformat-on-save settings that affect untouched files.

Learn more via [CONTRIBUTING.md](CONTRIBUTING.md).

---

## License

This project is licensed under the [GPL-3.0 License](http://www.gnu.org/licenses/gpl-3.0.html).

See [LICENSE](LICENSE) for the full license text.
