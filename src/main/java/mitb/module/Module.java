package mitb.module;

import mitb.event.events.CommandEvent;

/**
 * Abstract class which all modules should extend.
 */
public abstract class Module implements ScriptModule {
    protected Module() {
        this.register();
    }
}
