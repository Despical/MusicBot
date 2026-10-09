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
 * Spotify link resolution into metadata and playable search queries.
 *
 * <p>{@link dev.despical.musicbot.spotify.SpotifyService} reads track, album, and
 * playlist metadata through the Spotify Web API, follows playlist pagination,
 * and returns descriptors containing a search query, display title, and source
 * URL. The playback service uses these descriptors to find playable audio.</p>
 *
 * <p>API requests require configured Spotify credentials and use a cached
 * client-credentials access token. Resolution failures are reported to callers
 * so the interaction layer can present an appropriate response.</p>
 *
 * @author Despical
 */
package dev.despical.musicbot.spotify;
