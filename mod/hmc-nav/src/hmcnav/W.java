package hmcnav;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** World helpers: classifies blocks for walking. */
public final class W {
    public static final int AIR = 0, SOLID = 1, LOW = 2, TALL = 3, WATER = 4, LAVA = 5, FIRE = 6, DANGER = 7, UNKNOWN = 8, DOOR = 9, LADDER = 10;
    private static final java.util.Map<Block, Integer> special = new java.util.IdentityHashMap<Block, Integer>();

    private static Material matWater, matLava, matFire, matCactus, matWeb;
    private static boolean matInit;
    private static Object registry;
    private static Method nameMethod;
    private static boolean regInit;

    private W() {}

    public static Minecraft mc() { return Minecraft.func_71410_x(); }
    public static EntityClientPlayerMP player() { Minecraft mc = mc(); return mc == null ? null : mc.field_71439_g; }
    public static World world() { Minecraft mc = mc(); return mc == null ? null : mc.field_71441_e; }

    private static Material mat(String name) {
        try {
            Block b = Block.func_149684_b(name);
            return b == null ? null : b.func_149688_o();
        } catch (Throwable t) { return null; }
    }

    private static void initMats() {
        if (matInit) return;
        matWater = mat("minecraft:water");
        matLava = mat("minecraft:lava");
        matFire = mat("minecraft:fire");
        matCactus = mat("minecraft:cactus");
        matWeb = mat("minecraft:web");
        matInit = matLava != null;
    }

    public static boolean loaded(World w, int x, int y, int z) {
        if (y < 0) return false;
        if (y > 255) return true;
        try { return w.func_72899_e(x, y, z); } catch (Throwable t) { return false; }
    }

    public static int kind(World w, int x, int y, int z) {
        if (y > 255) return AIR;
        if (!loaded(w, x, y, z)) return UNKNOWN;
        initMats();
        Block b;
        try { b = w.func_147439_a(x, y, z); } catch (Throwable t) { return UNKNOWN; }
        if (b == null) return AIR;
        Material m;
        try { m = b.func_149688_o(); } catch (Throwable t) { return SOLID; }
        if (m != null) {
            if (m == matLava) return LAVA;
            if (m == matFire) return FIRE;
            if (m == matCactus || m == matWeb) return DANGER;
            try { if (m.func_76224_d()) return m == matWater ? WATER : (isHot(b) ? LAVA : WATER); } catch (Throwable ignored) {}
        }
        int sp = special(b);
        if (sp == LADDER) return LADDER;
        AxisAlignedBB bb;
        try { bb = b.func_149668_a(w, x, y, z); } catch (Throwable t) { return SOLID; }
        if (bb == null) return AIR;
        if (sp == DOOR) return DOOR;
        double h = bb.field_72337_e - y;
        if (h <= 0.5) return LOW;
        if (h > 1.05) return TALL;
        return SOLID;
    }

    /** DOOR for doors/fence gates a hand can open, LADDER for ladders, else AIR. */
    public static int special(Block b) {
        Integer v = special.get(b);
        if (v == null) {
            String n = name(b).toLowerCase();
            String path = n.contains(":") ? n.substring(n.indexOf(':') + 1) : n;
            if (path.contains("ladder")) v = LADDER;
            else if ((path.contains("door") && !path.contains("trap") && !path.contains("iron") && !path.contains("steel"))
                    || path.contains("fence_gate") || path.contains("fencegate")) v = DOOR;
            else v = AIR;
            special.put(b, v);
        }
        return v;
    }

    /** True if the door/gate at x,y,z currently blocks walking along X (alongX) or along Z. */
    public static boolean doorBlocks(World w, int x, int y, int z, boolean alongX) {
        try {
            Block b = w.func_147439_a(x, y, z);
            AxisAlignedBB bb = b.func_149668_a(w, x, y, z);
            if (bb == null) return false;
            double wx = bb.field_72336_d - bb.field_72340_a, wz = bb.field_72334_f - bb.field_72339_c;
            if (wx > 0.9 && wz > 0.9) return true;
            return alongX ? (wz > 0.9) : (wx > 0.9);
        } catch (Throwable t) { return true; }
    }

    /** Direction (dx,dz) from a ladder to the block it hangs on, from its metadata. */
    public static int[] ladderWall(World w, int x, int y, int z) {
        int m = 0;
        try { m = w.func_72805_g(x, y, z); } catch (Throwable ignored) {}
        switch (m) {
            case 2: return new int[]{0, 1};
            case 3: return new int[]{0, -1};
            case 4: return new int[]{1, 0};
            case 5: return new int[]{-1, 0};
            default: return null;
        }
    }

