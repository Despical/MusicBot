package me.despical.bot.commands.subcommands;

import me.despical.bot.commands.CommandArguments;
import me.despical.bot.commands.CommandHandler;
import me.despical.bot.commands.DCommand;
import net.dv8tion.jda.api.entities.MessageChannel;

/**
 * @author Despical
 * <p>
 * Created at 30.04.2022
 */
public class PrefixCommand extends DCommand {

	public PrefixCommand() {
		super ("prefix");
	}

	@Override
	public void execute(CommandArguments arguments) {
		final MessageChannel channel = arguments.getChannel();
		final String[] prefix = arguments.getMessage().getContentDisplay().split(" ");

		if (prefix.length < 1) {
			channel.sendMessage("Lütfen geçerli bir prefix giriniz.").queue();
			return;
		}

		CommandHandler.setPrefix(prefix[1]);

		channel.sendMessage("Prefix başarıyla `" + prefix[1] + "` olarak atanmıştır").queue();
	}
}