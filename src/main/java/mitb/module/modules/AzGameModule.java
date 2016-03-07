package mitb.module.modules;

import mitb.TitanBot;
import mitb.event.EventHandler;
import mitb.event.Listener;
import mitb.event.events.CommandEvent;
import mitb.event.events.MessageEvent;
import mitb.module.CommandModule;
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
     * Game sessions, each channel name gets a unique one.
     */
    private final HashMap<String, GameSession> gameSessions = new HashMap<>();
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
                + " stop | " + event.getArgs()[0] + " range | Type single words in channel");
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
        String channelName;

        if (event.getSource() instanceof org.pircbotx.hooks.events.MessageEvent) {
            channelName = ((org.pircbotx.hooks.events.MessageEvent)event.getSource()).getChannelSource();
        } else {
            return; // private message to bot, so we ignore it
        }

        // Process command
        if (cmd.equals("start")) { // starts a game session for a channel
            if (event.getSource().getBot().getUserChannelDao().containsChannel(channelName)) {
                GameSession session = gameSessions.get(channelName);

                if (session == null) { // no game session, start session
                    String word = getRandomWord();
                    gameSessions.put(channelName, new GameSession(word));
                    sendUpdate(event.getSource().getBot(), channelName);
                    TitanBot.LOGGER.info("A-Z instance started for " + channelName + " with solution: " + word);
                } else { // game in progress, nothing to do
                    TitanBot.sendReply(event.getSource(), "A game session for this channel already exists, try checking the progress.");
                }
            } else {
                TitanBot.sendReply(event.getSource(), "I am not in that channel and so cannot create a game session for it.");
            }
        } else if (cmd.equals("stop")) { // stops a game session for a channel
            GameSession session = gameSessions.get(channelName);

            if (session != null) {
                gameSessions.remove(channelName);
                TitanBot.sendReply(event.getSource(), "Game session ended, the word was: " + session.getWord());
            }
        } else if (cmd.equals("range")) { // displays the word range for a channel
            GameSession session = gameSessions.get(channelName);

            if (session == null) { // not existent
                TitanBot.sendReply(event.getSource(), "No game session in progress for this channel. Try creating one.");
            } else { // game in progress
                sendUpdate(event.getSource().getBot(), channelName);
            }
        }
    }

    @Listener
    public void onMessage(MessageEvent event) {
        if (gameSessions.size() == 0)
            return;

        // Check if this channel has a game session
        String channelName = event.getSource().getChannel().getName();

        if (!gameSessions.containsKey(channelName))
            return;

        // Process message
        GameSession session = gameSessions.get(channelName);
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
            gameSessions.remove(channelName);
        } else if (lbc < 0 && wc > 0) { // if new lower bound
            session.setLowerBoundWord(word);
            sendUpdate(event.getSource().getBot(), channelName);
        } else if (upc > 0 && wc < 0) { // if new upper bound
            session.setUpperBoundWord(word);
            sendUpdate(event.getSource().getBot(), channelName);
        }
    }

    @Override
    public void register() {
        EventHandler.register(this);
    }

    private void sendUpdate(PircBotX bot, String channelName) {
        GameSession session = gameSessions.get(channelName);
        bot.sendRaw().rawLine("PRIVMSG " + channelName + " :A-Z update: " + session.getLowerBoundWord() + "-"
                + session.getUpperBoundWord()); // XXX refactor this garbage
    }

    /**
     * Gets a random word from the word list, avoiding the endpoints.
     * @return
     */
    public String getRandomWord() {
        return StringHelper.wordList[RANDOM.nextInt(StringHelper.wordList.length - 2) + 1]; // XXX test range
    }


    /**
     * An a-z game session.
     */
    static final class GameSession {

        private String lowerBoundWord = StringHelper.wordList[0];
        private String upperBoundWord = StringHelper.wordList[StringHelper.wordList.length - 1];
        private String word;

        public GameSession(String word) {
            this.word = word;
        }

        public String getLowerBoundWord() {
            return lowerBoundWord;
        }

        public String getUpperBoundWord() {
            return upperBoundWord;
        }

        public String getWord() {
            return word;
        }

        public void setLowerBoundWord(String lowerBoundWord) {
            this.lowerBoundWord = lowerBoundWord;
        }

        public void setUpperBoundWord(String upperBoundWord) {
            this.upperBoundWord = upperBoundWord;
        }
    }
}