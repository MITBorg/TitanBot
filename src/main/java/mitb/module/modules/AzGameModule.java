package mitb.module.modules;

import mitb.TitanBot;
import mitb.event.EventHandler;
import mitb.event.Listener;
import mitb.event.events.CommandEvent;
import mitb.event.events.MessageEvent;
import mitb.module.CommandModule;
import mitb.util.PIrcBotXHelper;
import mitb.util.Properties;
import mitb.util.StringHelper;
import org.pircbotx.PircBotX;

import java.util.HashMap;
import java.util.Random;

/**
 * A word guessing game, given a range called a-z.
 */
public final class AzGameModule extends CommandModule {

    // TODO massive refactor
    // TODO kill instances in channels where we are removed (kicked/banned) from
    // TODO add rankings and a points system
    // TODO list active channels cmd?
    /**
     * If the games module is restricted to a channel, 'no' if not and channel name if so.
     */
    private static final String RESTRICTED_CHANNEL = Properties.getValue("games.restrict_channel");
    /**
     * Game sessions, each channel name gets a unique one.
     */
    private final HashMap<String, AzGameModule.GameSession> gameSessions = new HashMap<>();
    /**
     * Random instance, to generate words to challenge for.
     */
    private static final Random RANDOM = new Random();

    @Override
    public String[] getCommands() {
        return new String[]{"az", "a-z"};
    }

    @Override
    public void getHelp(CommandEvent event) {
        TitanBot.sendReply(event.getSource(), "Syntax: " + event.getArgs()[0] + " start | " + event.getArgs()[0]
                + " stop | " + event.getArgs()[0] + " range | Type single words in channel"
                + (AzGameModule.RESTRICTED_CHANNEL.equalsIgnoreCase("NO") ? "" : " | Restricted to " + AzGameModule
                .RESTRICTED_CHANNEL));
    }

    /**
     * Reply with an example response on command.
     *
     * @param event
     */
    @Override
    public void onCommand(CommandEvent event) {
        if (event.getArgs().length == 0)
            return;

        // Checking if word list was loaded
        if (!StringHelper.isWordListLoaded()) {
            TitanBot.sendReply(event.getSource(), "No word list was loaded.");
            return;
        }

        // Parse command
        String cmd = event.getArgs()[0];
        String channelName = PIrcBotXHelper.getChannelName(event.getSource());

        // Ignore non-channel messages
        if (channelName == null)
            return;

        // Process command
        AzGameModule.GameSession session = this.gameSessions.get(channelName);

        if (cmd.equals("start")) { // starts a game session for a channel
            this.startSession(channelName, session, event);
        } else if (cmd.equals("stop")) { // stops a game session for a channel
            this.stopSession(channelName, session, event);
        } else if (cmd.equals("range")) { // displays the word range for a channel
            this.updateRange(channelName, session, event);
        }
    }

    private void startSession(String channelName, AzGameModule.GameSession session, CommandEvent event) {
        if (event.getSource().getBot().getUserChannelDao().containsChannel(channelName)) {
            if (session == null) { // no game session, start session
                // check if allowed to run
                if (!this.isAllowed(channelName)) {
                    TitanBot.sendReply(event.getSource(), "This module is restricted to " + AzGameModule
                            .RESTRICTED_CHANNEL);
                    return;
                }

                // regular game session create logic
                String word = this.getRandomWord();
                this.gameSessions.put(channelName, new AzGameModule.GameSession(word));
                this.sendUpdate(event.getSource().getBot(), channelName);
                TitanBot.getLogger().info("A-Z instance started for " + channelName + " with solution: " + word);
            } else { // game in progress, nothing to do
                TitanBot.sendReply(event.getSource(), "A game session for this channel already exists, try checking the progress.");
            }
        } else {
            TitanBot.sendReply(event.getSource(), "I am not in that channel and so cannot create a game session for it.");
        }
    }

    private void updateRange(String channelName, AzGameModule.GameSession session, CommandEvent event) {
        if (session == null) { // not existent
            TitanBot.sendReply(event.getSource(), "No game session in progress for this channel. Try creating one.");
        } else { // game in progress
            this.sendUpdate(event.getSource().getBot(), channelName);
        }
    }

    private void stopSession(String channelName, AzGameModule.GameSession session, CommandEvent event) {
        if (session != null) {
            this.gameSessions.remove(channelName);
            TitanBot.sendReply(event.getSource(), "Game session ended, the word was: " + session.getWord());
        } else {
            TitanBot.sendReply(event.getSource(), "No game session to end in " + channelName);
        }
    }

    /**
     * If this module is allowed to start game sessions in the given channel.
     * @param channelName
     * @return
     */
    private boolean isAllowed(String channelName) {
        return AzGameModule.RESTRICTED_CHANNEL.equalsIgnoreCase("NO") || AzGameModule.RESTRICTED_CHANNEL.equalsIgnoreCase(channelName);
    }

    @Listener
    public void onMessage(MessageEvent event) {
        if (this.gameSessions.size() == 0)
            return;

        // Check if this channel has a game session
        String channelName = event.getSource().getChannel().getName();

        if (!this.gameSessions.containsKey(channelName))
            return;

        // Process message
        AzGameModule.GameSession session = this.gameSessions.get(channelName);
        String word = event.getSource().getMessage().toLowerCase();

        // Check if the message is a valid word
        if (!StringHelper.isWord(word))
            return;

        // Now try to update the game session
        int lbc = session.getLowerBoundWord().compareTo(word);
        int upc = session.getUpperBoundWord().compareTo(word);
        int wc = session.getWord().compareTo(word);

        if (session.getWord().equals(word)) { // if correct guess
            TitanBot.sendReply(event.getSource(), "This game was won by "
                    + event.getSource().getUser().getNick() + " who guessed the word " + word);
            this.gameSessions.remove(channelName);
        } else if (lbc < 0 && wc > 0) { // if new lower bound
            session.setLowerBoundWord(word);
            this.sendUpdate(event.getSource().getBot(), channelName);
        } else if (upc > 0 && wc < 0) { // if new upper bound
            session.setUpperBoundWord(word);
            this.sendUpdate(event.getSource().getBot(), channelName);
        }
    }

    @Override
    public void register() {
        EventHandler.register(this);
    }

    private void sendUpdate(PircBotX bot, String channelName) {
        AzGameModule.GameSession session = this.gameSessions.get(channelName);
        String state = "[A-Z Update] " + session.getLowerBoundWord() + "-" + session.getUpperBoundWord();
        bot.send().message(channelName, state);
    }

    /**
     * Gets a random word from the word list, avoiding the endpoints.
     * @return
     */
    public String getRandomWord() {
        return StringHelper.wordList[AzGameModule.RANDOM.nextInt(StringHelper.wordList.length - 2) + 1]; // XXX test range
    }


    /**
     * An a-z game session.
     */
    static final class GameSession {

        private String lowerBoundWord = StringHelper.wordList[0];
        private String upperBoundWord = StringHelper.wordList[StringHelper.wordList.length - 1];
        private final String word;

        public GameSession(String word) {
            this.word = word;
        }

        public String getLowerBoundWord() {
            return this.lowerBoundWord;
        }

        public String getUpperBoundWord() {
            return this.upperBoundWord;
        }

        public String getWord() {
            return this.word;
        }

        public void setLowerBoundWord(String lowerBoundWord) {
            this.lowerBoundWord = lowerBoundWord;
        }

        public void setUpperBoundWord(String upperBoundWord) {
            this.upperBoundWord = upperBoundWord;
        }
    }
}
