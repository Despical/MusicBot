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
package dev.despical.musicbot.listener;

import dev.despical.musicbot.audio.MusicService;
import dev.despical.musicbot.audio.MusicService.NowPlayingSnapshot;
import dev.despical.musicbot.audio.MusicService.QueueSnapshot;
import dev.despical.musicbot.audio.MusicService.QueuedTrack;
import dev.despical.musicbot.audio.MusicService.SearchResult;
import dev.despical.musicbot.i18n.BotLanguage;
import dev.despical.musicbot.i18n.TranslationService;
import dev.despical.musicbot.persistence.GuildStateStore;
import dev.despical.musicbot.persistence.GuildStateStore.TrackHistoryEntry;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @author Despical
 * <p>
 * Created at 16.04.2026
 */
public final class BotListener extends ListenerAdapter {

    private static final long AUTO_DISCONNECT_DELAY_SECONDS = 2;
    private static final long SEARCH_SELECTION_TIMEOUT_SECONDS = 120;
    private static final int SEARCH_RESULT_LIMIT = 5;
    private static final Color COLOR_PRIMARY = new Color(0x5865F2);
    private static final Color COLOR_SUCCESS = new Color(0x22C55E);
    private static final Color COLOR_WARNING = new Color(0xF59E0B);
    private static final Color COLOR_ERROR = new Color(0xEF4444);
    private static final String PLAY_SELECT_PREFIX = "play-select:";
    private static final String LAST_REPLAY_PREFIX = "last-replay:";

    private final MusicService musicService;
    private final GuildStateStore stateStore;
    private final TranslationService translations;
    private final ScheduledExecutorService scheduler;
    private final Map<String, ScheduledFuture<?>> pendingDisconnects;
    private final Map<String, PendingPlaySelection> pendingSelections;
    private final Map<String, Long> recentlyCompletedSelections;

    public BotListener(MusicService musicService, GuildStateStore stateStore, TranslationService translations) {
        this.musicService = musicService;
        this.stateStore = stateStore;
        this.translations = translations;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.pendingDisconnects = new ConcurrentHashMap<>();
        this.pendingSelections = new ConcurrentHashMap<>();
        this.recentlyCompletedSelections = new ConcurrentHashMap<>();
    }

    @Override
    public void onReady(ReadyEvent event) {
        event.getJDA().updateCommands()
            .addCommands(
                Commands.slash("play", "Play a song, URL, Spotify link, or search query")
                    .addOption(OptionType.STRING, "query", "Song name or URL", true)
                    .addOptions(new OptionData(OptionType.INTEGER, "repeat_count", "How many times to queue the same track").setRequired(false).setMinValue(1).setMaxValue(25))
                    .addOption(OptionType.BOOLEAN, "loop_forever", "Loop the selected track forever", false),
                Commands.slash("pause", "Pause playback"),
                Commands.slash("skip", "Skip one or more tracks")
                    .addOptions(new OptionData(OptionType.INTEGER, "count", "How many tracks to skip including the current one").setRequired(false).setMinValue(1).setMaxValue(25)),
                Commands.slash("resume", "Resume playback"),
                Commands.slash("stop", "Stop playback and clear the queue"),
                Commands.slash("queue", "Show the current queue")
                    .addOptions(new OptionData(OptionType.INTEGER, "page", "Queue page number").setRequired(false).setMinValue(1).setMaxValue(100)),
                Commands.slash("nowplaying", "Show the current track"),
                Commands.slash("volume", "Change playback volume")
                    .addOptions(new OptionData(OptionType.INTEGER, "value", "Volume percent from 1 to 200").setRequired(true).setMinValue(1).setMaxValue(200)),
                Commands.slash("leave", "Disconnect the bot from voice"),
                Commands.slash("last", "List or replay one of the last 5 songs")
                    .addOptions(new OptionData(OptionType.INTEGER, "index", "Replay a song from history using its list index").setRequired(false).setMinValue(1).setMaxValue(5)),
                Commands.slash("replaylast", "Replay the most recent song"),
                Commands.slash("language", "Select the bot language")
                    .addOption(OptionType.STRING, "value", "TR or EN", true, true),
                Commands.slash("help", "List available commands and descriptions")
            )
            .queue();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null || event.getMember() == null) {
            event.reply(translations.getDefault("error.guild_only")).setEphemeral(true).queue();
            return;
        }

