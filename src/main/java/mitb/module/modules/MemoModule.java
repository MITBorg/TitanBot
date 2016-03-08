package mitb.module.modules;

import com.google.common.base.Joiner;
import mitb.TitanBot;
import mitb.event.EventHandler;
import mitb.event.Listener;
import mitb.event.events.CommandEvent;
import mitb.event.events.JoinEvent;
import mitb.module.CommandModule;
import mitb.util.PIrcBotXHelper;
import org.pircbotx.User;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A way of sending memos to users when they next login.
 */
public final class MemoModule extends CommandModule {

    @Override
    public String[] getCommands() {
        return new String[] { "memo", "memoserv" };
    }

    @Override
    public void getHelp(CommandEvent event) {
        TitanBot.sendReply(event.getSource(), "Syntax: " + event.getArgs()[0] + " add (target_nick) (msg) | "
                + event.getArgs()[0] + " view (sender_nick) | "  + event.getArgs()[0]
                + " pending | Note: You must be using a verified IRC nickname to use this module.");
    }

    @Override
    public void onCommand(CommandEvent event) {
        if (event.getArgs().length == 0)
            return;

        // Parsing command
        String callerNick = PIrcBotXHelper.getNick(event.getSource());

        // Invalid caller/source event
        if (callerNick == null) {
            return;
        }

        // Only identified users can use memo
        User callerUser = PIrcBotXHelper.getUser(event.getSource());

        // Invalid caller/source event
        if (callerUser == null) {
            return;
        }

        // Disallow access by unverified users
        if (!callerUser.isVerified()) {
            TitanBot.sendReply(event.getSource(), "You must be identified for your nick to use memo.");
            return;
        }

        // Handle command
        String cmd = event.getArgs()[0].toLowerCase();

        if (cmd.equals("add") && event.getArgs().length >= 3) { // Adding a new message
            addMessage(event, callerNick);
        } else if (cmd.equals("view") && event.getArgs().length >= 2) { // Viewing a message
            viewMessage(event, callerNick);
        } else if (cmd.equals("pending")) { // Seeing pending messages
            sendPending(event, callerNick);
        }
    }

    /**
     * Sends a list of users who have pending memos for the caller.
     * @param event
     * @param callerNick
     */
    private void sendPending(CommandEvent event, String callerNick) {
        String msg = getPendingMessageSenders(getMessageSenders(callerNick));

        // Reply
        String output = msg == null ? "There are no memos for you." : "There are memo for you from: " + msg;
        TitanBot.sendReply(event.getSource(), output);
    }

    /**
     * Views a message from a user.
     * @param event
     * @param callerNick
     */
    private void viewMessage(CommandEvent event, String callerNick) {
        String senderNick = event.getArgs()[1].toLowerCase();
        String memoMessage = getMessage(senderNick, callerNick);

        // Attempting to view message
        if (memoMessage != null) {
            TitanBot.sendReply(event.getSource(), "[" + senderNick + "] " + memoMessage);
            deleteMessage(senderNick, callerNick);
        } else {
            TitanBot.sendReply(event.getSource(), "There is no message for you from: " + senderNick);
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
        if (!targetNick.equals(callerNick) && !targetNick.equalsIgnoreCase(event.getSource().getBot().getNick())) {
            // Add message to database
            updateMessage(callerNick, targetNick, msg);

            // Try to inform target, if they are online
            event.getSource().getBot().send().message(targetNick, "There is a new memo for you from: " + callerNick);

            // Reply
            TitanBot.sendReply(event.getSource(), "Your message to " + targetNick + " was recorded.");
        } else {
            TitanBot.sendReply(event.getSource(), "You cant queue a message to yourself/the bot!");
        }
    }

    /**
     * When a user joins a channel we are in, we tell them about any memos they might have pending for them.
     * @param event
     */
    @Listener
    public void onJoin(JoinEvent event) {
        org.pircbotx.hooks.events.JoinEvent evt = event.getSource();
        String targetNick = evt.getUser().getNick().toLowerCase();

        // Sending joined users a list of their pending memos
        if (!evt.getUser().getNick().equals(evt.getBot().getNick())) {
            String pendingMessageSenders = getPendingMessageSenders(getMessageSenders(targetNick));

            // Send message if memos are available
            if (pendingMessageSenders != null) {
                evt.getBot().send().message(targetNick, "There are memos for you from: " + pendingMessageSenders);
            }
        }
    }

    /**
     * Constructs a message for a user about who has pending messages for them, with who they are from.
     * @paran senders
     */
    private String getPendingMessageSenders(List<String> senders) {
        if (senders.size() == 0)
            return null;

        // Construct message
        StringBuilder sb = new StringBuilder();

        for (String sender : senders) {
            sb.append(sender).append(" ");
        }
        return sb.toString();
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
    private String getMessage(String senderNick, String targetNick) {
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
    private List<String> getMessageSenders(String targetNick) {
        try {
            List<String> l = new ArrayList<>();

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
