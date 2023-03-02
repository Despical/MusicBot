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
 * Created at 30.04.2022
 */
public class StopCommand extends DCommand {

	public StopCommand() {
		super ("stop");
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
			event.reply("Müziği durdurabilmek için odada olmalısın!").queue();
			return;
		}

		if (!memberVoiceState.getChannel().equals(voiceState.getChannel())) {
			event.reply("Botla aynı odada olmadan müziği durduramazsın!").queue();
			return;
		}

		final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(arguments.getGuild());
		final AudioPlayer audioPlayer = musicManager.audioPlayer;

		if (audioPlayer.getPlayingTrack() == null) {
			event.reply("Herhangi bir şarkı zaten çalınmıyor!").queue();
			return;
		}

		musicManager.scheduler.clearQueue();
		musicManager.scheduler.getPlayer().stopTrack();

		event.reply("Şarkı başarıyla durduruldu.").queue();
	}
}