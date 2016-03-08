package mitb.event.events;

import mitb.event.Event;
import mitb.event.ProxyEvent;

/**
 * Fired whenever a private message is sent.
 */
public final class PrivateMessageEvent implements Event, ProxyEvent {
    private org.pircbotx.hooks.events.PrivateMessageEvent event = null;

    public PrivateMessageEvent(org.pircbotx.hooks.events.PrivateMessageEvent sourceEvent) {
        this.event = sourceEvent;
    }

    public org.pircbotx.hooks.events.PrivateMessageEvent getSource() {
        return this.event;
    }
}
