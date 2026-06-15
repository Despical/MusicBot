package dev.despical.musicbot;

import club.minnced.discord.jdave.interop.JDaveSessionFactory;
import dev.despical.musicbot.audio.MusicService;
import dev.despical.musicbot.config.BotConfig;
import dev.despical.musicbot.i18n.TranslationService;
import dev.despical.musicbot.listener.BotListener;
import dev.despical.musicbot.persistence.GuildStateStore;
import dev.despical.musicbot.spotify.SpotifyService;
import lombok.experimental.UtilityClass;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.audio.AudioModuleConfig;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;

/**
 * @author Despical
 * <p>
 * Created at 16.04.2026
 */
@UtilityClass
public class MusicBotApplication {

    static void main() throws InterruptedException {
        var config = BotConfig.load();
        var stateStore = new GuildStateStore(config.defaultLanguage());
        var translations = new TranslationService(stateStore, config.defaultLanguage());
        var spotifyService = new SpotifyService(config.spotifyClientId(), config.spotifyClientSecret());
        var musicService = new MusicService(stateStore, spotifyService, translations);

        JDABuilder.createDefault(config.token())
            .enableIntents(GatewayIntent.GUILD_VOICE_STATES)
            .setAudioModuleConfig(new AudioModuleConfig().withDaveSessionFactory(new JDaveSessionFactory()))
            .setActivity(Activity.listening("/play"))
            .addEventListeners(new BotListener(musicService, stateStore, translations))
            .build()
            .awaitReady();
    }
}
