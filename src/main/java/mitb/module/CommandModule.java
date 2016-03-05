package mitb.module;

import mitb.command.CommandHandler;
import mitb.command.CommandListener;

/**
 * Abstract class which all modules which use commands should extend.
 */
public abstract class CommandModule extends Module implements CommandListener {
    public CommandModule() {
        super();
        CommandHandler.register(this, this.getCommands());
    }

    public abstract String[] getCommands();
}
