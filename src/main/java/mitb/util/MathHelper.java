package mitb.util;

/**
 * A math helper.
 */
public final class MathHelper {

    /**
     * Converts a temperature in fahrenheit to celsius.
     * @param f The temperature in fahrenheit.
     * @return The temperature in celsius.
     */
    public static int fahrenheitToCelsius(int f) {
        return (((f-32)*5)/9);
    }
}
