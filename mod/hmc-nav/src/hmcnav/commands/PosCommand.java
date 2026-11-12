package hmcnav.commands;

import hmcnav.Navigator;
import me.earth.headlessmc.mc.commands.AbstractMinecraftCommand;
import me.earth.headlessmc.mc.commands.CommandException;
import me.earth.headlessmc.mc.commands.MinecraftContext;
import me.earth.headlessmc.mc.commands.ScheduledCommand;
import picocli.CommandLine;

@CommandLine.Command(name = "pos", description = {"Shows position, facing, health, food and the goto state."})
public class PosCommand extends AbstractMinecraftCommand implements ScheduledCommand {
    public PosCommand(MinecraftContext ctx) { super(ctx); }

    @Override
    protected void execute() throws CommandException {
        if (hmcnav.W.player() == null) throw new CommandException("You are not ingame!");
        ctx.log(Navigator.INSTANCE.status());
    }
}
