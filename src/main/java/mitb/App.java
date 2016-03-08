package mitb;

import mitb.util.Properties;

public final class App {
    /**
     * Main entry point to the application.
     *
     * @param args
     */
    public static void main(String[] args) throws Exception {
        TitanBot.getLogger().info("TitanBot v" + Properties.getValue("bot.version"));
        (new TitanBot()).run();
    }
}
