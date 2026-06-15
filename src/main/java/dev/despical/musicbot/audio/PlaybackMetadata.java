package dev.despical.musicbot.audio;

/**
 * @author Despical
 * <p>
 * Created at 16.04.2026
 */
public record PlaybackMetadata(
    String playQuery,
    String displayTitle,
    String sourceUrl,
    String requestedBy,
    String requestedByAvatarUrl,
    String thumbnailUrl,
    boolean loopForever
) {
}
