package hmcnav.commands;

import me.earth.headlessmc.mc.commands.CommandException;

import java.util.ArrayList;
import java.util.List;

final class Args {
    private Args() {}

    /** Picocli may put negative numbers into the unmatched list; we keep only non-option-looking or numeric values. */
    static List<String> merge(List<String> params, List<String> unmatched) {
        List<String> r = new ArrayList<String>();
        if (params != null) r.addAll(params);
        if (unmatched != null) for (String s : unmatched) if (isNumber(s) || !s.startsWith("-")) r.add(s);
        return r;
    }

    static boolean isNumber(String s) {
        try { Double.parseDouble(s); return true; } catch (NumberFormatException e) { return false; }
    }

    static int coord(String s) throws CommandException {
        try { return (int) Math.floor(Double.parseDouble(s.replace(',', '.'))); }
        catch (NumberFormatException e) { throw new CommandException("Not a number: " + s); }
    }

    static double dbl(String s) throws CommandException {
        try { return Double.parseDouble(s.replace(',', '.')); }
        catch (NumberFormatException e) { throw new CommandException("Not a number: " + s); }
    }
}
