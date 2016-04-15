load('lib/humanized_time_span.js');

class Stats {
    register() {
    }

    getHelp(event) {
        helper.respond(event, `Syntax: ${event.getArgs()[0]}`);
    }

    getCommands() {
        return Java.to(['stats', 'statistics'], "java.lang.String[]");
    }

    onCommand(commandEvent) {
        var uptime = new Date().getTime() - Java.type('java.lang.management.ManagementFactory').getRuntimeMXBean().getUptime();
        var load = Java.type('java.lang.management.ManagementFactory').getOperatingSystemMXBean().getSystemLoadAverage();

        var started = humanized_time_span(uptime);

        helper.respond(commandEvent, `Up since ${started}. Currently under load ${load}. Accepting donations for a better server.`);
    }
}

export default new Stats();