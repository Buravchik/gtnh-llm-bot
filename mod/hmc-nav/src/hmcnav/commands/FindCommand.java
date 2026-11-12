package hmcnav.commands;

import hmcnav.W;
import me.earth.headlessmc.mc.commands.AbstractMinecraftCommand;
import me.earth.headlessmc.mc.commands.CommandException;
import me.earth.headlessmc.mc.commands.MinecraftContext;
import me.earth.headlessmc.mc.commands.ScheduledCommand;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.world.World;
import picocli.CommandLine;

import java.util.*;

@CommandLine.Command(name = "find", description = {"Finds blocks by name (door, ladder, chest, trapdoor, fence_gate, ...) or openings (cave/tunnel/building entrances) around you or around a point."},
        customSynopsis = {"find <name> [radius] [--at x y z]%n   or: find openings [radius] [--y <level>] [--at x y z]"})
public class FindCommand extends AbstractMinecraftCommand implements ScheduledCommand {
    @CommandLine.Parameters(arity = "0..2", paramLabel = "<args>")
    private List<String> params = new ArrayList<String>();
    @CommandLine.Option(names = {"--y", "-y"}, paramLabel = "<level>", description = {"For openings: feet level to look at (+-2)."})
    private Integer level;
    @CommandLine.Option(names = {"--at", "-at"}, arity = "1..3", paramLabel = "<x y z>", description = {"Search around this point instead of the player."})
    private List<String> at = new ArrayList<String>();
    @CommandLine.Unmatched
    private List<String> unmatched = new ArrayList<String>();

    public FindCommand(MinecraftContext ctx) { super(ctx); }

