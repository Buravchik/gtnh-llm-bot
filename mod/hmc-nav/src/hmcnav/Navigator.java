package hmcnav;

import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.util.MovementInput;
import net.minecraft.world.World;

import java.util.List;
import java.util.Locale;

/** Walks the player to a target using local A* paths, driven from the player's movement input every tick. */
public final class Navigator {
    public static final Navigator INSTANCE = new Navigator();

    public enum State { IDLE, WALKING, ARRIVED, STUCK, STOPPED, FAILED }

    private volatile State state = State.IDLE;
    private volatile String message = "no goto yet";
    private EntityClientPlayerMP player;
    private int tx, tz; private Integer ty;
    private boolean sprint;
    private List<PathFinder.Node> path;
    private boolean pathComplete;
    private int idx;
    private int ticks, lastPlanTick, wpStartTick, lastProgressTick, replans, failedPlans;
    private double bestGoalDist, wpBestDist;
    private boolean sprintSet;
    private double startDist;
    private int jumpHold;

    private Navigator() {}

    public boolean isActive() { return state == State.WALKING; }
    public State getState() { return state; }

    /** Must be called on the main thread. */
    public synchronized String start(int x, Integer y, int z, boolean sprint) {
        EntityClientPlayerMP p = W.player();
        if (p == null) return "Not in a world.";
        if (p.func_110143_aJ() <= 0) return "Player is dead, respawn first.";
        this.player = p;
        this.tx = x; this.ty = y; this.tz = z; this.sprint = sprint;
        this.path = null; this.idx = 0; this.ticks = 0; this.lastPlanTick = -1000; this.replans = 0; this.failedPlans = 0;
        this.bestGoalDist = ty == null ? goalDist(p) : Math.sqrt(Math.pow(goalDist(p), 2) + Math.pow(ty - W.feetY(p), 2)); this.startDist = bestGoalDist; this.lastProgressTick = 0; this.lastCloserTick = 0; this.jumpHold = 0; this.doorClicks = 0; this.doorTicks = 0;
        installInput(p);
        state = State.WALKING;
        message = "walking";
        log("goto started -> " + target() + ", distance " + W.fmt(bestGoalDist));
        return "goto started: target " + target() + ", horizontal distance " + W.fmt(bestGoalDist)
                + ". Poll 'goto status' until state is ARRIVED/STUCK/FAILED.";
    }

    public synchronized void stop(String why) {
        if (state == State.WALKING) finish(State.STOPPED, why);
    }

    void fail(String why) { finish(State.FAILED, why); }

    private void finish(State s, String why) {
        state = s; message = why;
        EntityClientPlayerMP p = player;
        if (p != null) {
            if (sprintSet) { try { p.func_70031_b(false); } catch (Throwable ignored) {} sprintSet = false; }
            restoreInput(p);
        }
        log("goto finished: " + s + " (" + why + ") at " + posStr(p) + ", target " + target());
    }

    private static void installInput(EntityClientPlayerMP p) {
        MovementInput in = p.field_71158_b;
        if (!(in instanceof NavMovementInput)) p.field_71158_b = new NavMovementInput(in);
    }

    private static void restoreInput(EntityClientPlayerMP p) {
        MovementInput in = p.field_71158_b;
        if (in instanceof NavMovementInput && ((NavMovementInput) in).original != null) p.field_71158_b = ((NavMovementInput) in).original;
    }

    private String target() { return ty == null ? (tx + " ~ " + tz) : (tx + " " + ty + " " + tz); }

    private double goalDist(EntityClientPlayerMP p) {
        double dx = tx + 0.5 - p.field_70165_t, dz = tz + 0.5 - p.field_70161_v;
        return Math.sqrt(dx * dx + dz * dz);
    }

    static String posStr(EntityClientPlayerMP p) {
        if (p == null) return "?";
        return W.fmt(p.field_70165_t) + " " + W.fmt(W.feetPosY(p)) + " " + W.fmt(p.field_70161_v);
    }

