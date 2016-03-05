package mitb.module.modules;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonValue;
import com.google.common.base.Joiner;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import mitb.TitanBot;
import mitb.event.events.CommandEvent;
import mitb.module.CommandModule;

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
    private static final String API_KEY = "rBWb4Cg1Gdmsh3ykEYXwSbSeq9ZZp18MvbUjsn1leuWzeRVaxE";

    @Override
    public String[] getCommands() {
        return new String[]{"urban", "urbandictionary", "ub"};
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
        String url, query;
        String[] args = entryValue.isCustomEntry() ? Arrays.copyOfRange(event.getArgs(), 1, event.getArgs().length)
                : event.getArgs();
        String arg = Joiner.on(" ").join(args);

        try {
            query = URLEncoder.encode(arg, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            TitanBot.sendReply(event.getOriginalEvent(), "Error encoding query.");
            return;
        }

        // Api call
        url = API_URL + query;
        AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
        asyncHttpClient.prepareGet(url)
                .addHeader("X-Mashape-Key", API_KEY)
                .addHeader("Accept", "text/plain")
                .execute(new AsyncCompletionHandler<Response>() {

                    @Override
                    public Response onCompleted(Response response) throws Exception {
                        // Parsing response
                        String body = response.getResponseBody();
                        JsonArray items = Json.parse(body).asObject().get("list").asArray();

                        // Evaluating response
                        if (items.size() > finalEntryNo) {
                            UrbanDictionaryEntry entry = UrbanDictionaryEntry.getEntry(items.get(finalEntryNo));
                            TitanBot.sendReply(event.getOriginalEvent(), entry.toString());
                        } else {
                            TitanBot.sendReply(event.getOriginalEvent(), "There are no entries for: " + query + " at "
                                    + finalEntryNo);
                        }
                        return response;
                    }

                    @Override
                    public void onThrowable(Throwable t) {
                        // Something wrong happened.
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

    /**
     * An UrbanDictionary entry.
     */
    static final class UrbanDictionaryEntry {

        private String definition;
        private String author;
        private String word;
        private String example;
        private int thumbsUp;
        private int thumbsDown;

        private UrbanDictionaryEntry(String definition, String author, String word, String example, int thumbsUp,
                                     int thumbsDown) {
            this.definition = definition;
            this.author = author;
            this.word = word;
            this.example = example;
            this.thumbsUp = thumbsUp;
            this.thumbsDown = thumbsDown;
        }

        public static UrbanDictionaryEntry getEntry(JsonValue item) {
            String def = item.asObject().getString("definition", "Unknown Description");
            String link = item.asObject().getString("permalink", "Unknown Link");
            int thumbsUp = item.asObject().getInt("thumbs_up", 0);
            String author = item.asObject().getString("author", "Unknown Author");
            String word = item.asObject().getString("word", "Unknown Word");
            int defId = item.asObject().getInt("defid", -1);
            String currentVote = item.asObject().getString("current_vote", "Unknown Current Vote");
            String example = item.asObject().getString("example", "Unknown Example");
            int thumbsDown = item.asObject().getInt("thumbs_down", 0);
            return new UrbanDictionaryEntry(def, author, word, example, thumbsUp, thumbsDown);
        }

        public String getDefinition() {
            return definition.replaceAll("\r\n", " ");
        }

        public String getAuthor() {
            return author;
        }

        public String getWord() {
            return BOLD + word + BOLD;
        }

        public String getExample() {
            return example;
        }

        public int getThumbsUp() {
            return thumbsUp;
        }

        public int getThumbsDown() {
            return thumbsDown;
        }

        @Override
        public String toString() {
            return String.format("%s: %s [by %s +%d/-%d]",
                    getWord(), getDefinition(), getAuthor(), getThumbsUp(), getThumbsDown());
        }
    }
}
