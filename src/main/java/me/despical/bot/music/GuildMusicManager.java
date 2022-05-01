package me.despical.bot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;

/**
 * @author Despical
 * <p>
 * Created at 30.04.2022
 */
public class GuildMusicManager {

	public final AudioPlayer audioPlayer;
	public final TrackScheduler scheduler;

	private final AudioPlayerSendHandler sendHandler;

	public GuildMusicManager(AudioPlayerManager manager) {
		this.audioPlayer = manager.createPlayer();
		this.scheduler = new TrackScheduler(audioPlayer);
		this.audioPlayer.addListener(this.scheduler);
		this.sendHandler = new AudioPlayerSendHandler(this.audioPlayer);
	}

	public AudioPlayerSendHandler getSendHandler() {
		return sendHandler;
	}
}