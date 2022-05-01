package me.despical.bot.commands.subcommands;

import me.despical.bot.commands.CommandArguments;
import me.despical.bot.commands.DCommand;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.managers.AudioManager;

/**
 * @author Despical
 * <p>
 * Created at 30.04.2022
 */
public class JoinCommand extends DCommand {

	public JoinCommand() {
		super ("join", "j");
	}

	@Override
	@SuppressWarnings("ConstantConditions")
	public void execute(CommandArguments arguments) {
		final TextChannel channel = arguments.getTextChannel();
		final Member self = channel.getGuild().getSelfMember();
		final GuildVoiceState state = self.getVoiceState();

		if (state.inVoiceChannel()) {
			channel.sendMessage("Bot zaten bir odada!").queue();
			return;
		}

		final Member member = arguments.getMember();
		final GuildVoiceState memberVoiceState = member.getVoiceState();

		if (!memberVoiceState.inVoiceChannel()) {
			channel.sendMessage("Bu komutu kullanabilmek için bir ses kanalı içinde olmalısın!").queue();
			return;
		}

		final AudioManager audioManager = arguments.getGuild().getAudioManager();
		final VoiceChannel memberChannel = memberVoiceState.getChannel();

		audioManager.openAudioConnection(memberChannel);
		channel.sendMessageFormat("%s adlı ses kanalına bağlanılıyor...", memberChannel.getName()).queue();
	}
}