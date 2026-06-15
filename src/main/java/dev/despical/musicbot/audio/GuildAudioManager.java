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
