package com.oddlabs.tt.landscape;

import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.resource.BlendInfo;
import com.oddlabs.tt.resource.BlendLighting;
import com.oddlabs.tt.resource.GLByteImage;
import com.oddlabs.tt.resource.GLIntImage;
import com.oddlabs.tt.resource.StructureBlend;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public final class LandscapeLayers {
    public static final int LAYER_COUNT = 6;
    public static final float METERS_PER_STRUCTURE_TILE = 64f;

    private static final int LIGHTING_STEP = 5;
    private static final int BLEND_STEP_COUNT = 7;

    private final Texture[] layers = new Texture[LAYER_COUNT];
    private final Texture layerAlphas;
    private final Texture overlayAlphas;
    private final StructureBlend terrainShadows;
    private final StructureBlend supplyShadows;
    private final float lightingR;
    private final float lightingG;
    private final float lightingB;

    public LandscapeLayers(BlendInfo[] blendInfos, StructureBlend terrainShadows, StructureBlend supplyShadows) {
        if (blendInfos.length != BLEND_STEP_COUNT || !(blendInfos[LIGHTING_STEP] instanceof BlendLighting lighting)) {
            throw new IllegalArgumentException("Unexpected landscape blend order");
        }
        int layer = 0;
        for (int i = 0; i < blendInfos.length; i++) {
            if (i == LIGHTING_STEP) continue;
            if (!(blendInfos[i] instanceof StructureBlend structure)) {
                throw new IllegalArgumentException("Expected a structure layer at blend step " + i);
            }
            layers[layer++] = withMipmaps(structure.getStructureMap());
        }
        layerAlphas = packAlphas(blendInfos[1].getSourceImage(), blendInfos[2].getSourceImage(),
                blendInfos[3].getSourceImage(), blendInfos[4].getSourceImage());
        overlayAlphas = packAlphas(lighting.getSourceImage(), blendInfos[6].getSourceImage(), null, null);
        lightingR = lighting.getR();
        lightingG = lighting.getG();
        lightingB = lighting.getB();
        this.terrainShadows = terrainShadows;
        this.supplyShadows = supplyShadows;
    }

    private static Texture withMipmaps(Texture texture) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.getHandle());
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        return texture;
    }

    private static Texture packAlphas(GLByteImage r, GLByteImage g, GLByteImage b, GLByteImage a) {
        int width = r.getWidth();
        int height = r.getHeight();
        GLIntImage packed = new GLIntImage(width, height, GL11.GL_RGBA);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                packed.putPixel(x, y, channel(a, x, y) << 24 | channel(b, x, y) << 16 | channel(g, x,
                        y) << 8 | r.getPixel(x, y));
            }
        }
        return new Texture(packed.buildMipMaps(0, 1f, true, false), GL11.GL_RGBA8, GL11.GL_LINEAR_MIPMAP_LINEAR,
                GL11.GL_LINEAR, GL11.GL_REPEAT, GL11.GL_REPEAT);
    }

    private static int channel(GLByteImage image, int x, int y) {
        return image != null ? image.getPixel(x, y) : 0;
    }

    public Texture getLayer(int index) {
        return layers[index];
    }

    public Texture getLayerAlphas() {
        return layerAlphas;
    }

    public Texture getOverlayAlphas() {
        return overlayAlphas;
    }

    public StructureBlend terrainShadows() {
        return terrainShadows;
    }

    public StructureBlend supplyShadows() {
        return supplyShadows;
    }

    public float getLightingR() {
        return lightingR;
    }

    public float getLightingG() {
        return lightingG;
    }

    public float getLightingB() {
        return lightingB;
    }
}
