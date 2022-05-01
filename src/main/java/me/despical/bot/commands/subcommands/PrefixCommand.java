package me.despical.bot.commands.subcommands;

import me.despical.bot.commands.CommandArguments;
import me.despical.bot.commands.CommandHandler;
import me.despical.bot.commands.DCommand;
import net.dv8tion.jda.api.MessageBuilder;

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
		String message = arguments.getMessage().getContentDisplay(), prefix = message.split(" ")[1];
		CommandHandler.setPrefix(prefix);

		arguments.getChannel().sendMessage(new MessageBuilder("Prefix başarıyla `" + prefix + "` olarak atanmıştır").build()).queue();
	}
}