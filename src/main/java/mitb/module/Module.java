package mitb.module;

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
}