    @Override
    protected void execute() throws CommandException {
        EntityClientPlayerMP p = W.player();
        World w = W.world();
        if (p == null || w == null) throw new CommandException("You are not ingame!");
        List<String> a = Args.merge(params, unmatched);
        if (a.isEmpty()) throw new CommandException("Usage: find <name> [radius] | find openings [radius] [--y level] [--at x y z]");
        String what = a.get(0).toLowerCase(Locale.ROOT);
        int asked = a.size() > 1 ? Args.coord(a.get(1)) : 16;
        int r = Math.max(2, Math.min(32, asked));
        int cx = W.feetX(p), cy = W.feetY(p), cz = W.feetZ(p);
        if (at.size() == 1 && at.get(0).contains(",")) at = new ArrayList<String>(Arrays.asList(at.get(0).split(",")));
        if (!at.isEmpty() && at.size() != 3) throw new CommandException("--at needs x y z (or x,y,z)");
        if (at.size() == 3) { cx = Args.coord(at.get(0)); cy = Args.coord(at.get(1)); cz = Args.coord(at.get(2)); }
        final int px = W.feetX(p), py = W.feetY(p), pz = W.feetZ(p);
        List<int[]> hits = new ArrayList<int[]>();
        StringBuilder sb = new StringBuilder();
        if (what.equals("openings") || what.equals("opening") || what.equals("entrance") || what.equals("entrances")) {
            int y0 = level != null ? level : cy;
            sb.append("Openings (walkable spots with a roof, next to walkable spots open to the sky) at y ").append(y0 - 2).append("..").append(y0 + 2)
              .append(" within ").append(r).append(" of ").append(cx).append(' ').append(y0).append(' ').append(cz).append(":\n");
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) for (int y = y0 - 2; y <= y0 + 2; y++) {
                int x = cx + dx, z = cz + dz;
                if (!W.canStand(w, x, y, z) || !covered(w, x, y, z)) continue;
                int[][] n4 = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                for (int[] d : n4) {
                    int nx = x + d[0], nz = z + d[1];
                    boolean outside = false;
                    for (int ny = y - 1; ny <= y + 1 && !outside; ny++) outside = W.canStand(w, nx, ny, nz) && !covered(w, nx, ny, nz);
                    if (outside) {
                        String door = W.kind(w, x, y, z) == W.DOOR ? " DOOR " + W.blockName(w, x, y, z) : "";
                        hits.add(new int[]{x, y, z, d[0], d[1], door.isEmpty() ? 0 : 1});
                        break;
                    }
                }
            }
            sortAndCluster(hits, cx, y0, cz);
            int shown = 0;
            for (int[] h : hits) {
                if (shown++ >= 20) break;
                sb.append("  ").append(h[0]).append(' ').append(h[1]).append(' ').append(h[2])
                  .append(" (inside from ").append(dirName(-h[3], -h[4])).append(" side; ").append(W.fmt(dist(h, px, py, pz))).append(" from you)")
                  .append(h[5] == 1 ? " door" : "").append(" depth ").append(depth(w, h)).append('\n');
            }
            if (hits.isEmpty()) sb.append("  none found. Try another --y level, a bigger radius or move closer.\n");
            sb.append("Use 'goto x y z' on one of them; 'scan --level <y>' or 'scan --slice <dir>' to look inside.");
        } else {
            List<hmcnav.Finder.Hit> ents = hmcnav.Finder.entities(what);
            sb.append("Entities (mobs/animals/players/items) matching '").append(what).append("' among all entities the client has loaded (what the server sends, usually ~64-128 blocks; view distance ").append(viewDist()).append("): ")
              .append(ents.isEmpty() ? "none" : ents.size() + " found").append('\n');
            for (int i = 0; i < Math.min(15, ents.size()); i++) sb.append("  ").append(ents.get(i).describe(p)).append('\n');
            int ry = Math.min(r, 16);
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) for (int dy = -ry; dy <= ry; dy++) {
                int x = cx + dx, y = cy + dy, z = cz + dz;
                if (y < 0 || y > 255 || !W.loaded(w, x, y, z)) continue;
                String n = W.blockName(w, x, y, z).toLowerCase(Locale.ROOT);
                if (n.contains(what)) hits.add(new int[]{x, y, z});
            }
            sortAndCluster(hits, cx, cy, cz);
            sb.append("Blocks matching '").append(what).append("' searched radius ").append(r).append(asked > r ? " (asked " + asked + ", max is 32)" : "").append(", +-").append(ry).append(" vertically (").append(hits.size()).append(" after merging neighbours), nearest first:\n");
            int shown = 0;
            for (int[] h : hits) {
                if (shown++ >= 25) break;
                sb.append("  ").append(h[0]).append(' ').append(h[1]).append(' ').append(h[2]).append(' ').append(W.blockName(w, h[0], h[1], h[2]))
                  .append(" (").append(W.fmt(dist(h, px, py, pz))).append(" from you)\n");
            }
            if (hits.isEmpty()) sb.append("  none.\n");
        }
        ctx.log(sb.toString());
    }

    static int viewDist() {
        try {
            Object mc = W.mc();
            Object gs = mc.getClass().getField("field_71474_y").get(mc);
            return gs.getClass().getField("field_151451_c").getInt(gs) * 16;
        } catch (Throwable t) { return 128; }
    }

    static boolean covered(World w, int x, int y, int z) {
        for (int yy = y + 2; yy <= y + 24 && yy <= 255; yy++) {
            int k = W.kind(w, x, yy, z);
            if (k == W.SOLID || k == W.TALL || k == W.DOOR || k == W.UNKNOWN) return true;
        }
        return false;
    }

    /** How many blocks the roofed walkable area continues inward. */
    static int depth(World w, int[] h) {
        int d = 0;
        int x = h[0], y = h[1], z = h[2];
        for (int i = 1; i <= 12; i++) {
            int nx = x - h[3] * i, nz = z - h[4] * i;
            boolean ok = false;
            for (int ny = y - 1; ny <= y + 1 && !ok; ny++) if (W.canStand(w, nx, ny, nz) && covered(w, nx, ny, nz)) { ok = true; y = ny; }
            if (!ok) break;
            d = i;
        }
        return d;
    }

    static double dist(int[] h, int x, int y, int z) {
        double dx = h[0] - x, dy = h[1] - y, dz = h[2] - z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    static String dirName(int dx, int dz) {
        if (dz < 0) return "north"; if (dz > 0) return "south"; if (dx > 0) return "east"; return "west";
    }

    /** Sort by distance to center and drop hits within 2 blocks of an already kept hit. */
    static void sortAndCluster(List<int[]> hits, final int cx, final int cy, final int cz) {
        Collections.sort(hits, new Comparator<int[]>() {
            public int compare(int[] a, int[] b) { return Double.compare(dist(a, cx, cy, cz), dist(b, cx, cy, cz)); }
        });
        List<int[]> kept = new ArrayList<int[]>();
        outer:
        for (int[] h : hits) {
            for (int[] k : kept) if (Math.abs(k[0] - h[0]) <= 2 && Math.abs(k[1] - h[1]) <= 2 && Math.abs(k[2] - h[2]) <= 2) continue outer;
            kept.add(h);
        }
        hits.clear();
        hits.addAll(kept);
    }
}
