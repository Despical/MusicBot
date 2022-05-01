package me.despical.bot.commands;

import me.despical.bot.commands.subcommands.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Despical
 * <p>
 * Created at 30.04.2022
 */
public class CommandHandler extends ListenerAdapter {

	private static String prefix = ".";
	private final Set<DCommand> commands;

	public CommandHandler() {
		this.commands = new HashSet<DCommand>() {{
			add(new PrefixCommand());
			add(new JoinCommand());
			add(new LeaveCommand());
			add(new PlayCommand());
			add(new StopCommand());
			add(new SkipCommand());
			add(new PauseCommand());
			add(new RepeatCommand());
			add(new UptimeCommand());
		}};
	}

	public static void setPrefix(String prefix) {
		CommandHandler.prefix = prefix;
	}

	public static String getPrefix() {
		return prefix;
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		String message = event.getMessage().getContentDisplay(), start = message.substring(0, prefix.length());

		if (!start.equals(prefix)) return;

		for (DCommand command : this.commands) {
			if (command.isMatching(message.substring(prefix.length()).split(" ")[0])) {
				command.execute(new CommandArguments(event));
				break;
			}
		}
	}
}