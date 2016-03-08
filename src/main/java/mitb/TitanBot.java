package mitb;

import com.google.common.util.concurrent.RateLimiter;
import mitb.command.CommandHandler;
import mitb.event.EventHandler;
import mitb.irc.IRCListener;
import mitb.module.Module;
import mitb.module.modules.*;
import mitb.module.modules.googsearch.GoogleSearchModule;
import mitb.module.modules.urbandict.UrbanDictionaryModule;
import mitb.module.modules.weather.WeatherModule;
import mitb.module.modules.youtube.YoutubeLookupModule;
import mitb.util.Properties;
import mitb.util.StringHelper;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.UtilSSLSocketFactory;
import org.pircbotx.cap.TLSCapHandler;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.WhoisEvent;
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
public final class TitanBot {

    public static final Logger LOGGER = LoggerFactory.getLogger(TitanBot.class);
    private static final RateLimiter RATE_LIMITER = RateLimiter.create(Properties.getValueAsDouble("rate"));
    public static final List<Module> MODULES = new ArrayList<>();
    public static Connection databaseConnection;

    /**
     * Entry point to TitanBot.
     */
    public void run() throws Exception {
        databaseConnection = DriverManager.getConnection("jdbc:sqlite:database.db");

        StringHelper.loadWordList(Properties.getValue("games.wordlist"));
        EventHandler.register(new CommandHandler());
        this.registerModules();
        this.createTables();

        Configuration configuration = generateConfiguration();

        // Now start the bot
        PircBotX bot = new PircBotX(configuration);
        bot.startBot();
    }

    /**
     * Generates bot {@link Configuration}.
     * @return This bots configuration based on its {@link Properties}.
     */
    private Configuration generateConfiguration() {
        // Building configuration
        Configuration.Builder configBuilder = new Configuration.Builder()
                .setName(Properties.getValue("bot.nick"))
                .setLogin(Properties.getValue("bot.username"))
                .setVersion(Properties.getValue("bot.version"))
                .setRealName(Properties.getValue("bot.real_name"))
                .setSocketFactory(new UtilSSLSocketFactory().trustAllCertificates())
                .setAutoNickChange(true)
                .setAutoReconnect(true)
                .setAutoSplitMessage(false)
                .addListener(new IRCListener())
                .addCapHandler(new TLSCapHandler((SSLSocketFactory) SSLSocketFactory.getDefault(), true))
                .addServer(Properties.getValue("irc.server"), Properties.getValueAsInt("irc.port"));

        // Conditional parameters
        String pass = Properties.getValue("bot.password");

        if (!pass.equalsIgnoreCase("NONE")) {
            configBuilder.setNickservPassword(pass);
        }

        String autojoinChannel = Properties.getValue("irc.autojoin_channel");

        if (!autojoinChannel.equalsIgnoreCase("NONE")) {
            configBuilder.addAutoJoinChannel(autojoinChannel);
        }

        // Now build and return the configuration
        return configBuilder.buildConfiguration();
    }

    /**
     * Send a rate-limited reply to the channel
     *
     * @param event event we should reply to
     * @param reply message we should send.
     */
    public static void sendReply(GenericEvent event, String reply) {
        TitanBot.sendReply(event, reply, "");
    }

    /**
     * Send a rate-limited reply to the channel with a suffix if it is truncated to 3 lines.
     *
     * @param event event we should reply to
     * @param reply message we should send.
     */
    public static void sendReply(GenericEvent event, String reply, String truncatedText) {
        if (RATE_LIMITER.tryAcquire()) {
            if (event instanceof MessageEvent) {
                MessageEvent messageEvent = (MessageEvent) event;

                String prefix = "PRIVMSG " + messageEvent.getChannelSource() + " :" + messageEvent.getUser().getNick() + ": ";
                String suffix = "\r\n";

                int wrapLength = 450 - suffix.length() - prefix.length();

                int offset = 0;
                List<String> resultBuilder = new ArrayList<>();

                while ((reply.length() - offset) > wrapLength) {
                    if (reply.charAt(offset) == ' ') {
                        offset++;
                        continue;
                    }

                    int spaceToWrapAt = reply.lastIndexOf(' ', wrapLength + offset);
                    // if the next string with length maxLength doesn't contain ' '
                    if (spaceToWrapAt < offset) {
                        spaceToWrapAt = reply.indexOf(' ', wrapLength + offset);

                        // if no more ' '
                        if (spaceToWrapAt < 0) {
                            break;
                        }
                    }
                    resultBuilder.add(reply.substring(offset, spaceToWrapAt));
                    offset = spaceToWrapAt + 1;
                }

                resultBuilder.add(reply.substring(offset));

                if (resultBuilder.size() > 2) {
                    if (resultBuilder.size() > 3) {
                        resultBuilder.set(2, resultBuilder.get(2).substring(0, resultBuilder.get(2).length() - truncatedText.length()) + truncatedText);
                    }
                    resultBuilder = resultBuilder.subList(0, 3);
                }
                resultBuilder.forEach((s) -> event.getBot().sendRaw().rawLine(prefix + s));
            } else {
                event.respond(reply);
            }
        }
    }

    /**
     * Registers all the modules of the application.
     */
    private void registerModules() {
        // MODULES.add(new TestCommandModule()); // test module - demonstrates how to add your own
        MODULES.add(new LastSeenModule());
        MODULES.add(new StatsModule());
        MODULES.add(new UrbanDictionaryModule());
        MODULES.add(new HelpModule());
        MODULES.add(new SedReplacementModule());
        MODULES.add(new WeatherModule());
        MODULES.add(new MemoModule());
        MODULES.add(new FlameBotModule());
        MODULES.add(new GoogleSearchModule());
        MODULES.add(new QuotesModule());
        MODULES.add(new WolframEvaluationModule());
        MODULES.add(new AzGameModule());
        MODULES.add(new RepoModule());
        MODULES.add(new HangmanGameModule());
        MODULES.add(new YoutubeLookupModule());

        LOGGER.info("Registered all modules (count=" + MODULES.size() + ").");
    }

    /**
     * Create all the SQLite tables we need
     */
    private void createTables() {
        try {
            Statement stmt = databaseConnection.createStatement();
            stmt.execute("CREATE TABLE seen (id INTEGER PRIMARY KEY AUTOINCREMENT, nick VARCHAR(50), login VARCHAR(50), seen DATETIME)");
            stmt.execute("CREATE TABLE weather (id INTEGER PRIMARY KEY AUTOINCREMENT, nick VARCHAR(50), location VARCHAR(50))");
            stmt.execute("CREATE TABLE memo (id INTEGER PRIMARY KEY AUTOINCREMENT, target_nick VARCHAR(50), sender_nick VARCHAR(50), message VARCHAR(250))");
            stmt.execute("CREATE TABLE quotes (id INTEGER PRIMARY KEY AUTOINCREMENT, creator_nick VARCHAR(50), quote VARCHAR(350))");
        } catch(Exception ignored) {}
    }
}
