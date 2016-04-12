package mitb;

import com.google.common.util.concurrent.RateLimiter;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import mitb.command.CommandHandler;
import mitb.event.EventHandler;
import mitb.irc.IRCListener;
import mitb.module.JSModule;
import mitb.module.ScriptCommandModule;
import mitb.module.ScriptModule;
import mitb.module.modules.*;
import mitb.module.modules.googsearch.GoogleSearchModule;
import mitb.module.modules.weather.WeatherModule;
import mitb.module.modules.youtube.YoutubeLookupModule;
import mitb.util.Properties;
import mitb.util.ScriptingHelper;
import mitb.util.StringHelper;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.UtilSSLSocketFactory;
import org.pircbotx.cap.TLSCapHandler;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.types.GenericEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSocketFactory;
import javax.script.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Main class for TitanBot. Handles the main functionality of the bot.
 */
public final class TitanBot {

    public static final List<ScriptModule> MODULES = new ArrayList<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(TitanBot.class);
    private static final RateLimiter RATE_LIMITER = RateLimiter.create(Properties.getValueAsDouble("rate"));
    private static Connection databaseConnection;

    /**
     * Get bot logger instance.
     * @return
     */
    public static Logger getLogger() {
        return TitanBot.LOGGER;
    }

    /**
     * Gets the database connection.
     * @return
     */
    public static Connection getDatabaseConnection() {
        return TitanBot.databaseConnection;
    }

    /**
     * Sets the database connection.
     * @param databaseConnection
     */
    public static void setDatabaseConnection(Connection databaseConnection) {
        TitanBot.databaseConnection = databaseConnection;
    }

    /**
     * Send a rate-limited reply to the channel
     *
     * @param event event we should reply to
     * @param reply message we should send.
     */
    public static void sendReply(GenericEvent event, String reply, String truncatedText) {
        TitanBot.sendReply(event, reply, truncatedText, false);
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
     * Send a reply to the channel
     *
     * @param event event we should reply to
     * @param reply message we should send.
     * @param ignoreRate should we ignore the rate limit
     */
    public static void sendReply(GenericEvent event, String reply, boolean ignoreRate) {
        TitanBot.sendReply(event, reply, "", ignoreRate);
    }

    /**
     * Send a reply to the channel with a suffix if it is truncated to 3 lines.
     *
     * @param event event we should reply to
     * @param reply message we should send.
     * @param ignoreRate should we ignore the rate limit
     */
    public static void sendReply(GenericEvent event, String reply, String truncatedText, boolean ignoreRate) {
        if (ignoreRate || !(event instanceof MessageEvent) || TitanBot.RATE_LIMITER.tryAcquire()) {
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
     * Entry point to TitanBot.
     */
    public void run() throws Exception {
        TitanBot.setDatabaseConnection(DriverManager.getConnection("jdbc:sqlite:database.db"));

        StringHelper.loadWordList(Properties.getValue("games.wordlist"));
        EventHandler.register(new CommandHandler());
        this.registerModules();
        this.createTables();

        Configuration configuration = TitanBot.generateConfiguration();

        // Now start the bot
        try (PircBotX bot = new PircBotX(configuration)) {
            bot.startBot();
        }
    }

    /**
     * Generates bot {@link Configuration}.
     * @return This bots configuration based on its {@link Properties}.
     */
    private static Configuration generateConfiguration() {
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
     * Registers all the modules of the application.
     */
    private void registerModules() throws ScriptException, IOException {
        TitanBot.MODULES.add(new LastSeenModule());
        TitanBot.MODULES.add(new WeatherModule());
        TitanBot.MODULES.add(new MemoModule());
        //TitanBot.MODULES.add(new GoogleSearchModule());
        TitanBot.MODULES.add(new QuotesModule());
        TitanBot.MODULES.add(new WolframEvaluationModule());
        TitanBot.MODULES.add(new AzGameModule());
        //TitanBot.MODULES.add(new HangmanGameModule());
        TitanBot.MODULES.add(new YoutubeLookupModule());

        ScriptEngineManager engineManager = new ScriptEngineManager();
        ScriptEngine engine = engineManager.getEngineByName("nashorn");

        Bindings bindings = engine.getBindings(ScriptContext.GLOBAL_SCOPE);
        bindings.put("helper", new ScriptingHelper());

        engine.eval(new String(Files.readAllBytes(Paths.get("lib/babel-standalone.js")), StandardCharsets.UTF_8));
        ScriptEngine babelEngine = engine;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get("modules"))) {
            for (Path p : stream) {
                engine = engineManager.getEngineByName("nashorn");
                engine.getBindings(ScriptContext.ENGINE_SCOPE).put("engine", engine);
                engine.getBindings(ScriptContext.ENGINE_SCOPE).put("exports", ((Invocable) engine).invokeMethod(
                        engine.eval("JSON"), "parse", "{}"));

                if (p.toString().endsWith(".js")) {
                    engine.eval(Files.newBufferedReader(p));
                } else if (p.toString().endsWith(".es6")) {
                    String source = new String(Files.readAllBytes(p));
                    babelEngine.getBindings(ScriptContext.ENGINE_SCOPE).put("source", source);
                    String transform = (String) babelEngine.eval("Babel.transform(source, {presets: ['es2015']}).code");
                    engine.eval(transform);
                }

                ScriptObjectMirror clazz = (ScriptObjectMirror) engine.eval("exports.default");
                ScriptModule obj;

                try {
                    String[] commands = (String[]) ((Invocable) engine).invokeMethod(clazz, "getCommands");
                    obj = new JSModule((Invocable) engine, clazz).commandProxy();
                    CommandHandler.register((ScriptCommandModule) obj, commands);
                } catch (NoSuchMethodException e) {
                    obj = new JSModule((Invocable) engine, clazz).proxy();
                }

                TitanBot.MODULES.add(obj);
                obj.register();
            }
        } catch (IOException | ScriptException e) {
            TitanBot.LOGGER.error("Error when opening or executing script.", e);
        } catch (NoSuchMethodException e) {
            TitanBot.LOGGER.error("Error when registering new script.", e);
        }

        TitanBot.getLogger().info("Registered all modules (count=" + TitanBot.MODULES.size() + ").");
    }

    /**
     * Create all the SQLite tables we need
     */
    private void createTables() {
        try {
            Statement stmt = TitanBot.databaseConnection.createStatement();
            stmt.execute("CREATE TABLE seen (id INTEGER PRIMARY KEY AUTOINCREMENT, nick VARCHAR(50), login VARCHAR(50), seen DATETIME)");
            stmt.execute("CREATE TABLE weather (id INTEGER PRIMARY KEY AUTOINCREMENT, nick VARCHAR(50), location VARCHAR(50))");
            stmt.execute("CREATE TABLE memo (id INTEGER PRIMARY KEY AUTOINCREMENT, target_nick VARCHAR(50), sender_nick VARCHAR(50), message VARCHAR(250))");
            stmt.execute("CREATE TABLE quotes (id INTEGER PRIMARY KEY AUTOINCREMENT, creator_nick VARCHAR(50), quote VARCHAR(350))");
        } catch(Exception ignored) {}
    }
}
