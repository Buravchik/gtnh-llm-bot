package hmcnav;

import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads players and waypoints, from JourneyMap (via reflection, optional) and the client world. */
public final class JM {
    private JM() {}

    public static final class Wp {
        public final String name, dim; public final int x, y, z; public final boolean enabled;
        Wp(String name, int x, int y, int z, String dim, boolean enabled) { this.name = name; this.x = x; this.y = y; this.z = z; this.dim = dim; this.enabled = enabled; }
    }

    public static final class Pl {
        public final String name; public final double x, y, z; public final String source;
        Pl(String name, double x, double y, double z, String source) { this.name = name; this.x = x; this.y = y; this.z = z; this.source = source; }
    }

    private static Object call(Object o, String m) throws Exception {
        Method mm = o.getClass().getMethod(m);
        mm.setAccessible(true);
        return mm.invoke(o);
    }

    public static String jmVersion() {
        try {
            Class<?> c = Class.forName("journeymap.common.Journeymap");
            try { Field f = c.getField("JM_VERSION"); return String.valueOf(f.get(null)); } catch (Throwable ignored) {}
            return "installed";
        } catch (Throwable t) { return null; }
    }

    /** Players: loaded in the world (exact), plus JourneyMap's server-sent radar (if the server runs JourneyMap), plus tab-list names. */
    public static List<Pl> players(List<String> tabOnly) {
        List<Pl> out = new ArrayList<Pl>();
        Set<String> seen = new HashSet<String>();
        EntityClientPlayerMP me = W.player();
        World w = W.world();
        if (w != null) {
            try {
                for (Object o : new ArrayList<Object>(w.field_73010_i)) {
                    if (!(o instanceof Entity) || o == me) continue;
                    Entity e = (Entity) o;
                    String n = e.func_70005_c_();
                    if (seen.add(n)) out.add(new Pl(n, e.field_70165_t, e.field_70121_D != null ? e.field_70121_D.field_72338_b : e.field_70163_u, e.field_70161_v, "seen"));
                }
            } catch (Throwable ignored) {}
        }
        try {
            Class<?> c = Class.forName("journeymap.common.util.PlayerRadarManager");
            Object inst = c.getMethod("getInstance").invoke(null);
            Map<?, ?> m = (Map<?, ?>) c.getMethod("getPlayers").invoke(inst);
            for (Object o : new ArrayList<Object>(m.values())) {
                if (!(o instanceof Entity) || o == me) continue;
                Entity e = (Entity) o;
                String n = e.func_70005_c_();
                if (seen.add(n)) out.add(new Pl(n, e.field_70165_t, e.field_70163_u, e.field_70161_v, "journeymap-radar"));
            }
        } catch (Throwable ignored) {}
        if (tabOnly != null && me != null) {
            try {
                Object nh = me.getClass().getField("field_71174_a").get(me);
                List<?> infos = (List<?>) nh.getClass().getField("field_147303_b").get(nh);
                for (Object gi : new ArrayList<Object>(infos)) {
                    String n = String.valueOf(gi.getClass().getField("field_78831_a").get(gi));
                    if (!seen.contains(n) && !n.equals(me.func_70005_c_())) tabOnly.add(n);
                }
            } catch (Throwable ignored) {}
        }
        return out;
    }

    /** Waypoints from JourneyMap's live store, falling back to its json files. */
    public static List<Wp> waypoints(StringBuilder note) {
        List<Wp> out = new ArrayList<Wp>();
        try {
            Class<?> c = Class.forName("journeymap.common.waypoint.WaypointStore");
            Object store = c.getMethod("getInstance").invoke(null);
            Collection<?> all = (Collection<?>) c.getMethod("getAll").invoke(store);
            for (Object o : new ArrayList<Object>(all)) {
                try {
                    String name = String.valueOf(call(o, "getName"));
                    int x = ((Number) call(o, "getX")).intValue(), y = ((Number) call(o, "getY")).intValue(), z = ((Number) call(o, "getZ")).intValue();
                    String dim = "?"; try { dim = String.valueOf(call(o, "getPrimaryDimension")); } catch (Throwable ignored) {}
                    boolean en = true; try { en = (Boolean) call(o, "isEnabled"); } catch (Throwable ignored) {}
                    out.add(new Wp(name, x, y, z, dim, en));
                } catch (Throwable ignored) {}
            }
            if (note != null) note.append("source: JourneyMap ").append(jmVersion()).append(" live waypoint store");
            if (!out.isEmpty()) return out;
        } catch (Throwable t) {
            if (note != null) note.append("JourneyMap store unavailable (").append(t.getClass().getSimpleName()).append(")");
        }
        // fallback: json files of all multiplayer worlds
        try {
            File base = new File(W.gameDir(), "journeymap/data/mp");
            File[] worlds = base.listFiles();
            if (worlds != null) for (File wd : worlds) {
                File[] files = new File(wd, "waypoints").listFiles();
                if (files == null) continue;
                for (File f : files) {
                    if (!f.getName().endsWith(".json")) continue;
                    String s = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                    String name = str(s, "name"); Integer x = num(s, "x"), y = num(s, "y"), z = num(s, "z");
                    if (name != null && x != null && y != null && z != null) out.add(new Wp(name, x, y, z, wd.getName(), !s.contains("\"enable\": false")));
                }
            }
            if (note != null && !out.isEmpty()) note.append("; read from journeymap/data/mp/*/waypoints files");
        } catch (Throwable ignored) {}
        return out;
    }

    private static String str(String s, String k) {
        Matcher m = Pattern.compile("\"" + k + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(s);
        return m.find() ? m.group(1) : null;
    }

    private static Integer num(String s, String k) {
        Matcher m = Pattern.compile("\"" + k + "\"\\s*:\\s*(-?\\d+)").matcher(s);
        return m.find() ? Integer.valueOf(m.group(1)) : null;
    }

    public static Wp findWaypoint(String q) {
        String ql = q.toLowerCase(Locale.ROOT);
        Wp best = null;
        for (Wp w : waypoints(null)) {
            String n = w.name.toLowerCase(Locale.ROOT);
            if (n.equals(ql)) return w;
            if (best == null && n.contains(ql)) best = w;
        }
        return best;
    }

    public static Pl findPlayer(String q) {
        for (Pl p : players(null)) if (p.name.equalsIgnoreCase(q)) return p;
        return null;
    }
}