        switch (event.getName()) {
            case "play" -> handlePlay(event);
            case "pause" ->
                handleStateTextCommand(event, "state.paused", musicService::pause, this::isAlreadyPausedError);
            case "skip" -> handleSkip(event);
            case "resume" ->
                handleStateTextCommand(event, "state.resumed", musicService::resume, this::isAlreadyResumedError);
            case "stop" -> handleStop(event);
            case "queue" -> handleQueue(event);
            case "nowplaying" -> handleNowPlaying(event);
            case "volume" -> handleVolume(event);
            case "leave" -> handleLeave(event);
            case "last" -> handleLast(event);
            case "replaylast" -> handleReplayLast(event);
            case "language" -> handleLanguage(event);
            case "help" -> handleHelp(event);
            default ->
                event.reply(translations.get(event.getGuild().getId(), "error.guild_only")).setEphemeral(true).queue();
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();

        if (componentId.startsWith(PLAY_SELECT_PREFIX)) {
            handlePlaySelection(event);
            return;
        }

        if (componentId.startsWith("playback-control:")) {
            handlePlaybackControl(event);
            return;
        }

        if (componentId.startsWith(LAST_REPLAY_PREFIX)) {
            handleLastReplaySelection(event);
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (!"language".equals(event.getName()) || !"value".equals(event.getFocusedOption().getName())) {
            return;
        }

        String query = event.getFocusedOption().getValue().trim().toUpperCase(Locale.ROOT);
        List<String> suggestions = Stream.of("TR", "EN")
            .filter(value -> query.isBlank() || value.startsWith(query))
            .toList();
        event.replyChoiceStrings(suggestions).queue();
    }

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        if (event.getMember().getUser().isBot() && event.getJDA().getSelfUser().getId().equals(event.getMember().getId())) {
            return;
        }

        Guild guild = event.getGuild();
        String guildId = guild.getId();
        VoiceChannel botChannel = null;

        if (guild.getSelfMember().getVoiceState() != null && guild.getSelfMember().getVoiceState().getChannel() instanceof VoiceChannel channel) {
            botChannel = channel;
        }

        if (botChannel == null) {
            cancelPendingDisconnect(guildId);
            return;
        }

        boolean isChannelLeft = event.getChannelLeft() instanceof VoiceChannel && event.getChannelLeft().getId().equals(botChannel.getId());
        boolean isChannelJoined = event.getChannelJoined() instanceof VoiceChannel && event.getChannelJoined().getId().equals(botChannel.getId());

        if (isChannelJoined) {
            cancelPendingDisconnect(guildId);
        }

        if (isChannelLeft) {
            boolean hasHumanListeners = botChannel.getMembers().stream().anyMatch(member -> !member.getUser().isBot());

            if (hasHumanListeners) {
                cancelPendingDisconnect(guildId);
                return;
            }

            cancelPendingDisconnect(guildId);
            String channelId = botChannel.getId();

            ScheduledFuture<?> pendingTask = scheduler.schedule(() -> {
                pendingDisconnects.remove(guildId);
                tryAutoDisconnect(guild, channelId);
            }, AUTO_DISCONNECT_DELAY_SECONDS, TimeUnit.SECONDS);
            pendingDisconnects.put(guildId, pendingTask);
        }
    }

    private void tryAutoDisconnect(Guild guild, String expectedChannelId) {
        if (guild.getSelfMember().getVoiceState() == null || !(guild.getSelfMember().getVoiceState().getChannel() instanceof VoiceChannel currentChannel)) {
            return;
        }

        if (!Objects.equals(currentChannel.getId(), expectedChannelId)) {
            return;
        }

        boolean hasHumanListeners = currentChannel.getMembers().stream().anyMatch(member -> !member.getUser().isBot());
        if (!hasHumanListeners) {
            musicService.leave(guild);
            musicService.sendTextNotification(guild.getId(), translations.get(guild.getId(), "state.auto_left"));
        }
    }

    private void cancelPendingDisconnect(String guildId) {
        ScheduledFuture<?> pendingTask = pendingDisconnects.remove(guildId);
        if (pendingTask != null) {
            pendingTask.cancel(false);
        }
    }

    private void handlePlay(SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        if (member == null || member.getVoiceState() == null || member.getVoiceState().getChannel() == null) {
            replyTranslated(event, "error.member_voice_required", false);
            return;
        }

        VoiceChannel voiceChannel = member.getVoiceState().getChannel().asVoiceChannel();
        if (!musicService.canControl(member, voiceChannel)) {
            replyTranslated(event, "error.same_voice_required", false);
            return;
        }

        cancelPendingDisconnect(event.getGuild().getId());
        musicService.registerNotificationChannel(event.getGuild().getId(), event.getChannel());

        String query = event.getOption("query", OptionMapping::getAsString);
        int repeatCount = Math.toIntExact(event.getOption("repeat_count", 1L, OptionMapping::getAsLong));
        boolean loopForever = event.getOption("loop_forever", false, OptionMapping::getAsBoolean);

        if (musicService.shouldPromptForSearchSelection(query)) {
            handlePlaySearchSelection(event, member, query, repeatCount, loopForever);
            return;
        }

        event.deferReply().queue();
        musicService.play(member, query, repeatCount, loopForever)
            .whenComplete((_, throwable) -> {
                if (throwable != null) {
                    event.getHook().editOriginal(resolveErrorMessage(event.getGuild().getId(), throwable)).queue();
                    return;
                }

                event.getHook().deleteOriginal().queue();
            });
    }

