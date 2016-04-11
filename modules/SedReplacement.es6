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
        var matches = this.regex.exec(msg);

        var callerNick = Java.type('mitb.util.PIrcBotXHelper').getNick(event.getSource());

        if (callerNick == null) {
            return;
        }

        if (matches == null) {
            this.cache[callerNick] = msg;
            return;
        }

        if (!this.cache.hasOwnProperty(callerNick)) {
            return;
        }

        msg = this.cache[callerNick].replace(new RegExp(matches[1], matches[3]), matches[2]);

        if (matches[4] != null && matches[5] != null) {
            msg = msg.replace(new RegExp(matches[4], matches[6]), matches[5]);
        }

        event.getSource().respondWith(`${callerNick} meant: ${msg}`);
    }
}

export default new SedReplacement()