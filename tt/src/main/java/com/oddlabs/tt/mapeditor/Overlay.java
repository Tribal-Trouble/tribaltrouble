package com.oddlabs.tt.mapeditor;

import com.oddlabs.tt.model.RacesResources;
import org.jspecify.annotations.NonNull;

import java.nio.IntBuffer;

/** What the editor can lay over the island, picked from the overlay dropdown, and how each tints the ground. */
enum Overlay {
    ACCESS("show_access", "access_legend"),
    EDGES("show_edges", "edges_legend"),
    FAIR_ALL("overlay_fair_all", "fair_all_legend"),
    FAIR_WOOD("overlay_fair_wood", "fair_legend"),
    FAIR_ROCK("overlay_fair_rock", "fair_legend"),
    FAIR_IRON("overlay_fair_iron", "fair_legend"),
    BUILD_QUARTERS("overlay_build_quarters", "build_legend"),
    BUILD_ARMORY("overlay_build_armory", "build_legend"),
    BUILD_TOWER("overlay_build_tower", "build_legend"),
    BUILD_DOCK("overlay_build_dock", "dock_legend"),
    DEFENSE("overlay_defense", "defense_legend");

    // Premultiplied colours: red, green, blue, opacity.
    private static final int REGION = TintOverlay.texel(.15f, .85f, .25f, .35f);
    private static final int CUT_OFF = TintOverlay.texel(1f, .6f, .05f, .45f);
    private static final int CLIFF = TintOverlay.texel(.95f, .1f, .1f, .5f);
    private static final int CAN_BUILD = TintOverlay.texel(.15f, .9f, .25f, .5f);
    private static final int CANNOT_BUILD = TintOverlay.texel(.95f, .1f, .1f, .25f);
    private static final int DOCK_OPEN = TintOverlay.texel(.15f, .9f, .25f, .7f);
    private static final int DOCK_BLOCKED = TintOverlay.texel(.95f, .1f, .1f, .6f);

    /** A heat scale from poor to rich: blue, cyan, green, yellow, red. */
    private static final float @NonNull [] @NonNull [] HEAT_STOPS = {{.2f, .25f, .95f}, {.1f, .75f, .95f}, {.25f, .85f, .25f}, {1f, .8f, .1f}, {.95f, .15f, .1f}};
    private static final float HEAT_OPACITY = .5f;
    private static final int @NonNull [] HEAT = heatScale(256);

    private final @NonNull String name_key;
    private final @NonNull String legend_key;

    Overlay(@NonNull String name_key, @NonNull String legend_key) {
        this.name_key = name_key;
        this.legend_key = legend_key;
    }

    @NonNull
    String getName() {
        return MapEditor.i18n(name_key);
    }

    @NonNull
    String getLegend() {
        return MapEditor.i18n(legend_key, getName());
    }

    /** Whether it tints the ground; the map edges stand a wall instead. */
    boolean isTint() {
        return this != EDGES;
    }

    /** Puts one premultiplied RGBA texel per height map cell, in rows, from the island as it was captured. */
    void paint(@NonNull MapAnalysis analysis, @NonNull IntBuffer texels) {
        int n = analysis.getSize() * analysis.getSize();
        switch (this) {
            case ACCESS -> {
                for (int i = 0; i < n; i++)
                    texels.put(switch (analysis.kind(i)) {
                        case REGION -> REGION;
                        case CUT_OFF -> CUT_OFF;
                        case CLIFF -> CLIFF;
                        case NONE -> 0;
                    });
            }
            case EDGES -> {
                for (int i = 0; i < n; i++)
                    texels.put(0);
            }
            case FAIR_ALL -> paintHeat(analysis, analysis.fairnessAll(), texels);
            case FAIR_WOOD -> paintHeat(analysis, analysis.fairness(MapAnalysis.Supply.WOOD), texels);
            case FAIR_ROCK -> paintHeat(analysis, analysis.fairness(MapAnalysis.Supply.ROCK), texels);
            case FAIR_IRON -> paintHeat(analysis, analysis.fairness(MapAnalysis.Supply.IRON), texels);
            case BUILD_QUARTERS -> paintBuildable(analysis, RacesResources.QUARTERS_SIZE, texels);
            case BUILD_ARMORY -> paintBuildable(analysis, RacesResources.ARMORY_SIZE, texels);
            case BUILD_TOWER -> paintBuildable(analysis, RacesResources.TOWER_SIZE, texels);
            case BUILD_DOCK -> {
                for (byte dock : analysis.docks())
                    texels.put(
                            dock == MapAnalysis.DOCK_OPEN ? DOCK_OPEN : dock == MapAnalysis.DOCK_BLOCKED ? DOCK_BLOCKED : 0);
            }
            case DEFENSE -> paintHeat(analysis, analysis.defense(), texels);
        }
    }

    /** Scores from 0 to 1 on the heat scale, over the playable region only. */
    private static void paintHeat(@NonNull MapAnalysis analysis, float @NonNull [] scores,
            @NonNull IntBuffer texels) {
        for (int i = 0; i < scores.length; i++) {
            int step = Math.round(Math.clamp(scores[i], 0f, 1f) * (HEAT.length - 1));
            texels.put(analysis.isRegion(i) ? HEAT[step] : 0);
        }
    }

    /** Green where a building's centre can go, red over the rest of the playable region. */
    private static void paintBuildable(@NonNull MapAnalysis analysis, int placing_size, @NonNull IntBuffer texels) {
        boolean[] legal = analysis.buildable(placing_size);
        for (int i = 0; i < legal.length; i++)
            texels.put(legal[i] ? CAN_BUILD : analysis.isRegion(i) ? CANNOT_BUILD : 0);
    }

    private static int @NonNull [] heatScale(int steps) {
        int[] scale = new int[steps];
        int last = HEAT_STOPS.length - 1;
        for (int s = 0; s < steps; s++) {
            float t = s / (steps - 1f) * last;
            int stop = Math.min(last - 1, (int) t);
            float f = t - stop;
            float[] a = HEAT_STOPS[stop];
            float[] b = HEAT_STOPS[stop + 1];
            scale[s] = TintOverlay.texel(a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f,
                    a[2] + (b[2] - a[2]) * f, HEAT_OPACITY);
        }
        return scale;
    }
}
