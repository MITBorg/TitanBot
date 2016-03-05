package mitb.irc;

import mitb.TitanBot;
import mitb.event.EventHandler;
import mitb.event.events.MessageEvent;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.InviteEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.pircbotx.output.OutputIRC;

public class IRCListener extends ListenerAdapter {
    /**
     * Join the channel automatically when invited.
     *
     * @param event
     */
    @Override
    public void onInvite(InviteEvent event) {
        new OutputIRC(event.getBot()).joinChannel(event.getChannel());
        System.out.println("Invited to channel " + event.getChannel() + " by " + event.getUser().getNick() + "@" + event.getUser().getHostmask() + ". Joining.");
    }

    /**
     * Handles all incoming messages and delegates them to their needed module.
     *
     * @param event
     */
    @Override
    public void onGenericMessage(GenericMessageEvent event) {
        EventHandler.trigger(new MessageEvent(event));
    }
}
