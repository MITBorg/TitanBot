package mitb.module.modules.googsearch;

import com.google.common.base.Joiner;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import mitb.TitanBot;
import mitb.event.events.CommandEvent;
import mitb.module.CommandModule;
import mitb.module.modules.googsearch.json.GoogleSearchQuery;
import mitb.module.modules.googsearch.json.Result;
import mitb.util.StringHelper;
import org.jsoup.Jsoup;
import org.jsoup.examples.HtmlToPlainText;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Searches google for the first result.
 */
public final class GoogleSearchModule extends CommandModule {

    /**
     * API URL.
     */
    private static final String API_URL = "https://ajax.googleapis.com/ajax/services/search/web?v=1.0&q=";
    /**
     * A cache for previously evaluated expressions.
     */
    private static final Cache<String, String> CACHE = CacheBuilder.newBuilder().maximumSize(100L)
            .expireAfterAccess(10L, TimeUnit.MINUTES).build();
    /**
     * An instance to convert output character entities (and other html elements) to plain ext.
     */
    private static final HtmlToPlainText HTML_TO_PLAIN_TXT = new HtmlToPlainText();

    @Override
    public String[] getCommands() {
        return new String[]{"googlesearch", "g", "gsearch", "gs","googles", "google"};
    }

    @Override
    public void getHelp(CommandEvent event) {
        TitanBot.sendReply(event.getSource(), "Syntax: " + event.getArgs()[0] + " [term #] (query)");
    }

    @Override
    public String getName() {
        return "Google";
    }

    /**
     * Reply with an example response on command.
     *
     * @param commandEvent
     */
    @Override
    public void onCommand(CommandEvent commandEvent) {
        /*if (commandEvent.getArgs().length == 0) {
            return;
        }

        // Checking if we want a custom definition
        //UrbanDictionaryModule.EntryValuePair entryValue = UrbanDictionaryModule.getEntryValues(commandEvent);
        //final int finalEntryNo = entryValue.getEntryNumber();

        // Validating entry number
        //if (finalEntryNo < 0) {
            TitanBot.sendReply(commandEvent.getSource(), "Invalid definition number.");
            return;
        //}

        // Constructing query
        //String[] args = entryValue.isCustomEntry() ? Arrays.copyOfRange(commandEvent.getArgs(), 1, commandEvent
        //        .getArgs().length)
        //        : commandEvent.getArgs();
        //String query = Joiner.on(" ").join(args);

        // Check cache
        String result = GoogleSearchModule.CACHE.getIfPresent(query);

        if (result != null) {
            TitanBot.sendReply(commandEvent.getSource(), result);
            return;
        }

        // Sanitize query
        String sanitizedQuery = StringHelper.urlEncode(query);

        if (sanitizedQuery == null) {
            TitanBot.sendReply(commandEvent.getSource(), "Error encoding query for google search.");
            return;
        }

        // Api call
        String url = GoogleSearchModule.API_URL + sanitizedQuery;
        try (AsyncHttpClient asyncHttpClient = new AsyncHttpClient()) {
            asyncHttpClient.prepareGet(url)
                    .addHeader("Accept", "text/plain")
                    .execute(new AsyncCompletionHandler<Response>() {

                        @Override
                        public Response onCompleted(Response response) throws Exception {
                            // Parsing response
                            String body = response.getResponseBody();
                            GoogleSearchQuery resp = new Gson().fromJson(body, GoogleSearchQuery.class);

                            // Evaluating response
                            List<Result> results = resp.getResponseData().getResults();

                            if (results.size() > finalEntryNo) {
                                // Result
                                String output = GoogleSearchModule.formatResponse(results, finalEntryNo);

                                // Update cache
                                GoogleSearchModule.CACHE.put(query, output);

                                // Send reply
                                TitanBot.sendReply(commandEvent.getSource(), output);
                            } else {
                                String position = entryValue.isCustomEntry() ? (" [at " + (finalEntryNo + 1) + ']') : "";
                                TitanBot.sendReply(commandEvent.getSource(), "There are no entries for: " + query +
                                        position);
                            }
                            return response;
                        }

                        @Override
                        public void onThrowable(Throwable t) {
                            // XXX output error
                        }
                    });
        }*/
    }

    private static String formatResponse(List<Result> results, int entryNo) {
        // Strip out html tags and new lines
        String title = GoogleSearchModule.HTML_TO_PLAIN_TXT.getPlainText(Jsoup.parse(results.get(entryNo).getTitle()));
        String content = StringHelper.stripNewlines(
                GoogleSearchModule.HTML_TO_PLAIN_TXT.getPlainText(Jsoup.parse(results.get(entryNo).getContent())));
        String link = GoogleSearchModule.HTML_TO_PLAIN_TXT.getPlainText(Jsoup.parse(results.get(entryNo).getVisibleUrl()));
        return StringHelper.wrapBold(title) + ": " + content + " [ More at " + link + " ]";
    }

    @Override
    public void register() {

    }
}
