package mitb.module.modules;

import mitb.TitanBot;
import mitb.event.EventHandler;
import mitb.event.Listener;
import mitb.event.events.CommandEvent;
import mitb.event.events.MessageEvent;
import mitb.module.Module;
import mitb.util.Properties;

/**
 * In loving memory of the previous bot that did this. Previous bot name not found.
 */
public class DonkModule extends Module {

    @Listener
    public void onMessage(MessageEvent event) {
        org.pircbotx.hooks.events.MessageEvent evt = event.getOriginalEvent();
        String msg = evt.getMessage().toLowerCase();
        String botName = evt.getBot().getNick().toLowerCase();
        if(msg.contains("donk") && !msg.startsWith(botName) && !evt.getUser().getNick().equals(botName)) {
            evt.respondWith("www.youtube.co.uk/watch?v=ckMvj1piK58");
        }
    }

    @Override
    protected void register() {
        EventHandler.register(this);
    }

    @Override
    public void getHelp(CommandEvent event) {
        TitanBot.sendReply(event.getOriginalEvent(), "This module puts a banging donk on it.");
    }
}
