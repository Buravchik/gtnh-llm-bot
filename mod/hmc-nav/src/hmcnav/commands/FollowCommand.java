package hmcnav.commands;

import hmcnav.Navigator;
import me.earth.headlessmc.mc.commands.AbstractMinecraftCommand;
import me.earth.headlessmc.mc.commands.CommandException;
import me.earth.headlessmc.mc.commands.MinecraftContext;
import me.earth.headlessmc.mc.commands.ScheduledCommand;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(name = "follow", description = {"Keeps following a player (re-targets every 0.5 s, keeps a distance) until 'follow stop' or the time is up."},
        customSynopsis = {"follow <nick> [--seconds N] [--distance D]%n   or: follow stop%n   or: follow status"})
public class FollowCommand extends AbstractMinecraftCommand implements ScheduledCommand {
    @CommandLine.Parameters(arity = "0..1", paramLabel = "<nick|stop|status>")
    private List<String> params = new ArrayList<String>();
    @CommandLine.Option(names = {"--seconds", "-seconds", "-s"}, paramLabel = "<N>", description = {"Stop after N seconds (default: until stopped)."})
    private int seconds = 0;
    @CommandLine.Option(names = {"--distance", "-distance", "-d"}, paramLabel = "<D>", description = {"Distance to keep, default 2."})
    private double distance = 2.0;

    public FollowCommand(MinecraftContext ctx) { super(ctx); }

    @Override
    protected void execute() throws CommandException {
        Navigator nav = Navigator.INSTANCE;
        if (params.isEmpty() || params.get(0).equalsIgnoreCase("status")) { ctx.log(nav.status()); return; }
        if (params.get(0).equalsIgnoreCase("stop")) {
            if (nav.followName() != null) nav.stop("follow stopped by command");
            ctx.log("Follow stopped.\n" + nav.status());
            return;
        }
        ctx.log(nav.follow(params.get(0), seconds, distance));
    }
}
