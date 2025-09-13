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
        // Optional metadata (present if writer provided, else may be null)
        public String name;
        public String author;
        public String description;
        public float[][] heights; // [y][x]
        public boolean[][] access;
        public boolean[][] dock;
        public boolean[][] water;
        public byte[][] build;
    public final List<int[]> rocks = new ArrayList<>();
    public final List<int[]> iron = new ArrayList<>();
    public final List<Plant> plants = new ArrayList<>();
    }

    // Lightweight summary for file listing/preview without inflating HM3Z
    public static final class MapSummary {
        public final File file;
        public final int metersPerWorld;
        public final int terrainType;
        public final float seaLevel;
        public final int size;
        public final String name;
        public final String author;
        public final String description;
        public final long lastModified;
        public MapSummary(File file,
                          int metersPerWorld,
                          int terrainType,
                          float seaLevel,
                          int size,
                          String name,
                          String author,
                          String description,
                          long lastModified) {
            this.file = file;
            this.metersPerWorld = metersPerWorld;
            this.terrainType = terrainType;
            this.seaLevel = seaLevel;
            this.size = size;
            this.name = name;
            this.author = author;
            this.description = description;
            this.lastModified = lastModified;
        }
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
        // Ensure directory exists
        File parent = target.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        // Derive default meta strings
        String baseName = target.getName();
        int dot = baseName.lastIndexOf('.');
        if (dot > 0) baseName = baseName.substring(0, dot);
        String metaName = baseName;
        String metaAuthor = ""; // unknown author by default
        String metaDesc = "";   // optional description
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
                // Backward-compatible optional metadata strings (length-prefixed UTF-8)
                writeString(meta, metaName);
                writeString(meta, metaAuthor);
                writeString(meta, metaDesc);
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

    private static void writeString(DataOutputStream out, String s) throws IOException {
        if (s == null) {
            out.writeInt(-1);
        } else {
            byte[] bytes = s.getBytes("UTF-8");
            out.writeInt(bytes.length);
            out.write(bytes);
        }
    }

    private static String readString(DataInputStream in, int remainingHint) throws IOException {
        // If no more bytes are available, treat as absent
        if (remainingHint == 0 && in instanceof DataInputStream) {
            // Best effort: use available() on underlying ByteArrayInputStream
            try { if (in.available() <= 0) return null; } catch (IOException ignore) {}
        }
        // Length-prefixed UTF-8 string; -1 means absent
        int len;
        try {
            len = in.readInt();
        } catch (EOFException eof) {
            return null;
        }
        if (len < 0) return null;
        byte[] buf = new byte[len];
        in.readFully(buf);
        return new String(buf, "UTF-8");
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
                        // Optional extended strings (name, author, description)
                        try {
                            int avail = s.available();
                            if (avail > 0) lm.name = readString(s, avail);
                            avail = s.available();
                            if (avail > 0) lm.author = readString(s, avail);
                            avail = s.available();
                            if (avail > 0) lm.description = readString(s, avail);
                        } catch (Throwable ignore) {}
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

    // Peek only META to build a summary without inflating the heightmap
    public static MapSummary peek(File file) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            int magic = in.readInt();
            if (magic != MAGIC_TTMP) throw new IOException("Not a TT map file");
            int version = in.readInt();
            if (version != VERSION_1) throw new IOException("Unsupported TT map version: " + version);
            // Read sections until META found (or EOF)
            int metersPerWorld = 0, terrainType = 0, size = 0; float sea = 0f;
            String name = null, author = null, description = null;
            while (true) {
                int tag, len;
                try {
                    tag = in.readInt();
                    len = in.readInt();
                } catch (EOFException eof) { break; }
                if (len < 0) throw new IOException("Corrupt section length");
                if (tag == TAG_META) {
                    byte[] buf = new byte[len]; in.readFully(buf);
                    DataInputStream s = new DataInputStream(new ByteArrayInputStream(buf));
                    metersPerWorld = s.readInt();
                    terrainType = s.readInt();
                    sea = s.readFloat();
                    size = s.readInt();
                    try {
                        int avail = s.available();
                        if (avail > 0) name = readString(s, avail);
                        avail = s.available();
                        if (avail > 0) author = readString(s, avail);
                        avail = s.available();
                        if (avail > 0) description = readString(s, avail);
                    } catch (Throwable ignore) {}
                    break; // we got what we needed
                } else {
                    long skipped = 0; while (skipped < len) { long k = in.skip(len - skipped); if (k <= 0) break; skipped += k; }
                }
            }
            return new MapSummary(file, metersPerWorld, terrainType, sea, size, name, author, description, file.lastModified());
        }
    }

    // List .ttmap files in mapsDir (unsorted or sorted by name)
    public static java.util.List<MapSummary> listMaps() {
        File dir = mapsDir();
        File[] files = dir.listFiles(new FilenameFilter() {
            public boolean accept(File d, String name) { return name.toLowerCase(java.util.Locale.ROOT).endsWith(".ttmap"); }
        });
        java.util.List<MapSummary> out = new java.util.ArrayList<>();
        if (files == null) return out;
        java.util.Arrays.sort(files, new java.util.Comparator<File>() {
            public int compare(File a, File b) { return a.getName().compareToIgnoreCase(b.getName()); }
        });
        for (File f : files) {
            try { out.add(peek(f)); } catch (Throwable t) {
                // Fallback: minimal summary
                out.add(new MapSummary(f, 0, 0, 0f, 0, null, null, null, f.lastModified()));
            }
        }
        return out;
    }

    // Very conservative filename check for map files (without extension)
    public static boolean isValidFilename(String name) {
        if (name == null) return false;
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return false;
        if (trimmed.length() > 64) return false;
        // allow letters, numbers, space, underscore, dash
        if (!trimmed.matches("[A-Za-z0-9 _-]+")) return false;
        // disallow dot-start and reserved Windows names
        String upper = trimmed.toUpperCase(java.util.Locale.ROOT);
        if (trimmed.startsWith(".")) return false;
        String[] reserved = {"CON","PRN","AUX","NUL","COM1","COM2","COM3","COM4","COM5","COM6","COM7","COM8","COM9","LPT1","LPT2","LPT3","LPT4","LPT5","LPT6","LPT7","LPT8","LPT9"};
        for (String r : reserved) if (upper.equals(r)) return false;
        return true;
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
