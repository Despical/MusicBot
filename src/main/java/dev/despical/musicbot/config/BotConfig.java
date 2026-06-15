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
