class Donk {
    register() {
        helper.register(engine, 'onMessage', Java.type('mitb.event.events.MessageEvent'));
        this.regex = /.*donk.*/ig;
        this.donk = 'https://www.youtube.com/watch?v=ckMvj1piK58';
    }

    getHelp(event) {
        helper.respond(event, `Puts a donk on it.`);
    }

    onMessage(event) {
        if (!this.regex.test(event.getSource().getMessage())) return;
        event.getSource().respondWith(this.donk);
    }
}

export default new Donk();