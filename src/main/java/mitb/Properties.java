package mitb;

import java.util.ResourceBundle;

/**
 * Provides helper methods for dealing with properties files.
 */
public class Properties {
    private static final ResourceBundle RESOURCE_BUNDLE;

    static {
        RESOURCE_BUNDLE = ResourceBundle.getBundle("props");
    }

    /**
     * Get a value from the configuration file and return it.
     *
     * @param key key to get value from
     * @return configuration value
     */
    public static String getValue(String key) {
        return RESOURCE_BUNDLE.getString(key);
    }
}
