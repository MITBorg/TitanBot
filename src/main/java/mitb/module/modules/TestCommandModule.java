package mitb.module.modules;

import mitb.event.events.CommandEvent;
import mitb.module.CommandModule;

public class TestCommandModule extends CommandModule {
    @Override
    public String[] getCommands() {
        return new String[]{"testcmd", "testo"};
    }

    /**
     * Reply with an example response on command.
     *
     * @param commandEvent
     */
    @Override
    public void onCommand(CommandEvent commandEvent) {
        commandEvent.getOriginalEvent().respond("response from test command");
    }

    @Override
    public void register() {

    }
}
