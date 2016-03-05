package mitb.module.modules;

import com.google.common.base.Joiner;
import mitb.TitanBot;
import mitb.event.EventHandler;
import mitb.event.Listener;
import mitb.event.events.CommandEvent;
import mitb.event.events.JoinEvent;
import mitb.module.CommandModule;
import org.pircbotx.hooks.events.MessageEvent;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A way of sending memos to users when they next login.
 */
public class MemoModule extends CommandModule {

    @Override
    public String[] getCommands() {
        return new String[] { "memo", "reminder" };
    }

    @Override
    public void getHelp(CommandEvent event) {
        TitanBot.sendReply(event.getOriginalEvent(), "Syntax: " + event.getArgs()[0] + " [add] [to_nick] [msg]; "
                + event.getArgs()[0] + " [view] [from_nick]");
    }

    @Override
    public void onCommand(CommandEvent event) {
        // Parsing command
        String callerNick = ((MessageEvent)event.getOriginalEvent()).getUser().getNick().toLowerCase();
        String cmd = event.getArgs()[0];

        if (cmd.equals("add") && event.getArgs().length >= 3) { // Adding a new message
            addMessage(event, callerNick);
        } else if (cmd.equals("view") && event.getArgs().length >= 2) { // Viewing a message
            viewMessage(event, callerNick);
        }
    }

    /**
     * Views a message from a user.
     * @param event
     * @param callerNick
     */
    private void viewMessage(CommandEvent event, String callerNick) {
        String senderNick = event.getArgs()[1].toLowerCase();
        String msg = getMessage(senderNick, callerNick);

        // Attempting to view message
        if (msg != null) {
            event.getOriginalEvent().getBot().sendRaw().rawLine("PRIVMSG " + callerNick + " :[" + senderNick + "] " + msg); // XXX make this nice
            deleteMessage(senderNick, callerNick);
        } else {
            TitanBot.sendReply(event.getOriginalEvent(), "There is no message for you from: " + senderNick);
        }
    }

    /**
     * Adds a message for a user.
     * @param event
     * @param callerNick
     */
    private void addMessage(CommandEvent event, String callerNick) {
        String targetNick = event.getArgs()[1].toLowerCase();
        String msg = Joiner.on(" ").join(Arrays.copyOfRange(event.getArgs(), 2, event.getArgs().length));

        // Ensuring length is valid
        if (msg.length() > 250)
            msg = msg.substring(0, 250 - 1 - 3) + "...";

        // Adding/updating message
        // TODO dont allow adding messages for present users
        if (!targetNick.equals(callerNick)) {
            updateMessage(callerNick, targetNick, msg);
            TitanBot.sendReply(event.getOriginalEvent(), "Your message to " + targetNick + " was recorded.");
        } else {
            TitanBot.sendReply(event.getOriginalEvent(), "You cant queue a message to yourself!");
        }
    }

    @Listener
    public void onJoin(JoinEvent event) {
        org.pircbotx.hooks.events.JoinEvent evt = event.getOriginalEvent();
        String targetNick = evt.getUser().getNick().toLowerCase();

        // Sending joined users a list of their pending memos
        if (!evt.getUser().getNick().equals(evt.getBot().getNick())) {
            sendPendingMessages(evt, targetNick, getMessageSenders(targetNick));
        }
    }

    /**
     * Tells a user about who has pending messages for them.
     * @param event
     * @param targetNick The user who we are sending this information to/for.
     * @paran senders
     */
    private void sendPendingMessages(org.pircbotx.hooks.events.JoinEvent event, String targetNick, List<String> senders) {
        if (senders.size() == 0)
            return;

        // Construct message
        StringBuilder sb = new StringBuilder();

        for (String sender : senders) {
            sb.append(sender).append(" ");
        }

        // Send message
        event.getBot().sendRaw().rawLine("PRIVMSG " + targetNick + " :There are memos for you from: " + sb.toString());  // XXX make this nice
    }

    @Override
    protected void register() {
        EventHandler.register(this);
    }

    /**
     * Updates a stored message.
     * @param fromNick
     * @param targetNick
     * @param message
     */
    private void updateMessage(String fromNick, String targetNick, String message) {
        try {
            PreparedStatement statement = TitanBot.databaseConnection.prepareStatement(
                    "INSERT OR REPLACE INTO memo (id, target_nick, sender_nick, message) VALUES ((SELECT id FROM memo WHERE target_nick = ? AND sender_nick = ?), ?, ?, ?)"
            );
            statement.setString(1, targetNick);
            statement.setString(2, fromNick);
            statement.setString(3, targetNick);
            statement.setString(4, fromNick);
            statement.setString(5, message);
            statement.executeUpdate();
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Deletes a stored message.
     * @param senderNick
     * @param targetNick
     */
    private void deleteMessage(String senderNick, String targetNick) {
        try {
            PreparedStatement statement = TitanBot.databaseConnection.prepareStatement(
                    "DELETE FROM memo WHERE target_nick = ? AND sender_nick = ?"
            );
            statement.setString(1, targetNick);
            statement.setString(2, senderNick);
            statement.executeUpdate();
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the stored message for a user from another user
     * @param senderNick
     * @param targetNick
     * @return
     */
    public String getMessage(String senderNick, String targetNick) {
        try {
            PreparedStatement statement = TitanBot.databaseConnection.prepareStatement(
                    "SELECT message FROM memo WHERE sender_nick = ? AND target_nick = ?"
            );
            statement.setString(1, senderNick);
            statement.setString(2, targetNick);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.getString("message");
        } catch (SQLException e) {
            return null;
        }
    }

    /**
     * A list of users who have messages for this user.
     * @param targetNick
     */
    public List<String> getMessageSenders(String targetNick) {
        try {
            List<String> l = new ArrayList();

            PreparedStatement statement = TitanBot.databaseConnection.prepareStatement(
                    "SELECT sender_nick FROM memo WHERE target_nick = ?"
            );
            statement.setString(1, targetNick);
            ResultSet resultSet = statement.executeQuery();

            // Add results to list
            while (resultSet.next()) {
                l.add(resultSet.getString("sender_nick"));
            }
            return l;
        } catch (SQLException e) {
            return null;
        }
    }
}
