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
 * Guild-scoped audio playback, queue management, and Discord notifications.
 *
 * <p>{@link dev.despical.musicbot.audio.MusicService} resolves playable sources,
 * manages voice connections, and exposes playback controls and queue snapshots.
 * Each {@link dev.despical.musicbot.audio.GuildAudioManager} owns a LavaPlayer
 * player, a {@link dev.despical.musicbot.audio.TrackScheduler}, and an
 * {@link dev.despical.musicbot.audio.AudioPlayerSendHandler} that supplies Opus
 * frames to JDA.</p>
 *
 * <p>Playback metadata preserves requester and source information. Scheduler
 * events update recent playback history and notify the service when tracks
 * start or end.</p>
 *
 * @author Despical
 */
package dev.despical.musicbot.audio;
