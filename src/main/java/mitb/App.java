package mitb;

import mitb.util.Properties;

public class App {
    /**
     * Main entry point to the application.
     *
     * @param args
     */
    public static void main(String[] args) throws Exception {
        TitanBot.LOGGER.info("TitanBot v" + Properties.getValue("bot.version"));
        TitanBot.LOGGER.info("Jordan, Pure, Bootnecklad");
        (new TitanBot()).run();
    }
}
