package me.despical.bot.commands.subcommands;

import me.despical.bot.commands.CommandArguments;
import me.despical.bot.commands.CommandHandler;
import me.despical.bot.commands.DCommand;
import me.despical.bot.music.PlayerManager;
import me.despical.bot.util.Utils;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 * @author Despical
 * <p>
 * Created at 30.04.2022
 */
public class PlayCommand extends DCommand {

	public PlayCommand() {
		super ("play", "p");
	}

	@Override
	@SuppressWarnings("ConstantConditions")
	public void execute(CommandArguments arguments) {
		final TextChannel channel = arguments.getTextChannel();
		final Member self = channel.getGuild().getSelfMember();
		final GuildVoiceState voiceState = self.getVoiceState();

		if (!voiceState.inVoiceChannel()) {
			channel.sendMessage("Bir odada olmadan müzik çalamam!").queue();
			return;
		}

		final Member member = arguments.getMember();
		final GuildVoiceState memberVoiceState = member.getVoiceState();

		if (!memberVoiceState.inVoiceChannel()) {
			channel.sendMessage("Müzik çalmak için bir odada olmalısın!").queue();
			return;
		}

		if (!memberVoiceState.getChannel().equals(voiceState.getChannel())) {
			channel.sendMessage("Botla aynı odada olmadan müzik çalamazsın!").queue();
			return;
		}

		final String msg = arguments.getMessage().getContentDisplay();
		final String[] array = msg.split(" ");

		if (array.length == 0) {
			channel.sendMessage("Bir link veya şarkı ismi olmadan bir şey çalamam!").queue();
			return;
		}

		String url = Utils.getVideoId(msg.split(" ")[1]);

		if (!Utils.isURL(url)) {
			url = "ytsearch:" + msg.substring(CommandHandler.getPrefix().length() + name.length()).trim();
		}

		PlayerManager.getInstance()
				.loadAndPlay(channel, url);
	}
}