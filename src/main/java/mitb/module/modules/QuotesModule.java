package mitb.module.modules;

import com.google.common.base.Joiner;
import mitb.TitanBot;
import mitb.event.events.CommandEvent;
import mitb.module.CommandModule;
import mitb.util.StringHelper;
import org.pircbotx.hooks.events.MessageEvent;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * A quote database.
 */
public final class QuotesModule extends CommandModule {
    @Override
    public String[] getCommands() {
        return new String[]{"quotes", "quote"};
    }

    @Override
    public void getHelp(CommandEvent event) {
        TitanBot.sendReply(event.getOriginalEvent(), "Syntax: " + event.getArgs()[0] + " add (quote) | " + event.getArgs()[0]+ " view (number)");
    }

    /**
     * Reply with an example response on command.
     *
     * @param event
     */
    @Override
    public void onCommand(CommandEvent event) {
        // TODO add quote listing command, add quote amount count, add get quote by name
        if (event.getArgs().length < 1)
            return;

        String callerNick = ((MessageEvent)event.getOriginalEvent()).getUser().getNick().toLowerCase();
        String cmd = event.getArgs()[0];

        if (cmd.equals("add") && event.getArgs().length > 1) {
            addQuote(event, callerNick);
        } else if (cmd.equals("view") && event.getArgs().length > 1) {
            viewQuote(event);
        }
    }

    /**
     * Adds a quote to the database.
     * @param event
     * @param callerNick
     */
    private void addQuote(CommandEvent event, String callerNick) {
        // Construct quote
        String[] args = Arrays.copyOfRange(event.getArgs(), 1, event.getArgs().length);
        String quote = Joiner.on(" ").join(args);

        // Ensure length is valid
        if (quote.length() > 350)
            quote = quote.substring(0, 350 - 1 - 3) + "...";

        // Adding message
        insertQuote(callerNick, quote);
        TitanBot.sendReply(event.getOriginalEvent(), "Your quote has been recorded.");
    }

    /**
     * Fetches and displays a quote from the database.
     * @param event
     */
    private void viewQuote(CommandEvent event) {
        // Parse quote id
        int quoteId;

        try {
            quoteId = Integer.parseInt(event.getArgs()[1]);
        } catch (NumberFormatException ex) {
            TitanBot.sendReply(event.getOriginalEvent(), "Invalid quote id.");
            return;
        }

        // Display message if possible
        String msg = fetchQuote(quoteId);
        TitanBot.sendReply(event.getOriginalEvent(), msg);
    }

    /**
     * Gets the quote with the given id out of the database in a formatted manner.
     * @param quoteId
     * @return
     */
    private String fetchQuote(int quoteId) {
        try {
            PreparedStatement statement = TitanBot.databaseConnection.prepareStatement(
                    "SELECT creator_nick, quote FROM quotes WHERE id = ?"
            );
            statement.setInt(1, quoteId);
            ResultSet resultSet = statement.executeQuery();
            return StringHelper.wrapBold(resultSet.getString("creator_nick")) + ": " + resultSet.getString("quote");
        } catch (SQLException e) {
            return "No quote found with id: " + quoteId;
        }
    }

    /**
     * Adds a quote to the database.
     * @param creatorNick
     * @param quote
     */
    private void insertQuote(String creatorNick, String quote) {
        try {
            PreparedStatement statement = TitanBot.databaseConnection.prepareStatement(
                    "INSERT INTO quotes (id, creator_nick, quote) VALUES (NULL, ?, ?)"
            );
            statement.setString(1, creatorNick);
            statement.setString(2, quote);
            statement.executeUpdate();
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void register() {

    }
}
