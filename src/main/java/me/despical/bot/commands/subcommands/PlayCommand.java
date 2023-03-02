package me.despical.bot.commands.subcommands;

import me.despical.bot.commands.CommandArguments;
import me.despical.bot.commands.DCommand;
import me.despical.bot.music.PlayerManager;
import me.despical.bot.util.Utils;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

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
		final SlashCommandEvent event = arguments.getEvent();

		if (!voiceState.inVoiceChannel()) {
			event.reply("Bir odada olmadan müzik çalamam!").queue();
			return;
		}

		final Member member = arguments.getMember();
		final GuildVoiceState memberVoiceState = member.getVoiceState();

		if (!memberVoiceState.inVoiceChannel()) {
			event.reply("Müzik çalmak için bir odada olmalısın!").queue();
			return;
		}

		if (!memberVoiceState.getChannel().equals(voiceState.getChannel())) {
			event.reply("Botla aynı odada olmadan müzik çalamazsın!").queue();
			return;
		}

		final String msg = event.getOption("yt_link").getAsString();

		if (msg == null) {
			event.reply("Bir link veya şarkı ismi olmadan bir şey çalamam!").queue();
			return;
		}

		String url = Utils.getVideoId(msg);

		if (!Utils.isURL(url)) {
			url = "ytsearch:" + msg.substring(name.length()).trim();
		}

		PlayerManager.getInstance()
				.loadAndPlay(channel, url);
	}
}