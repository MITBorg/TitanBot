package mitb.util;

/**
 * A collection of message formatting and symbols.
 */
public final class StringHelper {

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
    public static final String DEGREES_SYMBOL = "°";
    /**
     * Fahrenheit symbol.
     */
    public static final String FAHRENHEIT_SYMBOL = DEGREES_SYMBOL + "F";
    /**
     * Celsius symbol.
     */
    public static final String CELSIUS_SYMBOL = DEGREES_SYMBOL + "C";

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
     * Strips carriage feed and new lines from the given string.
     * @param s
     * @return
     */
    public static String stripNewlines(String s) {
        return s.replaceAll("\r", "").replaceAll("\n", "");
    }
}
