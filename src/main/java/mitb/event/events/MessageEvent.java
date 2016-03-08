package mitb.event.events;

import mitb.event.Event;
import mitb.event.ProxyEvent;

/**
 * Fired whenever a message is sent in a channel.
 */
public final class MessageEvent implements Event, ProxyEvent {
    private org.pircbotx.hooks.events.MessageEvent event = null;

    public MessageEvent(org.pircbotx.hooks.events.MessageEvent sourceEvent) {
        this.event = sourceEvent;
    }

    public org.pircbotx.hooks.events.MessageEvent getSource() {
        return this.event;
    }
}
