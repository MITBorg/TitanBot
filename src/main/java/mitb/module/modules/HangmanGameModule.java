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
    private final HashMap<String, HangmanGameModule.GameSession> gameSessions = new HashMap<>();

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
        String channelName = PIrcBotXHelper.getChannelName(event.getSource());

        // Ignore non-channel messages
        if (channelName == null)
            return;

        // Process command
        HangmanGameModule.GameSession session = this.gameSessions.get(channelName);

        if (cmd.equals("start")) { // starts a game session for a channel
            this.startSession(channelName, session, event);
        } else if (cmd.equals("stop")) { // stops a game session for a channel
            this.stopSession(channelName, session, event);
        } else if (cmd.equals("progress")) { // displays the progress for a channel
            this.updateProgress(channelName, session, event);
        }
    }

    private void startSession(String channelName, HangmanGameModule.GameSession session, CommandEvent event) {
        if (event.getSource().getBot().getUserChannelDao().containsChannel(channelName)) {
            // check if allowed to start
            if (!HangmanGameModule.isAllowed(channelName)) {
                TitanBot.sendReply(event.getSource(), "This module is restricted to " + HangmanGameModule
                        .RESTRICTED_CHANNEL);
                return;
            }

            if (session == null) { // no game session, start session
                String word = StringHelper.getRandomWord();
                this.gameSessions.put(channelName, new HangmanGameModule.GameSession(word));
                this.sendUpdate(event.getSource().getBot(), channelName);
                TitanBot.getLogger().info("Hangman instance started for " + channelName + " with solution: " + word);
            } else { // game in progress, nothing to do
                TitanBot.sendReply(event.getSource(), "A game session for this channel already exists, try checking the progress.");
            }
        } else {
            TitanBot.sendReply(event.getSource(), "I am not in that channel and so cannot create a game session for it.");
        }
    }

    private void stopSession(String channelName, HangmanGameModule.GameSession session, CommandEvent event) {
        // Check if there is a session to stop
        if (session == null) {
            return;
        }

        // Stop session
        this.gameSessions.remove(channelName);
        TitanBot.sendReply(event.getSource(), "Game session ended, the word was: " + session.getWord());
    }

    private void updateProgress(String channelName, HangmanGameModule.GameSession session, CommandEvent event) {
        if (session == null) { // non-existent
            TitanBot.sendReply(event.getSource(), "No game session in progress for this channel. Try creating one.");
        } else { // game in progress
            this.sendUpdate(event.getSource().getBot(), channelName);
        }
    }

    private void sendUpdate(PircBotX bot, String channelName) {
        HangmanGameModule.GameSession session = this.gameSessions.get(channelName);
        String state = "[Hangman Update] Word: " + session.getGuessWord() + " | Lives: " + session.getLives()
                + " | Used Letters: " + session.getGuessedLetters();
        bot.send().message(channelName, state);
    }
    @Listener
    public void onMessage(MessageEvent event) {
        if (this.gameSessions.isEmpty())
            return;

        // Check if this channel has a game session
        String channelName = event.getSource().getChannel().getName();

        if (!this.gameSessions.containsKey(channelName)) {
            return;
        }

        // Process message
        HangmanGameModule.GameSession session = this.gameSessions.get(channelName);

        String word = event.getSource().getMessage();

        // Skip unrelated messages
        if (word.contains(" ")) {
            return;
        }

        // Lowercase case guess
        word = word.toLowerCase();
        char letter = word.charAt(0);

        // Try to process message
        if (word.length() > 1) { // Attempt guess of word
            if (session.getWord().equals(word)) {
                TitanBot.sendReply(event.getSource(), "This game was won by "
                        + event.getSource().getUser().getNick() + " who guessed the word " + word);
                this.gameSessions.remove(channelName);
                return;
            } else {
                session.reduceLives();
                this.sendUpdate(event.getSource().getBot(), channelName);
            }
        } else { // Attempt guess of letter
            if (!session.isGuessedLetter(letter) && Character.isLetter(letter)) { // check if letter is unused

                // Check if guess letter is right
                if (session.getWord().contains(String.valueOf(letter))) {
                    session.addGuess(letter);
                    session.updateGuess(letter);

                    // Check if finished
                    if (session.isFinished()) {
                        TitanBot.sendReply(event.getSource(), "This game was won by "
                                + event.getSource().getUser().getNick() + " who guessed the last letter " + word
                                + " of word " + session.getWord());
                        this.gameSessions.remove(channelName);
                        return;
                    } else { // update
                        this.sendUpdate(event.getSource().getBot(), channelName);
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
            this.gameSessions.remove(channelName);
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
                + (HangmanGameModule.RESTRICTED_CHANNEL.equalsIgnoreCase("NO") ? "" : " | Restricted to " + HangmanGameModule
                .RESTRICTED_CHANNEL));
    }

    /**
     * If this module is allowed to start game sessions in the given channel.
     * @param channelName
     * @return
     */
    private static boolean isAllowed(String channelName) {
        return HangmanGameModule.RESTRICTED_CHANNEL.equalsIgnoreCase("NO") || HangmanGameModule.RESTRICTED_CHANNEL.equalsIgnoreCase(channelName);
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
        private int lives = HangmanGameModule.GameSession.INTIAL_LIVES;
        private final String word;
        private String guessWord;

        public GameSession(String word) {
            this.word = word;

            // Generate empty guess word
            String guess = "";

            for (int i = 0; i < word.length(); i++) {
                guess += "_";
            }
            this.guessWord = guess;
        }

        public String getWord() {
            return this.word;
        }

        public boolean isFinished() {
            return !this.guessWord.contains("_");
        }

        public void reduceLives() {
            this.lives--;
        }

        public boolean hasLives() {
            return this.lives > 0;
        }

        public String getGuessWord() {
            return this.guessWord;
        }

        public void addGuess(char letter) {
            this.guessedLetters.add(letter);
        }

        public void updateGuess(char letter) {
            String guess = "";

            for (int i = 0; i < this.word.length(); i++) {
                if (this.word.charAt(i) == letter) {
                    guess += letter;
                } else {
                    guess += this.guessWord.charAt(i);
                }
            }
            this.guessWord = guess;
        }

        public String getGuessedLetters() {
            String s = "";

            for (char c : this.guessedLetters) {
                s += c + " ";
            }
            return s;
        }

        public boolean isGuessedLetter(char c) {
            for (char x : this.guessedLetters)
                if (x == c)
                    return true;
            return false;
        }

        public int getLives() {
            return this.lives;
        }
    }
}
