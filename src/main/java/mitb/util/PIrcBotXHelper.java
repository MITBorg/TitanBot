package mitb.util;

import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.types.GenericEvent;

/**
 * A collection of utilty functions aimed at the pircbot dependency.
 */
public final class PIrcBotXHelper {

    /**
     * Gets the nickname of the sender of an event in lower case, or null if invalid.
     * @param event
     * @return
     */
    public static String getNick(GenericEvent event) {
        if (event instanceof PrivateMessageEvent) {
            return ((PrivateMessageEvent)event).getUser().getNick().toLowerCase();
        } else if (event instanceof MessageEvent) {
            return ((MessageEvent)event).getUser().getNick().toLowerCase();
        }
        return null; // invalid event type
    }
}
