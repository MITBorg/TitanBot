package mitb.util;

/**
 * A math helper.
 */
public final class MathHelper {

    /**
     * Converts a temperature in kelvins to celsius.
     * @param k The temperature in kelvins.
     * @return The temperature in celsius.
     */
    public static double kelvinsToCelsius(double k) {
        return k - 273.15;
    }

    /**
     * Converts a temperature in kelvins to fahrenheit.
     * @param k The temperature in kelvins.
     * @return The temperature in fahrenheit.
     */
    public static double kelvinsToFahrenheit(double k) {
        return ((k - 273.15) * 1.8) + 32;
    }
}
