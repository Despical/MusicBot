package me.despical.bot.commands.subcommands;

import me.despical.bot.commands.CommandArguments;
import me.despical.bot.commands.DCommand;
import me.despical.bot.music.GuildMusicManager;
import me.despical.bot.music.PlayerManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.managers.AudioManager;

/**
 * @author Despical
 * <p>
 * Created at 30.04.2022
 */
public class LeaveCommand extends DCommand {

	public LeaveCommand() {
		super ("leave", "l");
	}

	@Override
	@SuppressWarnings("ConstantConditions")
	public void execute(CommandArguments arguments) {
		final TextChannel channel = arguments.getTextChannel();
		final Member self = channel.getGuild().getSelfMember();
		final GuildVoiceState state = self.getVoiceState();

		if (!state.inVoiceChannel()) {
			channel.sendMessage("Bot zaten bir odada değil!").queue();
			return;
		}

		final Guild guild = arguments.getGuild();
		final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(guild);

		musicManager.scheduler.clearQueue();
		musicManager.scheduler.getPlayer().stopTrack();

		final AudioManager audioManager = arguments.getGuild().getAudioManager();
		audioManager.closeAudioConnection();

		channel.sendMessage("Bot mevcut odasından ayrıldı.").queue();
	}
}