class TitanBot {
    register() {
        helper.register(engine, 'onJoin', Java.type('mitb.event.events.JoinEvent'));
    }

    getHelp(event) {
        helper.respond(event, 'This module messages anybody who joins a channel with the nickname *bot');
    }

    onJoin(event) {
        var user = event.getSource().getUser();

        if (!user || user.getNick() === event.getSource().getBot().getNick())
            return;

        if (user.getNick().toLowerCase().endsWith('bot'))
            helper.respond(event, "im better than u noob bot");
    }
}

export default new TitanBot()