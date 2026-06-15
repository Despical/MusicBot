package dev.despical.musicbot.i18n;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.despical.musicbot.persistence.GuildStateStore;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.EnumMap;
import java.util.Map;

/**
 * @author Despical
 * <p>
 * Created at 16.04.2026
 */
public final class TranslationService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, String>> MAP_TYPE = new TypeReference<>() { };

    private final GuildStateStore stateStore;
    private final BotLanguage fallbackLanguage;
    private final Map<BotLanguage, Map<String, String>> bundles;

    public TranslationService(GuildStateStore stateStore, BotLanguage fallbackLanguage) {
        this.stateStore = stateStore;
        this.fallbackLanguage = fallbackLanguage;
        this.bundles = buildBundles();
    }

    public String get(String guildId, String key, Object... arguments) {
        BotLanguage language = stateStore.getLanguage(guildId).orElse(fallbackLanguage);
        return get(language, key, arguments);
    }

    public String getDefault(String key, Object... arguments) {
        return get(fallbackLanguage, key, arguments);
    }

    public String get(BotLanguage language, String key, Object... arguments) {
        String template = bundles.getOrDefault(language, bundles.get(fallbackLanguage)).getOrDefault(key, key);
        return MessageFormat.format(template, arguments);
    }

    private Map<BotLanguage, Map<String, String>> buildBundles() {
        Map<BotLanguage, Map<String, String>> result = new EnumMap<>(BotLanguage.class);

        for (BotLanguage language : BotLanguage.values()) {
            result.put(language, loadBundle(language));
        }

        return result;
    }

    private Map<String, String> loadBundle(BotLanguage language) {
        String resourcePath = "i18n/" + language.resourceFileName();

        try (InputStream inputStream = TranslationService.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing translation resource: " + resourcePath);
            }

            return Map.copyOf(OBJECT_MAPPER.readValue(inputStream, MAP_TYPE));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load translation resource: " + resourcePath, exception);
        }
    }
}
