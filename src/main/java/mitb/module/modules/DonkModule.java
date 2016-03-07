package mitb.module.modules;

import mitb.TitanBot;
import mitb.event.EventHandler;
import mitb.event.Listener;
import mitb.event.events.CommandEvent;
import mitb.event.events.MessageEvent;
import mitb.module.Module;
import mitb.util.Properties;

import java.util.Random;

/**
 * In loving memory of the previous bot that did this. Previous bot name not found.
 */
public final class DonkModule extends Module {

    private static final double DONK_CHANCE = 1 - 0.3; // 30% chance

    @Listener
    public void onMessage(MessageEvent event) {
        org.pircbotx.hooks.events.MessageEvent evt = event.getOriginalEvent();
        String msg = evt.getMessage().toLowerCase();
        String botName = evt.getBot().getNick().toLowerCase();

        // If a message with donk in it is said, we link them to our epic meme
        if(msg.contains("donk") && !msg.startsWith(botName) && !evt.getUser().getNick().equals(botName)
                && Math.random() >= DONK_CHANCE) {
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
