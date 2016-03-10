package mitb.event;

import mitb.util.PIrcBotXHelper;
import mitb.util.Properties;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public final class EventHandler {

    /**
     * A list of ignored nicks.
     */
    private static final String[] IGNORED_NICKS = Properties.getValue("ignore_nicks").split(",");
    /**
     * Event listeners.
     */
    private static final Map<Object, Map<String, Class<? extends Event>>> eventListeners = new HashMap<>();

    /**
     * Register some event handlers with the event bus.
     *
     * @param o object to register the events of
     */
    public static void register(Object o) {
        if (EventHandler.eventListeners.containsKey(o)) {
            return;
        }
        HashMap<String, Class<? extends Event>> list = new HashMap<>();

        for (Method method : o.getClass().getMethods()) {
            if (method.isAnnotationPresent(Listener.class)) {
                list.put(method.getName(), (Class<? extends Event>) method.getParameterTypes()[0]);
            }
        }
        EventHandler.eventListeners.put(o, list);
    }

    /**
     * Register an event handler with the event bus.
     *
     * @param engine object to register the event of
     */
    public static void register(ScriptEngine engine, String methodName, Class<? extends Event> event) {
        if (EventHandler.eventListeners.containsKey(engine)) {
            EventHandler.eventListeners.get(engine).put(methodName, event);
            return;
        }

        HashMap<String, Class<? extends Event>> list = new HashMap<>();
        list.put(methodName, event);
        EventHandler.eventListeners.put(engine, list);
    }

    /**
     * Loop over all of our event listeners and see if our `event` can trigger each event listener. If it can, do it.
     *
     * @param event event to send out to listeners
     */
    public static void trigger(Event event) {
        // Handle ignored nicks
        if (EventHandler.ignoredNicksLoaded() && (event instanceof ProxyEvent)) {
            ProxyEvent evt = (ProxyEvent)event;
            String callerNick = PIrcBotXHelper.getNick(evt.getSource());

            if (EventHandler.isIgnoredNick(callerNick)) {
                return;
            }
        }

        // Attempt trigger
        EventHandler.eventListeners.entrySet().stream().forEach(entry -> {
            Map<String, Class<? extends Event>> events = entry.getValue();

            events.entrySet().stream().filter(e -> e.getValue().isInstance(event)).forEach(e -> {
                try {
                    if (entry.getKey() instanceof ScriptEngine) {
                        Invocable engine = (Invocable) entry.getKey();
                        engine.invokeFunction(e.getKey(), event);
                    } else {
                        entry.getKey().getClass().getMethod(e.getKey(), event.getClass()).invoke(entry.getKey(), event);
                    }
                } catch(Exception e1) {
                    e1.printStackTrace();
                }
            });
        });
    }

    /**
     * If the ignored nick list is valid.
     * @return
     */
    private static boolean ignoredNicksLoaded() {
        int len = EventHandler.IGNORED_NICKS.length;
        return (len != 0) && !(EventHandler.IGNORED_NICKS[0].equalsIgnoreCase("NONE") && (len == 1));
    }

    /**
     * If the given nickname is to be ignored (i.e. in IGNORED_NICKS).
     * @param nick
     * @return
     */
    private static boolean isIgnoredNick(String nick) {
        for (int i = 0; i < EventHandler.IGNORED_NICKS.length; i++) {
            if (EventHandler.IGNORED_NICKS[i].equalsIgnoreCase(nick)) {
                return true;
            }
        }
        return false;
    }
}
