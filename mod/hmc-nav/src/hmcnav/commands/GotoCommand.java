package hmcnav.commands;

import hmcnav.Navigator;
import me.earth.headlessmc.mc.commands.AbstractMinecraftCommand;
import me.earth.headlessmc.mc.commands.CommandException;
import me.earth.headlessmc.mc.commands.MinecraftContext;
import me.earth.headlessmc.mc.commands.ScheduledCommand;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(name = "goto", description = {"Walks to coordinates with local pathfinding (jumps steps, avoids trees, walls, lava, fire, big drops)."},
        customSynopsis = {"goto <x> <y> <z> [--sprint]%n   or: goto <x> <z> [--sprint]%n   or: goto waypoint <name> [--sprint]%n   or: goto player <name> [--sprint]%n   or: goto status%n   or: goto stop"})
public class GotoCommand extends AbstractMinecraftCommand implements ScheduledCommand {
    @CommandLine.Parameters(arity = "0..*", paramLabel = "<args>", description = {"x y z, x z, 'status' or 'stop'"})
    private List<String> params = new ArrayList<String>();
    @CommandLine.Unmatched
    private List<String> unmatched = new ArrayList<String>();
    @CommandLine.Option(names = {"--sprint", "-sprint"}, description = {"Sprint on straight stretches (uses hunger)."})
    private boolean sprint;

    public GotoCommand(MinecraftContext ctx) { super(ctx); }

    @Override
    protected void execute() throws CommandException {
        List<String> args = Args.merge(params, unmatched);
        Navigator nav = Navigator.INSTANCE;
        if (args.isEmpty() || args.get(0).equalsIgnoreCase("status")) { ctx.log(nav.status()); return; }
        if (args.get(0).equalsIgnoreCase("stop")) {
            nav.stop("stopped by command");
            ctx.log(nav.status());
            return;
        }
        String a0 = args.get(0).toLowerCase();
        if ((a0.equals("waypoint") || a0.equals("wp")) && args.size() > 1) {
            String q = String.join(" ", args.subList(1, args.size()));
            hmcnav.JM.Wp wp = hmcnav.JM.findWaypoint(q);
            if (wp == null) throw new CommandException("No waypoint matching '" + q + "'. Use 'map waypoints'.");
            ctx.log("Waypoint " + wp.name + " = " + wp.x + " " + wp.y + " " + wp.z + ". " + nav.start(wp.x, wp.y, wp.z, sprint));
            return;
        }
        if (a0.equals("player") && args.size() > 1) {
            hmcnav.JM.Pl pl = hmcnav.JM.findPlayer(args.get(1));
            if (pl == null) throw new CommandException("Player '" + args.get(1) + "' position unknown (not in range). Use 'map players'.");
            int px = (int) Math.floor(pl.x), py = (int) Math.floor(pl.y), pz = (int) Math.floor(pl.z);
            ctx.log("Player " + pl.name + " at " + px + " " + py + " " + pz + ". " + nav.start(px, py, pz, sprint));
            return;
        }
        if (args.size() == 3) {
            ctx.log(nav.start(Args.coord(args.get(0)), Args.coord(args.get(1)), Args.coord(args.get(2)), sprint));
        } else if (args.size() == 2) {
            ctx.log(nav.start(Args.coord(args.get(0)), null, Args.coord(args.get(1)), sprint));
        } else {
            throw new CommandException("Usage: goto <x> <y> <z> | goto <x> <z> | goto status | goto stop");
        }
    }
}
