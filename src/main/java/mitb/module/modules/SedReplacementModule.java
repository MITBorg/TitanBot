package mitb.module.modules;

import mitb.TitanBot;
import mitb.event.EventHandler;
import mitb.event.Listener;
import mitb.event.events.MessageEvent;
import mitb.module.Module;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Allows people to use s/abc/def/ replacement.
 */
public class SedReplacementModule extends Module {
    private Map<String, String> cache = new HashMap<>();

    @Listener
    public void onMessage(MessageEvent event) {
        String msg = event.getOriginalEvent().getMessage();
        Pattern pattern = Pattern.compile("^(?:s/((?:[^\\\\/]|\\\\.)*)/((?:[^\\\\/]|\\\\.)*)/((?:g|i|\\d+)*))(?:;s/((?:[^\\\\/]|\\\\.)*)/((?:[^\\\\/]|\\\\.)*)/((?:g|i|\\d+)*))*$");
        Matcher matcher = pattern.matcher(msg);

        try {
            String nick = event.getOriginalEvent().getUser().getNick();

            if (nick == null) return;

            if (matcher.matches()) {
                if (!this.cache.containsKey(nick)) return;

                msg = this.cache.get(nick).replaceAll(matcher.group(1), matcher.group(2));

                if (matcher.group(4) != null && matcher.group(5) != null) {
                    msg = msg.replaceAll(matcher.group(4), matcher.group(5));
                }

                event.getOriginalEvent().respondWith(nick + " meant: " + msg);
            } else {
                this.cache.put(nick, msg);
            }
        } catch(Exception e) {}
    }

    @Override
    protected void register() {
        EventHandler.register(this);
    }
}
