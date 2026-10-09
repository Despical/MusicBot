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
/**
 * Local JSON persistence for guild language preferences and playback history.
 *
 * <p>{@link dev.despical.musicbot.persistence.GuildStateStore} loads and saves
 * {@code data/guild-state.json}, restores default values for guild state, and
 * provides synchronized access to language and history updates.</p>
 *
 * <p>History retains at most five recent tracks per guild and ignores a new
 * entry that matches the most recent track's query and display title. History
 * snapshots are immutable copies used by the playback and interaction layers.</p>
 *
 * @author Despical
 */
package dev.despical.musicbot.persistence;
