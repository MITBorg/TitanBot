package mitb.module.modules.urbandict;

import com.google.common.base.Joiner;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import mitb.TitanBot;
import mitb.event.events.CommandEvent;
import mitb.module.CommandModule;
import mitb.module.modules.urbandict.json.*;
import mitb.util.Properties;
import mitb.util.StringHelper;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Queries urban dictionary through a third-party API.
 */
public final class UrbanDictionaryModule extends CommandModule {

    /**
     * The API URL.
     */
    private static final String API_URL = "https://mashape-community-urban-dictionary.p.mashape.com/define?term=";
    /**
     * Our API key.
     */
    private static final String API_KEY = Properties.getValue("urbandict.api_key");
    /**
     * A cache for previously evaluated expressions.
     */
    private static final Cache<String, String> CACHE = CacheBuilder.newBuilder().maximumSize(100L)
            .expireAfterAccess(10L, TimeUnit.MINUTES).build();

    @Override
    public String[] getCommands() {
        return new String[]{"urbandictionary", "urban", "ub", "ud"};
    }

    @Override
    public void getHelp(CommandEvent event) {
        TitanBot.sendReply(event.getSource(), "Syntax: " + event.getArgs()[0] + " [result #] (query)");
    }

    @Override
    public void onCommand(CommandEvent commandEvent) {
        if (commandEvent.getArgs().length == 0) {
            return;
        }

        // Checking if api key is set
        if (UrbanDictionaryModule.API_KEY.equalsIgnoreCase("NONE")) {
            TitanBot.sendReply(commandEvent.getSource(), "API key for wolfram is not configured.");
            return;
        }

        // Checking if we are getting a custom entry id
        UrbanDictionaryModule.EntryValuePair entryValue = UrbanDictionaryModule.getEntryValues(commandEvent);
        final int finalEntryNo = entryValue.getEntryNumber();

        // Validating entry number
        if (finalEntryNo < 0) {
            TitanBot.sendReply(commandEvent.getSource(), "Invalid definition number.");
            return;
        }

        // Construct query
        String[] args = entryValue.isCustomEntry() ? Arrays.copyOfRange(commandEvent.getArgs(), 1, commandEvent.getArgs().length)
                : commandEvent.getArgs();
        String query = Joiner.on(" ").join(args);

        // Check cache
        String result = UrbanDictionaryModule.CACHE.getIfPresent(query);

        if (result != null) {
            TitanBot.sendReply(commandEvent.getSource(), result);
            return;
        }

        // Sanitize query
        String sanitizedQuery = StringHelper.urlEncode(query);

        if (sanitizedQuery == null) {
            TitanBot.sendReply(commandEvent.getSource(), "Error encoding query for urban dictionary.");
            return;
        }

        // Api call
        String url = UrbanDictionaryModule.API_URL + sanitizedQuery;
        try (AsyncHttpClient asyncHttpClient = new AsyncHttpClient()) {
            asyncHttpClient.prepareGet(url)
                    .addHeader("X-Mashape-Key", UrbanDictionaryModule.API_KEY)
                    .addHeader("Accept", "text/plain")
                    .execute(new AsyncCompletionHandler<Response>() {

                        @Override
                        public Response onCompleted(Response response) throws IOException, JsonSyntaxException {
                            // Parsing response
                            String body = response.getResponseBody();
                            UrbanDictionaryQuery entry = new Gson().fromJson(body, UrbanDictionaryQuery.class);

                            // Evaluating response
                            if (entry.getList().size() > finalEntryNo) {
                                String output = UrbanDictionaryModule.formatUrbanDictionaryQuery(entry, finalEntryNo);

                                // Update cache
                                UrbanDictionaryModule.CACHE.put(query, output);

                                // Send reply
                                TitanBot.sendReply(commandEvent.getSource(), output);
                            } else {
                                String position = entryValue.isCustomEntry() ? (" [at " + (finalEntryNo + 1) + ']') : "";
                                TitanBot.sendReply(commandEvent.getSource(), "There are no entries for: " + query + position);
                            }
                            return response;
                        }

                        @Override
                        public void onThrowable(Throwable t) {
                            // XXX output error
                        }
                    });
        }
    }

    /**
     * Parses an {@link UrbanDictionaryModule.EntryValuePair} from the {@link CommandEvent} arguments.
     *
     * @param event event triggering this method call
     * @return entry value
     */
    public static UrbanDictionaryModule.EntryValuePair getEntryValues(CommandEvent event) {
        if (event.getArgs().length > 1) {
            try {
                return new UrbanDictionaryModule.EntryValuePair(Integer.parseInt(event.getArgs()[0]) - 1, true);
            } catch (NumberFormatException ignored) {
            }
        }
        return UrbanDictionaryModule.EntryValuePair.DEFAULT; // fall-back response
    }

    @Override
    public void register() {

    }

    /**
     * Formats output for urban dictionary queries.
     * 
     * @param q query
     * @param entryNo entry number
     * @return formatted response
     */
    private static String formatUrbanDictionaryQuery(UrbanDictionaryQuery q, int entryNo) {
        List l = q.getList().get(entryNo);

        // Truncate and format definition length
        String def = l.getDefinition();

        if (def.length() > 200) {
            def = def.substring(0, 200 - 3) + "...";
        }
        def = StringHelper.stripNewlines(def) + " [ more at " + l.getPermalink() + " ]";

        // Return formatted output
        return String.format("%s: %s [by %s +%d/-%d]",
                StringHelper.wrapBold(l.getWord()), def,
                l.getAuthor(), l.getThumbsUp(), l.getThumbsDown());
    }


    /**
     * An entry value pair, used to represent if a custom definition number was requested.
     */
    public static final class EntryValuePair {

        public static final UrbanDictionaryModule.EntryValuePair DEFAULT = new UrbanDictionaryModule.EntryValuePair(0, false);

        private final int entryNumber;
        private final boolean customEntry;

        EntryValuePair(int entryNumber, boolean customEntry) {
            this.entryNumber = entryNumber;
            this.customEntry = customEntry;
        }

        public boolean isCustomEntry() {
            return this.customEntry;
        }

        public int getEntryNumber() {
            return this.entryNumber;
        }
    }
}
