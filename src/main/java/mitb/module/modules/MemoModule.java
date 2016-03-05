package mitb.module.modules;

import com.google.common.base.Joiner;
import mitb.TitanBot;
import mitb.event.events.CommandEvent;
import mitb.event.events.JoinEvent;
import mitb.module.CommandModule;
import org.pircbotx.hooks.events.MessageEvent;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

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
            String targetNick = event.getArgs()[1].toLowerCase();
            String msg = Joiner.on(" ").join(Arrays.copyOfRange(event.getArgs(), 2, event.getArgs().length));

            // Ensuring length is valid
            if (msg.length() > 350)
                msg = msg.substring(0, 349);

            // Adding/updating message
            if (!targetNick.equals(callerNick)) {
                updateMessage(callerNick, targetNick, msg);
                TitanBot.sendReply(event.getOriginalEvent(), "Your message to " + targetNick + " was recorded.");
            } else {
                TitanBot.sendReply(event.getOriginalEvent(), "You cant queue a message to yourself!");
            }
        } else if (cmd.equals("view") && event.getArgs().length >= 2) { // Viewing a message
            String senderNick = event.getArgs()[1].toLowerCase();
            String msg = getMessage(senderNick, callerNick);

            // Attempting to view message
            if (msg != null) {
                deleteMessage(senderNick, callerNick);
                TitanBot.sendReply(event.getOriginalEvent(), "[" + senderNick + "] " + msg); // TODO make this a notice
            } else {
                TitanBot.sendReply(event.getOriginalEvent(), "There is no message for you from: " + senderNick);
            }
        }
    }

    public void onJoin(JoinEvent event) {
        System.out.println("test");
    }

    @Override
    protected void register() {

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
     * If the user with the given nickname has any messages in the system.
     * @param targetNick
     */
    public boolean hasMessages(String targetNick) {
        try {
            PreparedStatement statement = TitanBot.databaseConnection.prepareStatement(
                    "SELECT message FROM memo WHERE target_nick = ?"
            );
            statement.setString(1, targetNick);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.getFetchSize() > 0;
        } catch (SQLException e) {
            return false;
        }
    }
}
