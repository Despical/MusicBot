package me.despical.bot.commands;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

/**
 * @author Despical
 * <p>
 * Created at 30.04.2022
 */
public class CommandArguments {

	private final String message;
	private final MessageChannel channel;
	private final TextChannel textChannel;
	private final Guild guild;
	private final SlashCommandEvent event;

	public CommandArguments(SlashCommandEvent event) {
		this.event = event;
		this.message = event.getName();
		this.channel = event.getChannel();
		this.textChannel = event.getTextChannel();
		this.guild = event.getGuild();
	}

	public SlashCommandEvent getEvent() {
		return this.event;
	}

	public String getMessage() {
		return this.message;
	}

	public User getAuthor() {
		return this.event.getUser();
	}

	public Member getMember() {
		return this.event.getMember();
	}

	public MessageChannel getChannel() {
		return channel;
	}

	public TextChannel getTextChannel() {
		return textChannel;
	}

	public Guild getGuild() {
		return guild;
	}
}