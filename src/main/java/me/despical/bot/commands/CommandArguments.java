package me.despical.bot.commands;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * @author Despical
 * <p>
 * Created at 30.04.2022
 */
public class CommandArguments {

	private final Message message;
	private final MessageChannel channel;
	private final TextChannel textChannel;
	private final Guild guild;

	public CommandArguments(MessageReceivedEvent event) {
		this.message = event.getMessage();
		this.channel = event.getChannel();
		this.textChannel = event.getTextChannel();
		this.guild = event.getGuild();
	}

	public Message getMessage() {
		return this.message;
	}

	public User getAuthor() {
		return this.message.getAuthor();
	}

	public Member getMember() {
		return this.message.getMember();
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