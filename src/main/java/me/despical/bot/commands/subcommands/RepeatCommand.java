package me.despical.bot.commands.subcommands;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import me.despical.bot.commands.CommandArguments;
import me.despical.bot.commands.DCommand;
import me.despical.bot.music.GuildMusicManager;
import me.despical.bot.music.PlayerManager;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

/**
 * @author Despical
 * <p>
 * Created at 1.05.2022
 */
public class RepeatCommand extends DCommand {

	public RepeatCommand() {
		super ("repeat");
	}

	@Override
	@SuppressWarnings("ConstantConditions")
	public void execute(CommandArguments arguments) {
		final TextChannel channel = arguments.getTextChannel();
		final Member self = channel.getGuild().getSelfMember();
		final GuildVoiceState voiceState = self.getVoiceState();
		final SlashCommandEvent event = arguments.getEvent();

		if (!voiceState.inVoiceChannel()) {
			event.reply("Zaten müzik çalmıyorum!").queue();
			return;
		}

		final Member member = arguments.getMember();
		final GuildVoiceState memberVoiceState = member.getVoiceState();

		if (!memberVoiceState.inVoiceChannel()) {
			event.reply("Müziği döngüye sokmak için bir odada olmalısın!").queue();
			return;
		}

		if (!memberVoiceState.getChannel().equals(voiceState.getChannel())) {
			event.reply("Botla aynı odada olmadan müziği döngüye sokamazsın!").queue();
			return;
		}

		final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(arguments.getGuild());
		final AudioPlayer audioPlayer = musicManager.audioPlayer;

		if (audioPlayer.getPlayingTrack() == null) {
			event.reply("Döngüye sokulacak bir şarkı bulunamadı.").queue();
			return;
		}

		boolean repeating = musicManager.scheduler.toggleRepeating();

		event.reply(repeating ? "Mevcut şarkı döngüye alındı." : "Mevcut şarkı döngüden çıkarıldı.").queue();
	}
}