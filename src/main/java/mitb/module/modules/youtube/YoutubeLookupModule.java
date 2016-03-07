package mitb.module.modules.youtube;

import com.google.gson.Gson;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import mitb.TitanBot;
import mitb.event.EventHandler;
import mitb.event.Listener;
import mitb.event.events.CommandEvent;
import mitb.event.events.MessageEvent;
import mitb.module.Module;
import mitb.module.modules.youtube.json.Item;
import mitb.module.modules.youtube.json.Snippet;
import mitb.module.modules.youtube.json.Statistics;
import mitb.module.modules.youtube.json.YoutubeQuery;
import mitb.util.Properties;
import mitb.util.StringHelper;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Looks up youtube links and displays video information.
 */
public final class YoutubeLookupModule extends Module {

    /**
     * The API url.
     */
    private static final String API_URL = "https://www.googleapis.com/youtube/v3/videos?id=";
    /**
     * The Google API key to use.
     */
    private static final String GOOGLE_API_KEY = Properties.getValue("google.api_key");
    /**
     * A RegEx to parse out Youtube video links - from http://stackoverflow.com/questions/3717115/regular-expression-for-youtube-links
     * This was adapted to support HTTPs links.
     */
    private static final Pattern YOUTUBE_LINK_PATTERN = Pattern.compile("https?:\\/\\/(?:www\\.)?youtu(?:be\\.com\\/watch\\?v=|\\.be\\/)([\\w\\-]+)(&(amp;)?[\\w\\?=]*)?");

    @Override
    protected void register() {
        EventHandler.register(this);
    }

    @Listener
    public void onMessage(MessageEvent event) {
        // Short-circuit if no api key configured
        if (GOOGLE_API_KEY.equalsIgnoreCase("NONE"))
            return;

        // TODO make it work for input regardless of case: note using String#toLowerCase() isnt going to achieve this!
        org.pircbotx.hooks.events.MessageEvent evt = event.getSource();
        String msg = evt.getMessage();

        // Attempt to match link pattern with message
        Matcher matcher = YOUTUBE_LINK_PATTERN.matcher(msg);

        if (matcher.matches()) {
            sendInfo(event, matcher.group(1)); // XXX validate what is captured?
        }
    }

    private void sendInfo(MessageEvent event, String videoId) {

        // Construct url
        String url = API_URL + videoId + "&part=snippet,statistics&key=" + GOOGLE_API_KEY;

        // API call
        AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
        asyncHttpClient.prepareGet(url)
                .addHeader("Accept", "text/plain")
                .execute(new AsyncCompletionHandler<Response>() {

                    @Override
                    public Response onCompleted(Response response) throws Exception {
                        // Parsing response
                        String body = response.getResponseBody();
                        YoutubeQuery resp = new Gson().fromJson(body, YoutubeQuery.class);

                        // Evaluating response
                        if (resp != null) {
                            String msg = formatVideoDetails(resp);

                            if (msg != null)
                            TitanBot.sendReply(event.getSource(), msg);
                        }
                        return response;
                    }

                    @Override
                    public void onThrowable(Throwable t) {
                        // Something wrong happened.
                    }
                });
    }

    /**
     * Formats the given query into a version of it.
     * @param resp pretty output or null if invalid query
     */
    private String formatVideoDetails(YoutubeQuery resp) {
        StringBuilder sb = new StringBuilder();
        List<Item> items = resp.getItems();

        // Check items length
        if (items.size() == 0) {
            return null;
        }
        Item item = items.get(0);
        Snippet snippet = item.getSnippet();
        Statistics stats = item.getStatistics();

        // Generate description
        String description = snippet.getDescription();

        if (description.length() > 200) {
            description = description.substring(0, 200 - 3) + "...";
        }
        description = StringHelper.stripNewlines(description).replaceAll("  ", " ");

        // Video title and description
        sb.append(StringHelper.wrapBold(snippet.getTitle())).append(": ")
                .append(description).append(" | ");

        // Video statistics and uploaded
        sb.append("Views: ").append(StringHelper.wrapBold(stats.getViewCount()))
                .append(", Likes/Dislikes: ").append(StringHelper.wrapBold("+" + stats.getLikeCount() + "/-" + stats.getDislikeCount()))
                .append(" (by ").append(StringHelper.wrapBold(snippet.getChannelTitle())).append(")");
        return sb.toString();
    }

    @Override
    public void getHelp(CommandEvent event) {
        TitanBot.sendReply(event.getSource(), "Displays youtube link information when a link is captured in an active channel.");
    }
}
