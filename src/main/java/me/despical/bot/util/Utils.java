package me.despical.bot.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Despical
 * <p>
 * Created at 30.04.2022
 */
public class Utils {

	private static final String YT_VIDEO_ID_REGEX = "http(?:s)?://(?:m.)?(?:www\\.)?youtu(?:\\.be/|be\\.com/(?:watch\\?(?:feature=youtu.be&)?v=|v/|embed/|user/(?:[\\w#]+/)+))([^&#?\\n]+)";
	private static final Pattern YT_VIDEO_ID_PATTERN = Pattern.compile(YT_VIDEO_ID_REGEX, Pattern.CASE_INSENSITIVE);

	public static String getVideoId(String url) {
		if (url == null) return "";

		String id = "";
		Matcher matcher = YT_VIDEO_ID_PATTERN.matcher(url);

		if (matcher.find()){
			return matcher.group(1);
		}

		return id;
	}

	public static boolean isURL(String url) {
		if (url.isEmpty()) return false;

		try {
			new URI(url);
			return true;
		} catch (URISyntaxException ignored) {
			return false;
		}
	}
}