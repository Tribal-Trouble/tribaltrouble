package com.oddlabs.converter;

import com.oddlabs.converter.xml.MeshFile;
import com.oddlabs.converter.xml.Polygon;
import com.oddlabs.converter.xml.Vertex;
import com.oddlabs.converter.xml.Xml;
import org.jspecify.annotations.NonNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Where a glow lights from: a few points among the triangles that show the bright part of the glow picture. A
 * glowing triangle is carried as x y z weight, a spot as x y z.
 */
final class LightSpots {
    private static final int MAX_SPOTS = 4;
    private static final float GLOWING = 0.3f;
    private static final int SETTLE_PASSES = 10;
    private static final int WEIGHT = 3;

    private LightSpots() {
    }

    /** Up to MAX_SPOTS points as x y z, in the model's space times scale; none when nothing glows. */
    static @NonNull List<float @NonNull []> find(@NonNull Path mesh, @NonNull Path glow,
            float scale) throws IOException {
        BufferedImage image = ImageIO.read(glow.toFile());
        if (image == null)
            throw new IOException("Cannot read the glow picture " + glow);
        List<float[]> glowing = glowingTriangles(Xml.read(mesh, MeshFile.class), image, scale);
        if (glowing.isEmpty())
            return List.of();
        List<float[]> spots = spreadOver(glowing);
        for (int pass = 0; pass < SETTLE_PASSES; pass++)
            settle(spots, glowing);
        return spots;
    }

    // Each triangle is judged by the glow pixel under its middle.
    private static @NonNull List<float @NonNull []> glowingTriangles(@NonNull MeshFile mesh,
            @NonNull BufferedImage image, float scale) {
        List<float[]> glowing = new ArrayList<>();
        for (Polygon polygon : mesh.polygons().triangles()) {
            float x = 0, y = 0, z = 0, u = 0, v = 0;
            for (Vertex vertex : polygon.vertices()) {
                x += vertex.x() / Polygon.CORNERS;
                y += vertex.y() / Polygon.CORNERS;
                z += vertex.z() / Polygon.CORNERS;
                u += vertex.u() / Polygon.CORNERS;
                v += vertex.v() / Polygon.CORNERS;
            }
            float weight = glowAt(image, u, v);
            if (weight > GLOWING)
                glowing.add(new float[]{x * scale, y * scale, z * scale, weight});
        }
        return glowing;
    }

    // How strongly the glow picture shines at a texture coordinate: its brightest channel, faded by its alpha.
    private static float glowAt(@NonNull BufferedImage image, float u, float v) {
        // Texture rows run from the top of the picture, v from its bottom.
        int px = Math.min(image.getWidth() - 1, (int) (fraction(u) * image.getWidth()));
        int py = Math.min(image.getHeight() - 1, (int) ((1f - fraction(v)) * image.getHeight()));
        int argb = image.getRGB(px, py);
        float brightest = Math.max(argb >> 16 & 0xff, Math.max(argb >> 8 & 0xff, argb & 0xff)) / 255f;
        return brightest * (argb >>> 24) / 255f;
    }

    // The brightest triangle first, then whichever triangle is farthest from every spot so far, so separate glowing
    // areas each get a spot.
    private static @NonNull List<float @NonNull []> spreadOver(@NonNull List<float @NonNull []> glowing) {
        List<float[]> spots = new ArrayList<>();
        spots.add(Arrays.copyOf(glowing.stream().max(Comparator.comparingDouble(p -> p[WEIGHT])).orElseThrow(),
                WEIGHT));
        while (spots.size() < Math.min(MAX_SPOTS, glowing.size())) {
            float[] farthest = glowing.stream().max(Comparator.comparingDouble(
                    p -> distanceSquared(spots.get(nearest(spots, p)), p))).orElseThrow();
            spots.add(Arrays.copyOf(farthest, WEIGHT));
        }
        return spots;
    }

    // One pass of: give every triangle to its nearest spot, then move each spot to the weighted middle of its
    // triangles.
    private static void settle(@NonNull List<float @NonNull []> spots, @NonNull List<float @NonNull []> glowing) {
        float[][] sums = new float[spots.size()][WEIGHT + 1];
        for (float[] p : glowing) {
            float[] sum = sums[nearest(spots, p)];
            for (int k = 0; k < WEIGHT; k++)
                sum[k] += p[k] * p[WEIGHT];
            sum[WEIGHT] += p[WEIGHT];
        }
        for (int s = 0; s < spots.size(); s++) {
            float total = sums[s][WEIGHT];
            if (total > 0)
                spots.set(s, new float[]{sums[s][0] / total, sums[s][1] / total, sums[s][2] / total});
        }
    }

    private static float fraction(float f) {
        return f - (float) Math.floor(f);
    }

    private static int nearest(@NonNull List<float @NonNull []> spots, float @NonNull [] p) {
        int best = 0;
        for (int s = 1; s < spots.size(); s++) {
            if (distanceSquared(spots.get(s), p) < distanceSquared(spots.get(best), p))
                best = s;
        }
        return best;
    }

    private static float distanceSquared(float @NonNull [] a, float @NonNull [] b) {
        float dx = a[0] - b[0];
        float dy = a[1] - b[1];
        float dz = a[2] - b[2];
        return dx * dx + dy * dy + dz * dz;
    }
}
