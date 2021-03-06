package mitb.event.events;

import mitb.event.Event;
import mitb.event.ProxyEvent;

/**
 * A channel join event.
 */
public final class JoinEvent implements Event, ProxyEvent {
    private org.pircbotx.hooks.events.JoinEvent event = null;

    public JoinEvent(org.pircbotx.hooks.events.JoinEvent sourceEvent) {
        this.event = sourceEvent;
    }

    public org.pircbotx.hooks.events.JoinEvent getSource() {
        return this.event;
    }
}
