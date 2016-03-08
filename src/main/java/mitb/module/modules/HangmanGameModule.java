package mitb.module.modules;

import mitb.TitanBot;
import mitb.event.EventHandler;
import mitb.event.Listener;
import mitb.event.events.CommandEvent;
import mitb.event.events.MessageEvent;
import mitb.module.CommandModule;
import mitb.util.Properties;
import mitb.util.StringHelper;
import org.pircbotx.PircBotX;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A simple hangman game.
 */
public final class HangmanGameModule extends CommandModule {

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
    private final HashMap<String, GameSession> gameSessions = new HashMap<>();

    @Override
    public String[] getCommands() {
        return new String[] {"hangman", "hang", "hm"};
    }

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

                // check if allowed to start
                if (!isAllowed(channelName)) {
                    TitanBot.sendReply(event.getSource(), "This module is restricted to " + RESTRICTED_CHANNEL);
                    return;
                }

                if (session == null) { // no game session, start session
                    String word = StringHelper.getRandomWord();
                    gameSessions.put(channelName, new GameSession(word));
                    sendUpdate(event.getSource().getBot(), channelName);
                    TitanBot.LOGGER.info("Hangman instance started for " + channelName + " with solution: " + word);
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
        } else if (cmd.equals("progress")) { // displays the progress for a channel
            GameSession session = gameSessions.get(channelName);

            if (session == null) { // not existent
                TitanBot.sendReply(event.getSource(), "No game session in progress for this channel. Try creating one.");
            } else { // game in progress
                sendUpdate(event.getSource().getBot(), channelName);
            }
        }
    }

    private void sendUpdate(PircBotX bot, String channelName) {
        GameSession session = gameSessions.get(channelName);
        bot.sendRaw().rawLine("PRIVMSG " + channelName + " :[Hangman Update] Word: " + session.getGuessWord() + " | Lives: "
                + session.getLives() + " | Used Letters: " + session.getGuessedLetters()); // XXX refactor this garbage
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

        String word = event.getSource().getMessage();

        // Skip unrelated messages
        if (word.contains(" "))
            return;

        // Lowercase case guess
        word = word.toLowerCase();
        char letter = word.charAt(0);

        // Try to process message
        if (word.length() > 1) { // Attempt guess of word
            if (session.getWord().equals(word)) {
                TitanBot.sendReply(event.getSource(), "This game was won by "
                        + event.getSource().getUser().getNick() + " who guessed the word " + word);
                gameSessions.remove(channelName);
                return;
            } else {
                session.reduceLives();
                sendUpdate(event.getSource().getBot(), channelName);
            }
        } else { // Attempt guess of letter
            if (!session.isGuessedLetter(letter) && Character.isLetter(letter)) { // check if letter is unused

                // Check if guess letter is right
                if (session.getWord().contains(letter + "")) {
                    session.addGuess(letter);
                    session.updateGuess(letter);

                    // Check if finished
                    if (session.isFinished()) {
                        TitanBot.sendReply(event.getSource(), "This game was won by "
                                + event.getSource().getUser().getNick() + " who guessed the last letter " + word
                                + " of word " + session.getWord());
                        gameSessions.remove(channelName);
                        return;
                    } else { // update
                        sendUpdate(event.getSource().getBot(), channelName);
                    }
                } else {
                    session.addGuess(letter);
                    session.reduceLives();
                }
            }
        }

        // Check if out of lives
        if (!session.hasLives()) {
            TitanBot.sendReply(event.getSource(), "Game session ended, you lost. The word was: " + session.getWord());
            gameSessions.remove(channelName);
        }
    }

    @Override
    protected void register() {
        EventHandler.register(this);
    }

    @Override
    public void getHelp(CommandEvent event) {
        TitanBot.sendReply(event.getSource(), "Syntax: " + event.getArgs()[0] + " start | " + event.getArgs()[0]
                + " stop | " + event.getArgs()[0] + " progress | Type single letters/whole words in channel"
                + (RESTRICTED_CHANNEL.equalsIgnoreCase("NO") ? "" : " | Restricted to " + RESTRICTED_CHANNEL));
    }

    /**
     * If this module is allowed to start game sessions in the given channel.
     * @param channelName
     * @return
     */
    private boolean isAllowed(String channelName) {
        return RESTRICTED_CHANNEL.equalsIgnoreCase("NO") || RESTRICTED_CHANNEL.equalsIgnoreCase(channelName);
    }

    /**
     * An a-z game session.
     */
    static final class GameSession {

        /**
         * Maximum guesses before a game session is over.
         */
        private static final int INTIAL_LIVES = 8;
        private final List<Character> guessedLetters = new ArrayList<>();
        private int lives = INTIAL_LIVES;
        private String word;
        private String guessWord;

        public GameSession(String word) {
            this.word = word;

            // Generate empty guess word
            String guess = "";

            for (int i = 0; i < word.length(); i++) {
                guess += "_";
            }
            guessWord = guess;
        }

        public String getWord() {
            return word;
        }

        public boolean isFinished() {
            return !guessWord.contains("_");
        }

        public void reduceLives() {
            lives--;
        }

        public boolean hasLives() {
            return lives > 0;
        }

        public String getGuessWord() {
            return guessWord;
        }

        public void addGuess(char letter) {
            guessedLetters.add(letter);
        }

        public void updateGuess(char letter) {
            String guess = "";

            for (int i = 0; i < word.length(); i++) {
                if (word.charAt(i) == letter) {
                    guess += letter;
                } else {
                    guess += guessWord.charAt(i);
                }
            }
            guessWord = guess;
        }

        public String getGuessedLetters() {
            String s = "";

            for (char c : guessedLetters) {
                s += c + " ";
            }
            return s;
        }

        public boolean isGuessedLetter(char c) {
            for (char x : guessedLetters)
                if (x == c)
                    return true;
            return false;
        }

        public int getLives() {
            return lives;
        }
    }
}
