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
package dev.despical.musicbot.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import dev.despical.musicbot.i18n.TranslationService;
import dev.despical.musicbot.persistence.GuildStateStore;
import dev.despical.musicbot.persistence.GuildStateStore.TrackHistoryEntry;
import dev.despical.musicbot.spotify.SpotifyService;
import dev.despical.musicbot.spotify.SpotifyService.SpotifyTrackDescriptor;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.managers.AudioManager;

import java.net.URI;
import java.time.Instant;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Despical
 * <p>
 * Created at 16.04.2026
 */
public final class MusicService implements PlaybackEventListener {

    private static final int QUEUE_PAGE_SIZE = 10;
    private static final long AUDIO_CONNECT_TIMEOUT_MILLIS = 30_000;
    private static final Color COLOR_PLAYING = new Color(0x22C55E);
    private static final Color COLOR_QUEUED = new Color(0x3B82F6);
    private static final Color COLOR_REPLAY = new Color(0x14B8A6);
    private static final Color COLOR_REPLAY_QUEUE = new Color(0x2563EB);
    private static final Color COLOR_ENDED = new Color(0xF59E0B);

    public static final String PLAYBACK_TOGGLE_BUTTON_ID = "playback-control:toggle";
    public static final String PLAYBACK_SKIP_BUTTON_ID = "playback-control:skip";
    public static final String PLAYBACK_STOP_BUTTON_ID = "playback-control:stop";

    private final AudioPlayerManager playerManager;
    private final Map<Long, GuildAudioManager> musicManagers;
    private final Map<String, MessageChannel> notificationChannels;
    private final Map<Long, AtomicInteger> pendingPlaybackRequests;
    private final Map<String, AtomicInteger> suppressedStartNotifications;
    private final Map<String, AtomicInteger> suppressedQueuedNotifications;
    private final Map<String, AtomicInteger> suppressedEndNotifications;
    private final Map<String, ActiveControlMessage> activeControlMessages;
    private final GuildStateStore stateStore;
    private final SpotifyService spotifyService;
    private final TranslationService translations;

    public MusicService(GuildStateStore stateStore, SpotifyService spotifyService, TranslationService translations) {
        this.stateStore = stateStore;
        this.spotifyService = spotifyService;
        this.translations = translations;
        this.musicManagers = new ConcurrentHashMap<>();
        this.notificationChannels = new ConcurrentHashMap<>();
        this.pendingPlaybackRequests = new ConcurrentHashMap<>();
        this.suppressedStartNotifications = new ConcurrentHashMap<>();
        this.suppressedQueuedNotifications = new ConcurrentHashMap<>();
        this.suppressedEndNotifications = new ConcurrentHashMap<>();
        this.activeControlMessages = new ConcurrentHashMap<>();
        this.playerManager = new DefaultAudioPlayerManager();
        this.registerSources();
    }

    public void registerNotificationChannel(String guildId, MessageChannel messageChannel) {
        notificationChannels.put(guildId, messageChannel);
    }

    public CompletableFuture<QueueResult> play(Member member, String query, int repeatCount, boolean loopForever) {
        VoiceChannel memberChannel = requireMemberChannel(member);
        return playInChannel(member.getGuild(), memberChannel, member.getEffectiveName(), member.getEffectiveAvatarUrl(), query, repeatCount, loopForever, false);
    }

    public CompletableFuture<QueueResult> playSearchResult(Member member, SearchResult searchResult, int repeatCount, boolean loopForever) {
        VoiceChannel memberChannel = requireMemberChannel(member);
        return playSearchResultInChannel(member.getGuild(), memberChannel, member.getEffectiveName(), member.getEffectiveAvatarUrl(), searchResult, repeatCount, loopForever, false);
    }

    public CompletableFuture<List<SearchResult>> search(String query, int limit) {
        if (query == null || query.isBlank()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("no_matches"));
        }

        CompletableFuture<List<SearchResult>> future = new CompletableFuture<>();
        playerManager.loadItem("ytsearch:" + query.trim(), new AudioLoadResultHandler() {

            @Override
            public void trackLoaded(AudioTrack track) {
                future.complete(List.of(toSearchResult(track)));
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                List<SearchResult> results = playlist.getTracks().stream()
                    .limit(Math.max(1, limit))
                    .map(MusicService.this::toSearchResult)
                    .toList();

                if (results.isEmpty()) {
                    future.completeExceptionally(new IllegalStateException("no_matches"));
                    return;
                }

                future.complete(results);
            }

            @Override
            public void noMatches() {
                future.completeExceptionally(new IllegalStateException("no_matches"));
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                future.completeExceptionally(exception);
            }
        });

