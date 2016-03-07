package mitb.module.modules;

import mitb.TitanBot;
import mitb.event.EventHandler;
import mitb.event.Listener;
import mitb.event.events.CommandEvent;
import mitb.event.events.JoinEvent;
import mitb.module.Module;

/**
 * Flames other bots when they join a channel this bot is on.
 */
public final class FlameBotModule extends Module {

    @Listener
    public void onJoin(JoinEvent event) {
        org.pircbotx.hooks.events.JoinEvent evt = event.getSource();

        if(!evt.getUser().getNick().equals(evt.getBot().getNick()) && evt.getUser().getNick().toLowerCase().endsWith("bot")) {
            evt.respond("im better than u noob bot");
        }
    }

    @Override
    protected void register() {
        EventHandler.register(this);
    }

    @Override
    public void getHelp(CommandEvent event) {
        TitanBot.sendReply(event.getSource(), "This module messages any user who joins with nickname *bot");
    }
}
