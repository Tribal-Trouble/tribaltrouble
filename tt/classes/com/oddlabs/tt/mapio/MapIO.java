package com.oddlabs.tt.mapio;

import com.oddlabs.tt.gui.LocalInput;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.model.IronSupply;
import com.oddlabs.tt.model.Plants;
import com.oddlabs.tt.model.RockSupply;
import com.oddlabs.tt.model.SupplyModel;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.render.SpriteKey;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Minimal TLV-based .ttmap reader/writer for the editor.
 * Persists heightmap, gameplay grids, and a subset of resources (rock, iron, plants).
 * Skips unknown sections and tolerates missing ones (regenerating defaults at load sites).
 */
public final class MapIO {
    private MapIO() {}

    // File header
    private static final int MAGIC_TTMP = 0x54544D50; // 'TTMP'
    private static final int VERSION_1 = 1;

    // Section tags (4-char ints)
    private static final int TAG_META = 0x4D455441;   // 'META'
    private static final int TAG_HM3Z = 0x484D335A;   // 'HM3Z' (float32 heightmap, deflated)
    private static final int TAG_GRID = 0x47524944;   // 'GRID'
    private static final int TAG_ROCK = 0x524F434B;   // 'ROCK'
    private static final int TAG_IRON = 0x49524F4E;   // 'IRON'
    private static final int TAG_PLTS = 0x504C5453;   // 'PLTS' (plants)

    public static final class LoadedMap {
        public int metersPerWorld;
        public int terrainType;
        public float seaLevel;
        public int size;
        public float[][] heights; // [y][x]
        public boolean[][] access;
        public boolean[][] dock;
        public boolean[][] water;
        public byte[][] build;
    public final List<int[]> rocks = new ArrayList<>();
    public final List<int[]> iron = new ArrayList<>();
    public final List<Plant> plants = new ArrayList<>();
    }

    public static final class Plant {
        public final int typeIndex; // 0..3 for current terrain's plant sheet
        public final float x;
        public final float y;
        public Plant(int typeIndex, float x, float y) {
            this.typeIndex = typeIndex;
            this.x = x;
            this.y = y;
        }
    }

