/*
 * MusicBot - A modern Discord music bot with polished embeds and playback controls.
 * Copyright (C) 2026  Berke Akçen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
/**
 * Startup configuration loaded from dotenv and environment variables.
 *
 * <p>{@link dev.despical.musicbot.config.BotConfig} supplies the required Discord
 * token, optional Spotify credentials, default bot language, and
 * {@link dev.despical.musicbot.config.YoutubeConfig} settings for YouTube OAuth2
 * and remote cipher resolution. Loading validates required values and boolean
 * options, and applies defaults for omitted optional settings.</p>
 *
 * @author Despical
 */
package dev.despical.musicbot.config;
