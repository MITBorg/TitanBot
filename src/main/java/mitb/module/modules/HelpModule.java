package mitb.module.modules;

import mitb.TitanBot;
import mitb.event.events.CommandEvent;
import mitb.module.CommandModule;
import mitb.module.Module;

import java.util.Arrays;

/**
 * Provides the "help" command to show syntax of a command.
 */
public final class HelpModule extends CommandModule {
    @Override
    public String[] getCommands() {
        return new String[]{"help"};
    }

    @Override
    public void getHelp(CommandEvent event) {
        TitanBot.sendReply(event.getOriginalEvent(), "Syntax: help [command]");
    }

    @Override
    public void onCommand(CommandEvent commandEvent) {
        if (commandEvent.getArgs().length == 0) { // List of modules
            sendModulesList(commandEvent);
        } else { // Help for specific module
            sendModuleHelp(commandEvent);
        }
    }

    private void sendModuleHelp(CommandEvent event) {
        String help = event.getArgs()[0].toLowerCase();

        for (Module module : TitanBot.MODULES) {
            if (module instanceof CommandModule) {
                CommandModule commandModule = (CommandModule) module;

                if (Arrays.asList(commandModule.getCommands()).contains(help)) {
                    commandModule.getHelp(event);
                    return;
                }
            }
        }
    }

    private void sendModulesList(CommandEvent event) {
        // Generate list of modules
        StringBuilder moduleList = new StringBuilder();

        for (Module module : TitanBot.MODULES) {
            if (module instanceof CommandModule) {
                CommandModule cmd = (CommandModule) module;
                moduleList.append(cmd.getCommands()[0]).append(" ");
            } else {
                String className = module.getClass().getSimpleName();
                String parsedClassName = className.substring(0, className.lastIndexOf("Module"));
                moduleList.append(parsedClassName).append(" ");
            }
        }

        // Send to caller
        TitanBot.sendReply(event.getOriginalEvent(), "Modules: " + moduleList);
    }

    @Override
    public void register() {

    }
}
