package mitb.module.modules;

import mitb.TitanBot;
import mitb.event.EventHandler;
import mitb.event.Listener;
import mitb.event.events.CommandEvent;
import mitb.event.events.MessageEvent;
import mitb.module.Module;
import mitb.util.PIrcBotXHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Allows people to use s/abc/def/ replacement.
 */
public final class SedReplacementModule extends Module {

    private final Map<String, String> cache = new HashMap<>();

    @Listener
    public void onMessage(MessageEvent event) {
        String msg = event.getSource().getMessage();
        Pattern pattern = Pattern.compile("^(?:s/((?:[^\\\\/]|\\\\.)*)/((?:[^\\\\/]|\\\\.)*)/((?:g|i|\\d+)*))(?:;s/((?:[^\\\\/]|\\\\.)*)/((?:[^\\\\/]|\\\\.)*)/((?:g|i|\\d+)*))*$");
        Matcher matcher = pattern.matcher(msg);

        try {
            String callerNick = PIrcBotXHelper.getNick(event.getSource());

            // Invalid caller/source event
            if (callerNick == null) {
                return;
            }

            // Continue as normal
            if (matcher.matches()) {
                if (!this.cache.containsKey(callerNick)) return;

                msg = this.cache.get(callerNick).replaceAll(matcher.group(1), matcher.group(2));

                if (matcher.group(4) != null && matcher.group(5) != null) {
                    msg = msg.replaceAll(matcher.group(4), matcher.group(5));
                }
                event.getSource().respondWith(callerNick + " meant: " + msg);
            } else {
                this.cache.put(callerNick, msg);
            }
        } catch(Exception ignored) {}
    }

    @Override
    protected void register() {
        EventHandler.register(this);
    }

    @Override
    public void getHelp(CommandEvent event) {
        TitanBot.sendReply(event.getSource(), "Syntax: s/old text/new text/");
    }
}
