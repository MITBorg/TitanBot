package mitb.module.modules;

import mitb.TitanBot;
import mitb.event.events.CommandEvent;
import mitb.module.CommandModule;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Display some information about the bot and the server the bot is running on.
 */
public class StatsModule extends CommandModule {

    @Override
    public String[] getCommands() {
        return new String[]{"stats", "statistics"};
    }

    @Override
    public void getHelp(CommandEvent event) {
        TitanBot.sendReply(event.getOriginalEvent(), "Syntax: " + event.getArgs()[0]);
    }

    @Override
    public void onCommand(CommandEvent commandEvent) {
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        double load = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
        String started = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL)
                .withLocale(Locale.UK)
                .withZone(ZoneId.systemDefault())
                .format(Instant.now().minus(uptime, ChronoUnit.MILLIS));

        TitanBot.sendReply(commandEvent.getOriginalEvent(), "Up since " + started + ". Currently under load " + load + ". Accepting donations for a better server.");
    }

    @Override
    public void register() {

    }
}
