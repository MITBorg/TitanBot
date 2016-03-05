package mitb.event;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class EventHandler {
    private static final Map<Object, Map<Method, Class<? extends Event>>> eventListeners = new HashMap<>();

    /**
     * Register some event handlers with the event bus.
     *
     * @param o object to register the events of
     */
    public static void register(Object o) {
        if (eventListeners.containsKey(o)) {
            return;
        }

        HashMap<Method, Class<? extends Event>> list = new HashMap<>();

        for (Method method : o.getClass().getMethods()) {
            if (method.isAnnotationPresent(Listener.class)) {
                list.put(method, (Class<? extends Event>) method.getParameterTypes()[0]);
            }
        }

        eventListeners.put(o, list);
    }

    /**
     * Loop over all of our event listeners and see if our `event` can trigger each event listener. If it can, do it.
     *
     * @param event event to send out to listeners
     */
    public static void trigger(Event event) {
        eventListeners.entrySet().stream().forEach(entry -> {
            Map<Method, Class<? extends Event>> events = entry.getValue();

            events.entrySet().stream().filter(e -> e.getValue().isInstance(event)).forEach(e -> {
                try {
                    e.getKey().invoke(entry.getKey(), event);
                } catch(Exception e1) {
                    e1.printStackTrace();
                }
            });
        });
    }
}
