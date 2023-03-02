package me.despical.bot.commands.subcommands;

import me.despical.bot.commands.CommandArguments;
import me.despical.bot.commands.DCommand;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

import java.lang.management.ManagementFactory;

/**
 * @author Despical
 * <p>
 * Created at 1.05.2022
 */
public class UptimeCommand extends DCommand {

	public UptimeCommand() {
		super ("uptime");
	}

	@Override
	public void execute(CommandArguments arguments) {
		final SlashCommandEvent event = arguments.getEvent();
		final MessageChannel channel = arguments.getTextChannel();
		final long
				duration = ManagementFactory.getRuntimeMXBean().getUptime(),
				days = duration / 86400000L % 30,
				hours = duration / 3600000L % 24,
				minutes = duration / 60000L % 60,
				seconds = duration / 1000L % 60;

		event.replyFormat("Tam olarak %d gün, %d saat, %d dakika ve %d saniyedir çalışıyorum.", days, hours, minutes, seconds).queue();
	}
}