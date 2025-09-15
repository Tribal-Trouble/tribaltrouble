package com.oddlabs.tt.editor;

// Avoid additional imports; use fully qualified names to keep coupling minimal.

/**
 * Cached overlay renderer for the editor.
 *
 * Strategy:
 * - Build OpenGL display lists per patch and per layer (WATER/DOCK/ACCESS/BUILD/RESOURCE/SLOPE).
 * - Rebuild only patches that are marked dirty after terrain edits (ROI).
 * - Draw lists every frame; cost is proportional to number of visible patches (currently all).
 */
final class EditorOverlayRenderer {
    enum Layer { WATER, DOCK, ACCESS, BUILD, RESOURCE, SLOPE }

    private final com.oddlabs.tt.landscape.World world;
    private final com.oddlabs.tt.landscape.HeightMap hm;
    private final int N;                 // grid units per world
    private final int patchUnits;        // grid units per patch
    private final int numPatches;        // patches per dimension
    // One display-list grid per layer
    private final int[][] dlWater;
    private final int[][] dlDock;
    private final int[][] dlAccess;
    private final int[][] dlBuild;
    private final int[][] dlResource;
    private final int[][] dlSlope;
    // Dirty flags shared by all layers (we rebuild per layer lazily)
    private final boolean[][] dirty;

    EditorOverlayRenderer(com.oddlabs.tt.landscape.World w) {
        this.world = w;
        this.hm = w.getHeightMap();
        this.N = hm.getGridUnitsPerWorld();
        this.patchUnits = hm.getGridUnitsPerPatch();
        this.numPatches = (N + patchUnits - 1) / patchUnits;
        this.dlWater = allocDL();
        this.dlDock = allocDL();
        this.dlAccess = allocDL();
        this.dlBuild = allocDL();
        this.dlResource = allocDL();
        this.dlSlope = allocDL();
        this.dirty = new boolean[numPatches][numPatches];
        markDirtyAll();
    }

    private int[][] allocDL() {
        int[][] ids = new int[numPatches][numPatches];
        for (int y = 0; y < numPatches; y++)
            for (int x = 0; x < numPatches; x++) ids[y][x] = 0;
        return ids;
    }

    void markDirtyAll() {
        for (int py = 0; py < numPatches; py++)
            for (int px = 0; px < numPatches; px++) dirty[py][px] = true;
    }

    void markDirtyROI(int gx0, int gy0, int gx1, int gy1) {
        int px0 = clampToPatch(gx0 / patchUnits);
        int py0 = clampToPatch(gy0 / patchUnits);
        int px1 = clampToPatch(gx1 / patchUnits);
        int py1 = clampToPatch(gy1 / patchUnits);
        for (int py = py0; py <= py1; py++)
            for (int px = px0; px <= px1; px++) dirty[py][px] = true;
    }

    private int clampToPatch(int v) {
        if (v < 0) return 0; if (v >= numPatches) return numPatches - 1; return v;
    }

    void draw(Layer layer) {
        // For simplicity, draw all patches. With display lists this is cheap and avoids seams.
        for (int py = 0; py < numPatches; py++) {
            for (int px = 0; px < numPatches; px++) {
                int id = getOrBuild(layer, px, py);
                if (id != 0) org.lwjgl.opengl.GL11.glCallList(id);
            }
        }
    }

    private int getOrBuild(Layer layer, int px, int py) {
        int[][] table = table(layer);
        if (dirty[py][px] || table[py][px] == 0) {
            // delete any previous list
            if (table[py][px] != 0) org.lwjgl.opengl.GL11.glDeleteLists(table[py][px], 1);
            table[py][px] = buildDisplayList(layer, px, py);
            dirty[py][px] = false;
        }
        return table[py][px];
    }

    private int[][] table(Layer layer) {
        switch (layer) {
            case WATER: return dlWater;
            case DOCK: return dlDock;
            case ACCESS: return dlAccess;
            case BUILD: return dlBuild;
            case RESOURCE: return dlResource;
            case SLOPE: return dlSlope;
            default: return dlWater;
        }
    }

    private int buildDisplayList(Layer layer, int patchX, int patchY) {
    int id = org.lwjgl.opengl.GL11.glGenLists(1);
        if (id == 0) return 0;
    final float cell = com.oddlabs.tt.landscape.HeightMap.METERS_PER_UNIT_GRID;

        boolean[][] water = hm.getWaterGrid();
        boolean[][] dock = hm.getDockGrid();
        boolean[][] access = hm.getAccessGrid();
        byte[][] build = hm.getBuildGrid();
    boolean[][] place = com.oddlabs.tt.editor.EditorResourceValidity.getPlacementGrid(world);

        int gx0 = patchX * patchUnits;
        int gy0 = patchY * patchUnits;
        int gx1 = Math.min(N - 1, gx0 + patchUnits - 1);
        int gy1 = Math.min(N - 1, gy0 + patchUnits - 1);

    org.lwjgl.opengl.GL11.glNewList(id, org.lwjgl.opengl.GL11.GL_COMPILE);
    org.lwjgl.opengl.GL11.glBegin(org.lwjgl.opengl.GL11.GL_QUADS);
        for (int gy = gy0; gy <= gy1; gy++) {
            for (int gx = gx0; gx <= gx1; gx++) {
                boolean draw;
                switch (layer) {
                    case WATER:
                        draw = (water != null && water[gy][gx]);
                        break;
                    case DOCK:
                        draw = (dock != null && dock[gy][gx]);
                        break;
                    case ACCESS:
                        draw = (access != null && access[gy][gx]);
                        break;
                    case BUILD:
                        draw = (build != null && build[gy][gx] != 0);
                        break;
                    case RESOURCE:
                        draw = (place != null && place[gy][gx]);
                        break;
                    case SLOPE: {
                        float h = hm.getWrappedHeight(gx, gy);
                        float hR = hm.getWrappedHeight(gx + 1, gy);
                        float hU = hm.getWrappedHeight(gx, gy + 1);
                        float sx = Math.abs(hR - h) / cell;
                        float sy = Math.abs(hU - h) / cell;
                        float value = (float) StrictMath.min(1f, StrictMath.hypot(sx, sy) * 0.5f);
                        draw = value >= 0.5f;
                        break;
                    }
                    default:
                        draw = false;
                }
                if (!draw) continue;
                float wx = gx * cell;
                float wy = gy * cell;
                // Sample each corner to reduce gaps over steep terrain
                float z00 = hm.getNearestHeight(wx, wy) + 0.02f;
                float z10 = hm.getNearestHeight(wx + cell, wy) + 0.02f;
                float z11 = hm.getNearestHeight(wx + cell, wy + cell) + 0.02f;
                float z01 = hm.getNearestHeight(wx, wy + cell) + 0.02f;
                org.lwjgl.opengl.GL11.glVertex3f(wx, wy, z00);
                org.lwjgl.opengl.GL11.glVertex3f(wx + cell, wy, z10);
                org.lwjgl.opengl.GL11.glVertex3f(wx + cell, wy + cell, z11);
                org.lwjgl.opengl.GL11.glVertex3f(wx, wy + cell, z01);
            }
        }
        org.lwjgl.opengl.GL11.glEnd();
        org.lwjgl.opengl.GL11.glEndList();
        return id;
    }

    // TODO: Frustum-aware draw can be added later using LandscapeRenderer utilities.
}