    private void handlePlaySearchSelection(SlashCommandInteractionEvent event, Member member, String query, int repeatCount, boolean loopForever) {
        event.deferReply(true).queue();

        musicService.search(query, SEARCH_RESULT_LIMIT)
            .whenComplete((results, throwable) -> {
                if (throwable != null) {
                    event.getHook().editOriginal(resolveErrorMessage(event.getGuild().getId(), throwable)).queue();
                    return;
                }

                String token = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
                pendingSelections.put(token, new PendingPlaySelection(
                    event.getGuild().getId(),
                    member.getId(),
                    query.trim(),
                    repeatCount,
                    loopForever,
                    System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(SEARCH_SELECTION_TIMEOUT_SECONDS),
                    results
                ));

                scheduler.schedule(() -> pendingSelections.remove(token), SEARCH_SELECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                event.getHook().editOriginalEmbeds(searchResultsEmbed(event.getGuild().getId(), query, results, member))
                    .setComponents(buildSearchSelectionRows(token, results.size()))
                    .queue();
            });
    }

    private void handlePlaySelection(ButtonInteractionEvent event) {
        ParsedSelection parsedSelection = parseSelection(event.getComponentId());
        if (parsedSelection == null) {
            event.reply(translations.get(resolveGuildId(event), "search.invalid_selection")).setEphemeral(true).queue();
            return;
        }

        PendingPlaySelection selection = pendingSelections.get(parsedSelection.token());
        if (selection == null && isRecentlyCompletedSelection(parsedSelection.token())) {
            event.deferEdit().queue(_ -> {
            }, _ -> {
            });
            return;
        }

        if (!isUsableSelection(event, selection)) {
            return;
        }

        if (parsedSelection.index() < 0 || parsedSelection.index() >= selection.results().size()) {
            event.reply(translations.get(selection.guildId(), "search.invalid_selection")).setEphemeral(true).queue();
            return;
        }

        Member member = event.getMember();
        if (member == null || member.getVoiceState() == null || member.getVoiceState().getChannel() == null) {
            event.reply(translations.get(selection.guildId(), "error.member_voice_required")).setEphemeral(true).queue();
            return;
        }

        VoiceChannel voiceChannel = member.getVoiceState().getChannel().asVoiceChannel();
        if (!musicService.canControl(member, voiceChannel)) {
            event.reply(translations.get(selection.guildId(), "error.same_voice_required")).setEphemeral(true).queue();
            return;
        }

        pendingSelections.remove(parsedSelection.token());
        markSelectionCompleted(parsedSelection.token());
        cancelPendingDisconnect(selection.guildId());
        musicService.registerNotificationChannel(selection.guildId(), event.getChannel());

        SearchResult selectedResult = selection.results().get(parsedSelection.index());
        event.deferEdit().queue();
        event.getMessage().editMessageComponents(List.of()).queue(_ -> {
        }, _ -> {
        });
        musicService.playSearchResult(member, selectedResult, selection.repeatCount(), selection.loopForever())
            .whenComplete((_, throwable) -> {
                if (throwable != null) {
                    event.getMessage().editMessage(resolveErrorMessage(selection.guildId(), throwable))
                        .setComponents(List.of())
                        .queue();
                    return;
                }

                event.getHook().deleteOriginal().queue();
            });
    }

    private boolean isUsableSelection(ButtonInteractionEvent event, PendingPlaySelection selection) {
        String guildId = resolveGuildId(event);
        if (selection == null || selection.isExpired()) {
            if (selection != null) {
                pendingSelections.values().remove(selection);
            }

            event.getMessage().editMessageComponents(List.of()).queue();
            event.reply(translations.get(guildId, "search.expired")).setEphemeral(true).queue();
            return false;
        }

        if (!Objects.equals(selection.guildId(), guildId)) {
            event.reply(translations.get(guildId, "search.expired")).setEphemeral(true).queue();
            return false;
        }

        if (event.getUser() == null || !Objects.equals(selection.requestedByUserId(), event.getUser().getId())) {
            event.reply(translations.get(selection.guildId(), "search.unauthorized")).setEphemeral(true).queue();
            return false;
        }

        return true;
    }

    private void markSelectionCompleted(String token) {
        recentlyCompletedSelections.put(token, System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10));
        scheduler.schedule(() -> recentlyCompletedSelections.remove(token), 10, TimeUnit.SECONDS);
    }

    private boolean isRecentlyCompletedSelection(String token) {
        Long expiresAt = recentlyCompletedSelections.get(token);
        if (expiresAt == null) {
            return false;
        }

        if (System.currentTimeMillis() > expiresAt) {
            recentlyCompletedSelections.remove(token);
            return false;
        }

        return true;
    }

    private ParsedSelection parseSelection(String componentId) {
        if (!componentId.startsWith(PLAY_SELECT_PREFIX)) {
            return null;
        }

        String payload = componentId.substring(PLAY_SELECT_PREFIX.length());
        String[] pieces = payload.split(":", 2);

        if (pieces.length != 2) {
            return null;
        }

        try {
            return new ParsedSelection(pieces[0], Integer.parseInt(pieces[1]));
        } catch (NumberFormatException _) {
            return null;
        }
    }

    private List<ActionRow> buildSearchSelectionRows(String token, int resultCount) {
        List<Button> selectionButtons = new ArrayList<>();

        for (int index = 0; index < resultCount; index++) {
            selectionButtons.add(Button.primary(PLAY_SELECT_PREFIX + token + ":" + index, String.valueOf(index + 1)));
        }

        return List.of(ActionRow.of(selectionButtons));
    }

    private MessageEmbed searchResultsEmbed(String guildId, String query, List<SearchResult> results, Member actor) {
        StringBuilder description = new StringBuilder(translations.get(guildId, "search.description", query)).append("\n");

        for (int index = 0; index < results.size(); index++) {
            SearchResult result = results.get(index);
            description.append("`")
                .append(index + 1)
                .append("` ")
                .append(markdownLink(result.title(), result.url()))
                .append(" - ")
                .append(formatDuration(result.duration(), result.live()))
                .append('\n');
        }

        description.append("\n").append(translations.get(guildId, "search.instructions"));

        EmbedBuilder embed = new EmbedBuilder()
            .setColor(COLOR_PRIMARY)
            .setTitle(translations.get(guildId, "search.title"))
            .setDescription(withSpacing(description.toString()));

        results.stream()
            .map(SearchResult::thumbnailUrl)
            .filter(url -> url != null && !url.isBlank())
            .findFirst()
            .ifPresent(embed::setThumbnail);

        return decorateEmbed(embed, actor);
    }

