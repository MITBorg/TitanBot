package mitb.util;

import org.pircbotx.User;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.types.GenericEvent;

/**
 * A collection of utilty functions aimed at the pircbot dependency.
 */
public final class PIrcBotXHelper {

    /**
     * Gets the user of the sender of an {@link MessageEvent} or {@link PrivateMessageEvent}, or null if an invalid.
     * @param event The event.
     * @return {@link User} or null if the event type is invalid or the user object instance is null.
     */
    public static User getUser(GenericEvent event) {
        if (event instanceof PrivateMessageEvent) {
            return ((PrivateMessageEvent)event).getUser();
        } else if (event instanceof MessageEvent) {
            return ((MessageEvent)event).getUser();
        }
        return null; // invalid event type
    }

    /**
     * Gets the nickname of the sender of an {@link MessageEvent} or {@link PrivateMessageEvent} in lower case,
     * or null if invalid.
     * @param event The event.
     * @return {@link User} or null if the event type is invalid or the user object instance is null.
     */
    public static String getNick(GenericEvent event) {
        User u = PIrcBotXHelper.getUser(event);
        return (u == null) ? null : u.getNick().toLowerCase();
    }

    /**
     * Gets the channel name of an {@link MessageEvent} or or null if invalid.
     * @param event The event.
     * @return Channel name or null if the event type is invalid
     */
    public static String getChannelName(GenericEvent event) {
        if (event instanceof MessageEvent) {
            return ((MessageEvent)event).getChannelSource();
        }
        return null; // invalid event type
    }
}
