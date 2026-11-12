package hmcnav.commands;

import hmcnav.W;
import me.earth.headlessmc.mc.commands.AbstractMinecraftCommand;
import me.earth.headlessmc.mc.commands.CommandException;
import me.earth.headlessmc.mc.commands.MinecraftContext;
import me.earth.headlessmc.mc.commands.ScheduledCommand;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import picocli.CommandLine;

import java.util.*;

@CommandLine.Command(name = "scan", description = {"Describes the surroundings: a top-down walkability map, what is in each direction, nearby blocks and entities."},
        customSynopsis = {"scan [radius 3-16, default 8] [--nomap] [--level <y>]%n   or: scan [length] --slice <N|NE|E|SE|S|SW|W|NW> [--depth <n>]"})
public class ScanCommand extends AbstractMinecraftCommand implements ScheduledCommand {
    @CommandLine.Parameters(arity = "0..1", paramLabel = "<radius>")
    private List<String> params = new ArrayList<String>();
    @CommandLine.Option(names = {"--nomap", "-nomap"}, description = {"Skip the map, only text summary."})
    private boolean noMap;
    @CommandLine.Option(names = {"--level", "-level"}, paramLabel = "<y>", description = {"Draw the map/directions for feet level y instead of your own (look below/above you)."})
    private Integer level;
    @CommandLine.Option(names = {"--slice", "-slice"}, paramLabel = "<dir>", description = {"Vertical cross-section from you towards N/NE/E/SE/S/SW/W/NW (side view)."})
    private String slice;
    @CommandLine.Option(names = {"--depth", "-depth"}, paramLabel = "<n>", description = {"For --slice: how many blocks below you to show (default 12, max 40)."})
    private int depth = 12;

    public ScanCommand(MinecraftContext ctx) { super(ctx); }

    static final class Col { final char c; final int y; Col(char c, int y) { this.c = c; this.y = y; } }

    /** Classifies column x,z for someone whose feet are at level y. */
    static Col col(World w, int x, int y, int z) {
        if (!W.loaded(w, x, y, z)) return new Col('?', y);
        int kf = W.kind(w, x, y, z), kh = W.kind(w, x, y + 1, z);
        if (kf == W.LAVA || kh == W.LAVA) return new Col('L', y);
        if (kf == W.FIRE || kh == W.FIRE) return new Col('F', y);
        if (kf == W.DANGER || kh == W.DANGER) return new Col('x', y);
        if (kf == W.DOOR || kh == W.DOOR) return new Col('D', y);
        if (kf == W.LADDER) return new Col('H', y);
        boolean feetOk = W.passable(kf) || kf == W.LOW;
        if (!feetOk || !W.passable(kh) || W.kind(w, x, y - 1, z) == W.TALL) {
            if (W.isWood(w, x, y, z) || W.isWood(w, x, y + 1, z)) return new Col('T', y);
            if (W.isLeaves(w, x, y, z) || W.isLeaves(w, x, y + 1, z)) return new Col('l', y);
            if (W.canStand(w, x, y + 1, z) && W.passable(W.kind(w, x, y + 2, z))) return new Col('+', y + 1);
            if (kf == W.TALL) return new Col('|', y);
            return new Col('#', y);
        }
        if (W.canStand(w, x, y, z)) return new Col(kf == W.WATER ? '~' : '.', y);
        for (int d = 1; d <= 12; d++) {
            int k = W.kind(w, x, y - d, z);
            if (k == W.LAVA) return new Col('L', y - d);
            if (k == W.FIRE) return new Col('F', y - d);
            if (W.canStand(w, x, y - d, z)) {
                if (k == W.WATER) return new Col('~', y - d);
                if (d <= 3) return new Col(d == 1 ? '-' : '=', y - d);
                return new Col('v', y - d);
            }
            if (!W.passable(k) && k != W.LOW) return new Col('v', y - d);
        }
        return new Col('v', y - 12);
    }

    static int blockingY(World w, int x, int y, int z) {
        if (W.isWood(w, x, y, z) || W.isLeaves(w, x, y, z)) return y;
        if (W.isWood(w, x, y + 1, z) || W.isLeaves(w, x, y + 1, z)) return y + 1;
        int kf = W.kind(w, x, y, z);
        if (!(W.passable(kf) || kf == W.LOW)) return y;
        if (!W.passable(W.kind(w, x, y + 1, z))) return y + 1;
        return y - 1;
    }

