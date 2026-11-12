package hmcnav;

import net.minecraft.world.World;

import java.util.*;

/** Small local A* over walkable block positions (feet positions). */
public final class PathFinder {
    public static final class Node {
        public final int x, y, z;
        double g, h, f;
        Node parent;
        boolean closed;
        public final int move; // 0 walk, 1 jump up, 2 drop, 3 swim up, 4 ladder up, 5 ladder down
        Node(int x, int y, int z, int move) { this.x = x; this.y = y; this.z = z; this.move = move; }
        public String toString() { return x + "," + y + "," + z; }
    }

    public static final class Result {
        public final List<Node> path;   // excludes start
        public final boolean complete;  // reaches the goal
        public final double startH, endH;
        public final int expanded;
        Result(List<Node> path, boolean complete, double startH, double endH, int expanded) {
            this.path = path; this.complete = complete; this.startH = startH; this.endH = endH; this.expanded = expanded;
        }
    }

    private final World w;
    private final Map<Long, Integer> kinds = new HashMap<Long, Integer>();
    private final Map<Long, Boolean> stand = new HashMap<Long, Boolean>();
    private final int gx, gz; private final Integer gy;
    private final int sx, sz;
    private final int radius;

    public PathFinder(World w, int sx, int sz, int gx, Integer gy, int gz, int radius) {
        this.w = w; this.sx = sx; this.sz = sz; this.gx = gx; this.gy = gy; this.gz = gz; this.radius = radius;
    }

