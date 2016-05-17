class Sed {
    constructor() {
        this.cache = {};
        this.regex = new RegExp('^sed (.*)$');

        this.ProcessBuilder = Java.type('java.lang.ProcessBuilder');
        this.BufferedReader = Java.type('java.io.BufferedReader');
        this.BufferedWriter = Java.type('java.io.BufferedWriter');
        this.InputStreamReader = Java.type('java.io.InputStreamReader');
        this.OutputStreamWriter = Java.type('java.io.OutputStreamWriter');
        this.Collectors = Java.type('java.util.stream.Collectors');
    }

    register() {
        helper.register(engine, 'onMessage', Java.type('mitb.event.events.MessageEvent'));
    }

    getHelp(event) {
        helper.respond(event, 'Syntax: s/old text/new text/flags');
    }

    onMessage(event) {
        var originalMsg = event.getSource().getMessage();
        var msg = originalMsg;
        var channel = event.getSource().getChannel().getName();

        var targeted = false;

        var callerNick = Java.type('mitb.util.PIrcBotXHelper').getNick(event.getSource());

        if (callerNick == null)
            return;
        else
            callerNick = callerNick.toLowerCase();

        var word = /^([a-zA-Z\[\]\\`_\^\{\|\}][a-zA-Z0-9\[\]\\`_\^\{\|\}-]{1,31})/;

        if (word.test(msg)) {
            var firstWord = word.exec(msg)[0].toLowerCase();
            var found = false;

            // if the first word is targeting someone in the channel
            event.getSource().getChannel().getUsersNicks().forEach((user) => {
                if (!found && user.toLowerCase() == firstWord) {
                    targeted = user;
                    found = true;
                }
            });

            if (found)
                msg = msg.substr(msg.indexOf(' ') + 1);
        }

        var matches = this.regex.exec(msg);

        if (matches == null) {
            if (!this.cache.hasOwnProperty(callerNick))
                this.cache[callerNick] = {};

            if (!this.cache[callerNick].hasOwnProperty(channel))
                this.cache[callerNick][channel] = [];

            if (this.cache[callerNick][channel].length > 2)
                this.cache[callerNick][channel].splice(0, 1);

            this.cache[callerNick][channel].push(originalMsg);
            return;
        }

        if (!this.cache.hasOwnProperty(targeted || callerNick) || !this.cache[targeted || callerNick].hasOwnProperty(channel))
            return;

        //let regex = new RegExp(matches[1], matches[3]);
        let command = matches[1];
        found = false;

        this.cache[targeted || callerNick][channel].reverse();

        for (let cached of this.cache[targeted || callerNick][channel]) {
            var pb = new this.ProcessBuilder('sed', command);
            pb.redirectErrorStream(true);
            var process = pb.start();

            var input = process.getOutputStream();
            var output = process.getInputStream();

            var reader = new this.BufferedReader(new this.InputStreamReader(output));
            var writer = new this.BufferedWriter(new this.OutputStreamWriter(input));

            writer.write(cached);
            writer.flush();
            writer.close();

            var newMsg = reader.lines().collect(this.Collectors.joining('\n'));
            reader.close();

            if (cached !== newMsg && newMsg) {
                found = true;
                msg = newMsg;
                break;
            }
        }

        this.cache[targeted || callerNick][channel].reverse();

        if (found)
            event.getSource().respondWith(`${targeted ? `${callerNick} thinks ${targeted}` : callerNick} meant: ${msg}`);
    }
}

export default new Sed();