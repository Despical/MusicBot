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
 * Turkish and English translations for guild-specific bot responses.
 *
 * <p>{@link dev.despical.musicbot.i18n.BotLanguage} identifies supported languages
 * and their JSON resources. {@link dev.despical.musicbot.i18n.TranslationService}
 * loads those resources, selects each guild's persisted language or the
 * configured fallback, and formats message arguments with
 * {@link java.text.MessageFormat}.</p>
 *
 * <p>Missing message keys are returned as their key names. Missing or unreadable
 * translation resources prevent the translation service from being created.</p>
 *
 * @author Despical
 */
package dev.despical.musicbot.i18n;
