package hmcnav.commands;

import hmcnav.Navigator;
import me.earth.headlessmc.mc.commands.AbstractMinecraftCommand;
import me.earth.headlessmc.mc.commands.MinecraftContext;
import me.earth.headlessmc.mc.commands.ScheduledCommand;
import picocli.CommandLine;

@CommandLine.Command(name = "halt", description = {"Stops everything hmc-nav is doing: goto, follow and jump. (HMC's own 'stop' quits the game!)"})
public class HaltCommand extends AbstractMinecraftCommand implements ScheduledCommand {
    public HaltCommand(MinecraftContext ctx) { super(ctx); }

    @Override
    protected void execute() {
        Navigator nav = Navigator.INSTANCE;
        nav.stopJump();
        nav.stop("halted");
        nav.maybeRestore();
        ctx.log("Halted.\n" + nav.status());
    }
}
