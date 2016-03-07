package mitb.module.modules;

import mitb.TitanBot;
import mitb.event.events.CommandEvent;
import mitb.module.CommandModule;

/**
 * Links the github repo of this bot.
 */
public class RepoModule extends CommandModule {

    /**
     * The URL to this project on github.
     */
    private static final String REPO_URL = "https://github.com/MoparScape/TitanBot";

    @Override
    public String[] getCommands() {
        return new String[]{"repo", "github"};
    }

    @Override
    public void getHelp(CommandEvent event) {
        TitanBot.sendReply(event.getSource(), "Syntax: " + event.getArgs()[0]);
    }

    /**
     * Reply with an example response on command.
     *
     * @param commandEvent
     */
    @Override
    public void onCommand(CommandEvent commandEvent) {
        commandEvent.getSource().respond(REPO_URL);
    }

    @Override
    public void register() {

    }
}
