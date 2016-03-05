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
import mitb.util.MathHelper;
import org.pircbotx.hooks.events.MessageEvent;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
     * Degrees symbol.
     */
    private static final String DEGREES = "°";
    /**
     * Fahrenheit symbol.
     */
    private static final String FAHRENHEIT_SYMBOL = DEGREES + "F";
    /**
     * Celsius symbol.
     */
    private static final String CELSIUS_SYMBOL = DEGREES + "C";

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
        // TODO cache results to speed up repeat queries
        boolean useCachedLocation = false;
        String nick = ((MessageEvent)event.getOriginalEvent()).getUser().getNick();

        // Checking if we should use a cached location for the user
        if (event.getArgs().length == 0) {
            useCachedLocation = true;
        }

        // Construct query and url
        String location = useCachedLocation ? getCachedLocation(nick) : Joiner.on(" ").join(event.getArgs());
        String sanitizedLocation;

        // Ensure cached location was found
        if (location == null) {
            TitanBot.sendReply(event.getOriginalEvent(), "There is no cached location for your nickname.");
            return;
        }

        try {
            sanitizedLocation = URLEncoder.encode(location, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            TitanBot.sendReply(event.getOriginalEvent(), "Error encoding query for weather.");
            return;
        }
        String url = API_URL_PART_1 + sanitizedLocation + API_URL_PART_2; // XXX clean up this garbage

        // Api call
        final boolean finalUseCachedLocation = useCachedLocation;

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

                            // Update database if necessary
                            if (!finalUseCachedLocation) {
                                updateCachedLocation(nick, location);
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

    @Override
    public void register() {

    }

    /**
     * Updates the cached location for the given nickname.
     * @param nick
     * @param location
     */
    private void updateCachedLocation(String nick, String location) {
        try {
            PreparedStatement statement = TitanBot.databaseConnection.prepareStatement(
                    "INSERT OR REPLACE INTO weather (id, nick, location) VALUES ((SELECT id FROM weather WHERE nick = ?), ?, ?)"
            );
            statement.setString(1, nick);
            statement.setString(2, nick);
            statement.setString(3, location);
            statement.executeUpdate();
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets a user's cached location from the database.
     * @return Cached location or null if not found.
     */
    public String getCachedLocation(String nick) {
        try {
            PreparedStatement statement = TitanBot.databaseConnection.prepareStatement(
                    "SELECT location FROM weather WHERE nick = ?"
            );
            statement.setString(1, nick);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.getString("location");
        } catch (SQLException e) {
            return null;
        }
    }

    /**
     * Formats the given query for output.
     * @param query
     * @return
     */
    private String formatWeatherQuery(Query query) {
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

            // Calculate temperatures
            String highF = forecast.getHigh() + FAHRENHEIT_SYMBOL;
            String lowF = forecast.getLow() + FAHRENHEIT_SYMBOL;
            String highC = MathHelper.fahrenheitToCelsius(Integer.parseInt(forecast.getHigh())) + CELSIUS_SYMBOL;
            String lowC = MathHelper.fahrenheitToCelsius(Integer.parseInt(forecast.getLow())) + CELSIUS_SYMBOL;

            // Append data
            sb.append(forecast.getDay()).append(": ")
                    .append("High is ").append(wrapBold(highF + "/" + highC))
                    .append(" Low is ").append(wrapBold(lowF + "/" + lowC))
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
