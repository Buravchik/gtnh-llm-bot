package hmcnav.commands;

import hmcnav.Survival;
import me.earth.headlessmc.mc.commands.AbstractMinecraftCommand;
import me.earth.headlessmc.mc.commands.CommandException;
import me.earth.headlessmc.mc.commands.MinecraftContext;
import me.earth.headlessmc.mc.commands.ScheduledCommand;
import picocli.CommandLine;
import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(name = "eat", description = {"Eats food from the inventory until full (or until 'eat <N>' food points)."})
public class EatCommand extends AbstractMinecraftCommand implements ScheduledCommand {
    @CommandLine.Parameters(arity = "0..*") private List<String> a = new ArrayList<String>();
    public EatCommand(MinecraftContext ctx) { super(ctx); Survival.ensure(); }
    @Override protected void execute() throws CommandException {
        if (hmcnav.W.player() == null) throw new CommandException("You are not ingame!");
        ctx.log(Survival.eat(a.isEmpty() ? 20 : Integer.parseInt(a.get(0))));
    }
}