        return future;
    }

    public boolean shouldPromptForSearchSelection(String query) {
        return query != null && !query.isBlank() && !isUrl(query) && !spotifyService.isSpotifyUrl(query);
    }

    public CompletableFuture<QueueResult> playInChannel(Guild guild, VoiceChannel targetChannel, String requestedBy, String requestedByAvatarUrl, String query, int repeatCount, boolean loopForever, boolean forceMove) {
        GuildAudioManager guildAudioManager = getGuildAudioManager(guild);
        boolean shouldNotifyQueued = shouldSendQueuedNotification(guildAudioManager);
        beginPlaybackRequest(guild.getIdLong());

        try {
            connectIfNeeded(guild, targetChannel, guildAudioManager, forceMove);

            CompletableFuture<QueueResult> playRequest;

            if (spotifyService.isSpotifyUrl(query)) {
                if (!spotifyService.isConfigured()) {
                    throw new IllegalStateException("spotify_credentials_missing");
                }

                List<SpotifyTrackDescriptor> descriptors = spotifyService.resolve(query);
                if (descriptors.isEmpty()) {
                    throw new IllegalStateException("no_matches");
                }

                List<CompletableFuture<LoadResult>> futures = new ArrayList<>();
                int effectiveRepeatCount = loopForever ? 1 : repeatCount;

                for (SpotifyTrackDescriptor descriptor : descriptors) {
                    futures.add(loadAndQueue(guildAudioManager, descriptor.playQuery(), descriptor.displayTitle(), descriptor.sourceUrl(), requestedBy, requestedByAvatarUrl, effectiveRepeatCount, loopForever));
                }

                playRequest = CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                    .thenApply(_ -> futures.stream().map(CompletableFuture::join).toList())
                    .thenApply(results -> {
                        if (shouldNotifyQueued && results.size() == 1) {
                            maybeSendQueuedNotification(guild.getId(), results.getFirst());
                        }

                        int total = results.stream().mapToInt(LoadResult::queuedTracks).sum();
                        return new QueueResult(total, descriptors.getFirst().displayTitle(), targetChannel.getName(), loopForever);
                    });
            } else {
                String effectiveQuery = isUrl(query) ? query.trim() : "ytsearch:" + query.trim();
                int effectiveRepeatCount = loopForever ? 1 : repeatCount;

                playRequest = loadAndQueue(guildAudioManager, effectiveQuery, query.trim(), query.trim(), requestedBy, requestedByAvatarUrl, effectiveRepeatCount, loopForever)
                    .thenApply(loadResult -> {
                        if (shouldNotifyQueued) {
                            maybeSendQueuedNotification(guild.getId(), loadResult);
                        }

                        return new QueueResult(loadResult.queuedTracks(), query.trim(), targetChannel.getName(), loopForever);
                    });
            }

            return playRequest.whenComplete((_, _) -> finishPlaybackRequest(guild.getIdLong()));
        } catch (RuntimeException exception) {
            finishPlaybackRequest(guild.getIdLong());
            return CompletableFuture.failedFuture(exception);
        }
    }

    public CompletableFuture<QueueResult> playSearchResultInChannel(
        Guild guild,
        VoiceChannel targetChannel,
        String requestedBy,
        String requestedByAvatarUrl,
        SearchResult searchResult,
        int repeatCount,
        boolean loopForever,
        boolean forceMove
    ) {
        GuildAudioManager guildAudioManager = getGuildAudioManager(guild);
        boolean shouldNotifyQueued = shouldSendQueuedNotification(guildAudioManager);
        beginPlaybackRequest(guild.getIdLong());

        try {
            connectIfNeeded(guild, targetChannel, guildAudioManager, forceMove);
            int effectiveRepeatCount = loopForever ? 1 : repeatCount;

            return loadAndQueue(guildAudioManager, searchResult.playQuery(), searchResult.title(), searchResult.url(), requestedBy, requestedByAvatarUrl, effectiveRepeatCount, loopForever)
                .thenApply(loadResult -> {
                    if (shouldNotifyQueued) {
                        maybeSendQueuedNotification(guild.getId(), loadResult);
                    }

                    return new QueueResult(loadResult.queuedTracks(), searchResult.title(), targetChannel.getName(), loopForever);
                })
                .whenComplete((_, _) -> finishPlaybackRequest(guild.getIdLong()));
        } catch (RuntimeException exception) {
            finishPlaybackRequest(guild.getIdLong());
            return CompletableFuture.failedFuture(exception);
        }
    }

    public void pause(Guild guild) {
        GuildAudioManager manager = musicManagers.get(guild.getIdLong());
        if (manager == null || !manager.getScheduler().isPlaying()) {
            throw new IllegalStateException("nothing_playing");
        }

        if (manager.getScheduler().isPaused()) {
            throw new IllegalStateException("already_paused");
        }

        manager.getScheduler().pause();
    }

    public void resume(Guild guild) {
        GuildAudioManager manager = musicManagers.get(guild.getIdLong());
        if (manager == null || !manager.getScheduler().isPlaying()) {
            throw new IllegalStateException("nothing_playing");
        }

        if (!manager.getScheduler().isPaused()) {
            throw new IllegalStateException("already_resumed");
        }

        manager.getScheduler().resume();
    }

    public boolean isConnectedToVoice(Guild guild) {
        return Optional.ofNullable(guild.getSelfMember().getVoiceState())
            .map(GuildVoiceState::getChannel)
            .filter(channel -> channel instanceof VoiceChannel)
            .isPresent();
    }

    public boolean isPaused(Guild guild) {
        GuildAudioManager manager = musicManagers.get(guild.getIdLong());

        if (manager == null || !manager.getScheduler().isPlaying()) {
            throw new IllegalStateException("nothing_playing");
        }

        return manager.getScheduler().isPaused();
    }

    public void stop(Guild guild) {
        GuildAudioManager manager = musicManagers.get(guild.getIdLong());

        if (manager == null || !manager.getScheduler().isPlaying()) {
            throw new IllegalStateException("nothing_playing");
        }

        suppressNotification(suppressedEndNotifications, guild.getId());
        manager.getScheduler().stopAndClear();
    }

    public SkipResult skip(Guild guild, int count) {
        GuildAudioManager manager = musicManagers.get(guild.getIdLong());
        if (manager == null) {

            throw new IllegalStateException("nothing_playing");
        }

        AudioTrack currentTrack = manager.getScheduler().getCurrentTrack();
        if (currentTrack == null) {
            throw new IllegalStateException("nothing_playing");
        }

        String currentTrackTitle = resolveTrackTitle(currentTrack);
        suppressNotification(suppressedEndNotifications, guild.getId());

        int skipped = manager.getScheduler().skip(count);
        if (skipped == 0) {
            throw new IllegalStateException("nothing_playing");
        }

        return new SkipResult(skipped, currentTrackTitle);
    }

    public QueueSnapshot getQueue(Guild guild, int page) {
        GuildAudioManager manager = musicManagers.get(guild.getIdLong());
        if (manager == null || manager.getScheduler().getCurrentTrack() == null) {
            throw new IllegalStateException("nothing_playing");
        }

        AudioTrack currentTrack = manager.getScheduler().getCurrentTrack();
        List<AudioTrack> queuedTracks = manager.getScheduler().getQueueSnapshot();
        PlaybackMetadata metadata = extractMetadata(currentTrack);
        List<QueuedTrack> items = queuedTracks.stream()
            .map(track -> new QueuedTrack(resolveTrackTitle(track), resolveTrackUrl(track), resolveRequestedBy(track), resolveLoopState(track), track.getDuration(), track.getInfo().isStream))
            .toList();

        int totalPages = Math.max(1, (int) Math.ceil(items.size() / (double) QUEUE_PAGE_SIZE));
        int safePage = Math.max(1, page);
        if (safePage > totalPages) {
            throw new IllegalArgumentException("invalid_queue_page:" + totalPages);
        }
        int fromIndex = Math.min((safePage - 1) * QUEUE_PAGE_SIZE, items.size());
        int toIndex = Math.min(fromIndex + QUEUE_PAGE_SIZE, items.size());

        return new QueueSnapshot(
            new NowPlayingSnapshot(
                resolveTrackTitle(currentTrack),
                resolveTrackUrl(currentTrack),
                resolveThumbnailUrl(currentTrack),
                resolveRequestedBy(currentTrack),
                currentTrack.getPosition(),
                currentTrack.getDuration(),
                currentTrack.getInfo().isStream,
                metadata != null && metadata.loopForever(),
                manager.getScheduler().getVolume()
            ),
            items.subList(fromIndex, toIndex),
            safePage,
            totalPages,
            items.size()
        );
    }

    public NowPlayingSnapshot getNowPlaying(Guild guild) {
        return getQueue(guild, 1).nowPlaying();
    }

    public int setVolume(Guild guild, int volume) {
        if (volume < 1 || volume > 200) {
            throw new IllegalArgumentException("invalid_volume");
        }

        GuildAudioManager manager = musicManagers.get(guild.getIdLong());
        if (manager == null) {
            throw new IllegalStateException("nothing_playing");
        }

        if (manager.getScheduler().getVolume() == volume) {
            throw new IllegalStateException("same_volume:" + volume);
        }

        manager.getScheduler().setVolume(volume);
        return volume;
    }

    @Override
    public void onTrackStarted(String guildId, PlaybackMetadata metadata, AudioTrack track) {
        if (consumeSuppressedNotification(suppressedStartNotifications, guildId)) {
            return;
        }

        sendPlaybackNotification(guildId, buildStartEmbed(guildId, metadata, track), buildPlaybackControlRows(guildId, false));
    }

    @Override
    public void onTrackEnded(String guildId, PlaybackMetadata metadata, AudioTrack track, AudioTrackEndReason endReason) {
        clearActiveControlMessage(guildId);
        if (endReason == AudioTrackEndReason.FINISHED) {
            return;
        }

        if (consumeSuppressedNotification(suppressedEndNotifications, guildId)) {
            return;
        }

        sendPlaybackNotification(guildId, buildEndEmbed(guildId, metadata, track), List.of());
    }

    public void leave(Guild guild) {
        pendingPlaybackRequests.remove(guild.getIdLong());
        clearActiveControlMessage(guild.getId());

        GuildAudioManager manager = musicManagers.get(guild.getIdLong());
        if (manager != null) {
            if (manager.getScheduler().isPlaying()) {
                suppressNotification(suppressedEndNotifications, guild.getId());
            }

            manager.getScheduler().stopAndClear();
        }

        guild.getAudioManager().closeAudioConnection();
    }

    public void sendTextNotification(String guildId, String message) {
        MessageChannel channel = notificationChannels.get(guildId);
        if (channel == null) {
            return;
        }

        channel.sendMessage(message).queue(
            _ -> {
            },
            _ -> notificationChannels.remove(guildId)
        );
    }

    public void suppressNextStartNotification(String guildId) {
        suppressNotification(suppressedStartNotifications, guildId);
    }

    public void cancelSuppressedStartNotification(String guildId) {
        releaseSuppressedNotification(suppressedStartNotifications, guildId);
    }

    public void suppressNextQueuedNotification(String guildId) {
        suppressNotification(suppressedQueuedNotifications, guildId);
    }

    public void cancelSuppressedQueuedNotification(String guildId) {
        releaseSuppressedNotification(suppressedQueuedNotifications, guildId);
    }

    public MessageEmbed buildHistoryReplayEmbed(String guildId, TrackHistoryEntry entry, Member requester, boolean queued, boolean replayLast) {
        PlaybackMetadata metadata = new PlaybackMetadata(
            entry.playQuery,
            entry.displayTitle,
            entry.sourceUrl,
            requester.getEffectiveName(),
            requester.getEffectiveAvatarUrl(),
            resolveThumbnailUrl(entry.sourceUrl),
            false
        );

        Color color = queued
            ? replayLast ? COLOR_REPLAY_QUEUE : COLOR_QUEUED
            : replayLast ? COLOR_REPLAY : COLOR_PLAYING;

        String title = queued
            ? translations.get(guildId, "notify.queued_title")
            : translations.get(guildId, "notify.started_title");

        return buildTrackNotificationEmbed(guildId, color, title, metadata, entry.duration, entry.live, queued);
    }

    public void setActiveControlMessage(String guildId, MessageChannel channel, long messageId) {
        clearActiveControlMessage(guildId);
        activeControlMessages.put(guildId, new ActiveControlMessage(channel, messageId));
    }

    public List<TrackHistoryEntry> getHistory(String guildId) {
        return stateStore.getHistory(guildId);
    }

    public CompletableFuture<QueueResult> replayHistory(Member member, int index) {
        List<TrackHistoryEntry> history = stateStore.getHistory(member.getGuild().getId());
        if (history.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalStateException("no_history"));
        }

        if (index < 1 || index > history.size()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException(String.valueOf(history.size())));
        }

        TrackHistoryEntry entry = history.get(index - 1);
        return play(member, entry.playQuery, 1, false)
            .thenApply(result -> new QueueResult(result.queuedTracks(), entry.displayTitle, result.joinedChannelName(), false));
    }

    public boolean canControl(Member member, VoiceChannel memberChannel) {
        if (memberChannel == null) {
            return false;
        }

        VoiceChannel botChannel = (VoiceChannel) Optional.ofNullable(member.getGuild().getSelfMember().getVoiceState())
            .map(GuildVoiceState::getChannel)
            .filter(channel -> channel instanceof VoiceChannel)
            .orElse(null);

        return botChannel == null || Objects.equals(botChannel.getId(), memberChannel.getId());
    }

    public boolean hasActivePlayback(Guild guild) {
        if (hasPendingPlaybackRequest(guild.getIdLong())) {
            return true;
        }

        GuildAudioManager manager = musicManagers.get(guild.getIdLong());
        if (manager == null) {
            return false;
        }

        return manager.getScheduler().getCurrentTrack() != null || !manager.getScheduler().getQueueSnapshot().isEmpty();
    }

    public boolean isPlaying(Guild guild) {
        GuildAudioManager manager = musicManagers.get(guild.getIdLong());
        return manager != null && manager.getScheduler().isPlaying();
    }

    public List<ActionRow> buildPlaybackControlRows(Guild guild) {
        return buildPlaybackControlRows(guild.getId(), isPaused(guild));
    }

    private CompletableFuture<LoadResult> loadAndQueue(
        GuildAudioManager guildAudioManager,
        String loadIdentifier,
        String fallbackTitle,
        String sourceUrl,
        String requestedBy,
        String requestedByAvatarUrl,
        int repeatCount,
        boolean loopForever
    ) {
        CompletableFuture<LoadResult> future = new CompletableFuture<>();
        playerManager.loadItemOrdered(guildAudioManager, loadIdentifier, new AudioLoadResultHandler() {

            @Override
            public void trackLoaded(AudioTrack track) {
                String title = track.getInfo().title == null || track.getInfo().title.isBlank() ? fallbackTitle : track.getInfo().title;
                String trackUrl = track.getInfo().uri == null || track.getInfo().uri.isBlank() ? sourceUrl : track.getInfo().uri;

                PlaybackMetadata metadata = new PlaybackMetadata(loadIdentifier, title, trackUrl, requestedBy, requestedByAvatarUrl, resolveThumbnailUrl(trackUrl), loopForever);

                for (int index = 0; index < repeatCount; index++) {
                    AudioTrack trackToQueue = index == 0 ? track : track.makeClone();
                    trackToQueue.setUserData(metadata);
                    guildAudioManager.getScheduler().queue(trackToQueue);
                }

                future.complete(new LoadResult(repeatCount, metadata, track.getDuration(), track.getInfo().isStream));
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if (playlist.isSearchResult()) {
                    trackLoaded(playlist.getTracks().getFirst());
                    return;
                }

                AtomicInteger totalQueued = new AtomicInteger();

                for (AudioTrack track : playlist.getTracks()) {
                    String title = track.getInfo().title;
                    String trackUrl = track.getInfo().uri;

                    for (int index = 0; index < repeatCount; index++) {
                        AudioTrack trackToQueue = track.makeClone();
                        trackToQueue.setUserData(new PlaybackMetadata(trackUrl, title, trackUrl, requestedBy, requestedByAvatarUrl, resolveThumbnailUrl(trackUrl), false));
                        guildAudioManager.getScheduler().queue(trackToQueue);

                        totalQueued.incrementAndGet();
                    }
                }

                future.complete(new LoadResult(totalQueued.get(), null, 0, false));
            }

            @Override
            public void noMatches() {
                future.completeExceptionally(new IllegalStateException("no_matches"));
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                future.completeExceptionally(exception);
            }
        });

        return future;
    }

    private GuildAudioManager getGuildAudioManager(Guild guild) {
        return musicManagers.computeIfAbsent(guild.getIdLong(), _ -> {
            GuildAudioManager guildAudioManager = new GuildAudioManager(playerManager, stateStore, guild.getId(), this);
            AudioManager audioManager = guild.getAudioManager();
            audioManager.setSendingHandler(guildAudioManager.getSendHandler());
            audioManager.setConnectTimeout(AUDIO_CONNECT_TIMEOUT_MILLIS);
            audioManager.setAutoReconnect(true);
            audioManager.setSelfDeafened(true);
            return guildAudioManager;
        });
    }

    private void registerSources() {
        playerManager.registerSourceManager(new YoutubeAudioSourceManager());
        AudioSourceManagers.registerRemoteSources(playerManager, YoutubeAudioSourceManager.class);
        AudioSourceManagers.registerLocalSource(playerManager);
    }

    private VoiceChannel requireMemberChannel(Member member) {
        return Optional.ofNullable(member.getVoiceState())
            .map(GuildVoiceState::getChannel)
            .filter(channel -> channel instanceof VoiceChannel)
            .map(channel -> (VoiceChannel) channel)
            .orElseThrow(() -> new IllegalStateException("member_voice_required"));
    }

    private void connectIfNeeded(Guild guild, VoiceChannel memberChannel, GuildAudioManager guildAudioManager, boolean forceMove) {
        AudioManager audioManager = guild.getAudioManager();
        VoiceChannel connectedChannel = (VoiceChannel) Optional.ofNullable(guild.getSelfMember().getVoiceState())
            .map(GuildVoiceState::getChannel)
            .filter(channel -> channel instanceof VoiceChannel)
            .orElse(null);

        if (connectedChannel != null && !Objects.equals(connectedChannel.getId(), memberChannel.getId()) && !forceMove) {
            throw new IllegalStateException("same_voice_required");
        }

        audioManager.setSendingHandler(guildAudioManager.getSendHandler());
        audioManager.setConnectTimeout(AUDIO_CONNECT_TIMEOUT_MILLIS);
        audioManager.setAutoReconnect(true);

        if (connectedChannel == null || !Objects.equals(connectedChannel.getId(), memberChannel.getId())) {
            audioManager.openAudioConnection(memberChannel);
        }
    }

    private void beginPlaybackRequest(long guildId) {
        pendingPlaybackRequests.compute(guildId, (_, counter) -> {
            if (counter == null) {
                return new AtomicInteger(1);
            }

            counter.incrementAndGet();
            return counter;
        });
    }

    private void finishPlaybackRequest(long guildId) {
        pendingPlaybackRequests.computeIfPresent(guildId, (_, counter) -> counter.decrementAndGet() <= 0 ? null : counter);
    }

    private void suppressNotification(Map<String, AtomicInteger> suppressions, String guildId) {
        suppressions.compute(guildId, (_, counter) -> {
            if (counter == null) {
                return new AtomicInteger(1);
            }

            counter.incrementAndGet();
            return counter;
        });
    }

    private boolean consumeSuppressedNotification(Map<String, AtomicInteger> suppressions, String guildId) {
        AtomicInteger counter = suppressions.get(guildId);
        if (counter == null) {
            return false;
        }

        int remaining = counter.decrementAndGet();
        if (remaining <= 0) {
            suppressions.remove(guildId, counter);
        }

        return true;
    }

    private void releaseSuppressedNotification(Map<String, AtomicInteger> suppressions, String guildId) {
        AtomicInteger counter = suppressions.get(guildId);

        if (counter == null) {
            return;
        }

        int remaining = counter.decrementAndGet();
        if (remaining <= 0) {
            suppressions.remove(guildId, counter);
        }
    }

    private boolean hasPendingPlaybackRequest(long guildId) {
        AtomicInteger counter = pendingPlaybackRequests.get(guildId);
        return counter != null && counter.get() > 0;
    }

    private boolean isUrl(String query) {
        try {
            URI uri = URI.create(query.trim());
            return uri.getScheme() != null && (uri.getScheme().equalsIgnoreCase("http") || uri.getScheme().equalsIgnoreCase("https"));
        } catch (IllegalArgumentException _) {
            return false;
        }
    }

    private SearchResult toSearchResult(AudioTrack track) {
        String title = track.getInfo().title == null || track.getInfo().title.isBlank() ? track.getIdentifier() : track.getInfo().title;
        String url = track.getInfo().uri == null || track.getInfo().uri.isBlank() ? track.getIdentifier() : track.getInfo().uri;
        return new SearchResult(title, url, resolveThumbnailUrl(url), track.getDuration(), track.getInfo().isStream, url);
    }

    private PlaybackMetadata extractMetadata(AudioTrack track) {
        if (track.getUserData() instanceof PlaybackMetadata metadata) {
            return metadata;
        }

        return null;
    }

    private String resolveTrackTitle(AudioTrack track) {
        PlaybackMetadata metadata = extractMetadata(track);

        if (metadata != null && metadata.displayTitle() != null && !metadata.displayTitle().isBlank()) {
            return metadata.displayTitle();
        }

        return track.getInfo().title;
    }

    private String resolveTrackUrl(AudioTrack track) {
        PlaybackMetadata metadata = extractMetadata(track);

        if (metadata != null && metadata.sourceUrl() != null && !metadata.sourceUrl().isBlank()) {
            return metadata.sourceUrl();
        }

        return track.getInfo().uri;
    }

    private String resolveRequestedBy(AudioTrack track) {
        PlaybackMetadata metadata = extractMetadata(track);
        return metadata == null ? "Unknown" : metadata.requestedBy();
    }

    private boolean resolveLoopState(AudioTrack track) {
        PlaybackMetadata metadata = extractMetadata(track);
        return metadata != null && metadata.loopForever();
    }

    private String resolveThumbnailUrl(AudioTrack track) {
        PlaybackMetadata metadata = extractMetadata(track);

        if (metadata != null && metadata.thumbnailUrl() != null && !metadata.thumbnailUrl().isBlank()) {
            return metadata.thumbnailUrl();
        }

        return resolveThumbnailUrl(track.getInfo().uri);
    }

    private String resolveThumbnailUrl(String sourceUrl) {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            return null;
        }

        String videoId = extractYoutubeVideoId(sourceUrl);

        if (videoId == null || videoId.isBlank()) {
            return null;
        }

        return "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
    }

    private String extractYoutubeVideoId(String sourceUrl) {
        try {
            URI uri = URI.create(sourceUrl.trim());
            String host = uri.getHost();

            if (host == null || host.isBlank()) {
                return null;
            }

            host = host.toLowerCase();

            if (host.contains("youtu.be")) {
                String path = uri.getPath();

                if (path == null || path.isBlank() || "/".equals(path)) {
                    return null;
                }

                return path.substring(1).split("/", 2)[0];
            }

            if (host.contains("youtube.com")) {
                String path = uri.getPath();
                String query = uri.getQuery();

                if (path == null) {
                    return null;
                }

                if ("/watch".equals(path) && query != null) {
                    for (String part : query.split("&")) {
                        String[] pieces = part.split("=", 2);

                        if (pieces.length == 2 && "v".equals(pieces[0]) && !pieces[1].isBlank()) {
                            return pieces[1];
                        }
                    }
                }

                String[] segments = path.split("/");

                for (int index = 0; index < segments.length - 1; index++) {
                    if (("embed".equals(segments[index]) || "shorts".equals(segments[index]) || "live".equals(segments[index])) && !segments[index + 1].isBlank()) {
                        return segments[index + 1];
                    }
                }
            }
        } catch (IllegalArgumentException _) {
            return null;
        }

        return null;
    }

    private void sendPlaybackNotification(String guildId, MessageEmbed embed, List<ActionRow> components) {
        MessageChannel channel = notificationChannels.get(guildId);

        if (channel == null) {
            return;
        }

        if (components.isEmpty()) {
            channel.sendMessageEmbeds(embed).queue(
                _ -> {
                },
                _ -> notificationChannels.remove(guildId)
            );

            return;
        }

        clearActiveControlMessage(guildId);

        channel.sendMessageEmbeds(embed)
            .setComponents(components)
            .queue(
                message -> activeControlMessages.put(guildId, new ActiveControlMessage(channel, message.getIdLong())),
                _ -> notificationChannels.remove(guildId)
            );
    }

    private void clearActiveControlMessage(String guildId) {
        ActiveControlMessage activeMessage = activeControlMessages.remove(guildId);
        if (activeMessage == null) {
            return;
        }

        activeMessage.channel().retrieveMessageById(activeMessage.messageId()).queue(
            message -> message.editMessageComponents(List.of()).queue(_ -> { }, _ -> { }), _ -> {}
        );
    }

    private boolean shouldSendQueuedNotification(GuildAudioManager guildAudioManager) {
        return guildAudioManager.getScheduler().getCurrentTrack() != null
            || !guildAudioManager.getScheduler().getQueueSnapshot().isEmpty();
    }

    private void maybeSendQueuedNotification(String guildId, LoadResult loadResult) {
        if (loadResult.queuedTracks() != 1 || loadResult.metadata() == null) {
            return;
        }

        if (consumeSuppressedNotification(suppressedQueuedNotifications, guildId)) {
            return;
        }

        sendPlaybackNotification(guildId, buildQueuedEmbed(guildId, loadResult.metadata(), loadResult.duration(), loadResult.live()), List.of());
    }

    private List<ActionRow> buildPlaybackControlRows(String guildId, boolean paused) {
        Button pauseOrResumeButton = paused
            ? Button.success(PLAYBACK_TOGGLE_BUTTON_ID, translations.get(guildId, "controls.resume"))
            : Button.secondary(PLAYBACK_TOGGLE_BUTTON_ID, translations.get(guildId, "controls.pause"));

        return List.of(ActionRow.of(
            Button.primary(PLAYBACK_SKIP_BUTTON_ID, translations.get(guildId, "controls.skip")),
            pauseOrResumeButton,
            Button.danger(PLAYBACK_STOP_BUTTON_ID, translations.get(guildId, "controls.stop"))
        ));
    }

    private MessageEmbed buildStartEmbed(String guildId, PlaybackMetadata metadata, AudioTrack track) {
        return buildTrackNotificationEmbed(guildId, COLOR_PLAYING, translations.get(guildId, "notify.started_title"), metadata, track.getDuration(), track.getInfo().isStream, false);
    }

    private MessageEmbed buildQueuedEmbed(String guildId, PlaybackMetadata metadata, long duration, boolean live) {
        return buildTrackNotificationEmbed(guildId, COLOR_QUEUED, translations.get(guildId, "notify.queued_title"), metadata, duration, live, true);
    }

    private MessageEmbed buildEndEmbed(String guildId, PlaybackMetadata metadata, AudioTrack track) {
        return buildTrackNotificationEmbed(guildId, COLOR_ENDED, translations.get(guildId, "notify.ended_title"), metadata, track.getDuration(), track.getInfo().isStream, true);
    }

    private MessageEmbed buildTrackNotificationEmbed(String guildId, Color color, String title, PlaybackMetadata metadata, long duration, boolean live, boolean includeRequestedBy) {
        EmbedBuilder embed = baseNotificationEmbed(color, title, metadata)
            .setDescription("**" + markdownLink(metadata.displayTitle(), metadata.sourceUrl()) + "**")
            .addField(translations.get(guildId, "player.position"), formatDuration(duration, live), true);

        if (includeRequestedBy && metadata.requestedBy() != null && !metadata.requestedBy().isBlank()) {
            embed.addField(translations.get(guildId, "player.requested_by"), metadata.requestedBy(), true);
        }

        if (metadata.loopForever()) {
            embed.addField(translations.get(guildId, "common.loop"), translations.get(guildId, "player.loop_enabled"), false);
        }

        return embed.build();
    }

    private EmbedBuilder baseNotificationEmbed(Color color, String title, PlaybackMetadata metadata) {
        EmbedBuilder embed = new EmbedBuilder()
            .setColor(color)
            .setTitle(title)
            .setFooter(translations.getDefault("success.title"))
            .setTimestamp(Instant.now());

        if (metadata.requestedBy() != null && !metadata.requestedBy().isBlank()) {
            embed.setAuthor(metadata.requestedBy(), null, metadata.requestedByAvatarUrl());
        }

        if (metadata.thumbnailUrl() != null && !metadata.thumbnailUrl().isBlank()) {
            embed.setThumbnail(metadata.thumbnailUrl());
        }

        return embed;
    }

    private String formatDuration(long milliseconds, boolean live) {
        if (live) {
            return translations.getDefault("player.duration_live");
        }

        long totalSeconds = Math.max(milliseconds, 0) / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return "%d:%02d:%02d".formatted(hours, minutes, seconds);
        }

        return "%02d:%02d".formatted(minutes, seconds);
    }

    private String markdownLink(String label, String url) {
        if (url == null || url.isBlank()) {
            return label;
        }

        return "[" + label + "](" + url + ")";
    }

    private String withSpacing(String description) {
        if (description == null || description.isBlank()) {
            return description;
        }

        return description.stripTrailing();
    }

    public record QueueResult(int queuedTracks, String displayTitle, String joinedChannelName, boolean loopForever) {
    }

    public record SkipResult(int skippedTracks, String currentTrackTitle) {
    }

    private record LoadResult(int queuedTracks, PlaybackMetadata metadata, long duration, boolean live) {
    }

    public record QueueSnapshot(NowPlayingSnapshot nowPlaying, List<QueuedTrack> queuedTracks, int currentPage,
                                int totalPages, int totalQueuedTracks) {
    }

    public record NowPlayingSnapshot(
        String title,
        String url,
        String thumbnailUrl,
        String requestedBy,
        long position,
        long duration,
        boolean live,
        boolean loopForever,
        int volume
    ) {
    }

    public record QueuedTrack(String title, String url, String requestedBy, boolean loopForever, long duration,
                              boolean live) {
    }

    public record SearchResult(String title, String url, String thumbnailUrl, long duration, boolean live,
                               String playQuery) {
    }

    private record ActiveControlMessage(MessageChannel channel, long messageId) {
    }
}
