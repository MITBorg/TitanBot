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
        TitanBot.sendReply(event.getSource(), "Syntax: " + event.getArgs()[0] + " (module)");
    }

    @Override
    public void onCommand(CommandEvent commandEvent) {
        if (commandEvent.getArgs().length == 0) { // List of modules
            HelpModule.sendModulesList(commandEvent);
        } else { // Help for specific module
            this.sendModuleHelp(commandEvent);
        }
    }

    private void sendModuleHelp(CommandEvent event) {
        String moduleName = event.getArgs()[0].toLowerCase();

        // Iterating all loaded modules
        for (Module module : TitanBot.MODULES) {
            if (module instanceof CommandModule) { // Command module specialised help
                CommandModule cmd = (CommandModule) module;

                if (Arrays.asList(cmd.getCommands()).contains(moduleName)) {
                    cmd.getHelp(event);
                    return;
                }
            } else { // Regular module help
                String className = HelpModule.getModuleName(module);

                if (className.equalsIgnoreCase(moduleName)) {
                    module.getHelp(event);
                    return;
                }
            }
        }

        // Module not found response
        TitanBot.sendReply(event.getSource(), "No module found with name: " + moduleName);
    }

    private static void sendModulesList(CommandEvent event) {
        // Generate list of modules
        StringBuilder moduleList = new StringBuilder();

        // Iterating each module
        for (Module module : TitanBot.MODULES) {
            if (module instanceof CommandModule) { // Command module name, from commands array
                CommandModule cmd = (CommandModule) module;
                moduleList.append(cmd.getCommands()[0]).append(' ');
            } else { // Regular module name, derived from class name
                moduleList.append(HelpModule.getModuleName(module)).append(' ');
            }
        }

        // Send to caller
        TitanBot.sendReply(event.getSource(), "Syntax: help (module) | Modules: " + moduleList);
    }

    @Override
    public void register() { }

    /**
     * Gets the name of a module class instance, without the Module at the end in lower case.
     * @param module
     * @return
     */
    private static String getModuleName(Module module) {
        String className = module.getClass().getSimpleName();
        return className.substring(0, className.lastIndexOf("Module")).toLowerCase();
    }
}
