package mitb.event.events;

import mitb.event.Event;
import org.pircbotx.hooks.types.GenericEvent;

public class MessageEvent implements Event {
    private GenericEvent event;

    public MessageEvent(GenericEvent originalEvent) {
        this.event = originalEvent;
    }

    public GenericEvent getOriginalEvent() {
        return this.event;
    }
}
