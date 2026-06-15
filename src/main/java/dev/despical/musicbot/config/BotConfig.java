/*
 * MusicBot - A self-hostable Discord music bot with slash-command playback.
 * Copyright (C) 2026 Despical
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package dev.despical.musicbot.config;

import dev.despical.musicbot.i18n.BotLanguage;
import io.github.cdimascio.dotenv.Dotenv;

/**
 * @author Despical
 * <p>
 * Created at 16.04.2026
 */
public record BotConfig(
    String token,
    String spotifyClientId,
    String spotifyClientSecret,
    BotLanguage defaultLanguage
) {

    public static BotConfig load() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        String token = readValue(dotenv, "DISCORD_TOKEN", true);
        String spotifyClientId = readValue(dotenv, "SPOTIFY_CLIENT_ID", false);
        String spotifyClientSecret = readValue(dotenv, "SPOTIFY_CLIENT_SECRET", false);
        String languageValue = readValue(dotenv, "DEFAULT_LANGUAGE", false);

        BotLanguage defaultLanguage = BotLanguage.fromCode(languageValue).orElse(BotLanguage.EN);
        return new BotConfig(token, spotifyClientId, spotifyClientSecret, defaultLanguage);
    }

    private static String readValue(Dotenv dotenv, String key, boolean required) {
        String value = dotenv.get(key);

        if (value == null || value.isBlank()) {
            value = System.getenv(key);
        }

        if (required && (value == null || value.isBlank())) {
            throw new IllegalStateException(key + " is required in .env or environment variables.");
        }

        return value == null ? "" : value.trim();
    }
}