    private static long key(int x, int y, int z) { return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF); }

    int kind(int x, int y, int z) {
        long k = key(x, y, z);
        Integer v = kinds.get(k);
        if (v == null) { v = W.kind(w, x, y, z); kinds.put(k, v); }
        return v;
    }

    boolean pass(int x, int y, int z) { return W.passable(kind(x, y, z)) && kind(x, y - 1, z) != W.TALL; }

    boolean canStand(int x, int y, int z) {
        long k = key(x, y, z);
        Boolean v = stand.get(k);
        if (v == null) {
            int kf = kind(x, y, z);
            boolean ok;
            if (!(W.passable(kf) || kf == W.LOW) || !W.passable(kind(x, y + 1, z)) || kind(x, y - 1, z) == W.TALL) ok = false;
            else if (kf == W.WATER || kf == W.LOW || kf == W.LADDER) ok = true;
            else { int kb = kind(x, y - 1, z); ok = kb == W.SOLID; }
            v = ok; stand.put(k, v);
        }
        return v;
    }

    private boolean hazardNear(int x, int y, int z) {
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) for (int dy = -1; dy <= 1; dy++) {
            int k = kind(x + dx, y + dy, z + dz);
            if (k == W.LAVA || k == W.FIRE || k == W.DANGER) return true;
        }
        return false;
    }

    double heur(int x, int y, int z) {
        int dx = Math.abs(x - gx), dz = Math.abs(z - gz);
        double hz = Math.max(dx, dz) + 0.4142 * Math.min(dx, dz);
        if (gy != null) hz += Math.abs(y - gy);
        return hz;
    }

    boolean isGoal(Node n) {
        if (n.x != gx || n.z != gz) return false;
        return gy == null || Math.abs(n.y - gy) <= 1;
    }

    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    public Result find(int x0, int y0, int z0, int maxNodes) {
        Map<Long, Node> nodes = new HashMap<Long, Node>();
        PriorityQueue<Node> open = new PriorityQueue<Node>(256, new Comparator<Node>() {
            public int compare(Node a, Node b) { return Double.compare(a.f, b.f); }
        });
        Node start = new Node(x0, y0, z0, 0);
        start.h = heur(x0, y0, z0); start.f = start.h;
        nodes.put(key(x0, y0, z0), start);
        open.add(start);
        Node best = start;
        int expanded = 0;
        while (!open.isEmpty() && expanded < maxNodes) {
            Node cur = open.poll();
            if (cur.closed) continue;
            cur.closed = true;
            expanded++;
            if (isGoal(cur)) return build(cur, true, start.h, expanded);
            if (cur.h < best.h - 1e-6 || (Math.abs(cur.h - best.h) < 1e-6 && cur.g < best.g)) best = cur;
            boolean curWater = kind(cur.x, cur.y, cur.z) == W.WATER;
            // swim straight up
            if (curWater && canStand(cur.x, cur.y + 1, cur.z)) relax(nodes, open, cur, cur.x, cur.y + 1, cur.z, 1.0, 3);
            // ladders: climb up / down
            int kc = kind(cur.x, cur.y, cur.z);
            if (kc == W.LADDER && canStand(cur.x, cur.y + 1, cur.z)) relax(nodes, open, cur, cur.x, cur.y + 1, cur.z, 1.5, 4);
            if (kind(cur.x, cur.y - 1, cur.z) == W.LADDER && canStand(cur.x, cur.y - 1, cur.z)) relax(nodes, open, cur, cur.x, cur.y - 1, cur.z, 1.2, 5);
            for (int[] d : DIRS) {
                int nx = cur.x + d[0], nz = cur.z + d[1];
                if (Math.abs(nx - sx) > radius || Math.abs(nz - sz) > radius) continue;
                boolean diag = d[0] != 0 && d[1] != 0;
                double base = diag ? 1.4142 : 1.0;
                if (diag) {
                    if (!pass(cur.x + d[0], cur.y, cur.z) || !pass(cur.x + d[0], cur.y + 1, cur.z)
                            || !pass(cur.x, cur.y, cur.z + d[1]) || !pass(cur.x, cur.y + 1, cur.z + d[1])) continue;
                }
                int y = cur.y;
                if (diag && (kind(nx, y, nz) == W.DOOR || kind(nx, y + 1, nz) == W.DOOR || kind(cur.x, cur.y, cur.z) == W.DOOR)) continue;
                if (canStand(nx, y, nz)) {
                    relax(nodes, open, cur, nx, y, nz, base, 0);
                } else if (!diag && canStand(nx, y + 1, nz) && pass(cur.x, y + 2, cur.z)) {
                    relax(nodes, open, cur, nx, y + 1, nz, base + 1.0, 1);
                } else if (!diag && pass(nx, y, nz) && pass(nx, y + 1, nz)) {
                    for (int dd = 1; dd <= 12; dd++) {
                        int ny = y - dd;
                        if (ny < 1) break;
                        if (canStand(nx, ny, nz)) {
                            boolean water = kind(nx, ny, nz) == W.WATER;
                            if (dd <= 3 || water) relax(nodes, open, cur, nx, ny, nz, base + dd * 0.5, 2);
                            break;
                        }
                        if (!pass(nx, ny, nz)) break;
                    }
                }
            }
        }
        if (best == start) return new Result(Collections.<Node>emptyList(), false, start.h, start.h, expanded);
        return build(best, false, start.h, expanded);
    }

    private void relax(Map<Long, Node> nodes, PriorityQueue<Node> open, Node cur, int x, int y, int z, double cost, int move) {
        int kf = kind(x, y, z), kh = kind(x, y + 1, z);
        if (kf == W.WATER) cost += 1.0;
        if (kf == W.DOOR || kh == W.DOOR) cost += 2.0;
        if (kh == W.WATER) cost += 4.0; // head under water
        if (hazardNear(x, y, z)) cost += 12.0;
        long k = key(x, y, z);
        Node n = nodes.get(k);
        double g = cur.g + cost;
        if (n == null) {
            n = new Node(x, y, z, move);
            n.h = heur(x, y, z);
            nodes.put(k, n);
        } else if (n.closed || g >= n.g) {
            return;
        }
        Node m = n.move == move ? n : copy(n, move, nodes, k);
        m.g = g; m.parent = cur; m.f = g + m.h * 1.1;
        open.add(m);
    }

    private Node copy(Node n, int move, Map<Long, Node> nodes, long k) {
        Node m = new Node(n.x, n.y, n.z, move);
        m.h = n.h;
        n.closed = true; // invalidate old entry
        nodes.put(k, m);
        return m;
    }

    private Result build(Node end, boolean complete, double startH, int expanded) {
        LinkedList<Node> path = new LinkedList<Node>();
        for (Node n = end; n != null && n.parent != null; n = n.parent) path.addFirst(n);
        return new Result(new ArrayList<Node>(path), complete, startH, end.h, expanded);
    }
}
