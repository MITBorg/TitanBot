class SedReplacement {
    constructor() {
        this.cache = {};
        this.regex = new RegExp('^(?:s/((?:[^\\\\/]|\\\\.)*)/((?:[^\\\\/]|\\\\.)*)/((?:g|i|\\d+)*))(?:;s/((?:[^\\\\/]|\\\\.)*)/((?:[^\\\\/]|\\\\.)*)/((?:g|i|\\d+)*))*$');
    }

    register() {
        helper.register(engine, 'onMessage', Java.type('mitb.event.events.MessageEvent'));
    }

    getHelp(event) {
        helper.respond(event, 'Syntax: s/old text/new text/flags');
    }

    onMessage(event) {
        var msg = event.getSource().getMessage();
        var channel = event.getSource().getChannel().getName();

        var targeted = false;

        var callerNick = Java.type('mitb.util.PIrcBotXHelper').getNick(event.getSource());

        if (callerNick == null)
            return;

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

            this.cache[callerNick][channel] = msg;
            return;
        }

        if (!this.cache.hasOwnProperty(targeted || callerNick) || !this.cache[targeted || callerNick].hasOwnProperty(channel))
            return;

        msg = this.cache[targeted || callerNick][channel].replace(new RegExp(matches[1], matches[3]), matches[2]);

        if (matches[4] != null && matches[5] != null)
            msg = msg.replace(new RegExp(matches[4], matches[6]), matches[5]);

        event.getSource().respondWith(`${targeted ? `${callerNick} thinks ${targeted}` : callerNick} meant: ${msg}`);
    }
}

export default new SedReplacement()