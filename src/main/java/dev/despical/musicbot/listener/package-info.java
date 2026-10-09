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
 * Discord event handling and user interaction flows for the music bot.
 *
 * <p>{@link dev.despical.musicbot.listener.BotListener} registers slash commands,
 * routes command and button interactions to the playback service, and builds
 * localized replies and embeds. It also handles language autocomplete,
 * interactive search selection, and playback-history replay.</p>
 *
 * <p>Voice-state events and scheduled tasks coordinate automatic disconnection,
 * while pending search selections track their requester and expiration.</p>
 *
 * @author Despical
 */
package dev.despical.musicbot.listener;
