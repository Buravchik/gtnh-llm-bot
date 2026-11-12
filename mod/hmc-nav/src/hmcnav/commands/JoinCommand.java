package hmcnav.commands;

import hmcnav.W;
import me.earth.headlessmc.mc.commands.AbstractMinecraftCommand;
import me.earth.headlessmc.mc.commands.CommandException;
import me.earth.headlessmc.mc.commands.MinecraftContext;
import me.earth.headlessmc.mc.commands.ScheduledCommand;
import picocli.CommandLine;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(name = "join", description = {"Joins a server from the multiplayer server list by its name (keeps JourneyMap's per-server map and waypoints)."},
        customSynopsis = {"join <server name from the list>%n   or: join   (lists servers)"})
public class JoinCommand extends AbstractMinecraftCommand implements ScheduledCommand {
    @CommandLine.Parameters(arity = "0..*", paramLabel = "<name>")
    private List<String> params = new ArrayList<String>();

    public JoinCommand(MinecraftContext ctx) { super(ctx); }

    @Override
    protected void execute() throws CommandException {
        try {
            Object mcObj = W.mc();
            Class<?> mcCls = Class.forName("net.minecraft.client.Minecraft");
            Class<?> slCls = Class.forName("net.minecraft.client.multiplayer.ServerList");
            Object list = slCls.getConstructor(mcCls).newInstance(mcObj);
            int n = (Integer) slCls.getMethod("func_78856_c").invoke(list);
            String want = String.join(" ", params).trim();
            StringBuilder names = new StringBuilder();
            Object found = null;
            for (int i = 0; i < n; i++) {
                Object sd = slCls.getMethod("func_78850_a", int.class).invoke(list, i);
                String name = String.valueOf(sd.getClass().getField("field_78847_a").get(sd));
                String ip = String.valueOf(sd.getClass().getField("field_78845_b").get(sd));
                names.append("  ").append(name).append(" (").append(ip).append(")\n");
                if (!want.isEmpty() && found == null && (name.equalsIgnoreCase(want) || ip.equalsIgnoreCase(want))) found = sd;
            }
            if (want.isEmpty() || found == null) {
                ctx.log((want.isEmpty() ? "" : "No server named '" + want + "'. ") + "Servers in the list:\n" + names);
                return;
            }
            final Object sd = found;
            if (W.world() != null) mc.disconnect();
            Class<?> gs = Class.forName("net.minecraft.client.gui.GuiScreen");
            Object mainMenu = Class.forName("net.minecraft.client.gui.GuiMainMenu").newInstance();
            Constructor<?> gmCtor = Class.forName("net.minecraft.client.gui.GuiMultiplayer").getConstructor(gs);
            Object parent = gmCtor.newInstance(mainMenu);
            Class<?> fml = Class.forName("cpw.mods.fml.client.FMLClientHandler");
            Object h = fml.getMethod("instance").invoke(null);
            fml.getMethod("connectToServer", gs, Class.forName("net.minecraft.client.multiplayer.ServerData")).invoke(h, parent, sd);
            ctx.log("Connecting to " + sd.getClass().getField("field_78847_a").get(sd) + " (" + sd.getClass().getField("field_78845_b").get(sd) + ")...");
        } catch (Throwable t) {
            throw new CommandException("join failed: " + t);
        }
    }
}
