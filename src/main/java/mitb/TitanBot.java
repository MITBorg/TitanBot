package mitb;

import com.google.common.util.concurrent.RateLimiter;
import mitb.event.EventHandler;
import mitb.event.Listener;
import mitb.event.events.MessageEvent;
import mitb.irc.IRCListener;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.UtilSSLSocketFactory;
import org.pircbotx.cap.TLSCapHandler;
import org.pircbotx.hooks.types.GenericEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSocketFactory;

/**
 * Main class for TitanBot. Handles the main functionality of the bot.
 */
public class TitanBot {
    public static final Logger LOGGER = LoggerFactory.getLogger(TitanBot.class);
    public static final RateLimiter RATE_LIMITER = RateLimiter.create(0.4);

    /**
     * Entry point to TitanBot.
     */
    public void run() throws Exception {
        Configuration configuration = new Configuration.Builder()
                .setName(Properties.getValue("bot.nick"))
                .setLogin(Properties.getValue("bot.username"))
                .setVersion(Properties.getValue("bot.version"))
                .setRealName(Properties.getValue("bot.real_name"))
                .setSocketFactory(new UtilSSLSocketFactory().trustAllCertificates())
                .setAutoReconnect(true)
                .setAutoNickChange(true)
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
        if(RATE_LIMITER.tryAcquire()) {
            event.respond(reply);
        }
    }
}
