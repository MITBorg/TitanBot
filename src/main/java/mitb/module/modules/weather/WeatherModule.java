package mitb.module.modules.weather;

import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import mitb.TitanBot;
import mitb.event.events.CommandEvent;
import mitb.module.CommandModule;
import mitb.module.modules.weather.json.*;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

/**
 * A weather lookup module through the Yahoo Weather API.
 */
public class WeatherModule extends CommandModule {

    /**
     * Bold encapsulation string.
     */
    private static final String BOLD = "\u0002";
    /**
     * Degrees symbol;
     */
    private static final String DEGREES = "°";

    private static final String API_URL_PART_1 = "https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20weather.forecast%20where%20woeid%20in%20(select%20woeid%20from%20geo.places(1)%20where%20text%3D%22";
    private static final String API_URL_PART_2 = "%22)&format=json&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys";

    @Override
    public String[] getCommands() {
        return new String[]{"weather"};
    }

    @Override
    public void getHelp(CommandEvent event) {
        TitanBot.sendReply(event.getOriginalEvent(), "Syntax: " + event.getArgs()[0] + " [location]");
    }

    /**
     * Reply with an example response on command.
     *
     * @param event
     */
    @Override
    public void onCommand(CommandEvent event) {
        // TODO record users location for future use (into db)
        // Construct query and url
        String location = Joiner.on(" ").join(event.getArgs());
        String sanitizedLocation;

        try {
            sanitizedLocation = URLEncoder.encode(location, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            TitanBot.sendReply(event.getOriginalEvent(), "Error encoding query for weather.");
            return;
        }
        String url = API_URL_PART_1 + sanitizedLocation + API_URL_PART_2; // XXX clean up this garbage

        // Api call
        AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
        asyncHttpClient.prepareGet(url)
                .addHeader("Accept", "text/plain")
                .execute(new AsyncCompletionHandler<Response>() {

                    @Override
                    public Response onCompleted(Response response) throws Exception {
                        // Parsing response
                        String body = response.getResponseBody();
                        WeatherQuery resp = new Gson().fromJson(body, WeatherQuery.class);

                        // Evaluating response
                        if (resp.getQuery().getResults() == null) {
                            TitanBot.sendReply(event.getOriginalEvent(), "There is no data for location: " + location);
                        } else {
                            TitanBot.sendReply(event.getOriginalEvent(), formatWeatherQuery(resp.getQuery()));
                        }
                        return response;
                    }

                    @Override
                    public void onThrowable(Throwable t) {
                        // Something wrong happened.
                    }
                });
    }

    @Override
    public void register() {

    }

    /**
     * Formats the given query for output.
     * @param query
     * @return
     */
    private String formatWeatherQuery(Query query) {
        // TODO convert units to users local system, between imperial and metric
        StringBuilder sb = new StringBuilder();
        Channel c = query.getResults().getChannel();

        // Units
        Units u = c.getUnits();

        // Location
        Location l = c.getLocation();
        sb.append(wrapBold(l.getCity() + ", " + l.getCountry())).append(": ");

        // Wind
        Wind w = c.getWind();
        sb.append("Wind: Chill is ").append(w.getChill()).append(u.getSpeed())
                .append(", Direction is ").append(w.getDirection()).append(DEGREES)
                .append(", Speed is ").append(w.getSpeed())
                .append(u.getSpeed()).append("; ");

        // Atmosphere
        Atmosphere a = c.getAtmosphere();
        sb.append("Atmosphere: Humidity is ").append(a.getHumidity()).append("%")
                .append(", Pressure is ").append(a.getPressure()).append(u.getPressure())
                .append(", Visibility is ").append(a.getVisibility())
                .append(u.getDistance()).append("; ");

        // Astronomy
        Astronomy astronomy = c.getAstronomy();
        sb.append("Astronomy: Sunrise is at ").append(astronomy.getSunrise())
                .append(" and Sunset is at ").append(astronomy.getSunset())
                .append("; ");

        // Forecast
        sb.append("Forecast: ");
        List<Forecast> f = c.getItem().getForecast();

        for (int i = 0; i < f.size(); i++) {
            Forecast forecast = f.get(i);
            sb.append(forecast.getDay()).append(": ")
                    .append("High is ").append(wrapBold(forecast.getHigh() + u.getTemperature()))
                    .append(" Low is ").append(wrapBold(forecast.getLow() + u.getTemperature()))
                    .append(" Conditions are ").append(wrapBold(forecast.getText()));

            // Not adding a comma at the end
            if (i + 1 < f.size()) {
                sb.append(", ");
            }
        }
        return sb.append(".").toString();
    }

    /**
     * Wraps some string in bold.
     * @param s
     * @return
     */
    private static String wrapBold(String s) {
        return BOLD + s + BOLD;
    }
}
