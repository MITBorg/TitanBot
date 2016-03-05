package mitb.event;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class EventHandler {
    private static Map<Object, Map<Method, Class<? extends Event>>> eventListeners = new HashMap<>();

    public static void register(Object o) {
        if(eventListeners.containsKey(o)) {
            return;
        }

        HashMap<Method, Class<? extends Event>> list = new HashMap<>();

        for(Method method : o.getClass().getMethods()) {
            if(method.isAnnotationPresent(Listener.class)) {
                Listener annotation = method.getAnnotation(Listener.class);
                list.put(method, annotation.wants());
            }
        }

        eventListeners.put(o, list);
    }

    public static void trigger(Event event) {
        eventListeners.entrySet().stream().forEach(entry -> {
            Map<Method, Class<? extends Event>> events = entry.getValue();

            events.entrySet().stream().filter(e -> event.getClass().isAssignableFrom(e.getValue().getClass())).forEach(e -> {
                try {
                    e.getKey().invoke(entry.getKey(), event);
                } catch(Exception e1) {
                    e1.printStackTrace();
                }
            });
        });
    }
}
