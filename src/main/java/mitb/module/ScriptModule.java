package mitb.module;

import mitb.event.events.CommandEvent;

public interface ScriptModule {
    /**
     * Called upon module register. Used for registering event listeners, etc.
     */
    void register();

    /**
     * Displays help for this module when access, through the help module.
     * @param event
     */
    void getHelp(CommandEvent event);

    /**
     * Get a clean name of this module.
     * @return name
     */
    String getName();
}
