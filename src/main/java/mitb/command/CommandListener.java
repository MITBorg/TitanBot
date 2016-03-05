package mitb.command;

import mitb.event.events.CommandEvent;

/**
 * Implemented by all modules which need commands.
 */
public interface CommandListener {
    void onCommand(CommandEvent commandEvent);
}
