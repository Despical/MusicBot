/*
 * MusicBot - A modern Discord music bot with polished embeds and playback controls.
 * Copyright (C) 2026  Berke Akçen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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
