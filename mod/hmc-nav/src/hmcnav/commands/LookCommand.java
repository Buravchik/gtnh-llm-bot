package hmcnav.commands;

import hmcnav.W;
import me.earth.headlessmc.mc.commands.AbstractMinecraftCommand;
import me.earth.headlessmc.mc.commands.CommandException;
import me.earth.headlessmc.mc.commands.MinecraftContext;
import me.earth.headlessmc.mc.commands.ScheduledCommand;
import net.minecraft.client.entity.EntityClientPlayerMP;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(name = "look", description = {"Turns the camera: yaw 0=south(+Z) 90=west(-X) 180=north(-Z) -90=east(+X); pitch -90=up 0=straight 90=down. Or a direction name."},
        customSynopsis = {"look <yaw> [pitch]%n   or: look north|south|east|west|up|down"})
public class LookCommand extends AbstractMinecraftCommand implements ScheduledCommand {
    @CommandLine.Parameters(arity = "0..2", paramLabel = "<args>")
    private List<String> params = new ArrayList<String>();
    @CommandLine.Unmatched
    private List<String> unmatched = new ArrayList<String>();

    public LookCommand(MinecraftContext ctx) { super(ctx); }

    @Override
    protected void execute() throws CommandException {
        EntityClientPlayerMP p = W.player();
        if (p == null) throw new CommandException("You are not ingame!");
        List<String> a = Args.merge(params, unmatched);
        if (a.isEmpty()) { ctx.log(describe(p)); return; }
        String s = a.get(0).toLowerCase();
        float yaw = p.field_70177_z, pitch = p.field_70125_A;
        if (s.equals("north") || s.equals("n")) { yaw = 180; pitch = 0; }
        else if (s.equals("south") || s.equals("s")) { yaw = 0; pitch = 0; }
        else if (s.equals("east") || s.equals("e")) { yaw = -90; pitch = 0; }
        else if (s.equals("west") || s.equals("w")) { yaw = 90; pitch = 0; }
        else if (s.equals("up")) { pitch = -90; }
        else if (s.equals("down")) { pitch = 90; }
        else {
            yaw = (float) Args.dbl(a.get(0));
            if (a.size() > 1) pitch = (float) Args.dbl(a.get(1));
        }
        set(p, yaw, pitch);
        ctx.log(describe(p));
    }

    static void set(EntityClientPlayerMP p, float yaw, float pitch) {
        pitch = Math.max(-90f, Math.min(90f, pitch));
        // keep yaw continuous with the current value to avoid a visible spin
        float cur = p.field_70177_z;
        p.field_70177_z = cur + W.wrap(yaw - cur);
        p.field_70125_A = pitch;
    }

    static String describe(EntityClientPlayerMP p) {
        return "Looking yaw " + W.fmt(W.wrap(p.field_70177_z)) + " pitch " + W.fmt(p.field_70125_A) + " (facing " + W.facing(p.field_70177_z) + ")";
    }
}
