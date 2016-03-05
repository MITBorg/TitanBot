package mitb.module.modules.urbandict;

import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import mitb.TitanBot;
import mitb.event.events.CommandEvent;
import mitb.module.CommandModule;
import mitb.module.modules.urbandict.json.*;
import mitb.util.Properties;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;

/**
 * Queries urban dictionary through a third-party API.
 */
public class UrbanDictionaryModule extends CommandModule {

    /**
     * Bold encapsulation string.
     */
    private static final String BOLD = "\u0002";

    /**
     * The API URL.
     */
    private static final String API_URL = "https://mashape-community-urban-dictionary.p.mashape.com/define?term=";

    /**
     * Our API key.
     */
    private static final String API_KEY = Properties.getValue("urbandict.api_key");

    @Override
    public String[] getCommands() {
        return new String[]{"urbandictionary", "urban", "ub", "ud"};
    }

    @Override
    public void getHelp(CommandEvent event) {
        TitanBot.sendReply(event.getOriginalEvent(), "Syntax: " + event.getArgs()[0] + " [result #] term");
    }

    /**
     * Reply with an example response on command.
     *
     * @param event
     */
    @Override
    public void onCommand(CommandEvent event) {
        if (event.getArgs().length == 0) {
            return;
        }

        // Checking if we are getting a custom entry id
        EntryValuePair entryValue = getEntryValues(event);
        final int finalEntryNo = entryValue.getEntryNumber();

        // Validating entry number
        if (finalEntryNo < 0) {
            TitanBot.sendReply(event.getOriginalEvent(), "Invalid definition number.");
            return;
        }

        // Construct query and sanitize for url
        String url, sanitizedQuery;
        String[] args = entryValue.isCustomEntry() ? Arrays.copyOfRange(event.getArgs(), 1, event.getArgs().length)
                : event.getArgs();
        String query = Joiner.on(" ").join(args);

        try {
            sanitizedQuery = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            TitanBot.sendReply(event.getOriginalEvent(), "Error encoding query for urban dictionary.");
            return;
        }

        // Api call
        url = API_URL + sanitizedQuery;
        AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
        asyncHttpClient.prepareGet(url)
                .addHeader("X-Mashape-Key", API_KEY)
                .addHeader("Accept", "text/plain")
                .execute(new AsyncCompletionHandler<Response>() {

                    @Override
                    public Response onCompleted(Response response) throws Exception {
                        // Parsing response
                        String body = response.getResponseBody();

                        // Evaluating response
                        UrbanDictionaryQuery entry = new Gson().fromJson(body, UrbanDictionaryQuery.class);

                        if (entry.getList().size() > finalEntryNo) {
                            TitanBot.sendReply(event.getOriginalEvent(), formatUrbanDictionaryQuery(entry, finalEntryNo));
                        } else {
                            String position = entryValue.isCustomEntry() ? " [at " + (finalEntryNo + 1) + "]" : "";
                            TitanBot.sendReply(event.getOriginalEvent(), "There are no entries for: " + query + position);
                        }
                        return response;
                    }

                    @Override
                    public void onThrowable(Throwable t) {
                        // XXX output error
                    }
                });
    }

    private EntryValuePair getEntryValues(CommandEvent event) {
        if (event.getArgs().length > 1) {
            try {
                return new EntryValuePair(Integer.parseInt(event.getArgs()[0]) - 1, true);
            } catch (NumberFormatException ignored) {
            }
        }
        return EntryValuePair.DEFAULT; // fall-back response
    }

    @Override
    public void register() {

    }

    /**
     * Formats output for urban dictionary queries.
     * @param q
     * @param entryNo
     * @return
     */
    public String formatUrbanDictionaryQuery(UrbanDictionaryQuery q, int entryNo) {
        List l = q.getList().get(entryNo);
        return String.format("%s: %s [by %s +%d/-%d]",
                wrapBold(l.getWord()), l.getDefinition().replaceAll("\r", "").replaceAll("\n", " "),
                l.getAuthor(), l.getThumbsUp(), l.getThumbsDown());
    }

    /**
     * Wraps some string in bold.
     * @param s
     * @return
     */
    private static String wrapBold(String s) {
        return BOLD + s + BOLD;
    }


    /**
     * An UrbanDictionary entry value pair, used to represent if a custom definition number was requested.
     */
    static final class EntryValuePair {

        public static final EntryValuePair DEFAULT = new EntryValuePair(0, false);

        private int entryNumber;
        private boolean customEntry;

        EntryValuePair(int entryNumber, boolean customEntry) {
            this.entryNumber = entryNumber;
            this.customEntry = customEntry;
        }

        public boolean isCustomEntry() {
            return customEntry;
        }

        public int getEntryNumber() {
            return entryNumber;
        }
    }
}
