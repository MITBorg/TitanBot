load('lib/lodash.js');

class Help {
    register() {

    }

    getHelp(event) {
        helper.respond(event, `Syntax: help (module)`);
    }

    getCommands() {
        return Java.to(['help'], "java.lang.String[]");
    }

    onCommand(commandEvent) {
        if (commandEvent.getArgs().length) {
            this.sendModuleHelp(commandEvent);
        } else {
            this.sendModuleList(commandEvent);
        }
    }

    sendModuleHelp(commandEvent) {
        var moduleName = commandEvent.getArgs()[0].toLowerCase();

        _.each(Java.type('mitb.TitanBot').MODULES, (module) => {
            if (module instanceof Java.type('mitb.module.ScriptCommandModule')) {
                if (_.some(module.getCommands(), (value) => value === moduleName)) {
                    module.getHelp(commandEvent);
                    return;
                }
            } else {
                var className = module.getClass().getSimpleName();
                if (className.substr(0, className.lastIndexOf("Module")).toLowerCase() === moduleName) {
                    module.getHelp(commandEvent);
                    return;
                }
            }
        });
    }

    sendModuleList(commandEvent) {
        var modules = _.map(Java.type('mitb.TitanBot').MODULES, (module) => {
            print(module.getClass().getSimpleName());

            if (module instanceof Java.type('mitb.module.ScriptCommandModule')) {
                return module.getCommands()[0];
            } else {
                var className = module.getClass().getSimpleName();
                return className.substr(0, className.lastIndexOf("Module")).toLowerCase();
            }
        }).join(' ');

        helper.respond(commandEvent, `Syntax: help (module) | Modules: ${modules}`);
    }
}

export default new Help();