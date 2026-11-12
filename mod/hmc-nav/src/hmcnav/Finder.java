package hmcnav;

import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import java.util.*;

/** Finds loaded entities and nearby blocks by name. */
public final class Finder {
    private Finder() {}

    public static final class Hit {
        public final String name; public final double x, y, z, dist;
        Hit(String name, double x, double y, double z, double dist) { this.name = name; this.x = x; this.y = y; this.z = z; this.dist = dist; }
        public String describe(EntityClientPlayerMP me) {
            double dx = x - me.field_70165_t, dz = z - me.field_70161_v;
            return name + " at " + W.fmt(x) + " " + W.fmt(y) + " " + W.fmt(z) + " (" + W.fmt(dist) + " blocks " + W.facing(W.yawTo(dx, dz)) + ")";
        }
    }

    static String entName(Entity e) {
        String cls = e.getClass().getSimpleName();
        String n;
        try { n = e.func_70005_c_(); } catch (Throwable t) { n = cls; }
        return n == null || n.isEmpty() || n.equals(cls) ? cls : n + " [" + cls + "]";
    }

    /** All loaded entities (client view, about render distance) whose name or class contains q. */
    public static List<Hit> entities(String q) {
        List<Hit> out = new ArrayList<Hit>();
        EntityClientPlayerMP me = W.player(); World w = W.world();
        if (me == null || w == null) return out;
        String ql = q.toLowerCase(Locale.ROOT);
        String qs = ql.endsWith("s") && ql.length() > 3 ? ql.substring(0, ql.length() - 1) : ql; // sheeps/cows -> sheep/cow
        try {
            for (Object o : new ArrayList<Object>(w.field_72996_f)) {
                if (!(o instanceof Entity) || o == me) continue;
                Entity e = (Entity) o;
                String n = entName(e);
                String nl = n.toLowerCase(Locale.ROOT);
                if (!nl.contains(ql) && !nl.contains(qs)) continue;
                double dx = e.field_70165_t - me.field_70165_t, dy = e.field_70163_u - me.field_70163_u, dz = e.field_70161_v - me.field_70161_v;
                out.add(new Hit(n, e.field_70165_t, e.field_70121_D != null ? e.field_70121_D.field_72338_b : e.field_70163_u, e.field_70161_v, Math.sqrt(dx * dx + dy * dy + dz * dz)));
            }
        } catch (Throwable ignored) {}
        Collections.sort(out, new Comparator<Hit>() { public int compare(Hit a, Hit b) { return Double.compare(a.dist, b.dist); } });
        return out;
    }

    /** Nearest block whose registry name contains q within r (horizontal) and ry (vertical). */
    public static Hit block(String q, int r, int ry) {
        EntityClientPlayerMP me = W.player(); World w = W.world();
        if (me == null || w == null) return null;
        String ql = q.toLowerCase(Locale.ROOT);
        int cx = W.feetX(me), cy = W.feetY(me), cz = W.feetZ(me);
        Hit best = null;
        Map<Object, Boolean> cache = new IdentityHashMap<Object, Boolean>();
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) for (int dy = -ry; dy <= ry; dy++) {
            int x = cx + dx, y = cy + dy, z = cz + dz;
            if (y < 0 || y > 255 || !W.loaded(w, x, y, z)) continue;
            Object b;
            try { b = w.func_147439_a(x, y, z); } catch (Throwable t) { continue; }
            if (b == null) continue;
            Boolean m = cache.get(b);
            if (m == null) { m = W.name((net.minecraft.block.Block) b).toLowerCase(Locale.ROOT).contains(ql); cache.put(b, m); }
            if (!m) continue;
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (best == null || d < best.dist) best = new Hit(W.blockName(w, x, y, z), x, y, z, d);
        }
        return best;
    }
}
