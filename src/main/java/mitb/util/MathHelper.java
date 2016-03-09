package mitb.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

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

    /**
     * Converts the given quantity in metres per second (m/s) to miles per hour (mph).
     * @param ms
     * @return
     */
    public static double msToMph(double ms) {
        return 2.23694 * ms;
    }

    /**
     * Rounds the given quantity to the amount of decimal places specified.
     * @param value
     * @param places
     * @return
     */
    public static double round(double value, int places) {
        if (places < 0)
            throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
    /**
     * Rounds the given quantity to 2 decimal places.
     * @param value
     * @return
     */
    public static double round2dp(double value) {
        return round(value, 2);
    }
}
