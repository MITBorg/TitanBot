package mitb;

import com.google.common.util.concurrent.RateLimiter;
import mitb.command.CommandHandler;
import mitb.event.EventHandler;
import mitb.irc.IRCListener;
import mitb.module.Module;
import mitb.module.modules.*;
import mitb.module.modules.urbandict.UrbanDictionaryModule;
import mitb.module.modules.weather.WeatherModule;
import mitb.util.Properties;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.UtilSSLSocketFactory;
import org.pircbotx.cap.TLSCapHandler;
import org.pircbotx.hooks.types.GenericEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSocketFactory;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Main class for TitanBot. Handles the main functionality of the bot.
 */
public class TitanBot {

    public static final Logger LOGGER = LoggerFactory.getLogger(TitanBot.class);
    public static final RateLimiter RATE_LIMITER = RateLimiter.create(Double.parseDouble(Properties.getValue("rate")));
    public static final List<Module> MODULES = new ArrayList<>();
    public static Connection databaseConnection;

    /**
     * Entry point to TitanBot.
     */
    public void run() throws Exception {
        databaseConnection = DriverManager.getConnection("jdbc:sqlite:database.db");

        EventHandler.register(new CommandHandler());
        this.registerModules();
        this.createTables();

        Configuration configuration = new Configuration.Builder()
                .setName(Properties.getValue("bot.nick"))
                .setLogin(Properties.getValue("bot.username"))
                .setVersion(Properties.getValue("bot.version"))
                .setRealName(Properties.getValue("bot.real_name"))
                .setSocketFactory(new UtilSSLSocketFactory().trustAllCertificates())
                .setAutoNickChange(true)
                .setAutoReconnect(true)
                .setAutoSplitMessage(true)
                .setMaxLineLength(512)
                //.addAutoJoinChannel("#mopar")
                .addListener(new IRCListener())
                .addCapHandler(new TLSCapHandler((SSLSocketFactory) SSLSocketFactory.getDefault(), true))
                .addServer(Properties.getValue("irc.server"), 6697)
                .buildConfiguration();

        PircBotX bot = new PircBotX(configuration);
        bot.startBot();
    }

    /**
     * Send a rate-limited reply to the channel.
     *
     * @param event event we should reply to
     * @param reply message we should send.
     */
    public static void sendReply(GenericEvent event, String reply) {
        if (RATE_LIMITER.tryAcquire()) {
            event.respond(reply);
        }
    }

    /**
     * Registers all the modules of the application.
     */
    private void registerModules() {
        MODULES.add(new TestCommandModule());
        MODULES.add(new LastSeenModule());
        MODULES.add(new StatsModule());
        MODULES.add(new UrbanDictionaryModule());
        MODULES.add(new HelpModule());
        MODULES.add(new SedReplacementModule());
        MODULES.add(new WeatherModule());

        LOGGER.info("Registered all modules.");
    }

    /**
     * Create all the sqlite tables we need
     */
    private void createTables() {
        try {
            Statement stmt = databaseConnection.createStatement();
            stmt.execute("CREATE TABLE seen (id INTEGER PRIMARY KEY AUTOINCREMENT, nick VARCHAR(50), login VARCHAR(50), seen DATETIME)");
            stmt.execute("CREATE TABLE weather (id INTEGER PRIMARY KEY AUTOINCREMENT, nick VARCHAR(50), location VARCHAR(50))");
        } catch(Exception ignored) {}
    }
}
