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

    public CommandEvent(String msg, GenericMessageEvent event) {
        this.msg = msg;
        this.event = event;
    }

    /**
     * Get the command which was passed sent.
     *
     * @return command sent by the user
     */
    public String getCommand() {
        return msg.split(" ")[0];
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
