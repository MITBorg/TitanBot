package mitb.irc;

import mitb.Properties;
import mitb.TitanBot;
import mitb.event.EventHandler;
import mitb.event.events.CommandEvent;
import mitb.event.events.MessageEvent;
import mitb.event.events.PrivateMessageEvent;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.InviteEvent;
import org.pircbotx.output.OutputIRC;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IRCListener extends ListenerAdapter {
    /**
     * Join the channel automatically when invited.
     *
     * @param event
     */
    @Override
    public void onInvite(InviteEvent event) {
        new OutputIRC(event.getBot()).joinChannel(event.getChannel());
        TitanBot.LOGGER.info("Invited to channel " + event.getChannel() + " by " + event.getUser().getNick() + "@" + event.getUser().getHostmask() + ". Joining.");
    }

    /**
     * Handles all incoming messages and delegates them to their needed module.
     *
     * @param event
     */
    @Override
    public void onMessage(org.pircbotx.hooks.events.MessageEvent event) {
        EventHandler.trigger(new MessageEvent(event));

        if(event.getMessage().startsWith(Properties.getValue("bot.nick"))) {
            Pattern pattern = Pattern.compile(Properties.getValue("bot.nick") + ".? (.*)");
            Matcher matcher = pattern.matcher(event.getMessage());

            if(matcher.find()) {
                EventHandler.trigger(new CommandEvent(matcher.group(1), event));
            }
        }
    }

    /**
     * Handles all incoming private and delegates them to their needed module.
     *
     * @param event
     */
    @Override
    public void onPrivateMessage(org.pircbotx.hooks.events.PrivateMessageEvent event) {
        EventHandler.trigger(new PrivateMessageEvent(event));
        EventHandler.trigger(new CommandEvent(event.getMessage(), event));
    }
}
