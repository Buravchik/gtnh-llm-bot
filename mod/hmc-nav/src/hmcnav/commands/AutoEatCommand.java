package hmcnav.commands;

import hmcnav.Survival;
import me.earth.headlessmc.mc.commands.AbstractMinecraftCommand;
import me.earth.headlessmc.mc.commands.CommandException;
import me.earth.headlessmc.mc.commands.MinecraftContext;
import me.earth.headlessmc.mc.commands.ScheduledCommand;
import picocli.CommandLine;
import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(name = "autoeat", description = {"autoeat on|off: eat automatically when food <= 14 (on by default)."})
public class AutoEatCommand extends AbstractMinecraftCommand implements ScheduledCommand {
    @CommandLine.Parameters(arity = "0..*") private List<String> a = new ArrayList<String>();
    public AutoEatCommand(MinecraftContext ctx) { super(ctx); Survival.ensure(); }
    @Override protected void execute() throws CommandException {
        if (hmcnav.W.player() == null) throw new CommandException("You are not ingame!");
        if (!a.isEmpty()) Survival.autoEat = !a.get(0).equalsIgnoreCase("off"); ctx.log("auto-eat " + (Survival.autoEat ? "on" : "off") + " (eats when food <= " + Survival.AUTO_AT + " until " + Survival.AUTO_UNTIL + "), food " + Survival.food(hmcnav.W.player()) + "/20, food in inventory: " + (Survival.hasFood() ? "yes" : "NO") + ", last: " + Survival.last);
    }
}
