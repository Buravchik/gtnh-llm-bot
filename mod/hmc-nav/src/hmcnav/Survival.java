package hmcnav;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

/** Inventory listing, eating and auto-eat (runs once a second on the client thread, independent of goto). */
public final class Survival {
    private Survival() {}
    public static volatile boolean autoEat = true;
    public static final int AUTO_AT = 14, AUTO_UNTIL = 18;
    private static volatile boolean started;
    private static boolean eating; private static int eatSecs, eatTarget, eaten;
    public static volatile String last = "never ate";
    private static final String[] BAD = {"rotten", "spider", "poison", "puffer", "raw chicken", "chicken (raw)", "rawchicken", "zombie", "flesh", "golden apple", "goldenapple"};

    public static synchronized void ensure() {
        if (started) return; started = true;
        Thread t = new Thread(new Runnable() { public void run() {
            while (true) {
                try { Thread.sleep(1000); final Minecraft mc = W.mc(); if (mc != null) mc.getClass().getMethod("func_152344_a", Runnable.class).invoke(mc, new Runnable() { public void run() { try { tick(); } catch (Throwable e) { last = "auto-eat error: " + e; } } }); }
                catch (Throwable ignored) {}
            }
        }}, "hmc-nav-survival");
        t.setDaemon(true); t.start();
    }

    // ---------- reflection helpers ----------
    static Object screen(Minecraft mc) { try { return mc.getClass().getField("field_71462_r").get(mc); } catch (Throwable t) { return null; } }
    static boolean blockingScreen(Minecraft mc) {
        Object sc = screen(mc);
        if (sc == null) return false;
        try { return Class.forName("net.minecraft.client.gui.inventory.GuiContainer").isInstance(sc); } catch (Throwable t) { return false; }
    }
    static Object inv(EntityClientPlayerMP p) { return p.field_71071_by; }
    static Object[] main(EntityClientPlayerMP p) throws Exception { Object i = inv(p); return (Object[]) i.getClass().getField("field_70462_a").get(i); }
    static int cur(EntityClientPlayerMP p) throws Exception { Object i = inv(p); return i.getClass().getField("field_70461_c").getInt(i); }
    static void setCur(EntityClientPlayerMP p, int s) throws Exception { Object i = inv(p); i.getClass().getField("field_70461_c").setInt(i, s); }
    public static int food(EntityClientPlayerMP p) { try { return p.func_71024_bL().func_75116_a(); } catch (Throwable t) { return 20; } }
    static Object item(Object st) { try { return st.getClass().getMethod("func_77973_b").invoke(st); } catch (Throwable t) { return null; } }
    static int count(Object st) { try { return st.getClass().getField("field_77994_a").getInt(st); } catch (Throwable t) { return 1; } }
    static String name(Object st) { try { return (String) st.getClass().getMethod("func_82833_r").invoke(st); } catch (Throwable t) { return String.valueOf(st); } }
    private static Class<?> itemFood;
    /** Hunger points restored, or -1 if not (safe) food. */
    static int heal(Object st) {
        try {
            if (itemFood == null) itemFood = Class.forName("net.minecraft.item.ItemFood");
            Object it = item(st);
            if (it == null || !itemFood.isInstance(it)) return -1;
            String n = name(st).toLowerCase(Locale.ROOT);
            for (String b : BAD) if (n.contains(b)) return -1;
            Method m = itemFood.getMethod("func_150905_g", st.getClass().getName().equals("net.minecraft.item.ItemStack") ? st.getClass() : Class.forName("net.minecraft.item.ItemStack"));
            return (Integer) m.invoke(it, st);
        } catch (Throwable t) { return -1; }
    }

    public static String inventory() {
        EntityClientPlayerMP p = W.player();
        if (p == null) return "Not in a world.";
        StringBuilder sb = new StringBuilder();
        try {
            Object[] m = main(p); int c = cur(p), n = 0;
            sb.append("Inventory (hotbar = slots 0-8, selected ").append(c).append("; food ").append(food(p)).append("/20, auto-eat ").append(autoEat ? "on" : "off").append("):\n");
            for (int i = 0; i < m.length; i++) {
                if (m[i] == null) continue; n++;
                int h = heal(m[i]);
                sb.append("  ").append(i < 9 ? "hotbar " + i : "slot " + i).append(i == c ? " [held]" : "").append(": ").append(name(m[i])).append(" x").append(count(m[i]))
                  .append(h >= 0 ? " (food +" + h + ")" : "").append('\n');
            }
            if (n == 0) sb.append("  empty\n");
        } catch (Throwable t) { sb.append("error reading inventory: ").append(t); }
        return sb.toString().trim();
    }

