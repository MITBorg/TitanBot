package mitb.module.modules;

import com.google.common.base.Joiner;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import mitb.TitanBot;
import mitb.event.events.CommandEvent;
import mitb.module.CommandModule;
import mitb.util.Properties;
import mitb.util.StringHelper;
import org.apache.commons.lang3.StringEscapeUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A wolfram alpha expression evaluation module with caching. Note this caching causes issue for a small number of
 * queries, which respond with dynamic results.
 */
public final class WolframEvaluationModule extends CommandModule {

    /**
     * API url.
     */
    private static final String API_URL = "http://api.wolframalpha.com/v2/query?input=";
    /**
     * API key for wolfram.
     */
    private static final String API_KEY = Properties.getValue("wolfram.api_key");
    /**
     * A cache for previously evaluated expressions.
     */
    private static final Cache<String, String> CACHE = CacheBuilder.newBuilder().maximumSize(100)
            .expireAfterAccess(10, TimeUnit.MINUTES).build();
    /**
     * XPath matching for results in API output generated from RESULT_POD_TITLES.
     */
    private static String XPATH_RESULT_TITLES;
    /**
     * A list of matched result pod titles.
     */
    private static final String[] RESULT_POD_TITLES = {"Result", "Exact result", "Limit", "Derivative", "Indefinite integral", "Definite integral"};

    static {
        WolframEvaluationModule.generateXPathCaptureGroup();
    }

    /**
     * Generates results for x-path capturing.
     */
    private static void generateXPathCaptureGroup() {
        WolframEvaluationModule.XPATH_RESULT_TITLES = "";

        for (int i = 0; i < WolframEvaluationModule.RESULT_POD_TITLES.length; i++) {
            String title = WolframEvaluationModule.RESULT_POD_TITLES[i];
            WolframEvaluationModule.XPATH_RESULT_TITLES += "@title='" + title + '\'';

            if ((i + 1) < WolframEvaluationModule.RESULT_POD_TITLES.length)
                WolframEvaluationModule.XPATH_RESULT_TITLES += " or ";
        }
    }

    @Override
    public String[] getCommands() {
        return new String[]{"wolframalpha", "wolfram", "wra", "wr"};
    }

    @Override
    public void getHelp(CommandEvent event) {
        TitanBot.sendReply(event.getSource(), "Syntax: " + event.getArgs()[0] + " (query)");
    }

    /**
     * Reply with an example response on command.
     *
     * @param commandEvent
     */
    @Override
    public void onCommand(CommandEvent commandEvent) {
        if (commandEvent.getArgs().length == 0) {
            return;
        }

        // Checking if api key is set
        if (WolframEvaluationModule.API_KEY.equalsIgnoreCase("NONE")) {
            TitanBot.sendReply(commandEvent.getSource(), "API key for wolfram is not configured.");
            return;
        }

        // Constructing query
        String query = Joiner.on(" ").join(commandEvent.getArgs());

        // Check cache
        String result = WolframEvaluationModule.CACHE.getIfPresent(query);

        if (result != null) {
            TitanBot.sendReply(commandEvent.getSource(), result);
            return;
        }

        // Continue constructing query
        String sanitizedQuery = StringHelper.urlEncode(query);

        if (sanitizedQuery == null) {
            TitanBot.sendReply(commandEvent.getSource(), "Error encoding query for wolfram.");
            return;
        }

        // Api call
        String url = WolframEvaluationModule.API_URL + sanitizedQuery + "&appid=" + WolframEvaluationModule.API_KEY;
        try (AsyncHttpClient asyncHttpClient = new AsyncHttpClient()) {
            asyncHttpClient.prepareGet(url)
                    .addHeader("Accept", "text/plain")
                    .execute(new AsyncCompletionHandler<Response>() {

                        @Override
                        public Response onCompleted(Response response) throws Exception {
                            // Parsing response
                            String body = response.getResponseBody();
                            String result = WolframEvaluationModule.parseResult(body);

                            // Outputting response
                            if (result != null) {
                                result = StringEscapeUtils.unescapeHtml4(result); // unescaping character entities
                                String output = query + " = " + result;

                                // Updating cache
                                WolframEvaluationModule.CACHE.put(query, output);

                                // Send response
                                TitanBot.sendReply(commandEvent.getSource(), output);
                            } else {
                                TitanBot.sendReply(commandEvent.getSource(), "No result found for: " + query);
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
     * Parses the result out of the given wolfram alpha api response.
     *
     * @param content
     * @return Result (in plaintext) or null if not found.
     */
    private static String parseResult(String content) {
        XML xml = new XMLDocument(content);

        // Global response capturing
        List results = xml.xpath("/queryresult[@success='true']/pod[" + WolframEvaluationModule.XPATH_RESULT_TITLES + "]/subpod/plaintext/text()");
        return (results.size() >= 0) ? results.get(0).toString() : null; // Fall-back no result found
    }

    @Override
    public void register() {

    }
}
