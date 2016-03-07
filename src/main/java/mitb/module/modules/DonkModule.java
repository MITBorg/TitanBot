package mitb.module.modules;

import mitb.TitanBot;
import mitb.event.EventHandler;
import mitb.event.Listener;
import mitb.event.events.CommandEvent;
import mitb.event.events.MessageEvent;
import mitb.module.Module;

/**
 * In loving memory of the previous bot that did this. Previous bot name not found.
 */
public final class DonkModule extends Module {

    /**
     * The delay between donk responses, in ms.
     */
    private static final long DONK_DELAY = 15 * 1000;
    /**
     * The timestamp of when the last donk response occurred in ms.
     */
    private long lastDonkTime;

    @Listener
    public void onMessage(MessageEvent event) {
        org.pircbotx.hooks.events.MessageEvent evt = event.getSource();
        String msg = evt.getMessage().toLowerCase();
        String botName = evt.getBot().getNick().toLowerCase();

        // If a message with donk in it is said, we link them to our epic meme
        if(msg.contains("donk") && !msg.startsWith(botName) && !evt.getUser().getNick().equals(botName) && canDonk()) {
            evt.respondWith("www.youtube.co.uk/watch?v=ckMvj1piK58");
            lastDonkTime = System.currentTimeMillis();
        }
    }

    /**
     * If we can perform a donk response, based when on the last donk response occurred.
     * @return
     */
    private boolean canDonk() {
        return System.currentTimeMillis() - lastDonkTime > DONK_DELAY;
    }

    @Override
    protected void register() {
        EventHandler.register(this);
    }

    @Override
    public void getHelp(CommandEvent event) {
        TitanBot.sendReply(event.getSource(), "This module puts a banging donk on it.");
    }
}
