package mitb.event.events;

import mitb.event.Event;
import mitb.event.ProxyEvent;

/**
 * A channel join event.
 */
public class JoinEvent implements Event, ProxyEvent {
    private org.pircbotx.hooks.events.JoinEvent event = null;

    public JoinEvent(org.pircbotx.hooks.events.JoinEvent originalEvent) {
        this.event = originalEvent;
    }

    public org.pircbotx.hooks.events.JoinEvent getOriginalEvent() {
        return this.event;
    }
}
