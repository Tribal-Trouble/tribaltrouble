package com.oddlabs.tt.procedural;

import com.oddlabs.procedural.Channel;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.resource.GLByteImage;
import com.oddlabs.tt.resource.GLIntImage;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;
import java.util.stream.Stream;

public final class GeneratorClouds extends TextureGenerator {
    private static final int TEXTURE_SIZE = 512;

    public static final int INNER = 0;
    public static final int OUTER = 1;

    private final Landscape.@NonNull TerrainType terrain;

    public GeneratorClouds(Landscape.@NonNull TerrainType terrain) {
        this.terrain = terrain;
    }

    @Override
    public Texture @NonNull [] generate() {
        int seed = Globals.LANDSCAPE_SEED;
        Channel clouds1 = new Midpoint(TEXTURE_SIZE, 3, 0.55f, seed).toChannel();
        Channel clouds2 = new Midpoint(TEXTURE_SIZE, 2, 0.4f, seed).toChannel();

        IntSupplier debugImageCount = (new AtomicInteger())::incrementAndGet;
        return Stream.of(terrain)
                .flatMap(terrainType -> switch (terrainType) {
                    case NATIVE -> Stream.of(
                            clouds1.dynamicRange(0.5f, 1f, 0f, 1f).gamma(0.75f).brightness(0.5f),
                            clouds2.dynamicRange(0.25f, 1f, 0f, 1f).gamma(0.5f).brightness(0.33f));
                    case VIKING -> Stream.of(
                            clouds1.dynamicRange(0.5f, 1f, 0f, 0.75f),
                            clouds2.dynamicRange(0.5f, 1f, 0f, 0.75f));
                }).peek(img -> {
                    if (Landscape.DEBUG)
                        new GLIntImage(img.toLayer()).saveAsPNG("generator_clouds_" + debugImageCount.getAsInt());
                }).map(cloud -> new GLByteImage(cloud, GL11.GL_RED))
                .map(image -> new Texture(image, Globals.COMPRESSED_LUMINANCE_FORMAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL11.GL_LINEAR, GL11.GL_REPEAT, GL11.GL_REPEAT))
                .toArray(Texture[]::new);
    }

    @Override
    public int hashCode() {
        return TEXTURE_SIZE;
    }

    @Override
    public boolean equals(@NonNull Object o) {
        return super.equals(o) && ((GeneratorClouds) o).terrain == terrain;
    }
}
