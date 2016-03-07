package mitb.module;

import mitb.event.events.CommandEvent;

/**
 * Abstract class which all modules should extend.
 */
public abstract class Module {
    protected Module() {
        this.register();
    }

    /**
     * Called upon module register. Used for registering event listeners, etc.
     */
    protected abstract void register();

    /**
     * Displays help for this module when access, through the help module.
     * @param event
     */
    public abstract void getHelp(CommandEvent event);
}
