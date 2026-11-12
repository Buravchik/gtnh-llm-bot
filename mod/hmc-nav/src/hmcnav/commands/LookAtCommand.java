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

@CommandLine.Command(name = "lookat", description = {"Turns the camera to look at the center of block x y z."}, customSynopsis = {"lookat <x> <y> <z>"})
public class LookAtCommand extends AbstractMinecraftCommand implements ScheduledCommand {
    @CommandLine.Parameters(arity = "0..3", paramLabel = "<xyz>")
    private List<String> params = new ArrayList<String>();
    @CommandLine.Unmatched
    private List<String> unmatched = new ArrayList<String>();

    public LookAtCommand(MinecraftContext ctx) { super(ctx); }

    @Override
    protected void execute() throws CommandException {
        EntityClientPlayerMP p = W.player();
        if (p == null) throw new CommandException("You are not ingame!");
        List<String> a = Args.merge(params, unmatched);
        if (a.size() != 3) throw new CommandException("Usage: lookat <x> <y> <z>");
        double x = Args.dbl(a.get(0)) + 0.5, y = Args.dbl(a.get(1)) + 0.5, z = Args.dbl(a.get(2)) + 0.5;
        double ex = p.field_70165_t, ey = W.feetPosY(p) + 1.62, ez = p.field_70161_v;
        double dx = x - ex, dy = y - ey, dz = z - ez;
        float yaw = W.yawTo(dx, dz);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        LookCommand.set(p, yaw, pitch);
        ctx.log(LookCommand.describe(p) + ", distance " + W.fmt(Math.sqrt(dx * dx + dy * dy + dz * dz)));
    }
}
