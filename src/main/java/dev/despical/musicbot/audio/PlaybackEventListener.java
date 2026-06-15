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
package dev.despical.musicbot.audio;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

/**
 * @author Despical
 * <p>
 * Created at 16.04.2026
 */
public interface PlaybackEventListener {

    void onTrackStarted(String guildId, PlaybackMetadata metadata, AudioTrack track);

    void onTrackEnded(String guildId, PlaybackMetadata metadata, AudioTrack track, AudioTrackEndReason endReason);
}
