package mitb.command;

import mitb.event.Listener;
import mitb.event.events.CommandEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles everything to do with bot commands
 */
public final class CommandHandler {
    private static final Map<String, List<CommandListener>> listeners = new HashMap<>();

    /**
     * Register some commands with command registry.
     */
    public static void register(CommandListener listener, String[] commands) {
        for (String command : commands) {
            command = command.toLowerCase();

            if (!CommandHandler.listeners.containsKey(command)) {
                CommandHandler.listeners.put(command, new ArrayList<>());
            }

            CommandHandler.listeners.get(command).add(listener);
        }
    }

    /**
     * Fire off a command to all listeners which match it.
     */
    @Listener
    public static void trigger(CommandEvent e) {
        if (!CommandHandler.listeners.containsKey(e.getCommand().toLowerCase())) {
            return;
        }

        for (CommandListener commandListener : CommandHandler.listeners.get(e.getCommand().toLowerCase())) {
            commandListener.onCommand(e);
        }
    }
}
