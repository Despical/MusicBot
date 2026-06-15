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

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import dev.despical.musicbot.persistence.GuildStateStore;
import lombok.Getter;

/**
 * @author Despical
 * <p>
 * Created at 16.04.2026
 */
@Getter
public final class GuildAudioManager {

    private final AudioPlayer player;
    private final TrackScheduler scheduler;
    private final AudioPlayerSendHandler sendHandler;

    public GuildAudioManager(AudioPlayerManager playerManager, GuildStateStore stateStore, String guildId, PlaybackEventListener playbackEventListener) {
        this.player = playerManager.createPlayer();
        this.scheduler = new TrackScheduler(player, stateStore, guildId, playbackEventListener);
        this.sendHandler = new AudioPlayerSendHandler(player);
        this.player.addListener(scheduler);
    }
}