    private static boolean isHot(Block b) {
        String n = name(b).toLowerCase();
        return n.contains("lava") || n.contains("pyrotheum") || n.contains("fire") || n.contains("molten") || n.contains("hot");
    }

    public static boolean isWater(World w, int x, int y, int z) { return kind(w, x, y, z) == WATER; }

    /** Block a body (feet or head) can be in. */
    public static boolean passable(int k) { return k == AIR || k == WATER || k == DOOR || k == LADDER; }

    public static boolean passableAt(World w, int x, int y, int z) {
        return passable(kind(w, x, y, z)) && kind(w, x, y - 1, z) != TALL;
    }

    /** Can the player's feet be in block (x,y,z)? */
    public static boolean canStand(World w, int x, int y, int z) {
        int kf = kind(w, x, y, z);
        if (!(passable(kf) || kf == LOW)) return false;
        if (!passable(kind(w, x, y + 1, z))) return false;
        if (kf == WATER || kf == LOW || kf == LADDER) return true;
        int kb = kind(w, x, y - 1, z);
        return kb == SOLID;
    }

    public static boolean isWood(World w, int x, int y, int z) {
        try { Block b = w.func_147439_a(x, y, z); return b != null && b.isWood(w, x, y, z); } catch (Throwable t) { return false; }
    }

    public static boolean isLeaves(World w, int x, int y, int z) {
        try { Block b = w.func_147439_a(x, y, z); return b != null && b.isLeaves(w, x, y, z); } catch (Throwable t) { return false; }
    }

    public static String name(Block b) {
        if (b == null) return "air";
        try {
            if (!regInit) {
                regInit = true;
                Field f = Block.class.getField("field_149771_c");
                registry = f.get(null);
                nameMethod = registry.getClass().getMethod("func_148750_c", Object.class);
            }
            if (nameMethod != null) {
                Object n = nameMethod.invoke(registry, b);
                if (n != null) return n.toString();
            }
        } catch (Throwable ignored) {}
        try { return String.valueOf(b.func_149739_a()); } catch (Throwable t) { return b.getClass().getSimpleName(); }
    }

    public static String blockName(World w, int x, int y, int z) {
        try { return name(w.func_147439_a(x, y, z)); } catch (Throwable t) { return "?"; }
    }

    // ---- player helpers ----
    public static int feetX(EntityClientPlayerMP p) { return floor(p.field_70165_t); }
    public static int feetZ(EntityClientPlayerMP p) { return floor(p.field_70161_v); }
    public static double feetPosY(EntityClientPlayerMP p) {
        AxisAlignedBB bb = p.field_70121_D;
        return bb != null ? bb.field_72338_b : p.field_70163_u - 1.62;
    }
    public static int feetY(EntityClientPlayerMP p) { return floor(feetPosY(p) + 0.01); }

    public static int floor(double d) { int i = (int) d; return d < i ? i - 1 : i; }

    public static float wrap(float a) {
        a %= 360f;
        if (a >= 180f) a -= 360f;
        if (a < -180f) a += 360f;
        return a;
    }

    /** Minecraft yaw for a direction: 0 = south (+Z), 90 = west (-X), 180 = north (-Z), -90 = east (+X). */
    public static float yawTo(double dx, double dz) { return (float) Math.toDegrees(Math.atan2(-dx, dz)); }

    public static String facing(float yaw) {
        String[] n = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
        int i = Math.round(wrap(yaw) / 45f);
        return n[((i % 8) + 8) % 8];
    }

    /** Bots run with the window in the background: turn off pause-on-lost-focus and close the pause menu it opened. */
    public static void keepAwake() {
        try {
            Object mc = mc();
            Object gs = mc.getClass().getField("field_71474_y").get(mc);
            java.lang.reflect.Field f = gs.getClass().getField("field_82881_y");
            if (f.getBoolean(gs)) f.setBoolean(gs, false);
            Object scr = mc.getClass().getField("field_71462_r").get(mc);
            if (scr != null && scr.getClass().getSimpleName().equals("GuiIngameMenu"))
                mc.getClass().getMethod("func_147108_a", Class.forName("net.minecraft.client.gui.GuiScreen")).invoke(mc, (Object) null);
        } catch (Throwable ignored) {}
    }

    public static java.io.File gameDir() {
        try { return (java.io.File) mc().getClass().getField("field_71412_D").get(mc()); } catch (Throwable t) { return new java.io.File("."); }
    }

    public static String fmt(double d) { return String.format(java.util.Locale.ROOT, "%.1f", d); }
}
