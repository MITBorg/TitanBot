import juration from 'juration';

class Reminder {
    constructor() {
    }

    register() {
    }

    getHelp(event) {
        helper.respond(event, `Syntax: remind (time) (message)`);
    }

    getCommands() {
        return Java.to(['remind', 'remindme!', 'reminder'], "java.lang.String[]");
    }

    onCommand(commandEvent) {
        if (commandEvent.getArgs().length < 2)
            return;

        var args = Java.type('java.util.Arrays').copyOfRange(commandEvent.getArgs(), 1, commandEvent.getArgs().length);

        try {
            var time = new Date().getTime() + (juration.parse(commandEvent.getArgs()[0]) * 1000);
            var Timer = Java.type("java.util.Timer");

            var timer = new Timer();
            timer.schedule(new java.util.TimerTask({
                run: () => {
                    helper.respond(commandEvent, `Reminder: ${Java.type('java.lang.String').join(" ", args)}`, true);
                }
            }), new java.util.Date(time));

            helper.respond(commandEvent, `Reminding you in ${commandEvent.getArgs()[0]}.`);
        } catch(e) {
            print(e);
        }
    }
}

export default new Reminder();