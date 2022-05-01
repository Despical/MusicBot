package me.despical.bot.commands.subcommands;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import me.despical.bot.commands.CommandArguments;
import me.despical.bot.commands.DCommand;
import me.despical.bot.music.GuildMusicManager;
import me.despical.bot.music.PlayerManager;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 * @author Despical
 * <p>
 * Created at 1.05.2022
 */
public class PauseCommand extends DCommand {

	public PauseCommand() {
		super ("pause");
	}

	@Override
	@SuppressWarnings("ConstantConditions")
	public void execute(CommandArguments arguments) {
		final TextChannel channel = arguments.getTextChannel();
		final Member self = channel.getGuild().getSelfMember();
		final GuildVoiceState voiceState = self.getVoiceState();

		if (!voiceState.inVoiceChannel()) {
			channel.sendMessage("Zaten müzik çalmıyorum!").queue();
			return;
		}

		final Member member = arguments.getMember();
		final GuildVoiceState memberVoiceState = member.getVoiceState();

		if (!memberVoiceState.inVoiceChannel()) {
			channel.sendMessage("Müziği durdurabilmek için odada olmalısın!").queue();
			return;
		}

		if (!memberVoiceState.getChannel().equals(voiceState.getChannel())) {
			channel.sendMessage("Botla aynı odada olmadan müziği durduramazsın!").queue();
			return;
		}

		final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(arguments.getGuild());
		final AudioPlayer audioPlayer = musicManager.audioPlayer;

		if (audioPlayer.getPlayingTrack() == null) {
			channel.sendMessage("Herhangi bir şarkı zaten çalınmıyor!").queue();
			return;
		}

		final boolean paused = musicManager.audioPlayer.isPaused();

		musicManager.audioPlayer.setPaused(!paused);

		channel.sendMessage(paused ? "Şarkı devam ettiriliyor." : "Şarkı duraklatıldı.").queue();
	}
}