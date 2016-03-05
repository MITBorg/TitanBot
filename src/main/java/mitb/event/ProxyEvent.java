package mitb.event;

import org.pircbotx.hooks.types.GenericEvent;

/**
 * Implemented by all events which are essentially a facade to a library's event.
 */
public interface ProxyEvent {
    GenericEvent getOriginalEvent();
}