    public synchronized String status() {
        EntityClientPlayerMP p = W.player();
        StringBuilder sb = new StringBuilder();
        sb.append("state: ").append(state).append(" (").append(message).append(")\n");
        if (p != null) {
            sb.append("position: ").append(posStr(p)).append(", facing ").append(W.facing(p.field_70177_z))
              .append(", health ").append(W.fmt(p.func_110143_aJ())).append("/20");
            try { sb.append(", food ").append(p.func_71024_bL().func_75116_a()).append("/20"); } catch (Throwable ignored) {}
            sb.append('\n');
        }
        if (state != State.IDLE) {
            sb.append("target: ").append(target());
            if (p != null) {
                sb.append(", horizontal distance ").append(W.fmt(goalDist(p)));
                if (ty != null) sb.append(", dy ").append(ty - W.feetY(p));
            }
            sb.append(", started at distance ").append(W.fmt(startDist));
            sb.append(", elapsed ").append(ticks / 20).append("s");
            if (path != null && state == State.WALKING)
                sb.append(", path ").append(Math.max(0, path.size() - idx)).append(" steps").append(pathComplete ? " (to target)" : " (partial, replans as it goes)");
        }
        return sb.toString();
    }

    // ---------------- tick ----------------

    void tick(NavMovementInput in) {
        EntityClientPlayerMP p = W.player();
        World w = W.world();
        if (p == null || w == null || p != player) { finish(State.STOPPED, "player changed (respawn/dimension change/disconnect)"); return; }
        if (p.func_110143_aJ() <= 0) { finish(State.FAILED, "player died"); return; }
        ticks++;
        int fx = W.feetX(p), fy = W.feetY(p), fz = W.feetZ(p);
        double gd = goalDist(p);
        boolean yOk = ty == null || Math.abs(ty - fy) <= 1;
        if (gd < 0.8 && yOk) { finish(State.ARRIVED, "reached target"); return; }
        if (ty != null && gd < 0.8 && !yOk) {
            // at the column but wrong height: try planning, the planner handles it
        }
        double gd3 = ty == null ? gd : Math.sqrt(gd * gd + (double) (ty - fy) * (ty - fy));
        if (gd3 < bestGoalDist - 0.5) { bestGoalDist = gd3; lastProgressTick = ticks; lastCloserTick = ticks; }
        if (ticks - lastCloserTick > 20 * 120) { finish(State.STUCK, "did not get closer to the target for 2 min (going in circles?) near " + fx + " " + fy + " " + fz); return; }
        if (ticks - lastProgressTick > 300) { finish(State.STUCK, "no progress for 15s, blocked near " + fx + " " + fy + " " + fz); return; }
        if (ticks > 20 * 60 * 30) { finish(State.STOPPED, "timeout 30 min"); return; }

        boolean needPlan = path == null || idx >= path.size();
        if (!needPlan && ticks - wpStartTick > 40) needPlan = true;              // waypoint not reached in 2s
        if (!needPlan && !pathComplete && path.size() - idx < 6 && ticks - lastPlanTick > 10) needPlan = true;
        if (!needPlan && ticks - lastPlanTick > 100) needPlan = true;             // refresh: world changes, more chunks loaded
        if (!needPlan) {
            PathFinder.Node wp = path.get(idx);
            if (W.kind(w, wp.x, wp.y, wp.z) == W.DOOR || W.kind(w, wp.x, wp.y + 1, wp.z) == W.DOOR) { /* door handling, no replan */ }
            else if (Math.abs(wp.x + 0.5 - p.field_70165_t) > 3 || Math.abs(wp.z + 0.5 - p.field_70161_v) > 3 || Math.abs(wp.y - fy) > 4) needPlan = true;
        }
        if (needPlan && ticks - lastPlanTick >= 5) {
            if (path != null && idx < path.size() && ticks - wpStartTick > 40) replans++;
            if (!plan(w, p, fx, fy, fz)) return;
        }
        if (path == null || path.isEmpty()) return;
        follow(in, p, w, fx, fy, fz);
    }

