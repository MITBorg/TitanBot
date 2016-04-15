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
        if (commandEvent.getArgs().length)
            this.sendModuleHelp(commandEvent);
        else
            this.sendModuleList(commandEvent);
    }

    sendModuleHelp(commandEvent) {
        var moduleName = _.camelCase(Java.type('com.google.common.base.Joiner').on(' ').join(commandEvent.getArgs()).trim()).toLowerCase();

        _.each(Java.type('mitb.TitanBot').MODULES, (module) => {
            if (module instanceof Java.type('mitb.module.ScriptCommandModule')) {
                if (_.includes(module.getCommands(), moduleName)) {
                    module.getHelp(commandEvent);
                    return;
                }
            } else if (_.camelCase(module.getName()).toLowerCase() === moduleName) {
                module.getHelp(commandEvent);
                return;
            }
        });
    }

    sendModuleList(commandEvent) {
        var modules = _.map(Java.type('mitb.TitanBot').MODULES, (module) => {
            if (module instanceof Java.type('mitb.module.ScriptCommandModule')) {
                return module.getCommands()[0];
            } else {
                return _.camelCase(module.getName().toLowerCase());
            }
        }).join(' ');

        helper.respond(commandEvent, `Syntax: help (module) | Modules: ${modules}`);
    }
}

export default new Help();