    private void handleQueue(SlashCommandInteractionEvent event) {
        try {
            int page = Math.toIntExact(event.getOption("page", 1L, OptionMapping::getAsLong));
            QueueSnapshot snapshot = musicService.getQueue(event.getGuild(), page);
            event.replyEmbeds(queueEmbed(event.getGuild().getId(), snapshot, event.getMember())).queue();
        } catch (Exception exception) {
            replyErrorText(event, resolveQueueErrorMessage(event.getGuild().getId(), exception), isNothingPlayingError(exception));
        }
    }

    private void handleSkip(SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        if (member == null || member.getVoiceState() == null || member.getVoiceState().getChannel() == null) {
            replyTranslated(event, "error.member_voice_required", false);
            return;
        }

        VoiceChannel voiceChannel = member.getVoiceState().getChannel().asVoiceChannel();
        if (!musicService.canControl(member, voiceChannel)) {
            replyTranslated(event, "error.same_voice_required", false);
            return;
        }

        try {
            int count = Math.toIntExact(event.getOption("count", 1L, OptionMapping::getAsLong));
            MusicService.SkipResult result = musicService.skip(event.getGuild(), count);
            String message = result.skippedTracks() > 1
                ? translations.get(event.getGuild().getId(), "state.skipped_multi", result.skippedTracks())
                : translations.get(event.getGuild().getId(), "state.skipped_named", result.currentTrackTitle());
            replyText(event, message, false);
        } catch (Exception exception) {
            replyErrorText(event, resolveErrorMessage(event.getGuild().getId(), exception), false);
        }
    }

    private void handleNowPlaying(SlashCommandInteractionEvent event) {
        try {
            NowPlayingSnapshot snapshot = musicService.getNowPlaying(event.getGuild());
            event.replyEmbeds(nowPlayingEmbed(event.getGuild().getId(), snapshot, event.getMember())).setEphemeral(true).queue();
        } catch (Exception exception) {
            replyErrorText(event, resolveErrorMessage(event.getGuild().getId(), exception), true);
        }
    }

    private void handleVolume(SlashCommandInteractionEvent event) {
        Member member = event.getMember();

        if (member == null || member.getVoiceState() == null || member.getVoiceState().getChannel() == null) {
            replyTranslated(event, "error.member_voice_required", false);
            return;
        }

        VoiceChannel voiceChannel = member.getVoiceState().getChannel().asVoiceChannel();
        if (!musicService.canControl(member, voiceChannel)) {
            replyTranslated(event, "error.same_voice_required", false);
            return;
        }

        try {
            int volume = Math.toIntExact(event.getOption("value", OptionMapping::getAsLong));
            int appliedVolume = musicService.setVolume(event.getGuild(), volume);
            replyText(event, translations.get(event.getGuild().getId(), "state.volume", appliedVolume), false);
        } catch (Exception exception) {
            replyErrorText(event, resolveErrorMessage(event.getGuild().getId(), exception), isSameVolumeError(exception));
        }
    }

    private void handleLast(SlashCommandInteractionEvent event) {
        List<TrackHistoryEntry> history = musicService.getHistory(event.getGuild().getId());
        OptionMapping indexOption = event.getOption("index");

        if (indexOption == null) {
            if (history.isEmpty()) {
                replyTranslated(event, "error.no_history", true);
                return;
            }

            String guildId = event.getGuild().getId();
            StringBuilder description = new StringBuilder(translations.get(guildId, "history.header")).append("\n\n");

            for (int index = 0; index < history.size(); index++) {
                TrackHistoryEntry entry = history.get(index);
                description.append(index + 1)
                    .append(". ")
                    .append(markdownLink(entry.displayTitle, entry.sourceUrl))
                    .append(" - ")
                    .append(formatDuration(entry.duration, entry.live))
                    .append(" - ")
                    .append(resolveHistoryRequestedBy(guildId, entry))
                    .append('\n');
            }

            event.replyEmbeds(decorateEmbed(new EmbedBuilder()
                    .setColor(COLOR_PRIMARY)
                    .setTitle(translations.get(guildId, "history.title"))
                    .setDescription(withSpacing(description.toString())), event.getMember()))
                .addComponents(ActionRow.of(buildLastReplayButtons(history.size())))
                .setEphemeral(true)
                .queue();
            return;
        }

        Member member = event.getMember();
        if (member == null || member.getVoiceState() == null || member.getVoiceState().getChannel() == null) {
            replyTranslated(event, "error.member_voice_required", false);
            return;
        }

        VoiceChannel voiceChannel = member.getVoiceState().getChannel().asVoiceChannel();
        if (!musicService.canControl(member, voiceChannel)) {
            replyTranslated(event, "error.same_voice_required", false);
            return;
        }

        int index = Math.toIntExact(indexOption.getAsLong());
        TrackHistoryEntry selectedEntry = history.get(index - 1);
        event.deferReply().queue();

        replyWithHistoryPlayback(event, member, selectedEntry, musicService.replayHistory(member, index), false);
    }

