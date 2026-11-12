package hmcnav.commands;

import hmcnav.Navigator;
import me.earth.headlessmc.mc.commands.AbstractMinecraftCommand;
import me.earth.headlessmc.mc.commands.CommandException;
import me.earth.headlessmc.mc.commands.MinecraftContext;
import me.earth.headlessmc.mc.commands.ScheduledCommand;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(name = "jump", description = {"Jumps repeatedly for N seconds, or while following; works together with goto/follow."},
        customSynopsis = {"jump [--seconds N]   (default 5)%n   or: jump --while-following%n   or: jump stop"})
public class JumpCommand extends AbstractMinecraftCommand implements ScheduledCommand {
    @CommandLine.Parameters(arity = "0..1", paramLabel = "<stop>")
    private List<String> params = new ArrayList<String>();
    @CommandLine.Option(names = {"--seconds", "-seconds", "-s"}, paramLabel = "<N>")
    private int seconds = 5;
    @CommandLine.Option(names = {"--while-following", "-while-following"})
    private boolean whileFollowing;

    public JumpCommand(MinecraftContext ctx) { super(ctx); }

    @Override
    protected void execute() throws CommandException {
        Navigator nav = Navigator.INSTANCE;
        if (!params.isEmpty() && params.get(0).equalsIgnoreCase("stop")) { nav.stopJump(); ctx.log("Jumping stopped."); return; }
        if (!params.isEmpty()) {
            try { seconds = Integer.parseInt(params.get(0)); } catch (NumberFormatException e) { throw new CommandException("Usage: jump [--seconds N] | jump --while-following | jump stop"); }
        }
        ctx.log(nav.startJump(seconds, whileFollowing));
    }
}
