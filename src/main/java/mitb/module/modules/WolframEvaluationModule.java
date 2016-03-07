package mitb.module.modules;

import com.google.common.base.Joiner;
import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import mitb.TitanBot;
import mitb.event.events.CommandEvent;
import mitb.module.CommandModule;
import mitb.util.Properties;
import org.apache.commons.lang3.StringEscapeUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

/**
 * A wolfram alpha expression evaluation module.
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
     * @param event
     */
    @Override
    public void onCommand(CommandEvent event) {
        // TODO cache queries using a FIFO structure, but note some queries give dynamic results (i.e. give me a joke)
        if (event.getArgs().length == 0)
            return;

        // Checking if api key is set
        if (API_KEY.equalsIgnoreCase("NONE")) {
            TitanBot.sendReply(event.getSource(), "API key for wolfram is not configured.");
            return;
        }

        // Constructing query
        String query = Joiner.on(" ").join(event.getArgs());
        String sanitizedQuery;

        try {
            sanitizedQuery = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            TitanBot.sendReply(event.getSource(), "Error encoding query for wolfram.");
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
                            result = StringEscapeUtils.unescapeHtml4(result); // unescaping character entities
                            TitanBot.sendReply(event.getSource(), query + " = " + result);
                        } else {
                            TitanBot.sendReply(event.getSource(), "No result found for: " + query);
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
        XML xml = new XMLDocument(content);

        // Exact result response
        List exactResults = xml.xpath("/queryresult/pod[@title='Exact result']/subpod/plaintext/text()");

        if (exactResults.size() > 0) {
            return exactResults.get(0).toString();
        }

        // Result response
        List results = xml.xpath("/queryresult/pod[@title='Result']/subpod/plaintext/text()");

        if (results.size() > 0) {
            return results.get(0).toString();
        }
        return null; // Fall-back no result found
    }

    @Override
    public void register() {

    }
}
