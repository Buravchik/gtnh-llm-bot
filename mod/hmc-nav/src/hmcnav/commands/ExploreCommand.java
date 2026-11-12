package hmcnav.commands;

import hmcnav.Navigator;
import me.earth.headlessmc.mc.commands.AbstractMinecraftCommand;
import me.earth.headlessmc.mc.commands.CommandException;
import me.earth.headlessmc.mc.commands.MinecraftContext;
import me.earth.headlessmc.mc.commands.ScheduledCommand;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(name = "explore", description = {"Walks outward in rings (safe pathfinding like goto) until an entity or block whose name contains <target> is seen, e.g. explore sheep."},
        customSynopsis = {"explore <target> [--radius R (default 160)] [--seconds N (default 180)]%n   status: goto status, cancel: halt"})
public class ExploreCommand extends AbstractMinecraftCommand implements ScheduledCommand {
    @CommandLine.Parameters(arity = "0..*", paramLabel = "<target>")
    private List<String> params = new ArrayList<String>();
    @CommandLine.Option(names = {"--radius", "-radius", "-r"}, paramLabel = "<R>")
    private int radius = 160;
    @CommandLine.Option(names = {"--seconds", "-seconds", "-s"}, paramLabel = "<N>")
    private int seconds = 180;

    public ExploreCommand(MinecraftContext ctx) { super(ctx); }

    @Override
    protected void execute() throws CommandException {
        if (params.isEmpty()) throw new CommandException("Usage: explore <target> [--radius R] [--seconds N]");
        ctx.log(Navigator.INSTANCE.explore(String.join(" ", params), radius, seconds));
    }
}
