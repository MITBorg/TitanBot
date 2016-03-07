package mitb.module.modules;

import mitb.TitanBot;
import mitb.event.EventHandler;
import mitb.event.Listener;
import mitb.event.events.CommandEvent;
import mitb.event.events.MessageEvent;
import mitb.module.CommandModule;
import mitb.util.Properties;
import org.pircbotx.PircBotX;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Arrays;
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
    private static final Random random = new Random();
    /**
     * A list of the words in use.
     */
    private static String[] wordList;
    /**
     * If the word list was loaded or not.
     */
    private static boolean wordListLoaded = false;

    @Override
    public String[] getCommands() {
        return new String[]{"az", "a-z"};
    }

    @Override
    public void getHelp(CommandEvent event) {
        TitanBot.sendReply(event.getOriginalEvent(), "Syntax: az start | az stop | az range | Type single words in channel.");
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
        if (!wordListLoaded || wordList == null || wordList.length == 0) {
            TitanBot.sendReply(event.getOriginalEvent(), "No word list was loaded for a-z.");
            return;
        }

        // Process command
        String cmd = event.getArgs()[0];
        String channel;

        if (event.getOriginalEvent() instanceof org.pircbotx.hooks.events.MessageEvent) {
            channel = ((org.pircbotx.hooks.events.MessageEvent)event.getOriginalEvent()).getChannelSource();
        } else {
            return; // private message to bot, so we ignore it
        }

        if (cmd.equals("start")) { // starts a game session for a channel
            if (event.getOriginalEvent().getBot().getUserChannelDao().containsChannel(channel)) {
                GameSession session = gameSessions.get(channel);

                if (session == null) { // no game session, start session
                    String word = getRandomWord();
                    gameSessions.put(channel, new GameSession(word));
                    sendUpdate(event.getOriginalEvent().getBot(), channel);
                    TitanBot.LOGGER.info("A-Z instance for " + channel + " with solution: " + word);
                } else { // game in progress, nothing to do
                    TitanBot.sendReply(event.getOriginalEvent(), "A game session for this channel already exists, try checking the progress.");
                }
            } else {
                TitanBot.sendReply(event.getOriginalEvent(), "I am not in that channel and so cannot create a game session for it.");
            }
        } else if (cmd.equals("stop")) { // stops a game session for a channel
            GameSession session = gameSessions.get(channel);

            if (session != null) {
                gameSessions.remove(channel);
                TitanBot.sendReply(event.getOriginalEvent(), "Game session ended, the word was: " + session.getWord());
            }
        } else if (cmd.equals("range")) { // displays the word range for a channel
            GameSession session = gameSessions.get(channel);

            if (session == null) { // not existent
                TitanBot.sendReply(event.getOriginalEvent(), "No game session in progress for this channel. Try creating one.");
            } else { // game in progress
                sendUpdate(event.getOriginalEvent().getBot(), channel);
            }
        }
    }

    @Listener
    public void onMessage(MessageEvent event) {
        if (gameSessions.size() == 0)
            return;

        // Check if this channel has a game session
        String channel = event.getOriginalEvent().getChannel().getName();

        if (!gameSessions.containsKey(channel))
            return;

        // Process message
        GameSession session = gameSessions.get(channel);
        String word = event.getOriginalEvent().getMessage().toLowerCase();

        // Check if the message is a valid word
        if (!isWord(word))
            return;

        // Now try to update the game session
        int lbc = session.getLowerBoundWord().compareTo(word);
        int upc = session.getUpperBoundWord().compareTo(word);
        int wc = session.getWord().compareTo(word);

        if (session.getWord().equals(word)) { // checking if correct guess
            TitanBot.sendReply(event.getOriginalEvent(), "This game was won by "
                    + event.getOriginalEvent().getUser().getNick() + " who guessed the word " + word);
            gameSessions.remove(channel);
        } else if (lbc < 0 && wc > 0) { // checking if new lower bound
            session.setLowerBoundWord(word);
            sendUpdate(event.getOriginalEvent().getBot(), channel);
        } else if (upc > 0 && wc < 0) { // checking if new upper bound
            session.setUpperBoundWord(word);
            sendUpdate(event.getOriginalEvent().getBot(), channel);
        }
    }

    @Override
    public void register() {
        EventHandler.register(this);

        // Loading the word list
        loadWordList(Properties.getValue("wordlist"));
    }

    private static boolean isWord(String word) {
        return Arrays.binarySearch(wordList, word) > -1;
    }

    /**
     * Reads the word-list for the game.
     */
    private static void loadWordList(String fileName) {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            // Word count line, must be the first in the file
            String header = br.readLine().toLowerCase();

            if (header.startsWith("words=")) {
                int count = Integer.parseInt(header.split("=")[1]);

                if (count < 4) { // Invalid length - word list not loaded
                    TitanBot.LOGGER.error("Invalid word-list file, need more than three entries.");
                    return;
                }
                wordList = new String[count];
            } else {
                return; // word list not loaded
            }

            // Iterate through all words
            int idx = 0;

            // Read default lower bound
            wordList[idx++] = br.readLine();

            // Read all other values
            for (int i = 1; i < wordList.length - 1; i++) {
                wordList[idx++] = br.readLine();
            }

            // Read default lower bound
            wordList[wordList.length - 1] = br.readLine();
            wordListLoaded = true;
        } catch (Exception e) {
            TitanBot.LOGGER.error("Error reading from word-list file.");
        }
    }

    private void sendUpdate(PircBotX bot, String channel) {
        GameSession session = gameSessions.get(channel);
        bot.sendRaw().rawLine("PRIVMSG " + channel + " :A-Z update: " + session.getLowerBoundWord() + "-"
                + session.getUpperBoundWord()); // XXX refactor this garbage
    }

    /**
     * Gets a random word from the word list, avoiding the endpoints.
     * @return
     */
    public String getRandomWord() {
        return wordList[random.nextInt(wordList.length - 2) + 1]; // XXX test range
    }


    /**
     * An a-z game session.
     */
    static final class GameSession {

        private String lowerBoundWord = wordList[0];
        private String upperBoundWord = wordList[wordList.length - 1];
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
