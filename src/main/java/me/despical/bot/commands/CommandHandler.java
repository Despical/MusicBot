package me.despical.bot.commands;

import me.despical.bot.commands.subcommands.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Despical
 * <p>
 * Created at 30.04.2022
 */
public class CommandHandler extends ListenerAdapter {

	@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
	private final Set<DCommand> commands;

	public CommandHandler() {
		this.commands = new HashSet<DCommand>() {{
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

	@Override
	public void onSlashCommand(@NotNull SlashCommandEvent event) {
		String message = event.getName();

		for (DCommand command : this.commands) {
			if (command.isMatching(message)) {
				command.execute(new CommandArguments(event));
				break;
			}
		}
	}
}