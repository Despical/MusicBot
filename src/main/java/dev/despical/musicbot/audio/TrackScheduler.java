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
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import dev.despical.musicbot.persistence.GuildStateStore;
import dev.despical.musicbot.persistence.GuildStateStore.TrackHistoryEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Despical
 * <p>
 * Created at 16.04.2026
 */
public final class TrackScheduler extends AudioEventAdapter {
    private final AudioPlayer player;
    private final Queue<AudioTrack> queue;
    private final GuildStateStore stateStore;
    private final String guildId;
    private final PlaybackEventListener playbackEventListener;

    public TrackScheduler(AudioPlayer player, GuildStateStore stateStore, String guildId, PlaybackEventListener playbackEventListener) {
        this.player = player;
        this.stateStore = stateStore;
        this.guildId = guildId;
        this.playbackEventListener = playbackEventListener;
        this.queue = new ConcurrentLinkedQueue<>();
    }

    public void queue(AudioTrack track) {
        if (player.getPlayingTrack() == null) {
            startTrackFresh(track);
            return;
        }
        if (!player.startTrack(track, true)) {
            queue.offer(track);
        }
    }

    public boolean isPlaying() {
        return player.getPlayingTrack() != null;
    }

    public boolean isPaused() {
        return player.isPaused();
    }

    public void pause() {
        player.setPaused(true);
    }

    public void resume() {
        player.setPaused(false);
    }

    public void stopAndClear() {
        queue.clear();
        player.stopTrack();
    }

    public void clearQueue() {
        queue.clear();
    }

    public int skip(int count) {
        AudioTrack currentTrack = player.getPlayingTrack();
        if (currentTrack == null) {
            return 0;
        }

        int effectiveCount = Math.max(1, count);
        int skippedTracks = 1;
        while (skippedTracks < effectiveCount && queue.poll() != null) {
            skippedTracks++;
        }

        AudioTrack nextTrack = queue.poll();
        if (nextTrack != null) {
            startTrackFresh(nextTrack);
        } else {
            player.stopTrack();
        }
        return skippedTracks;
    }

    public AudioTrack getCurrentTrack() {
        return player.getPlayingTrack();
    }

    public List<AudioTrack> getQueueSnapshot() {
        return new ArrayList<>(queue);
    }

    public int getVolume() {
        return player.getVolume();
    }

    public void setVolume(int volume) {
        player.setVolume(volume);
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        Object userData = track.getUserData();
        if (userData instanceof PlaybackMetadata metadata) {
            stateStore.pushHistory(guildId, new TrackHistoryEntry(metadata.playQuery(), metadata.displayTitle(), metadata.sourceUrl(), metadata.requestedBy(), track.getDuration(), track.getInfo().isStream));
            playbackEventListener.onTrackStarted(guildId, metadata, track);
        }
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        Object userData = track.getUserData();
        if (userData instanceof PlaybackMetadata metadata) {
            playbackEventListener.onTrackEnded(guildId, metadata, track, endReason);
        }

        if (!endReason.mayStartNext) {
            return;
        }

        if (userData instanceof PlaybackMetadata metadata && metadata.loopForever()) {
            AudioTrack clone = track.makeClone();
            clone.setUserData(metadata);
            startTrackFresh(clone);
            return;
        }

        AudioTrack nextTrack = queue.poll();
        if (nextTrack != null) {
            startTrackFresh(nextTrack);
        }
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        AudioTrack nextTrack = queue.poll();
        if (nextTrack != null) {
            startTrackFresh(nextTrack);
        }
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        AudioTrack nextTrack = queue.poll();
        if (nextTrack != null) {
            startTrackFresh(nextTrack);
            return;
        }
        player.stopTrack();
    }

    private void startTrackFresh(AudioTrack track) {
        player.setPaused(false);
        player.startTrack(track, false);
    }
}
