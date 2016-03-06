package mitb.event.events;

import mitb.event.Event;
import mitb.event.ProxyEvent;

/**
 * Fired whenever a message is sent in a channel.
 */
public final class MessageEvent implements Event, ProxyEvent {
    private org.pircbotx.hooks.events.MessageEvent event = null;

    public MessageEvent(org.pircbotx.hooks.events.MessageEvent originalEvent) {
        this.event = originalEvent;
    }

    public org.pircbotx.hooks.events.MessageEvent getOriginalEvent() {
        return this.event;
    }
}
