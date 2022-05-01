package me.despical.bot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Despical
 * <p>
 * Created at 30.04.2022
 */
public class TrackScheduler extends AudioEventAdapter {

	private boolean repeating = false;

	private final AudioPlayer player;
	private final BlockingQueue<AudioTrack> queue;

	public TrackScheduler(AudioPlayer player) {
		this.player = player;
		this.queue = new LinkedBlockingQueue<>();
	}

	public void queue(AudioTrack track) {
		if (!this.player.startTrack(track, true)) {
			this.queue.offer(track);
		}
	}

	public void playNextTrack() {
		this.player.startTrack(this.queue.poll(), false);
	}

	@Override
	public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
		if (endReason.mayStartNext) {
			if (this.repeating) {
				this.player.startTrack(track.makeClone(), false);
				return;
			}

			this.playNextTrack();
		}
	}

	public AudioPlayer getPlayer() {
		return this.player;
	}

	public void clearQueue() {
		this.queue.clear();
	}

	public boolean toggleRepeating() {
		return this.repeating = !this.repeating;
	}
}