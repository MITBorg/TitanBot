package mitb.module.modules;

import mitb.TitanBot;
import mitb.event.EventHandler;
import mitb.event.Listener;
import mitb.event.events.MessageEvent;
import mitb.module.Module;

/**
 * Just sends out annoying messages whenever anyone talks.
 */
public class AnnoyingModule extends Module {
    @Override
    public void register() {
        EventHandler.register(this);
    }

    /**
     * Respond every time we receive a message event
     * @param event
     */
    @Listener
    public void onTalk(MessageEvent event) {
        //TitanBot.sendReply(event.getOriginalEvent(), "Please just stop talking bro");
    }
}
