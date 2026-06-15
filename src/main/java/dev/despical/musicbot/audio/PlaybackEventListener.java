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
