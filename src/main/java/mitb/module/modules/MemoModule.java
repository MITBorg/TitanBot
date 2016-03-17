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
                + " pending | clear | Note: You must be using a verified IRC nickname to use this module.");
    }

    @Override
    public void onCommand(CommandEvent commandEvent) {
        if (commandEvent.getArgs().length == 0) {
            return;
        }

        User callerUser = PIrcBotXHelper.getUser(commandEvent.getSource());

        // Invalid caller/source event
        if (callerUser == null) {
            return;
        }

        // Parsing command
        String callerNick = callerUser.getNick().toLowerCase();

        // Invalid caller/source event
        if (callerNick == null) {
            return;
        }

        // Disallow access by unverified users
        if (!callerUser.isVerified()) {
            TitanBot.sendReply(commandEvent.getSource(), "You must be identified for your nick to use memo.");
            return;
        }

        // Handle command
        String cmd = commandEvent.getArgs()[0].toLowerCase();

        if (cmd.equals("add") && (commandEvent.getArgs().length >= 3)) { // Adding a new message
            this.addMessage(commandEvent, callerNick);
        } else if (cmd.equals("view") && (commandEvent.getArgs().length >= 2)) { // Viewing a message
            this.viewMessage(commandEvent, callerNick);
        } else if (cmd.equals("pending")) { // Seeing pending messages
            this.viewPending(commandEvent, callerNick);
        } else if (cmd.equals("clear")) {
            this.clearPending(commandEvent, callerNick);
        }
    }

    /**
     * Sends a list of users who have pending memos for the caller.
     *
     * @param event command event triggering this method call
     * @param callerNick nick of the person calling this method
     */
    private void viewPending(CommandEvent event, String callerNick) {
        String msg = MemoModule.getPendingMessageSenders(MemoModule.getMessageSenders(callerNick));

        // Reply
        String output = (msg == null) ? "There are no memos for you." : ("There are memo for you from: " + msg);
        TitanBot.sendReply(event.getSource(), output);
    }

    /**
     * Views a message from a user.
     *
     * @param event command event triggering this method call
     * @param callerNick nick of the person calling this method
     */
    private void viewMessage(CommandEvent event, String callerNick) {
        String senderNick = event.getArgs()[1].toLowerCase();
        String memoMessage = MemoModule.getMessage(senderNick, callerNick);

        // Attempting to view message
        if (memoMessage != null) {
            TitanBot.sendReply(event.getSource(), '[' + senderNick + "] " + memoMessage);
            MemoModule.deleteMessage(senderNick, callerNick);
        } else {
            TitanBot.sendReply(event.getSource(), "There is no message for you from: " + senderNick);
        }
    }

    /**
     * Delete all pending memos for a user
     *
     * @param event command event triggering this method call
     * @param callerNick nick of the person calling this method
     */
    private void clearPending(CommandEvent event,  String callerNick) {
        try {
            PreparedStatement statement = TitanBot.getDatabaseConnection().prepareStatement(
                    "DELETE FROM memo WHERE target_nick = ?"
            );
            statement.setString(1, callerNick);
            statement.executeUpdate();
        } catch(SQLException e) {
            TitanBot.getLogger().error("There was an error while deleting a memo.", e);
        }
    }

    /**
     * Adds a message for a user.
     *
     * @param event command event triggering this method call
     * @param callerNick nick of the person calling this method
     */
    private void addMessage(CommandEvent event, String callerNick) {
        String targetNick = event.getArgs()[1].toLowerCase();
        String msg = Joiner.on(" ").join(Arrays.copyOfRange(event.getArgs(), 2, event.getArgs().length));

        // Ensuring length is valid
        if (msg.length() > 250) {
            msg = msg.substring(0, 250 - 1 - 3) + "...";
        }

        // Adding/updating message
        if (!targetNick.equals(callerNick) && !targetNick.equalsIgnoreCase(event.getSource().getBot().getNick())) {
            // Add message to database
            MemoModule.updateMessage(callerNick, targetNick, msg);

            // Try to inform target, if they are online
            event.getSource().getBot().send().message(targetNick, "There is a new memo for you from: " + callerNick);

            // Reply
            TitanBot.sendReply(event.getSource(), "Your message to " + targetNick + " was recorded.");
        } else {
            TitanBot.sendReply(event.getSource(), "You cant queue a message to yourself or the bot!");
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
            String pendingMessageSenders = MemoModule.getPendingMessageSenders(MemoModule.getMessageSenders(targetNick));

            // Send message if memos are available
            if (pendingMessageSenders != null) {
                evt.getBot().send().message(targetNick, "There are memos for you from: " + pendingMessageSenders);
            }
        }
    }

    /**
     * Constructs a message for a user about who has pending messages for them, with who they are from.
     *
     * @param senders nicks who have sent this user message
     */
    private static String getPendingMessageSenders(List<String> senders) {
        if (senders.isEmpty()) {
            return null;
        }

        // Construct message
        StringBuilder sb = new StringBuilder();

        for (String sender : senders) {
            sb.append(sender).append(' ');
        }
        return sb.toString();
    }

    @Override
    protected void register() {
        EventHandler.register(this);
    }

    /**
     * Updates a stored message.
     *
     * @param fromNick nick of the person storing this memo
     * @param targetNick nick of the person the memo should go to
     * @param message message to queue for the user
     */
    private static void updateMessage(String fromNick, String targetNick, String message) {
        try {
            PreparedStatement statement = TitanBot.getDatabaseConnection().prepareStatement(
                    "INSERT OR REPLACE INTO memo (id, target_nick, sender_nick, message) VALUES ((SELECT id FROM memo WHERE target_nick = ? AND sender_nick = ?), ?, ?, ?)"
            );
            statement.setString(1, targetNick);
            statement.setString(2, fromNick);
            statement.setString(3, targetNick);
            statement.setString(4, fromNick);
            statement.setString(5, message);
            statement.executeUpdate();
        } catch(SQLException e) {
            TitanBot.getLogger().error("There was an error while queuing a memo.", e);
        }
    }

    /**
     * Deletes a stored message.
     *
     * @param senderNick nick of the user deleting this memo
     * @param targetNick nick of the user this memo was for
     */
    private static void deleteMessage(String senderNick, String targetNick) {
        try {
            PreparedStatement statement = TitanBot.getDatabaseConnection().prepareStatement(
                    "DELETE FROM memo WHERE target_nick = ? AND sender_nick = ?"
            );
            statement.setString(1, targetNick);
            statement.setString(2, senderNick);
            statement.executeUpdate();
        } catch(SQLException e) {
            TitanBot.getLogger().error("There was an error while deleting a memo.", e);
        }
    }

    /**
     * Gets the stored message for a user from another user
     * @param senderNick nick of who sent the message
     * @param targetNick nick of who the memo was for
     * @return queued memo
     */
    private static String getMessage(String senderNick, String targetNick) {
        try {
            PreparedStatement statement = TitanBot.getDatabaseConnection().prepareStatement(
                    "SELECT message FROM memo WHERE sender_nick = ? AND target_nick = ?"
            );
            statement.setString(1, senderNick);
            statement.setString(2, targetNick);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.getString("message");
        } catch (SQLException e) {
            TitanBot.getLogger().error("There was an error while getting a memo.", e);
            return null;
        }
    }

    /**
     * A list of users who have messages for this user.
     *
     * @param targetNick nick to get queued memo nicks for
     */
    private static List<String> getMessageSenders(String targetNick) {
        try {
            List<String> l = new ArrayList<>();

            PreparedStatement statement = TitanBot.getDatabaseConnection().prepareStatement(
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
            TitanBot.getLogger().error("There was an error while getting memo senders.", e);
            return null;
        }
    }
}