    // Location: <game_dir>/maps
    public static File mapsDir() {
        File base = LocalInput.getGameDir();
        File dir = new File(base, "maps");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public static void saveEditorWorld(World world, int terrainType, File target) throws IOException {
        HeightMap hm = world.getHeightMap();
        int n = hm.getGridUnitsPerWorld();
        // Collect resources
        List<int[]> rocks = new ArrayList<>();
        List<int[]> iron = new ArrayList<>();
        List<Plant> plants = new ArrayList<>();
        collectResourcesForSave(world, terrainType, rocks, iron, plants);

        try (DataOutputStream out =
                     new DataOutputStream(new BufferedOutputStream(new FileOutputStream(target)))) {
            // Header
            out.writeInt(MAGIC_TTMP);
            out.writeInt(VERSION_1);

            // META
            ByteArrayOutputStream metaBuf = new ByteArrayOutputStream();
            try (DataOutputStream meta = new DataOutputStream(metaBuf)) {
                meta.writeInt(hm.getMetersPerWorld());
                meta.writeInt(terrainType);
                meta.writeFloat(hm.getSeaLevelMeters());
                meta.writeInt(n); // grid size (redundant but handy)
                meta.flush();
            }
            writeSection(out, TAG_META, metaBuf.toByteArray());

            // HM3Z (floats row-major)
            ByteArrayOutputStream hmBuf = new ByteArrayOutputStream();
            try (DataOutputStream hmOut = new DataOutputStream(new DeflaterOutputStream(hmBuf))) {
                hmOut.writeInt(n);
                for (int y = 0; y < n; y++) {
                    for (int x = 0; x < n; x++) hmOut.writeFloat(hm.getHeight(x, y));
                }
                hmOut.flush();
            }
            writeSection(out, TAG_HM3Z, hmBuf.toByteArray());

            // GRID (uncompressed, simple 1 byte per cell)
            ByteArrayOutputStream gridBuf = new ByteArrayOutputStream();
            try (DataOutputStream gridOut = new DataOutputStream(gridBuf)) {
                gridOut.writeInt(n);
                // access
                boolean[][] access = hm.getAccessGrid();
                for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) gridOut.writeByte(access[y][x] ? 1 : 0);
                // dock
                boolean[][] dock = hm.getDockGrid();
                for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) gridOut.writeByte(dock[y][x] ? 1 : 0);
                // water
                boolean[][] water = hm.getWaterGrid();
                for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) gridOut.writeByte(water[y][x] ? 1 : 0);
                // build (bytes)
                byte[][] build = hm.getBuildGrid();
                for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) gridOut.writeByte(build[y][x]);
                gridOut.flush();
            }
            writeSection(out, TAG_GRID, gridBuf.toByteArray());

            // ROCK
            ByteArrayOutputStream rockBuf = new ByteArrayOutputStream();
            try (DataOutputStream rockOut = new DataOutputStream(rockBuf)) {
                rockOut.writeInt(rocks.size());
                for (int[] rc : rocks) { rockOut.writeInt(rc[0]); rockOut.writeInt(rc[1]); }
                rockOut.flush();
            }
            writeSection(out, TAG_ROCK, rockBuf.toByteArray());

            // IRON
            ByteArrayOutputStream ironBuf = new ByteArrayOutputStream();
            try (DataOutputStream ironOut = new DataOutputStream(ironBuf)) {
                ironOut.writeInt(iron.size());
                for (int[] ic : iron) { ironOut.writeInt(ic[0]); ironOut.writeInt(ic[1]); }
                ironOut.flush();
            }
            writeSection(out, TAG_IRON, ironBuf.toByteArray());

            // PLTS
            ByteArrayOutputStream plantBuf = new ByteArrayOutputStream();
            try (DataOutputStream plantOut = new DataOutputStream(plantBuf)) {
                plantOut.writeInt(plants.size());
                for (Plant p : plants) { plantOut.writeInt(p.typeIndex); plantOut.writeFloat(p.x); plantOut.writeFloat(p.y); }
                plantOut.flush();
            }
            writeSection(out, TAG_PLTS, plantBuf.toByteArray());
        }
    }

    private static void writeSection(DataOutputStream out, int tag, byte[] data) throws IOException {
        out.writeInt(tag);
        out.writeInt(data.length);
        out.write(data);
    }

    private static void collectResourcesForSave(
            World world, int terrainType, List<int[]> rocks, List<int[]> irons, List<Plant> plants) {
        // Precompute sprite mapping for plant type index
        SpriteKey[] plantSprites0 = world.getLandscapeResources().getPlants()[terrainType];

        com.oddlabs.tt.model.ElementNodeVisitor visitor = new com.oddlabs.tt.model.ElementNodeVisitor() {
            @Override public void visitNode(com.oddlabs.tt.model.ElementNode node) {
                node.visitChildren(this);
            }
            @Override public void visitLeaf(com.oddlabs.tt.model.ElementLeaf leaf) {
                leaf.visitElements(this);
            }
            @Override public void visit(com.oddlabs.tt.model.Element element) {
                if (element instanceof RockSupply) {
                    SupplyModel s = (SupplyModel) element;
                    rocks.add(new int[] {s.getGridX(), s.getGridY()});
                } else if (element instanceof IronSupply) {
                    SupplyModel s = (SupplyModel) element;
                    irons.add(new int[] {s.getGridX(), s.getGridY()});
                } else if (element instanceof Plants) {
                    Plants p = (Plants) element;
                    // Infer plant type index by sprite identity
                    int typeIdx = 0;
                    SpriteKey key = p.getSpriteRenderer();
                    for (int i = 0; i < plantSprites0.length; i++) {
                        if (plantSprites0[i] == key || key.equals(plantSprites0[i])) { typeIdx = i; break; }
                    }
                    plants.add(new Plant(typeIdx, p.getPositionX(), p.getPositionY()));
                }
            }
        };
        try { world.getElementRoot().visit(visitor); } catch (Throwable ignore) {}
    }

    public static LoadedMap load(File file) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            int magic = in.readInt();
            if (magic != MAGIC_TTMP) throw new IOException("Not a TT map file");
            int version = in.readInt();
            if (version != VERSION_1) throw new IOException("Unsupported TT map version: " + version);

            LoadedMap lm = new LoadedMap();
            while (true) {
                try {
                    int tag = in.readInt();
                    int len = in.readInt();
                    if (len < 0) throw new IOException("Corrupt section length");
                    if (tag == TAG_META) {
                        byte[] buf = new byte[len]; in.readFully(buf);
                        DataInputStream s = new DataInputStream(new ByteArrayInputStream(buf));
                        lm.metersPerWorld = s.readInt();
                        lm.terrainType = s.readInt();
                        lm.seaLevel = s.readFloat();
                        lm.size = s.readInt();
                    } else if (tag == TAG_HM3Z) {
                        byte[] buf = new byte[len]; in.readFully(buf);
                        DataInputStream s = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(buf)));
                        int n = s.readInt();
                        float[][] heights = new float[n][n];
                        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) heights[y][x] = s.readFloat();
                        lm.heights = heights; lm.size = n;
                    } else if (tag == TAG_GRID) {
                        byte[] buf = new byte[len]; in.readFully(buf);
                        DataInputStream s = new DataInputStream(new ByteArrayInputStream(buf));
                        int n = s.readInt();
                        boolean[][] access = new boolean[n][n];
                        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) access[y][x] = s.readByte() != 0;
                        boolean[][] dock = new boolean[n][n];
                        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) dock[y][x] = s.readByte() != 0;
                        boolean[][] water = new boolean[n][n];
                        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) water[y][x] = s.readByte() != 0;
                        byte[][] build = new byte[n][n];
                        for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) build[y][x] = s.readByte();
                        lm.access = access; lm.dock = dock; lm.water = water; lm.build = build; lm.size = n;
                    } else if (tag == TAG_ROCK) {
                        byte[] buf = new byte[len]; in.readFully(buf);
                        DataInputStream s = new DataInputStream(new ByteArrayInputStream(buf));
                        int c = s.readInt();
                        for (int i = 0; i < c; i++) lm.rocks.add(new int[] {s.readInt(), s.readInt()});
                    } else if (tag == TAG_IRON) {
                        byte[] buf = new byte[len]; in.readFully(buf);
                        DataInputStream s = new DataInputStream(new ByteArrayInputStream(buf));
                        int c = s.readInt();
                        for (int i = 0; i < c; i++) lm.iron.add(new int[] {s.readInt(), s.readInt()});
                    } else if (tag == TAG_PLTS) {
                        byte[] buf = new byte[len]; in.readFully(buf);
                        DataInputStream s = new DataInputStream(new ByteArrayInputStream(buf));
                        int c = s.readInt();
                        for (int i = 0; i < c; i++) lm.plants.add(new Plant(s.readInt(), s.readFloat(), s.readFloat()));
                    } else {
                        // skip unknown
                        long skipped = 0; while (skipped < len) { long k = in.skip(len - skipped); if (k <= 0) break; skipped += k; }
                    }
                } catch (EOFException eof) {
                    break;
                }
            }
            return lm;
        }
    }

    // Apply a loaded map to the current editor world (same grid size expected)
    public static void applyToEditorWorld(World world, LoadedMap map, int terrainType) {
        HeightMap hm = world.getHeightMap();
        int n = hm.getGridUnitsPerWorld();
        if (map.heights == null) return; // nothing to do
        if (map.heights.length != n || map.heights[0].length != n) {
            System.out.println("[MapIO] Loaded map size " + map.heights.length + " != world size " + n + "; abort apply");
            return;
        }

        // Overwrite heights (only if changed to reduce notifications)
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                float nh = map.heights[y][x];
                float oh = hm.getHeight(x, y);
                if (oh != nh) hm.editHeight(x, y, nh);
            }
        }

        // Copy grids if present
        if (map.access != null && map.access.length == n) {
            boolean[][] a = hm.getAccessGrid();
            for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) a[y][x] = map.access[y][x];
        }
        if (map.dock != null && map.dock.length == n) {
            boolean[][] d = hm.getDockGrid();
            for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) d[y][x] = map.dock[y][x];
        }
        if (map.water != null && map.water.length == n) {
            boolean[][] w = hm.getWaterGrid();
            for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) w[y][x] = map.water[y][x];
        }
        if (map.build != null && map.build.length == n) {
            byte[][] b = hm.getBuildGrid();
            for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) b[y][x] = map.build[y][x];
        }

        // Remove existing rock/iron/plants
        List<SupplyModel> toRemoveSupplies = new ArrayList<>();
        List<Plants> toRemovePlants = new ArrayList<>();
        com.oddlabs.tt.model.ElementNodeVisitor visitor = new com.oddlabs.tt.model.ElementNodeVisitor() {
            @Override public void visitNode(com.oddlabs.tt.model.ElementNode node) {
                node.visitChildren(this);
            }
            @Override public void visitLeaf(com.oddlabs.tt.model.ElementLeaf leaf) { leaf.visitElements(this); }
            @Override public void visit(com.oddlabs.tt.model.Element element) {
                if (element instanceof RockSupply || element instanceof IronSupply) toRemoveSupplies.add((SupplyModel) element);
                else if (element instanceof Plants) toRemovePlants.add((Plants) element);
            }
        };
        try { world.getElementRoot().visit(visitor); } catch (Throwable ignore) {}
        for (SupplyModel s : toRemoveSupplies) { try { s.editorRemoveNow(); } catch (Throwable ignore) {} }
        for (Plants p : toRemovePlants) { try { p.remove(); } catch (Throwable ignore) {} }

        // Spawn rock/iron at grid coords
        SpriteKey[] rockSprites = world.getLandscapeResources().getRockFragments();
        SpriteKey[] ironSprites = world.getLandscapeResources().getIronFragments();
        java.util.Random rnd = world.getRandom();
        for (int[] rc : map.rocks) {
            int gx = rc[0], gy = rc[1];
            float x = UnitGrid.coordinateFromGrid(gx) + (rnd.nextFloat() - .5f);
            float y = UnitGrid.coordinateFromGrid(gy) + (rnd.nextFloat() - .5f);
            float rot = rnd.nextFloat() * 360f;
            SpriteKey sk = rockSprites[(map.rocks.indexOf(rc)) % rockSprites.length];
            new RockSupply(world, sk, 2f, gx, gy, x, y, rot, true);
        }
        for (int[] ic : map.iron) {
            int gx = ic[0], gy = ic[1];
            float x = UnitGrid.coordinateFromGrid(gx) + (rnd.nextFloat() - .5f);
            float y = UnitGrid.coordinateFromGrid(gy) + (rnd.nextFloat() - .5f);
            float rot = rnd.nextFloat() * 360f;
            SpriteKey sk = ironSprites[(map.iron.indexOf(ic)) % ironSprites.length];
            new IronSupply(world, sk, 2f, gx, gy, x, y, rot, true);
        }

        // Spawn plants
        SpriteKey[] plantSprites = world.getLandscapeResources().getPlants()[terrainType];
        for (Plant pl : map.plants) {
            float dir_x = rnd.nextFloat();
            float dir_y = rnd.nextFloat();
            float len2 = dir_x * dir_x + dir_y * dir_y;
            if (len2 < 1e-3f) { dir_x = 1f; dir_y = 0f; }
            else { float inv = 1f / (float) StrictMath.sqrt(len2); dir_x *= inv; dir_y *= inv; }
            int idx = Math.max(0, Math.min(pl.typeIndex, plantSprites.length - 1));
            new Plants(world, pl.x, pl.y, dir_x, dir_y, plantSprites[idx]);
        }
    }
}
