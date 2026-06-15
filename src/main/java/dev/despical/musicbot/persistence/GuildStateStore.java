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
package dev.despical.musicbot.persistence;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dev.despical.musicbot.i18n.BotLanguage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * @author Despical
 * <p>
 * Created at 16.04.2026
 */
public final class GuildStateStore {

    private static final int HISTORY_LIMIT = 5;

    private final Path filePath;
    private final ObjectMapper objectMapper;
    private final BotLanguage defaultLanguage;
    private final PersistedState persistedState;

    public GuildStateStore(BotLanguage defaultLanguage) {
        this.filePath = Path.of("data", "guild-state.json");
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        this.defaultLanguage = defaultLanguage;
        this.persistedState = loadState();
    }

    public synchronized Optional<BotLanguage> getLanguage(String guildId) {
        GuildState state = persistedState.guilds.computeIfAbsent(guildId, _ -> new GuildState(defaultLanguage.name()));
        state.ensureDefaults(defaultLanguage);
        return BotLanguage.fromCode(state.language);
    }

    public synchronized void setLanguage(String guildId, BotLanguage language) {
        GuildState state = persistedState.guilds.computeIfAbsent(guildId, _ -> new GuildState(defaultLanguage.name()));
        state.ensureDefaults(defaultLanguage);
        state.language = language.name();

        saveState();
    }

    public synchronized void pushHistory(String guildId, TrackHistoryEntry entry) {
        GuildState state = persistedState.guilds.computeIfAbsent(guildId, _ -> new GuildState(defaultLanguage.name()));
        state.ensureDefaults(defaultLanguage);

        if (!state.history.isEmpty()) {
            TrackHistoryEntry current = state.history.getFirst();

            if (Objects.equals(current.playQuery, entry.playQuery) && Objects.equals(current.displayTitle, entry.displayTitle)) {
                return;
            }
        }

        state.history.addFirst(entry);

        while (state.history.size() > HISTORY_LIMIT) {
            state.history.removeLast();
        }

        saveState();
    }

    public synchronized List<TrackHistoryEntry> getHistory(String guildId) {
        GuildState state = persistedState.guilds.computeIfAbsent(guildId, _ -> new GuildState(defaultLanguage.name()));
        state.ensureDefaults(defaultLanguage);
        return List.copyOf(state.history);
    }

    private PersistedState loadState() {
        try {
            Files.createDirectories(filePath.getParent());

            if (Files.notExists(filePath)) {
                PersistedState state = new PersistedState();
                objectMapper.writeValue(filePath.toFile(), state);
                return state;
            }

            PersistedState state = objectMapper.readValue(filePath.toFile(), PersistedState.class);
            state.ensureDefaults();
            return state;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load guild state file", exception);
        }
    }

    private void saveState() {
        try {
            objectMapper.writeValue(filePath.toFile(), persistedState);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save guild state file", exception);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class PersistedState {
        public Map<String, GuildState> guilds = new HashMap<>();

        private void ensureDefaults() {
            if (guilds == null) {
                guilds = new HashMap<>();
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class GuildState {

        public String language;
        public List<TrackHistoryEntry> history = new ArrayList<>();

        public GuildState() {
        }

        public GuildState(String language) {
            this.language = language;
        }

        private void ensureDefaults(BotLanguage defaultLanguage) {
            if (language == null || language.isBlank()) {
                language = defaultLanguage.name();
            }

            if (history == null) {
                history = new ArrayList<>();
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class TrackHistoryEntry {

        public String playQuery;
        public String displayTitle;
        public String sourceUrl;
        public String requestedBy;
        public long duration;
        public boolean live;

        public TrackHistoryEntry() {
        }

        public TrackHistoryEntry(String playQuery, String displayTitle, String sourceUrl, String requestedBy, long duration, boolean live) {
            this.playQuery = playQuery;
            this.displayTitle = displayTitle;
            this.sourceUrl = sourceUrl;
            this.requestedBy = requestedBy;
            this.duration = duration;
            this.live = live;
        }
    }
}
