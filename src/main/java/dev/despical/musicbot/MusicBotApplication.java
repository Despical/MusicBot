/*
 * MusicBot - A self-hostable Discord music bot with slash-command playback.
 * Copyright (C) 2026 Despical
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
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