    private boolean plan(World w, EntityClientPlayerMP p, int fx, int fy, int fz) {
        lastPlanTick = ticks;
        int sy = fy;
        PathFinder pf = new PathFinder(w, fx, fz, tx, ty, tz, 48);
        if (!pf.canStand(fx, sy, fz)) {
            if (pf.canStand(fx, sy + 1, fz)) sy = sy + 1; else if (pf.canStand(fx, sy - 1, fz)) sy = sy - 1;
        }
        PathFinder.Result r = pf.find(fx, sy, fz, 30000);
        if (r.path.isEmpty() || (!r.complete && r.endH > r.startH - 1.0)) {
            failedPlans++;
            path = null;
            if (failedPlans >= 4 && goalDist(p) < 2.0 && ty != null) {
                int diff = ty - fy;
                finish(State.ARRIVED, "reached x/z of the target, but target y " + ty + " is " + Math.abs(diff) + " blocks "
                        + (diff < 0 ? "below" : "above") + " (inside terrain/cave or not walkable; no digging/building)");
                return false;
            }
            if (failedPlans >= 4) {
                finish(State.STUCK, "no walkable path found towards target from " + fx + " " + fy + " " + fz
                        + " (walls, water/lava, cliffs or unloaded chunks in the way). Use scan to look around.");
                return false;
            }
            return true;
        }
        failedPlans = 0;
        path = r.path; pathComplete = r.complete; idx = 0; wpStartTick = ticks; wpBestDist = 1e9;
        return true;
    }

    private int doorTicks, doorClicks, lastCloserTick;

    private void follow(NavMovementInput in, EntityClientPlayerMP p, World w, int fx, int fy, int fz) {
        // advance waypoints we are on or have passed
        for (int look = Math.min(path.size() - 1, idx + 4); look >= idx; look--) {
            PathFinder.Node n = path.get(look);
            double dx = n.x + 0.5 - p.field_70165_t, dz = n.z + 0.5 - p.field_70161_v;
            boolean vertical = n.move >= 3;
            boolean yMatch = n.y == fy || (!vertical && Math.abs(n.y - fy) <= 1 && W.kind(w, n.x, n.y, n.z) == W.WATER);
            boolean onIt = n.x == fx && n.z == fz && yMatch;
            boolean close = dx * dx + dz * dz < 0.35 * 0.35 && yMatch;
            if (onIt && !vertical || close) {
                idx = look + 1; wpStartTick = ticks; wpBestDist = 1e9; doorClicks = 0; lastProgressTick = ticks;
                break;
            }
        }
        if (idx >= path.size()) return;
        PathFinder.Node wp = path.get(idx);
        double dx = wp.x + 0.5 - p.field_70165_t, dz = wp.z + 0.5 - p.field_70161_v;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < wpBestDist) wpBestDist = dist;

        boolean inWater = false;
        try { inWater = p.func_70090_H(); } catch (Throwable ignored) {}
        boolean inLava = false;
        try { inLava = p.func_70058_J(); } catch (Throwable ignored) {}
        boolean sameColumn = wp.x == fx && wp.z == fz;

        // closed door / fence gate in the way: open it by hand (right click), never break
        if (dist < 2.6 && !sameColumn) {
            int px0, pz0;
            if (idx > 0) { px0 = path.get(idx - 1).x; pz0 = path.get(idx - 1).z; } else { px0 = fx; pz0 = fz; }
            boolean alongX = Math.abs(wp.x - px0) >= Math.abs(wp.z - pz0);
            int doorY = Integer.MIN_VALUE, dX = wp.x, dZ = wp.z;
            int[][] cells = {{wp.x, wp.y, wp.z}, {wp.x, wp.y + 1, wp.z}, {px0, wp.y, pz0}, {px0, wp.y + 1, pz0}};
            for (int[] c : cells) {
                if (W.kind(w, c[0], c[1], c[2]) == W.DOOR && W.doorBlocks(w, c[0], c[1], c[2], alongX)) { doorY = c[1]; dX = c[0]; dZ = c[2]; break; }
            }
            if (doorY != Integer.MIN_VALUE) {
                faceBlock(p, dX + 0.5, doorY + 0.5, dZ + 0.5);
                in.field_78900_b = 0f;
                wpStartTick = ticks; lastProgressTick = ticks;
                if (++doorTicks >= 10) {
                    doorTicks = 0;
                    if (doorClicks >= 3) { finish(State.STUCK, "door at " + dX + " " + doorY + " " + dZ + " does not open (locked/iron/protected)"); return; }
                    doorClicks++;
                    if (rightClick(p, w, dX, doorY, dZ)) log("clicked door at " + dX + " " + doorY + " " + dZ);
                }
                return;
            }
        }

