package hmcnav.commands;

import hmcnav.JM;
import hmcnav.W;
import me.earth.headlessmc.mc.commands.AbstractMinecraftCommand;
import me.earth.headlessmc.mc.commands.CommandException;
import me.earth.headlessmc.mc.commands.MinecraftContext;
import me.earth.headlessmc.mc.commands.ScheduledCommand;
import net.minecraft.client.entity.EntityClientPlayerMP;
import picocli.CommandLine;

import java.util.*;

@CommandLine.Command(name = "map", description = {"Map info: other players with positions (seen or from JourneyMap radar) and JourneyMap waypoints."},
        customSynopsis = {"map players%n   or: map waypoints [filter]"})
public class MapCommand extends AbstractMinecraftCommand implements ScheduledCommand {
    @CommandLine.Parameters(arity = "0..2", paramLabel = "<args>")
    private List<String> params = new ArrayList<String>();

    public MapCommand(MinecraftContext ctx) { super(ctx); }

    @Override
    protected void execute() throws CommandException {
        EntityClientPlayerMP me = W.player();
        if (me == null) throw new CommandException("You are not ingame!");
        String what = params.isEmpty() ? "players" : params.get(0).toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder();
        if (what.startsWith("p")) {
            List<String> tab = new ArrayList<String>();
            List<JM.Pl> pl = JM.players(tab);
            sb.append("Players with known position (you: ").append(W.fmt(me.field_70165_t)).append(' ').append(W.fmt(W.feetPosY(me))).append(' ').append(W.fmt(me.field_70161_v)).append("):\n");
            if (pl.isEmpty()) sb.append("  none in range\n");
            for (JM.Pl p : pl) {
                double dx = p.x - me.field_70165_t, dz = p.z - me.field_70161_v;
                sb.append("  ").append(p.name).append(" at ").append(W.fmt(p.x)).append(' ').append(W.fmt(p.y)).append(' ').append(W.fmt(p.z))
                  .append(" (").append(W.fmt(Math.sqrt(dx * dx + dz * dz))).append(" blocks ").append(W.facing(W.yawTo(dx, dz))).append(", ").append(p.source).append(")\n");
            }
            sb.append("Online but position unknown (out of range): ").append(tab.isEmpty() ? "none" : String.join(", ", tab));
            sb.append("\nJourneyMap: ").append(JM.jmVersion() == null ? "not installed" : JM.jmVersion());
        } else if (what.startsWith("w")) {
            String filter = params.size() > 1 ? params.get(1).toLowerCase(Locale.ROOT) : null;
            StringBuilder note = new StringBuilder();
            List<JM.Wp> wps = JM.waypoints(note);
            sb.append("Waypoints (").append(note).append("):\n");
            int n = 0;
            for (JM.Wp w : wps) {
                if (filter != null && !w.name.toLowerCase(Locale.ROOT).contains(filter)) continue;
                double dx = w.x + 0.5 - me.field_70165_t, dz = w.z + 0.5 - me.field_70161_v;
                sb.append("  ").append(w.name).append(": ").append(w.x).append(' ').append(w.y).append(' ').append(w.z)
                  .append(" dim ").append(w.dim).append(w.enabled ? "" : " (hidden)").append(", ").append(W.fmt(Math.sqrt(dx * dx + dz * dz))).append(" blocks away\n");
                if (++n >= 40) break;
            }
            if (n == 0) sb.append("  none\n");
            sb.append("Use 'goto waypoint <name>' to walk to one.");
        } else throw new CommandException("Usage: map players | map waypoints [filter]");
        ctx.log(sb.toString());
    }
}