    private String slice(World w, EntityClientPlayerMP p, int px, int py, int pz, int r) throws CommandException {
        String[] names = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        int[][] dirs = {{0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}};
        int di = -1;
        for (int i = 0; i < 8; i++) if (names[i].equalsIgnoreCase(slice)) di = i;
        if (di < 0) throw new CommandException("--slice needs one of N NE E SE S SW W NW");
        int len = Math.max(4, Math.min(32, r == 8 ? 16 : r));
        int down = Math.max(2, Math.min(40, depth));
        StringBuilder sb = new StringBuilder();
        sb.append("Side view from ").append(px).append(' ').append(py).append(' ').append(pz).append(" towards ").append(names[di])
          .append(" (left = you, each column 1 block further; rows are y levels):\n");
        sb.append(" @ you, . air, # solid, _ slab/carpet, | fence, T log, l leaves, ~ water, L lava, F fire, D door/gate, H ladder, x cactus/web, ? unloaded\n");
        for (int y = py + 6; y >= py - down; y--) {
            StringBuilder row = new StringBuilder();
            for (int i = 0; i <= len; i++) {
                int x = px + dirs[di][0] * i, z = pz + dirs[di][1] * i;
                if (i == 0 && (y == py || y == py + 1)) { row.append('@'); continue; }
                row.append(cell(w, x, y, z));
            }
            sb.append(String.format(Locale.ROOT, "y%4d ", y)).append(row).append('\n');
        }
        return sb.toString();
    }

    static char cell(World w, int x, int y, int z) {
        int k = W.kind(w, x, y, z);
        switch (k) {
            case W.AIR: return '.';
            case W.WATER: return '~';
            case W.LAVA: return 'L';
            case W.FIRE: return 'F';
            case W.DANGER: return 'x';
            case W.UNKNOWN: return '?';
            case W.DOOR: return 'D';
            case W.LADDER: return 'H';
            case W.LOW: return '_';
            case W.TALL: return '|';
            default:
                if (W.isWood(w, x, y, z)) return 'T';
                if (W.isLeaves(w, x, y, z)) return 'l';
                return '#';
        }
    }

    static boolean walkable(char c) { return c == '.' || c == '+' || c == '-' || c == '=' || c == '~' || c == 'D' || c == 'H'; }

    static String meaning(char c) {
        switch (c) {
            case 'T': return "tree trunk";
            case 'l': return "leaves";
            case '#': return "wall/solid block (2+ high)";
            case '|': return "fence/wall";
            case 'L': return "LAVA";
            case 'F': return "FIRE";
            case 'x': return "cactus/cobweb";
            case 'v': return "drop deeper than 3";
            case '?': return "unloaded";
            default: return String.valueOf(c);
        }
    }

