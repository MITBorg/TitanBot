class Repo {
    register() {

    }

    getHelp(event) {
        helper.respond(event, `Syntax: ${event.getArgs()[0]}`);
    }

    getCommands() {
        return Java.to(['repo', 'github'], "java.lang.String[]");
    }

    onCommand(commandEvent) {
        commandEvent.getSource().respond('https://github.com/MoparScape/TitanBot');
    }
}

export default new Repo();