package mitb.event.events;

import mitb.event.Event;
import mitb.event.ProxyEvent;
import org.pircbotx.hooks.types.GenericEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

import java.util.Arrays;

/**
 * This event is fired whenever a command is sent to the bot.
 */
public final class CommandEvent implements Event, ProxyEvent {
    private final String msg;
    private final GenericMessageEvent event;

    public CommandEvent(String msg, GenericMessageEvent sourceEvent) {
        this.msg = msg;
        this.event = sourceEvent;
    }

    /**
     * Get the command which was passed sent.
     *
     * @return command sent by the user
     */
    public String getCommand() {
        int spaceIdx = msg.indexOf(" ");
        return spaceIdx == -1 ? msg : msg.substring(0, spaceIdx);
    }

    /**
     * Get the arguments passed to this command.
     *
     * @return split arguments
     */
    public String[] getArgs() {
        String[] args = msg.split(" ");

        if (args.length < 2) {
            return new String[]{};
        }
        return Arrays.copyOfRange(args, 1, args.length);
    }

    @Override
    public GenericEvent getSource() {
        return this.event;
    }
}
