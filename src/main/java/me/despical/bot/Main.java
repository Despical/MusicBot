package me.despical.bot;

import me.despical.bot.commands.CommandHandler;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.Compression;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import javax.security.auth.login.LoginException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Despical
 * <p>
 * Created at 30.04.2022
 */
public class Main {

	public static void main(String[] args) throws LoginException {
		JDA jda = JDABuilder.createDefault("")
		.enableCache(CacheFlag.VOICE_STATE)
		.disableCache(CacheFlag.MEMBER_OVERRIDES)
		.enableIntents(GatewayIntent.GUILD_VOICE_STATES)
		.setMemberCachePolicy(MemberCachePolicy.VOICE.or(MemberCachePolicy.OWNER))
		.setBulkDeleteSplittingEnabled(false)
		.setCompression(Compression.NONE)
		.setActivity(Activity.listening("Müzik"))
		.addEventListeners(new CommandHandler())
		.build();

		OnlineStatus[] statuses = {OnlineStatus.ONLINE, OnlineStatus.DO_NOT_DISTURB, OnlineStatus.IDLE};

		ScheduledExecutorService pool = Executors.newSingleThreadScheduledExecutor(runnable -> {
			Thread thread = new Thread(runnable);
			thread.setDaemon(true);

			return thread;
		});

		int[] currentIndex = {0};

		pool.scheduleWithFixedDelay(() -> {
			currentIndex[0] = (currentIndex[0] + 1) % statuses.length;
			jda.getPresence().setStatus((statuses[currentIndex[0]]));
		}, 0, 5, TimeUnit.SECONDS);
	}
}