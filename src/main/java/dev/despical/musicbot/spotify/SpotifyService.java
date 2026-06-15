package dev.despical.musicbot.spotify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/**
 * @author Despical
 * <p>
 * Created at 16.04.2026
 */
public final class SpotifyService {

    private static final String SPOTIFY_HOST = "open.spotify.com";

    private final String clientId;
    private final String clientSecret;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private String accessToken;
    private Instant tokenExpiresAt;

    public SpotifyService(String clientId, String clientSecret) {
        this.clientId = clientId == null ? "" : clientId;
        this.clientSecret = clientSecret == null ? "" : clientSecret;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public boolean isSpotifyUrl(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }

        try {
            URI uri = URI.create(input.trim());
            return SPOTIFY_HOST.equalsIgnoreCase(uri.getHost()) || "spotify.link".equalsIgnoreCase(uri.getHost());
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public boolean isConfigured() {
        return !clientId.isBlank() && !clientSecret.isBlank();
    }

    public List<SpotifyTrackDescriptor> resolve(String input) {
        if (!isConfigured()) {
            throw new IllegalStateException("Spotify credentials are missing");
        }

        SpotifyUrlParts parts = parseUrl(input);
        return switch (parts.type()) {
            case "track" -> List.of(fetchTrack(parts.id(), input));
            case "playlist" -> fetchPlaylist(parts.id());
            case "album" -> fetchAlbum(parts.id());
            default -> throw new IllegalArgumentException("Unsupported Spotify URL type: " + parts.type());
        };
    }

    private SpotifyTrackDescriptor fetchTrack(String trackId, String originalUrl) {
        JsonNode trackNode = fetchApi("https://api.spotify.com/v1/tracks/" + trackId);
        return toDescriptor(trackNode, originalUrl);
    }

    private List<SpotifyTrackDescriptor> fetchPlaylist(String playlistId) {
        List<SpotifyTrackDescriptor> descriptors = new ArrayList<>();
        String nextUrl = "https://api.spotify.com/v1/playlists/" + playlistId + "/tracks?limit=100";

        while (nextUrl != null) {
            JsonNode page = fetchApi(nextUrl);

            for (JsonNode item : page.path("items")) {
                JsonNode track = item.path("track");

                if (!track.isMissingNode() && !track.isNull()) {
                    descriptors.add(toDescriptor(track, track.path("external_urls").path("spotify").asText("")));
                }
            }

            nextUrl = page.path("next").isNull() ? null : page.path("next").asText(null);
        }

        return descriptors;
    }

    private List<SpotifyTrackDescriptor> fetchAlbum(String albumId) {
        List<SpotifyTrackDescriptor> descriptors = new ArrayList<>();
        JsonNode albumNode = fetchApi("https://api.spotify.com/v1/albums/" + albumId);
        String artistName = firstArtistName(albumNode.path("artists"));
        JsonNode items = albumNode.path("tracks").path("items");

        for (JsonNode track : items) {
            String spotifyUrl = track.path("external_urls").path("spotify").asText("");
            descriptors.add(new SpotifyTrackDescriptor(
                buildSearchQuery(track.path("name").asText(), firstArtistName(track.path("artists")), artistName),
                buildDisplayTitle(track.path("name").asText(), firstArtistName(track.path("artists")), artistName),
                spotifyUrl
            ));
        }

        return descriptors;
    }

    private SpotifyTrackDescriptor toDescriptor(JsonNode trackNode, String originalUrl) {
        String title = trackNode.path("name").asText("Unknown title");
        String artist = firstArtistName(trackNode.path("artists"));
        String spotifyUrl = Optional.ofNullable(originalUrl)
            .filter(value -> !value.isBlank())
            .orElse(trackNode.path("external_urls").path("spotify").asText(""));

        return new SpotifyTrackDescriptor(
            buildSearchQuery(title, artist, ""),
            buildDisplayTitle(title, artist, ""),
            spotifyUrl
        );
    }

    private String firstArtistName(JsonNode artistsNode) {
        if (artistsNode.isArray() && !artistsNode.isEmpty()) {
            return artistsNode.get(0).path("name").asText("");
        }

        return "";
    }

    private String buildSearchQuery(String title, String primaryArtist, String fallbackArtist) {
        String artist = !primaryArtist.isBlank() ? primaryArtist : fallbackArtist;
        return (artist.isBlank() ? title : artist + " - " + title) + " audio";
    }

    private String buildDisplayTitle(String title, String primaryArtist, String fallbackArtist) {
        String artist = !primaryArtist.isBlank() ? primaryArtist : fallbackArtist;
        return artist.isBlank() ? title : artist + " - " + title;
    }

    private JsonNode fetchApi(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + getAccessToken())
                .header("Accept", "application/json")
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new IllegalStateException("Spotify API request failed: " + response.statusCode() + formatErrorDetail(response.body()));
            }

            return objectMapper.readTree(response.body());
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            throw new IllegalStateException("Failed to call Spotify API", exception);
        }
    }

    private String getAccessToken() {
        if (accessToken != null && tokenExpiresAt != null && Instant.now().isBefore(tokenExpiresAt)) {
            return accessToken;
        }

        try {
            String payload = "grant_type=" + URLEncoder.encode("client_credentials", StandardCharsets.UTF_8);
            String credentials = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder(URI.create("https://accounts.spotify.com/api/token"))
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new IllegalStateException("Spotify token request failed: " + response.statusCode() + formatErrorDetail(response.body()));
            }

            JsonNode jsonNode = objectMapper.readTree(response.body());
            accessToken = jsonNode.path("access_token").asText();
            long expiresIn = jsonNode.path("expires_in").asLong(3600);
            tokenExpiresAt = Instant.now().plusSeconds(Math.max(60, expiresIn - 30));
            return accessToken;
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            throw new IllegalStateException("Failed to authenticate with Spotify", exception);
        }
    }

    private String formatErrorDetail(String responseBody) {
        if (responseBody == null) {
            return "";
        }

        String trimmed = responseBody.trim();
        if (trimmed.isEmpty()) {
            return "";
        }

        return ": " + trimmed;
    }

    private SpotifyUrlParts parseUrl(String input) {
        URI uri = URI.create(input.trim());
        if (!SPOTIFY_HOST.equalsIgnoreCase(uri.getHost())) {
            throw new IllegalArgumentException("Unsupported Spotify host: " + uri.getHost().toLowerCase(Locale.ROOT));
        }

        String[] segments = uri.getPath().split("/");
        if (segments.length < 3) {
            throw new IllegalArgumentException("Invalid Spotify URL");
        }

        String type = segments[1];
        String id = segments[2];

        int queryIndex = id.indexOf('?');

        if (queryIndex >= 0) {
            id = id.substring(0, queryIndex);
        }

        return new SpotifyUrlParts(type, id);
    }

    public record SpotifyTrackDescriptor(String playQuery, String displayTitle, String sourceUrl) {
    }

    private record SpotifyUrlParts(String type, String id) {
    }
}