    private void handleLastReplaySelection(ButtonInteractionEvent event) {
        Integer index = parseLastReplayIndex(event.getComponentId());
        if (index == null) {
            event.reply(translations.get(resolveGuildId(event), "search.invalid_selection")).setEphemeral(true).queue();
            return;
        }

        Member member = event.getMember();
        if (member == null || member.getVoiceState() == null || member.getVoiceState().getChannel() == null) {
            event.reply(translations.get(resolveGuildId(event), "error.member_voice_required")).setEphemeral(true).queue();
            return;
        }

        VoiceChannel voiceChannel = member.getVoiceState().getChannel().asVoiceChannel();
        if (!musicService.canControl(member, voiceChannel)) {
            event.reply(translations.get(resolveGuildId(event), "error.same_voice_required")).setEphemeral(true).queue();
            return;
        }

        event.deferEdit().queue();
        TrackHistoryEntry selectedEntry = musicService.getHistory(resolveGuildId(event)).get(index - 1);
        replyWithHistoryPlayback(event, member, selectedEntry, musicService.replayHistory(member, index), false);
    }

    private void handlePlaybackControl(ButtonInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) {
            event.reply(translations.getDefault("error.guild_only")).setEphemeral(true).queue();
            return;
        }

        Member member = event.getMember();
        if (member == null || member.getVoiceState() == null || member.getVoiceState().getChannel() == null) {
            event.reply(translations.get(guild.getId(), "error.member_voice_required")).setEphemeral(true).queue();
            return;
        }

        VoiceChannel voiceChannel = member.getVoiceState().getChannel().asVoiceChannel();
        if (!musicService.canControl(member, voiceChannel)) {
            event.reply(translations.get(guild.getId(), "error.same_voice_required")).setEphemeral(true).queue();
            return;
        }

