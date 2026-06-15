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