        float desired;
        float forward;
        boolean jump = false;
        if (sameColumn && wp.move >= 3) {
            // vertical move: ladder or swimming
            int[] wall = W.kind(w, fx, fy, fz) == W.LADDER ? W.ladderWall(w, fx, fy, fz) : null;
            if (wall == null && W.kind(w, wp.x, wp.y, wp.z) == W.LADDER) wall = W.ladderWall(w, wp.x, wp.y, wp.z);
            desired = wall != null ? W.yawTo(wall[0], wall[1]) : p.field_70177_z;
            if (wp.y > fy) { forward = wall != null ? 1f : 0f; jump = true; }
            else { forward = 0f; }
            if (dist > 0.25) { desired = W.yawTo(dx, dz); forward = 0.3f; }
        } else {
            desired = W.yawTo(dx, dz);
            forward = 1f;
        }
        float delta = W.wrap(desired - p.field_70177_z);
        float step = Math.max(-40f, Math.min(40f, delta));
        p.field_70177_z += step;
        float pitchDelta = 20f - p.field_70125_A;
        p.field_70125_A += Math.max(-10f, Math.min(10f, pitchDelta));
        float remaining = Math.abs(delta - step);
        if (!(sameColumn && wp.move >= 3)) {
            forward = remaining < 30f ? 1f : (remaining < 70f ? 0.4f : 0f);
            if (dist < 0.25) forward = 0f;
            // slow down before a waypoint that is a drop or turn, so we don't overshoot ledges
            if (wp.move == 2 && dist < 1.2) forward = Math.min(forward, 0.6f);
        }
        in.field_78900_b = forward;

        if (!sameColumn && wp.y > fy && dist < 1.9 && !inWater) jump = true;            // step up
        if (inWater || inLava) {
            int headK = W.kind(w, fx, W.floor(W.feetPosY(p) + 1.4), fz);
            if (wp.y >= fy || headK == W.WATER || inLava) jump = true;   // swim up / keep head above water
        }
        if (p.field_70122_E && p.field_70123_F && forward > 0 && wp.y >= fy && W.kind(w, fx, fy, fz) != W.LADDER) jump = true;  // bumped into something
        if (jumpHold > 0) { jump = true; jumpHold--; }
        if (jump && p.field_70122_E && !inWater && !sameColumn) jumpHold = 2;
        in.field_78901_c = jump;

        boolean wantSprint = sprint && forward >= 1f && remaining < 10f && !inWater && p.field_70122_E && !sameColumn;
        if (sprint) {
            try {
                int food = p.func_71024_bL().func_75116_a();
                if (food <= 6) wantSprint = false;
            } catch (Throwable ignored) {}
            if (wantSprint) { p.func_70031_b(true); sprintSet = true; }
            else if (sprintSet && p.field_70122_E) { p.func_70031_b(false); }
        }
    }

    static void faceBlock(EntityClientPlayerMP p, double x, double y, double z) {
        double dx = x - p.field_70165_t, dy = y - (W.feetPosY(p) + 1.62), dz = z - p.field_70161_v;
        p.field_70177_z = p.field_70177_z + W.wrap(W.yawTo(dx, dz) - p.field_70177_z);
        p.field_70125_A = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
    }

    /** Right-clicks (uses) a block, like opening a door by hand. */
    public static boolean rightClick(EntityClientPlayerMP p, World w, int x, int y, int z) {
        try {
            net.minecraft.client.Minecraft mc = W.mc();
            double dx = x + 0.5 - p.field_70165_t, dz = z + 0.5 - p.field_70161_v;
            int side = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? 4 : 5) : (dz > 0 ? 2 : 3);
            net.minecraft.util.Vec3 hit = net.minecraft.util.Vec3.func_72443_a(x + 0.5, y + 0.5, z + 0.5);
            boolean r = mc.field_71442_b.func_78760_a(p, w, p.field_71071_by.func_70448_g(), x, y, z, side, hit);
            p.func_71038_i();
            return r;
        } catch (Throwable t) {
            log("right click failed: " + t);
            return false;
        }
    }

    static void log(String s) {
        System.out.println("[hmc-nav] " + s);
    }

    public static String fmtLoc(double x, double y, double z) {
        return String.format(Locale.ROOT, "%.1f %.1f %.1f", x, y, z);
    }
}
