package mitb.module.modules.weather;

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
import mitb.module.modules.weather.json.*;
import mitb.util.MathHelper;
import mitb.util.Properties;
import mitb.util.StringHelper;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * A weather lookup module through the Yahoo Weather API.
 */
public final class WeatherModule extends CommandModule {

    /**
     * The API URL.
     */
    private static final String API_URL= "http://api.openweathermap.org/data/2.5/weather?q=";
    /**
     * API key for weather.
     */
    private static final String API_KEY = Properties.getValue("weather.api_key");
    /**
     * Date time formatting for sunrise/sunset times.
     */
    private static DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
    /**
     * A cache for previously evaluated expressions.
     */
    private static final Cache<String, String> CACHE = CacheBuilder.newBuilder().maximumSize(100)
            .expireAfterAccess(10, TimeUnit.MINUTES).build();

    @Override
    public String[] getCommands() {
        return new String[]{"weather"};
    }

    @Override
    public void getHelp(CommandEvent event) {
        TitanBot.sendReply(event.getSource(), "Syntax: " + event.getArgs()[0] + " (location)");
    }

    /**
     * Reply with an example response on command.
     *
     * @param event
     */
    @Override
    public void onCommand(CommandEvent event) {
        // Checking if api key is set
        if (API_KEY.equalsIgnoreCase("NONE")) {
            TitanBot.sendReply(event.getSource(), "API key for weather is not configured.");
            return;
        }

        // TODO cache results to speed up repeat queries
        boolean useCachedLocation = false;
        String callerNick;

        if (event.getSource() instanceof PrivateMessageEvent) {
            callerNick = ((PrivateMessageEvent)event.getSource()).getUser().getNick().toLowerCase();
        } else if (event.getSource() instanceof MessageEvent) {
            callerNick = ((MessageEvent)event.getSource()).getUser().getNick().toLowerCase();
        } else {
            return; // invalid event source type
        }

        // Checking if we should use a cached location for the user
        if (event.getArgs().length == 0) {
            useCachedLocation = true;
        }

        // Construct query and url
        String location = useCachedLocation ? fetchCachedLocation(callerNick) : Joiner.on(" ").join(event.getArgs());
        String sanitizedLocation;

        // Ensure cached location was found
        if (location == null) {
            TitanBot.sendReply(event.getSource(), "There is no cached location for your nickname.");
            return;
        }

        // Check cache
        String result = CACHE.getIfPresent(location);

        if (result != null) {
            TitanBot.sendReply(event.getSource(), result);
            return;
        }

        // Sanitize query
        try {
            sanitizedLocation = URLEncoder.encode(location, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            TitanBot.sendReply(event.getSource(), "Error encoding query for weather.");
            return;
        }

        // Api call
        String url = API_URL + sanitizedLocation + "&appid=" + API_KEY;
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
                        if (resp == null || resp.getCod() != 200) {
                            TitanBot.sendReply(event.getSource(), "There is no data for location: " + location);
                        } else {
                            String output = formatWeatherQuery(resp);

                            // Update cache
                            CACHE.put(location, output);

                            // Send reply
                            TitanBot.sendReply(event.getSource(), output);

                            // Update database if necessary
                            if (!finalUseCachedLocation) {
                                updateCachedLocation(callerNick, location);
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
     * Updates the cached location for the given nickname in the dabase.
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
    private String fetchCachedLocation(String nick) {
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
     * Formats the given query for pretty output.
     * @param query
     * @return
     */
    private String formatWeatherQuery(WeatherQuery query) {
        StringBuilder sb = new StringBuilder();

        // Check api output size
        if (query.getWeather().size() == 0) {
            return "Erroneous API output detected.";
        }

        Weather w = query.getWeather().get(0);

        // Location
        sb.append(StringHelper.wrapBold(query.getName() + ", " + query.getSys().getCountry())).append(": ");

        // Current temperature
        double tempK = query.getMain().getTemp();
        int tempC = (int)MathHelper.kelvinsToCelsius(tempK);
        int tempF = (int)MathHelper.kelvinsToFahrenheit(tempK);
        sb.append("Current Temperature: ").append(StringHelper.wrapBold(tempC + StringHelper.CELSIUS_SYMBOL))
                .append("/").append(StringHelper.wrapBold(tempF + StringHelper.FAHRENHEIT_SYMBOL))
                .append(", Description: ").append(StringHelper.wrapBold(w.getDescription())).append(" | ");

        // Weather conditions
        sb.append("Conditions: Wind Speed: ").append(StringHelper.wrapBold(query.getWind().getSpeed() + "m/s"))
                .append(", Humidity: ").append(StringHelper.wrapBold(query.getMain().getHumidity() + "%"))
                .append(" | ");

        // Sunrise/Sunset
        Date sunriseTime = new Date(query.getSys().getSunrise() * 1000);
        Date sunsetTime = new Date(query.getSys().getSunset() * 1000);

        sb.append("Sunrise: ").append(StringHelper.wrapBold(dateFormat.format(sunriseTime) + " GMT"))
                .append(", Sunset: ").append(StringHelper.wrapBold(dateFormat.format(sunsetTime) + " GMT"));
        return sb.toString();
    }
}