        try {
            String componentId = event.getComponentId();

            switch (event.getComponentId()) {
                case MusicService.PLAYBACK_SKIP_BUTTON_ID -> musicService.skip(guild, 1);
                case MusicService.PLAYBACK_TOGGLE_BUTTON_ID -> {
                    if (musicService.isPaused(guild)) {
                        musicService.resume(guild);
                    } else {
                        musicService.pause(guild);
                    }
                }
                case MusicService.PLAYBACK_STOP_BUTTON_ID -> musicService.stop(guild);
                default -> {
                    event.reply(translations.get(guild.getId(), "search.invalid_selection")).setEphemeral(true).queue();
                    return;
                }
            }

            if (MusicService.PLAYBACK_TOGGLE_BUTTON_ID.equals(componentId) && musicService.hasActivePlayback(guild)) {
                event.editComponents(musicService.buildPlaybackControlRows(guild)).queue();
                return;
            }
            event.editComponents().queue();
        } catch (Exception exception) {
            event.reply(resolveErrorMessage(guild.getId(), exception)).setEphemeral(true).queue();
        }
    }

    private void handleReplayLast(SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        if (member == null || member.getVoiceState() == null || member.getVoiceState().getChannel() == null) {
            replyTranslated(event, "error.member_voice_required", false);
            return;
        }

        VoiceChannel voiceChannel = member.getVoiceState().getChannel().asVoiceChannel();
        if (!musicService.canControl(member, voiceChannel)) {
            replyTranslated(event, "error.same_voice_required", false);
            return;
        }

        List<TrackHistoryEntry> history = musicService.getHistory(event.getGuild().getId());
        if (history.isEmpty()) {
            replyTranslated(event, "error.no_history", false);
            return;
        }

        event.deferReply().queue();
        TrackHistoryEntry entry = history.getFirst();
        replyWithHistoryPlayback(event, member, entry, musicService.replayHistory(member, 1), true);
    }

    private void handleLeave(SlashCommandInteractionEvent event) {
        if (!musicService.isConnectedToVoice(event.getGuild())) {
            replyErrorText(event, translations.get(event.getGuild().getId(), "error.bot_not_connected"), true);
            return;
        }

        handleStateTextCommand(event, "state.left", musicService::leave);
    }

    private void replyWithHistoryPlayback(
        SlashCommandInteractionEvent event,
        Member member,
        TrackHistoryEntry entry,
        CompletionStage<MusicService.QueueResult> replayFuture,
        boolean replayLast
    ) {
        boolean hadPlayingTrack = musicService.isPlaying(event.getGuild());

        if (hadPlayingTrack) {
            musicService.suppressNextQueuedNotification(event.getGuild().getId());
        } else {
            musicService.suppressNextStartNotification(event.getGuild().getId());
        }

        replayFuture.whenComplete((_, throwable) -> {
            if (throwable != null) {
                if (hadPlayingTrack) {
                    musicService.cancelSuppressedQueuedNotification(event.getGuild().getId());
                } else {
                    musicService.cancelSuppressedStartNotification(event.getGuild().getId());
                }
                event.getHook().editOriginal(resolveErrorMessage(event.getGuild().getId(), throwable)).queue();
                return;
            }

            MessageEmbed replyEmbed = musicService.buildHistoryReplayEmbed(event.getGuild().getId(), entry, member, hadPlayingTrack, replayLast);
            if (hadPlayingTrack) {
                event.getHook().editOriginalEmbeds(replyEmbed).queue();
                return;
            }

            event.getHook()
                .editOriginalEmbeds(replyEmbed)
                .setComponents(musicService.buildPlaybackControlRows(event.getGuild()))
                .queue(message -> musicService.setActiveControlMessage(event.getGuild().getId(), event.getChannel(), message.getIdLong()));
        });
    }

    private void replyWithHistoryPlayback(
        ButtonInteractionEvent event,
        Member member,
        TrackHistoryEntry entry,
        CompletionStage<MusicService.QueueResult> replayFuture,
        boolean replayLast
    ) {
        boolean hadPlayingTrack = musicService.isPlaying(event.getGuild());

        if (hadPlayingTrack) {
            musicService.suppressNextQueuedNotification(resolveGuildId(event));
        } else {
            musicService.suppressNextStartNotification(resolveGuildId(event));
        }

        replayFuture.whenComplete((_, throwable) -> {
            if (throwable != null) {
                if (hadPlayingTrack) {
                    musicService.cancelSuppressedQueuedNotification(resolveGuildId(event));
                } else {
                    musicService.cancelSuppressedStartNotification(resolveGuildId(event));
                }

                event.getHook().editOriginal(resolveErrorMessage(resolveGuildId(event), throwable))
                    .setComponents(List.of())
                    .queue();
                return;
            }

            MessageEmbed replyEmbed = musicService.buildHistoryReplayEmbed(resolveGuildId(event), entry, member, hadPlayingTrack, replayLast);
            event.getMessage().editMessageComponents(List.of()).queue(_ -> {
            }, _ -> {
            });

            if (hadPlayingTrack) {
                event.getChannel().sendMessageEmbeds(replyEmbed).queue();
                return;
            }

            event.getChannel().sendMessageEmbeds(replyEmbed)
                .setComponents(musicService.buildPlaybackControlRows(event.getGuild()))
                .queue(message -> musicService.setActiveControlMessage(resolveGuildId(event), event.getChannel(), message.getIdLong()));
        });
    }

    private void handleLanguage(SlashCommandInteractionEvent event) {
        String value = event.getOption("value", OptionMapping::getAsString);
        BotLanguage language = BotLanguage.fromCode(value).orElse(null);

        if (language == null) {
            replyErrorText(event, translations.get(event.getGuild().getId(), "error.invalid_language"), false);
            return;
        }

        BotLanguage currentLanguage = stateStore.getLanguage(event.getGuild().getId()).orElse(BotLanguage.TR);
        if (currentLanguage == language) {
            replyText(event, translations.get(event.getGuild().getId(), "state.language_already", translations.get(language, "language.name")), false);
            return;
        }

        stateStore.setLanguage(event.getGuild().getId(), language);
        replyText(event, translations.get(event.getGuild().getId(), "state.language", translations.get(language, "language.name")), false);
    }

    private void handleHelp(SlashCommandInteractionEvent event) {
        String guildId = event.getGuild().getId();
        List<String> commandLines = List.of(
            formatHelpLine(guildId, "/play <query>", "help.play"),
            formatHelpLine(guildId, "/pause", "help.pause"),
            formatHelpLine(guildId, "/skip [count]", "help.skip"),
            formatHelpLine(guildId, "/resume", "help.resume"),
            formatHelpLine(guildId, "/stop", "help.stop"),
            formatHelpLine(guildId, "/queue [page]", "help.queue"),
            formatHelpLine(guildId, "/nowplaying", "help.nowplaying"),
            formatHelpLine(guildId, "/volume <1-200>", "help.volume"),
            formatHelpLine(guildId, "/leave", "help.leave"),
            formatHelpLine(guildId, "/last [index]", "help.last"),
            formatHelpLine(guildId, "/replaylast", "help.replaylast"),
            formatHelpLine(guildId, "/language <TR|EN>", "help.language"),
            formatHelpLine(guildId, "/help", "help.help")
        );

        String description = String.join("\n", commandLines)
            + "\n\n"
            + translations.get(guildId, "help.credit_prefix")
            + " [Despical](https://github.com/Despical).";

        event.replyEmbeds(decorateEmbed(new EmbedBuilder()
                .setColor(COLOR_PRIMARY)
                .setTitle(translations.get(guildId, "help.title"))
                .setDescription(withSpacing(description)), event.getMember()))
            .setEphemeral(true)
            .queue();
    }

    private String formatHelpLine(String guildId, String commandUsage, String translationKey) {
        return "`" + commandUsage + "` - " + translations.get(guildId, translationKey);
    }

    private List<Button> buildLastReplayButtons(int historySize) {
        List<Button> buttons = new ArrayList<>();
        for (int index = 0; index < historySize; index++) {
            buttons.add(Button.primary(LAST_REPLAY_PREFIX + (index + 1), String.valueOf(index + 1)));
        }
        return buttons;
    }

    private Integer parseLastReplayIndex(String componentId) {
        if (!componentId.startsWith(LAST_REPLAY_PREFIX)) {
            return null;
        }

        try {
            return Integer.parseInt(componentId.substring(LAST_REPLAY_PREFIX.length()));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void handleStateTextCommand(SlashCommandInteractionEvent event, String successKey, GuildAction action) {
        handleStateTextCommand(event, successKey, action, _ -> false);
    }

    private void handleStateTextCommand(SlashCommandInteractionEvent event, String successKey, GuildAction action, Predicate<Throwable> ephemeralErrorPredicate) {
        Member member = event.getMember();
        if (member == null || member.getVoiceState() == null || member.getVoiceState().getChannel() == null) {
            replyErrorText(event, translations.get(event.getGuild().getId(), "error.member_voice_required"), false);
            return;
        }

        VoiceChannel voiceChannel = member.getVoiceState().getChannel().asVoiceChannel();
        if (!musicService.canControl(member, voiceChannel)) {
            replyErrorText(event, translations.get(event.getGuild().getId(), "error.same_voice_required"), false);
            return;
        }

        try {
            action.run(event.getGuild());
            replyText(event, translations.get(event.getGuild().getId(), successKey), false);
        } catch (Exception exception) {
            replyErrorText(event, resolveErrorMessage(event.getGuild().getId(), exception), ephemeralErrorPredicate.test(exception));
        }
    }

    private void handleStop(SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        if (member == null || member.getVoiceState() == null || member.getVoiceState().getChannel() == null) {
            replyErrorText(event, translations.get(event.getGuild().getId(), "error.member_voice_required"), false);
            return;
        }

        VoiceChannel voiceChannel = member.getVoiceState().getChannel().asVoiceChannel();
        if (!musicService.canControl(member, voiceChannel)) {
            replyErrorText(event, translations.get(event.getGuild().getId(), "error.same_voice_required"), false);
            return;
        }

        try {
            musicService.stop(event.getGuild());
            replyText(event, translations.get(event.getGuild().getId(), "state.stopped"), false);
        } catch (Exception exception) {
            boolean ephemeral = isNothingPlayingError(exception);
            replyErrorText(event, resolveErrorMessage(event.getGuild().getId(), exception), ephemeral);
        }
    }

    private void replyTranslated(SlashCommandInteractionEvent event, String key, boolean ephemeral) {
        if (key.startsWith("error.")) {
            replyErrorText(event, translations.get(event.getGuild().getId(), key), ephemeral);
            return;
        }

        replyText(event, translations.get(event.getGuild().getId(), key), ephemeral);
    }

    private void replyText(SlashCommandInteractionEvent event, String message, boolean ephemeral) {
        replyStatus(event, message, ephemeral, COLOR_SUCCESS, "success.title");
    }

    private void replyErrorText(SlashCommandInteractionEvent event, String message, boolean ephemeral) {
        replyStatus(event, message, ephemeral, COLOR_ERROR, "error.title");
    }

    private void replyStatus(SlashCommandInteractionEvent event, String message, boolean ephemeral, Color color, String titleKey) {
        MessageEmbed embed = decorateEmbed(new EmbedBuilder()
            .setColor(color)
            .setTitle(translations.get(event.getGuild().getId(), titleKey))
            .setDescription(message), event.getMember());

        if (event.isAcknowledged()) {
            event.getHook().editOriginalEmbeds(embed).queue();
            return;
        }

        event.replyEmbeds(embed).setEphemeral(ephemeral).queue();
    }

    private String resolveErrorMessage(String guildId, Throwable throwable) {
        Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null ? throwable.getCause() : throwable;

        if (cause instanceof IllegalStateException illegalStateException) {
            String message = illegalStateException.getMessage();

            if (message != null && message.startsWith("same_volume:")) {
                return translations.get(guildId, "state.volume_already", message.split(":", 2)[1]);
            }

            return switch (message) {
                case "member_voice_required" -> translations.get(guildId, "error.member_voice_required");
                case "same_voice_required" -> translations.get(guildId, "error.same_voice_required");
                case "nothing_playing" -> translations.get(guildId, "error.nothing_playing");
                case "already_paused" -> translations.get(guildId, "error.already_paused");
                case "already_resumed" -> translations.get(guildId, "error.already_resumed");
                case "bot_not_connected" -> translations.get(guildId, "error.bot_not_connected");
                case "no_history" -> translations.get(guildId, "error.no_history");
                case "spotify_credentials_missing" -> translations.get(guildId, "error.spotify_credentials_missing");
                case "no_matches" -> translations.get(guildId, "error.no_matches");
                case "empty_queue" -> translations.get(guildId, "error.empty_queue");
                default -> translations.get(guildId, "error.load_failed", illegalStateException.getMessage());
            };
        }

        if (cause instanceof IllegalArgumentException illegalArgumentException) {
            if ("invalid_volume".equals(illegalArgumentException.getMessage())) {
                return translations.get(guildId, "error.invalid_volume");
            }

            if (illegalArgumentException.getMessage() != null && illegalArgumentException.getMessage().startsWith("invalid_queue_page:")) {
                return translations.get(guildId, "error.invalid_queue_page", illegalArgumentException.getMessage().split(":", 2)[1]);
            }

            return translations.get(guildId, "error.invalid_history_index", illegalArgumentException.getMessage());
        }

        return translations.get(guildId, "error.load_failed", cause.getMessage());
    }

    private boolean isNothingPlayingError(Throwable throwable) {
        Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null ? throwable.getCause() : throwable;
        return cause instanceof IllegalStateException illegalStateException
            && "nothing_playing".equals(illegalStateException.getMessage());
    }

    private boolean isAlreadyPausedError(Throwable throwable) {
        Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null ? throwable.getCause() : throwable;
        return cause instanceof IllegalStateException illegalStateException
            && "already_paused".equals(illegalStateException.getMessage());
    }

    private boolean isAlreadyResumedError(Throwable throwable) {
        Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null ? throwable.getCause() : throwable;
        return cause instanceof IllegalStateException illegalStateException
            && "already_resumed".equals(illegalStateException.getMessage());
    }

    private boolean isSameVolumeError(Throwable throwable) {
        Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null ? throwable.getCause() : throwable;
        return cause instanceof IllegalStateException illegalStateException
            && illegalStateException.getMessage() != null
            && illegalStateException.getMessage().startsWith("same_volume:");
    }

    private String resolveQueueErrorMessage(String guildId, Throwable throwable) {
        Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null ? throwable.getCause() : throwable;

        if (cause instanceof IllegalArgumentException illegalArgumentException && illegalArgumentException.getMessage() != null && illegalArgumentException.getMessage().startsWith("invalid_queue_page:")) {
            int totalPages = Integer.parseInt(illegalArgumentException.getMessage().split(":", 2)[1]);

            if (totalPages <= 1) {
                return translations.get(guildId, "error.single_queue_page");
            }
        }

        return resolveErrorMessage(guildId, throwable);
    }

    private MessageEmbed decorateEmbed(EmbedBuilder embed, Member actor) {
        if (actor != null) {
            embed.setAuthor(actor.getEffectiveName(), null, actor.getEffectiveAvatarUrl());
        }

        embed.setFooter(translations.getDefault("success.title"))
            .setTimestamp(Instant.now());

        return embed.build();
    }

    private MessageEmbed queueEmbed(String guildId, QueueSnapshot snapshot, Member actor) {
        StringBuilder queued = new StringBuilder();
        List<QueuedTrack> queuedTracks = snapshot.queuedTracks();

        for (int index = 0; index < queuedTracks.size(); index++) {
            QueuedTrack track = queuedTracks.get(index);
            int absoluteIndex = ((snapshot.currentPage() - 1) * 10) + index + 1;
            queued.append("`")
                .append(absoluteIndex)
                .append("` ")
                .append(markdownLink(track.title(), track.url()))
                .append(" - ")
                .append(formatDuration(track.duration(), track.live()))
                .append(" - ")
                .append(track.requestedBy());
            if (track.loopForever()) {
                queued.append(" (loop)");
            }

            queued.append('\n');
        }

        if (queuedTracks.isEmpty()) {
            queued
                .append("- ")
                .append(translations.get(guildId, "queue.empty"));
        }

        String nowPlaying = trackSummaryLine(snapshot.nowPlaying().title(), snapshot.nowPlaying().url(), snapshot.nowPlaying().duration(), snapshot.nowPlaying().live(), snapshot.nowPlaying().requestedBy());

        return decorateEmbed(new EmbedBuilder()
            .setColor(COLOR_PRIMARY)
            .setTitle(translations.get(guildId, "queue.title"))
            .setDescription(queueStatusText(guildId, snapshot))
            .addField(translations.get(guildId, "queue.now_playing"), nowPlaying, false)
            .addField(translations.get(guildId, "queue.up_next"), withSpacing(queued.toString()), false)
            .setThumbnail(snapshot.nowPlaying().thumbnailUrl()), actor);
    }

    private MessageEmbed nowPlayingEmbed(String guildId, NowPlayingSnapshot snapshot, Member actor) {
        EmbedBuilder embed = new EmbedBuilder()
            .setColor(COLOR_PRIMARY)
            .setTitle(translations.get(guildId, "player.title"))
            .setDescription("**" + markdownLink(snapshot.title(), snapshot.url()) + "**")
            .addField(translations.get(guildId, "player.position"), formatDuration(snapshot.duration(), snapshot.live()), true)
            .addField(translations.get(guildId, "player.requested_by"), snapshot.requestedBy(), true)
            .addField(translations.get(guildId, "player.volume"), "%" + snapshot.volume(), true)
            .setThumbnail(snapshot.thumbnailUrl());

        if (snapshot.loopForever()) {
            embed.addField(translations.get(guildId, "common.loop"), translations.get(guildId, "player.loop_enabled"), false);
        }

        return decorateEmbed(embed, actor);
    }

    private String queueStatusText(String guildId, QueueSnapshot snapshot) {
        if (snapshot.totalQueuedTracks() == 0) {
            return translations.get(guildId, "queue.no_up_next_status");
        }

        if (snapshot.totalPages() > 1) {
            return translations.get(guildId, "queue.page_status", snapshot.currentPage(), snapshot.totalPages(), snapshot.totalQueuedTracks());
        }

        return translations.get(guildId, "queue.total_up_next", snapshot.totalQueuedTracks());
    }

    private String trackSummaryLine(String title, String url, long duration, boolean live, String requestedBy) {
        return markdownLink(title, url) + " - " + formatDuration(duration, live) + " - " + requestedBy;
    }

    private String resolveHistoryRequestedBy(String guildId, TrackHistoryEntry entry) {
        if (entry.requestedBy != null && !entry.requestedBy.isBlank()) {
            return entry.requestedBy;
        }

        return translations.get(guildId, "history.unknown_requester");
    }

    private String withSpacing(String description) {
        if (description == null || description.isBlank()) {
            return description;
        }

        return description.stripTrailing();
    }

    private String formatDuration(long milliseconds, boolean live) {
        if (live) {
            return "LIVE";
        }

        Duration duration = Duration.ofMillis(Math.max(milliseconds, 0));
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        if (hours > 0) {
            return "%d:%02d:%02d".formatted(hours, minutes, seconds);
        }

        return "%02d:%02d".formatted(duration.toMinutes(), seconds);
    }

    private String markdownLink(String label, String url) {
        if (url == null || url.isBlank()) {
            return label;
        }

        return "[" + label + "](" + url + ")";
    }

    private String resolveGuildId(ButtonInteractionEvent event) {
        return event.getGuild() == null ? "0" : event.getGuild().getId();
    }

    @FunctionalInterface
    private interface GuildAction {

        void run(Guild guild);
    }

    private record PendingPlaySelection(
        String guildId,
        String requestedByUserId,
        String query,
        int repeatCount,
        boolean loopForever,
        long expiresAtMillis,
        List<SearchResult> results
    ) {

        private boolean isExpired() {
            return System.currentTimeMillis() > expiresAtMillis;
        }
    }

    private record ParsedSelection(String token, int index) {
    }
}
