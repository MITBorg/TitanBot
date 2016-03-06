package mitb.module.modules;

import com.google.common.base.Joiner;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import mitb.TitanBot;
import mitb.event.events.CommandEvent;
import mitb.module.CommandModule;
import mitb.util.Properties;
import mitb.util.StringHelper;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A wolfram alpha expression evaluation module.
 */
public class WolframEvaluationModule extends CommandModule {

    /**
     * API url.
     */
    private static final String API_URL = "http://api.wolframalpha.com/v2/query?input=";
    /**
     * API key for wolfram.
     */
    private static final String API_KEY = Properties.getValue("wolfram.api_key");
    /**
     * Finds the result pod.
     */
    private static final Pattern RESULT_POD_PATTERN = Pattern.compile("<pod title='Result'(.*?)<\\/pod>");
    /**
     * Finds the plain text pattern.
     */
    private static final Pattern PLAIN_TEXT_PATTERN = Pattern.compile("<plaintext>(.*?)<\\/plaintext>");

    @Override
    public String[] getCommands() {
        return new String[]{"wolfram", "wolframalpha", "wra"};
    }

    @Override
    public void getHelp(CommandEvent event) {
        TitanBot.sendReply(event.getOriginalEvent(), "Syntax: wolfram [expression]");
    }

    /**
     * Reply with an example response on command.
     *
     * @param event
     */
    @Override
    public void onCommand(CommandEvent event) {
        // TODO cache queries using a FIFO structure
        if (event.getArgs().length == 0)
            return;

        // Checking if api key is set
        if (API_KEY.equalsIgnoreCase("NONE")) {
            TitanBot.sendReply(event.getOriginalEvent(), "API key for wolfram is not configured.");
            return;
        }

        // Constructing query
        String query = Joiner.on(" ").join(event.getArgs());
        String sanitizedQuery;

        try {
            sanitizedQuery = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            TitanBot.sendReply(event.getOriginalEvent(), "Error encoding query for wolfram.");
            return;
        }

        // Api call
        String url = API_URL + sanitizedQuery + "&appid=" + API_KEY;
        AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
        asyncHttpClient.prepareGet(url)
                .addHeader("Accept", "text/plain")
                .execute(new AsyncCompletionHandler<Response>() {

                    @Override
                    public Response onCompleted(Response response) throws Exception {
                        // Parsing response
                        String body = response.getResponseBody();
                        String result = parseResult(body);

                        // Outputting response
                        if (result != null) {
                            TitanBot.sendReply(event.getOriginalEvent(), query + " = " + result);
                        } else {
                            TitanBot.sendReply(event.getOriginalEvent(), "No result found for: " + query);
                        }
                        return response;
                    }

                    @Override
                    public void onThrowable(Throwable t) {
                        // XXX output error
                    }
                });
    }

    /**
     * Parses the result out of the given wolfram alpha api response.
     * @param content
     * @return Result (in plaintext) or null if not found.
     */
    private String parseResult(String content) {
        // TODO use proper xml parsing OR optimise
        content = StringHelper.stripNewlines(content).replaceAll("   ", " ");

        // Step 2. regex out the region of interest
        Matcher resultMatcher = RESULT_POD_PATTERN.matcher(content);

        if (resultMatcher.find()) {
            String resultPod = resultMatcher.group(0);

            // Now extract answer
            Matcher plaintextMatcher = PLAIN_TEXT_PATTERN.matcher(resultPod);
            return plaintextMatcher.find() ? plaintextMatcher.group(0).substring(11, plaintextMatcher.group(0).length() - 12) : null;
        }
        return null;
    }

    @Override
    public void register() {

    }
}
