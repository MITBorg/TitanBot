package mitb.util;

import mitb.TitanBot;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Random;

/**
 * A collection of message formatting and symbols.
 */
public final class StringHelper {

    /**
     * Random instance, to generate words to challenge for.
     */
    private static final Random RANDOM = new Random();
    /**
     * Bold encapsulation string.
     */
    private static final String BOLD = "\u0002";
    /**
     * Italic encapsulation string.
     */
    private static final String ITALIC = "\u001D";
    /**
     * Degrees symbol.
     */
    private static final String DEGREES_SYMBOL = "°";
    /**
     * Fahrenheit symbol.
     */
    public static final String FAHRENHEIT_SYMBOL = DEGREES_SYMBOL + "F";
    /**
     * Celsius symbol.
     */
    public static final String CELSIUS_SYMBOL = DEGREES_SYMBOL + "C";
    /**
     * A list of words.
     */
    public static String[] wordList;
    /**
     * If the word list is loaded.
     */
    private static boolean wordListLoaded;

    /**
     * Wraps some string in bold.
     * @param s
     * @return
     */
    public static String wrapBold(String s) {
        return BOLD + s + BOLD;
    }

    /**
     * Wraps some string in italic.
     * @param s
     * @return
     */
    public static String wrapItalic(String s) {
        return ITALIC + s + ITALIC;
    }

    /**
     * Strips carriage feed and new lines from the given string, new lines are replaced with spaces.
     * @param s
     * @return
     */
    public static String stripNewlines(String s) {
        return s.replaceAll("\r", "").replaceAll("\n", " ");
    }

    /**
     * Reads the word-list.
     */
    public static void loadWordList(String fileName) {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            // Word count line, must be the first in the file
            String header = br.readLine().toLowerCase();

            if (header.startsWith("words=")) {
                int count = Integer.parseInt(header.split("=")[1]);

                if (count < 4) { // Invalid length - word list not loaded
                    TitanBot.getLogger().error("Invalid word-list file, need more than three entries.");
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
            TitanBot.getLogger().error("Error reading from word-list file.");
        }
    }

    /**
     * If the word list is loaded.
     * @return
     */
    public static boolean isWordListLoaded() {
        return wordListLoaded && wordList != null && wordList.length > 0;
    }

    /**
     * Checks if the given word is in the loaded word list.
     * @param word
     * @return
     */
    public static boolean isWord(String word) {
        return Arrays.binarySearch(wordList, word) > -1;
    }

    /**
     * Gets a random word from the word list.
     * @return
     */
    public static String getRandomWord() {
        return wordList[RANDOM.nextInt(wordList.length)];
    }

    /**
     * Gets the given input encoded for urls with UTF-8 encoding.
     * @param input
     * @return Encoded string or null if unsupported encoder set.
     */
    public static String urlEncode(String input) {
        return urlEncode(input, "UTF-8");
    }

    /**
     * Url encodes the given string.
     * @param input
     * @param encoding
     * @return Encoded string or null if unsupported encoder set.
     */
    public static String urlEncode(String input, String encoding) {
        try {
            return URLEncoder.encode(input, encoding);
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }
}
