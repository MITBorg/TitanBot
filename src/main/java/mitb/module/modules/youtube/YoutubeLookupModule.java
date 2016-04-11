package mitb.module.modules.youtube;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
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
import java.util.concurrent.TimeUnit;
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
    private static final Pattern YOUTUBE_LINK_PATTERN = Pattern.compile("https?:\\/\\/(?:www\\.)?youtu(?:be\\.com\\/watch\\?v=|\\.be\\/)([\\w\\-]+)(&(amp;)?[\\w\\?=]*)?",
            Pattern.CASE_INSENSITIVE);
    /**
     * A cache for previously evaluated expressions.
     */
    private static final Cache<String, String> CACHE = CacheBuilder.newBuilder().maximumSize(100)
            .expireAfterAccess(10, TimeUnit.MINUTES).build();

    @Override
    public void register() {
        // TODO: EventHandler.register(this);
    }

    @Listener
    public void onMessage(MessageEvent event) {
        // Short-circuit if no api key configured
        if (GOOGLE_API_KEY.equalsIgnoreCase("NONE"))
            return;

        org.pircbotx.hooks.events.MessageEvent evt = event.getSource();
        String msg = evt.getMessage();

        // Attempt to match link pattern with message
        Matcher matcher = YOUTUBE_LINK_PATTERN.matcher(msg);

        if (matcher.matches()) {
            sendInfo(event, matcher.group(1)); // XXX validate what is captured?
        }
    }

    private void sendInfo(MessageEvent event, String videoId) {
        // Check cache
        String result = CACHE.getIfPresent(videoId);

        if (result != null) {
            event.getSource().getBot().send().message(event.getSource().getChannel().getName(), result);
            return;
        }

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
                            String output = formatVideoDetails(resp);

                            if (output != null) {
                                // Update cache
                                CACHE.put(videoId, output);

                                // Send reply
                                event.getSource().getBot().send().message(event.getSource().getChannel().getName(), output);
                            }
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

        // Truncate title if necessary
        String title = snippet.getTitle();

        if (title.length() > 200)
            title = title.substring(0, 200) + "...";

        // Video title
        sb.append("Youtube: ");
        sb.append(StringHelper.wrapBold(title));
        return sb.toString();
    }

    @Override
    public void getHelp(CommandEvent event) {
        TitanBot.sendReply(event.getSource(), "Displays youtube link information when a link is captured in an active channel.");
    }
}
