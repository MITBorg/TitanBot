package mitb.module.modules;

import mitb.TitanBot;
import mitb.event.events.CommandEvent;
import mitb.module.CommandModule;

public final class TestCommandModule extends CommandModule {
    @Override
    public String[] getCommands() {
        return new String[]{"testcmd", "testo"};
    }

    @Override
    public void getHelp(CommandEvent event) {
        TitanBot.sendReply(event.getOriginalEvent(), "Syntax: " + event.getArgs()[0]);
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