    @Override
    protected void execute() throws CommandException {
        EntityClientPlayerMP p = W.player();
        World w = W.world();
        if (p == null || w == null) throw new CommandException("You are not ingame!");
        int r = 8;
        if (!params.isEmpty()) r = Math.max(3, Math.min(16, Args.coord(params.get(0))));
        int px = W.feetX(p), py = W.feetY(p), pz = W.feetZ(p);
        if (!W.canStand(w, px, py, pz) && W.canStand(w, px, py + 1, pz)) py++;
        if (slice != null) { ctx.log(slice(w, p, px, py, pz, r)); return; }
        int realY = py;
        if (level != null) py = level;
        StringBuilder sb = new StringBuilder();
        sb.append("Position ").append(px).append(' ').append(py).append(' ').append(pz)
          .append(", facing ").append(W.facing(p.field_70177_z)).append(" (yaw ").append(W.fmt(W.wrap(p.field_70177_z))).append(")")
          .append(", standing on ").append(W.blockName(w, px, py - 1, pz));
        boolean inWater = false; try { inWater = p.func_70090_H(); } catch (Throwable ignored) {}
        if (inWater) sb.append(", IN WATER");
        if (py != realY) sb.append(". Map/directions below are drawn for feet level y=").append(py).append(" (you are at ").append(realY).append(")");
        try { if (p.func_70027_ad()) sb.append(", BURNING"); } catch (Throwable ignored) {}
        sb.append('\n');

        // directions
        String[] names = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        int[][] dirs = {{0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}};
        sb.append("Directions (N=-Z, E=+X, S=+Z, W=-X):\n");
        for (int i = 0; i < 8; i++) {
            int level = py; String res = null; int ups = 0, downs = 0;
            for (int s = 1; s <= r; s++) {
                int x = px + dirs[i][0] * s, z = pz + dirs[i][1] * s;
                Col c = col(w, x, level, z);
                if (walkable(c.c)) {
                    if (c.y > level) ups++; else if (c.y < level) downs++;
                    level = c.y;
                    continue;
                }
                int by = c.c == 'v' ? c.y - 1 : (c.c == 'L' || c.c == 'F' ? c.y : blockingY(w, x, level, z));
                res = meaning(c.c) + " at " + s + " blocks (" + x + " " + by + " " + z + ": " + W.blockName(w, x, by, z) + ")";
                break;
            }
            sb.append("  ").append(names[i]).append(": ");
            if (res == null) sb.append("walkable ").append(r).append(" blocks");
            else sb.append(res);
            if (ups + downs > 0) sb.append(", terrain ").append(level - py >= 0 ? "+" : "").append(level - py).append(" (").append(ups).append(" up, ").append(downs).append(" down)");
            sb.append('\n');
        }

        if (!noMap) {
            sb.append("Map (top = north/-Z, left = west/-X; @ = you; one char per block; corner ").append(px - r).append(' ').append(pz - r).append("):\n");
            sb.append(" . walk same level, + one up (jump), - one down, = 2-3 down, v deeper drop, ~ water, T tree, l leaves, # wall, | fence, D door/gate, H ladder, L lava, F fire, x cactus/web, ? unloaded\n");
            for (int dz = -r; dz <= r; dz++) {
                StringBuilder row = new StringBuilder();
                for (int dx = -r; dx <= r; dx++) {
                    if (dx == 0 && dz == 0) { row.append('@'); continue; }
                    row.append(col(w, px + dx, py, pz + dz).c);
                }
                sb.append(String.format(Locale.ROOT, "%+4d ", dz)).append(row).append('\n');
            }
        }

        // notable blocks around body level
        Map<String, int[]> counts = new HashMap<String, int[]>();
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) for (int dy = -1; dy <= 2; dy++) {
            int x = px + dx, y = py + dy, z = pz + dz;
            if (!W.loaded(w, x, y, z) || W.kind(w, x, y, z) == W.AIR) continue;
            String n = W.blockName(w, x, y, z);
            int[] c = counts.get(n);
            int d = Math.abs(dx) + Math.abs(dz) + Math.abs(dy);
            if (c == null) counts.put(n, new int[]{1, d, x, y, z});
            else { c[0]++; if (d < c[1]) { c[1] = d; c[2] = x; c[3] = y; c[4] = z; } }
        }
        List<Map.Entry<String, int[]>> list = new ArrayList<Map.Entry<String, int[]>>(counts.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, int[]>>() {
            public int compare(Map.Entry<String, int[]> a, Map.Entry<String, int[]> b) { return b.getValue()[0] - a.getValue()[0]; }
        });
        sb.append("Blocks near body level (count, nearest): ");
        int n = 0;
        for (Map.Entry<String, int[]> e : list) {
            if (n++ >= 14) break;
            int[] c = e.getValue();
            sb.append(e.getKey()).append(" x").append(c[0]).append(" @").append(c[2]).append(',').append(c[3]).append(',').append(c[4]).append("; ");
        }
        sb.append('\n');

        // entities
        List<String> ents = new ArrayList<String>();
        try {
            List<?> all = w.field_72996_f;
            List<Object[]> near = new ArrayList<Object[]>();
            for (Object o : new ArrayList<Object>(all)) {
                if (!(o instanceof Entity) || o == p) continue;
                Entity e = (Entity) o;
                double ex = e.field_70165_t - p.field_70165_t, ey = e.field_70163_u - p.field_70163_u, ez = e.field_70161_v - p.field_70161_v;
                double d = Math.sqrt(ex * ex + ey * ey + ez * ez);
                if (d > r * 2) continue;
                String cls = e.getClass().getSimpleName();
                if (cls.contains("EntityItem") || cls.contains("XPOrb")) cls = "item"; 
                String name;
                try { name = e.func_70005_c_(); } catch (Throwable t) { name = cls; }
                near.add(new Object[]{d, name + (cls.equals("item") ? " (item)" : "") + " at " + W.fmt(e.field_70165_t) + " " + W.fmt(e.field_70163_u - (e instanceof EntityClientPlayerMP ? 1.62 : 0)) + " " + W.fmt(e.field_70161_v) + " (" + W.fmt(d) + " away)"});
            }
            Collections.sort(near, new Comparator<Object[]>() {
                public int compare(Object[] a, Object[] b) { return Double.compare((Double) a[0], (Double) b[0]); }
            });
            for (int i = 0; i < Math.min(12, near.size()); i++) ents.add((String) near.get(i)[1]);
        } catch (Throwable t) { ents.add("(entities unavailable: " + t + ")"); }
        sb.append("Entities within ").append(r * 2).append(": ").append(ents.isEmpty() ? "none" : String.join("; ", ents));
        ctx.log(sb.toString());
    }
}
