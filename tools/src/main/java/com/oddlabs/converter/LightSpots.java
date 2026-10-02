package com.oddlabs.converter;

import org.jspecify.annotations.NonNull;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Where a glow lights from: a few points among the triangles that show the bright part of the glow picture. */
final class LightSpots {
    private static final int MAX_SPOTS = 4;
    private static final float GLOWING = 0.3f;
    private static final int SETTLE_PASSES = 10;
    private static final String[] VERTEX_KEYS = {"x", "y", "z", "u", "v"};

    private LightSpots() {
    }

    /** Up to MAX_SPOTS points as x y z, in the model's space times scale; none when nothing glows. */
    static @NonNull List<float @NonNull []> find(@NonNull Path mesh, @NonNull Path glow,
            float scale) throws IOException, ParserConfigurationException, SAXException {
        BufferedImage image = ImageIO.read(glow.toFile());
        if (image == null)
            throw new IOException("Cannot read the glow picture " + glow);
        // x y z weight of every glowing triangle
        List<float[]> glowing = new ArrayList<>();
        NodeList polygons = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                mesh.toFile()).getElementsByTagName("polygon");
        for (int i = 0; i < polygons.getLength(); i++) {
            NodeList vertices = ((Element) polygons.item(i)).getElementsByTagName("vertex");
            float[] middle = new float[VERTEX_KEYS.length];
            for (int v = 0; v < vertices.getLength(); v++) {
                Element vertex = (Element) vertices.item(v);
                for (int k = 0; k < VERTEX_KEYS.length; k++)
                    middle[k] += Float.parseFloat(vertex.getAttribute(VERTEX_KEYS[k])) / vertices.getLength();
            }
            // Texture rows run from the top of the picture, v from its bottom.
            int px = Math.min(image.getWidth() - 1, (int) (fraction(middle[3]) * image.getWidth()));
            int py = Math.min(image.getHeight() - 1, (int) ((1f - fraction(middle[4])) * image.getHeight()));
            int argb = image.getRGB(px, py);
            float brightest = Math.max(argb >> 16 & 0xff, Math.max(argb >> 8 & 0xff, argb & 0xff)) / 255f;
            float weight = brightest * (argb >>> 24) / 255f;
            if (weight > GLOWING)
                glowing.add(new float[]{middle[0] * scale, middle[1] * scale, middle[2] * scale, weight});
        }
        if (glowing.isEmpty())
            return List.of();
        // Spread the spots as far apart as the glow goes, then settle each in the middle of the glow nearest to it.
        List<float[]> spots = new ArrayList<>();
        spots.add(Arrays.copyOf(glowing.stream().max(Comparator.comparingDouble(p -> p[3])).orElseThrow(), 3));
        while (spots.size() < Math.min(MAX_SPOTS, glowing.size())) {
            float[] farthest = glowing.stream().max(Comparator.comparingDouble(
                    p -> distanceSquared(spots.get(nearest(spots, p)), p))).orElseThrow();
            spots.add(Arrays.copyOf(farthest, 3));
        }
        for (int pass = 0; pass < SETTLE_PASSES; pass++) {
            float[][] sums = new float[spots.size()][4];
            for (float[] p : glowing) {
                float[] sum = sums[nearest(spots, p)];
                for (int k = 0; k < 3; k++)
                    sum[k] += p[k] * p[3];
                sum[3] += p[3];
            }
            for (int s = 0; s < spots.size(); s++) {
                if (sums[s][3] > 0)
                    spots.set(s,
                            new float[]{sums[s][0] / sums[s][3], sums[s][1] / sums[s][3], sums[s][2] / sums[s][3]});
            }
        }
        return spots;
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
