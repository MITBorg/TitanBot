package mitb.module;

import mitb.command.CommandListener;

public interface ScriptCommandModule extends CommandListener, ScriptModule {
    String[] getCommands();
}
