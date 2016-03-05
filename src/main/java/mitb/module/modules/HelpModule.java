package mitb.module.modules;

import mitb.TitanBot;
import mitb.event.events.CommandEvent;
import mitb.module.CommandModule;
import mitb.module.Module;

import java.util.Arrays;

/**
 * Provides the "help" command to show syntax of a command.
 */
public class HelpModule extends CommandModule {
    @Override
    public String[] getCommands() {
        return new String[]{"help"};
    }

    @Override
    public void getHelp(CommandEvent event) {
        TitanBot.sendReply(event.getOriginalEvent(), "Syntax: help command");
    }

    @Override
    public void onCommand(CommandEvent commandEvent) {
        if (commandEvent.getArgs().length == 0) {
            return;
        }

        String help = commandEvent.getArgs()[0].toLowerCase();

        for (Module module : TitanBot.MODULES) {
            if (module instanceof CommandModule) {
                CommandModule commandModule = (CommandModule) module;

                if (Arrays.asList(commandModule.getCommands()).contains(help)) {
                    commandModule.getHelp(commandEvent);
                    return;
                }
            }
        }
    }

    @Override
    public void register() {

    }
}
