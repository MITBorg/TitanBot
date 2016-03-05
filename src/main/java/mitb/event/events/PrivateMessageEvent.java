package mitb.event.events;

import mitb.event.Event;
import mitb.event.ProxyEvent;

/**
 * Fired whenever a private message is sent.
 */
public class PrivateMessageEvent implements Event, ProxyEvent {
    protected org.pircbotx.hooks.events.PrivateMessageEvent event = null;

    public PrivateMessageEvent(org.pircbotx.hooks.events.PrivateMessageEvent originalEvent) {
        this.event = originalEvent;
    }

    public org.pircbotx.hooks.events.PrivateMessageEvent getOriginalEvent() {
        return this.event;
    }
}
