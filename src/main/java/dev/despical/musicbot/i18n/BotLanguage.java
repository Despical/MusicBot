package dev.despical.musicbot.i18n;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * @author Despical
 * <p>
 * Created at 16.04.2026
 */
public enum BotLanguage {

    TR, EN;

    public static Optional<BotLanguage> fromCode(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        return Arrays.stream(values())
            .filter(language -> language.name().equalsIgnoreCase(value.trim()))
            .findFirst();
    }

    public Locale toLocale() {
        return this == TR ? Locale.forLanguageTag("tr-TR") : Locale.ENGLISH;
    }

    public String resourceFileName() {
        return name().toLowerCase(Locale.ROOT) + ".json";
    }
}