    public static boolean hasFood() {
        EntityClientPlayerMP p = W.player();
        try { for (Object s : main(p)) if (s != null && heal(s) >= 0) return true; } catch (Throwable ignored) {}
        return false;
    }
    public static boolean isEating() { return eating; }

    /** Start eating until food >= target. Must run on the client thread. */
    public static String eat(int target) {
        EntityClientPlayerMP p = W.player();
        if (p == null) return "Not in a world.";
        int f = food(p);
        if (f >= 20) return "Not hungry (food 20/20).";
        if (eating) return "Already eating (food " + f + "/20).";
        eatTarget = Math.min(20, target); eaten = 0;
        String r = biteNext(p);
        return r == null ? "No food in inventory (food " + f + "/20)." : r;
    }

    private static String biteNext(EntityClientPlayerMP p) {
        try {
            Minecraft mc = W.mc();
            W.keepAwake();
            if (blockingScreen(mc)) { last = "cannot eat: a container GUI is open"; return null; }
            Object[] m = main(p);
            int need = 20 - food(p), best = -1, bestScore = Integer.MAX_VALUE;
            for (int i = 0; i < m.length; i++) {
                if (m[i] == null) continue;
                int h = heal(m[i]); if (h < 0) continue;
                int score = Math.abs(need - h) + (i < 9 ? 0 : 1); // closest fit, prefer hotbar
                if (score < bestScore) { bestScore = score; best = i; }
            }
            if (best < 0) return null;
            int slot = best;
            if (best >= 9) { // swap into the selected hotbar slot
                int c = cur(p);
                Object pc = mc.getClass().getField("field_71442_b").get(mc);
                int win = p.getClass().getField("field_71069_bz").get(p).getClass().getField("field_75152_c").getInt(p.getClass().getField("field_71069_bz").get(p));
                pc.getClass().getMethod("func_78753_a", int.class, int.class, int.class, int.class, Class.forName("net.minecraft.entity.player.EntityPlayer")).invoke(pc, win, best, c, 2, p);
                slot = c;
            }
            setCur(p, slot);
            Object pc = mc.getClass().getField("field_71442_b").get(mc);
            try { Method sync = pc.getClass().getDeclaredMethod("func_78750_j"); sync.setAccessible(true); sync.invoke(pc); } catch (Throwable ignored) {}
            Object st = main(p)[slot];
            if (st == null) return null;
            pc.getClass().getMethod("func_78769_a", Class.forName("net.minecraft.entity.player.EntityPlayer"), Class.forName("net.minecraft.world.World"), Class.forName("net.minecraft.item.ItemStack"))
              .invoke(pc, p, W.world(), st);
            holdUse(mc, true);
            eating = true; eatSecs = 0;
            last = "eating " + name(st) + " (food " + food(p) + "/20)";
            return "Eating " + name(st) + " from hotbar " + slot + " until food " + eatTarget + "/20 (now " + food(p) + "/20).";
        } catch (Throwable t) { holdUse(W.mc(), false); eating = false; last = "eat failed: " + t; return "eat failed: " + t; }
    }

    static void holdUse(Minecraft mc, boolean on) {
        try {
            Object gs = mc.getClass().getField("field_71474_y").get(mc);
            Object kb = gs.getClass().getField("field_74313_G").get(gs);
            Field pr = kb.getClass().getDeclaredField("field_74513_e"); pr.setAccessible(true); pr.setBoolean(kb, on);
            Field d = mc.getClass().getDeclaredField("field_71467_ac"); d.setAccessible(true); d.setInt(mc, on ? 40 : 6); // no accidental block right-click
        } catch (Throwable ignored) {}
    }

    static void tick() {
        EntityClientPlayerMP p = W.player(); Minecraft mc = W.mc();
        if (p == null) { eating = false; return; }
        if (eating) {
            eatSecs++;
            holdUse(mc, true);
            boolean using = false; try { using = p.func_71039_bw(); } catch (Throwable ignored) {}
            if (!using || eatSecs > 6) {
                holdUse(mc, false); eating = false; eaten++;
                last = "ate, food now " + food(p) + "/20";
                if (food(p) < eatTarget && eaten < 12 && p.func_110143_aJ() > 0) biteNext(p);
            }
            return;
        }
        if (autoEat && p.func_110143_aJ() > 0 && food(p) <= AUTO_AT && !blockingScreen(mc)) {
            eatTarget = AUTO_UNTIL; eaten = 0;
            if (biteNext(p) != null) Navigator.log("auto-eat: " + last);
        }
    }
}
