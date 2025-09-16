package com.oddlabs.tt.editor;

import com.oddlabs.procedural.Channel;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.render.PixelFormat;
import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.render.LandscapeRenderer;
import com.oddlabs.tt.resource.BlendInfo;
import com.oddlabs.tt.resource.BlendLighting;
import com.oddlabs.tt.resource.GLByteImage;
import com.oddlabs.tt.resource.GLIntImage;
import com.oddlabs.tt.resource.StructureBlend;
import com.oddlabs.tt.util.GLState;
import com.oddlabs.tt.util.GLStateStack;
import com.oddlabs.tt.util.OffscreenRenderer;
import com.oddlabs.tt.util.OffscreenRendererFactory;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;

import java.nio.FloatBuffer;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.nio.IntBuffer;

/**
 * Minimal runtime colormap ROI reblend focused on lighting and seabottom.
 *
 * This pass preserves the existing chunk colormap as a base, then overlays:
 *   - dynamic highlight (BlendLighting)
 *   - dynamic shadow (black StructureBlend)
 *   - dynamic seabottom tint (constant seabottom color StructureBlend)
 *
 * It does not change base material distributions yet; that can be added later.
 */
public final class EditorColormapReblender {
    private EditorColormapReblender() {}

    /**
     * Legacy incremental reblend that drew overlays on top of the existing chunk colormap.
     *
     * Fixed notes (kept here for reference, but the method is now unused/commented out):
     * - Shadow alpha must be GL_ALPHA (not GL_LUMINANCE) when used with StructureBlend.
     * - The lighting highlight pass (BlendLighting) is required to balance shadows.
     * - Seabottom tint pass should be included for parity with world-gen.
     * - Starting from the existing chunk can double-stack darkening; prefer from-scratch rebuild.
     */
    /*
    public static void reblendLightingAndSeabottomROI(
            World world,
            LandscapeRenderer renderer,
            int terrainType,
            int minGX,
            int minGY,
            int maxGX,
            int maxGY) {
        try {
            HeightMap hm = world.getHeightMap();
            int N = hm.getGridUnitsPerWorld();
            if (N <= 0) return;

            // Build dynamic alpha channels from current heights
            Channels dyn = buildDynamicChannels(world);

            // Mask seabottom strictly to water cells to avoid tinting land
            boolean[][] water = world.getHeightMap().getWaterGrid();
            if (water != null) {
                for (int y = 0; y < N; y++) {
                    for (int x = 0; x < N; x++) {
                        if (!water[y][x]) dyn.seabottom.putPixel(x, y, 0f);
                    }
                }
            }
            // Correct formats: highlight (luminance for lighting combiner), shadow (alpha)
            GLByteImage highlight = new GLByteImage(dyn.highlight, GL11.GL_LUMINANCE);
            GLByteImage shadow = new GLByteImage(dyn.shadow); // GL_ALPHA

            // Build constant structure layer for shadow (black)
            GLIntImage black1x1 = new GLIntImage(1, 1, GL11.GL_RGBA);
            black1x1.putPixel(0, 0, 0x000000ff);

            // Compose overlay chain: base, materials, lighting, shadow, seabottom
            GLByteImage[] materials = buildMaterialAlphas(world, terrainType);
            BlendInfo[] overlays = buildOverlayChainFull(terrainType, materials, highlight, shadow, dyn.seabottom);

            // Compute affected chunk range from ROI in grid units
            int metersPerChunk = hm.getMetersPerChunk();
            int chunksPer = hm.getMetersPerWorld() / metersPerChunk;
            int minMX = minGX * HeightMap.METERS_PER_UNIT_GRID;
            int minMY = minGY * HeightMap.METERS_PER_UNIT_GRID;
            int maxMX = (maxGX + 1) * HeightMap.METERS_PER_UNIT_GRID;
            int maxMY = (maxGY + 1) * HeightMap.METERS_PER_UNIT_GRID;
            int minCX = clamp((int) StrictMath.floor((double) minMX / metersPerChunk) - 1, 0, chunksPer - 1);
            int minCY = clamp((int) StrictMath.floor((double) minMY / metersPerChunk) - 1, 0, chunksPer - 1);
            int maxCX = clamp((int) StrictMath.floor((double) maxMX / metersPerChunk) + 1, 0, chunksPer - 1);
            int maxCY = clamp((int) StrictMath.floor((double) maxMY / metersPerChunk) + 1, 0, chunksPer - 1);

            // Derive mapping from world to alpha texture space identical to generator
            int texelsPerGridUnit = Globals.TEXELS_PER_GRID_UNIT
                    / (int) StrictMath.pow(2, Globals.TEXTURE_MIP_SHIFT[Settings.getSettings().graphic_detail]);
            float alphaTexelSize = 1f / (N * texelsPerGridUnit);
            float alphaBorder = Globals.TEXELS_PER_CHUNK_BORDER * alphaTexelSize;

            // Offscreen renderer factory
            OffscreenRendererFactory factory = new OffscreenRendererFactory();

            // Render per-chunk into existing textures
            for (int cy = minCY; cy <= maxCY; cy++) {
                for (int cx = minCX; cx <= maxCX; cx++) {
                    Texture chunk = renderer.getColormap(cx, cy);
                    int texSize = chunk.getWidth(); // base level size
                    OffscreenRenderer offscreen = factory.createRenderer(
                            texSize,
                            texSize,
                            new PixelFormat(Globals.VIEW_BIT_DEPTH, 0, 0, 0, 0),
                            Settings.getSettings().use_copyteximage);

                    setup2D(texSize);

                    // Build per-chunk quad coordinates and base/structure/alpha texcoords
                    FloatBuffer coords = BufferUtils.createFloatBuffer(4 * 3);
                    coords.put(new float[] {0f, 0f, 0f, texSize, 0f, 0f, texSize, texSize, 0f, 0f, texSize, 0f});
                    coords.rewind();
                    FloatBuffer baseTex = BufferUtils.createFloatBuffer(4 * 2);
                    baseTex.put(new float[] {0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f});
                    baseTex.rewind();
                    FloatBuffer structureTex = BufferUtils.createFloatBuffer(4 * 2);
                    FloatBuffer alphaTex = BufferUtils.createFloatBuffer(4 * 2);

                    // Common client arrays
                    GLStateStack.switchState(
                            GLState.VERTEX_ARRAY | GLState.TEXCOORD0_ARRAY | GLState.TEXCOORD1_ARRAY);
                    GL11.glVertexPointer(3, GL11.GL_FLOAT, 0, coords);
                    // Unit 0 initially uses baseTex (for copying the chunk texture)
                    GL11.glTexCoordPointer(2, GL11.GL_FLOAT, 0, baseTex);
                    GLState.clientActiveTexture(GL13.GL_TEXTURE1);
                    GL11.glTexCoordPointer(2, GL11.GL_FLOAT, 0, alphaTex);
                    GLState.clientActiveTexture(GL13.GL_TEXTURE0);

                    // Iterate mip levels by scaling the modelview
                    int mipScale = 1;
                    int mipLevel = 0;
                    int mipSize = texSize;
                    while (mipSize >= 1) {
                        GL11.glLoadIdentity();
                        GL11.glScalef(1f / mipScale, 1f / mipScale, 1f);

                        // Draw base: copy current chunk texture into offscreen
                        GL11.glDisable(GL11.GL_BLEND);
                        GL11.glEnable(GL11.GL_TEXTURE_2D);
                        GL11.glBindTexture(GL11.GL_TEXTURE_2D, chunk.getHandle());
                        GL11.glDrawArrays(GL11.GL_QUADS, 0, 4);

                        // Prepare alpha texcoords for this chunk
                        float chunkAlphaOffset = texSize * alphaTexelSize;
                        float au = cx * chunkAlphaOffset - alphaBorder;
                        float av = cy * chunkAlphaOffset - alphaBorder;
                        float alen = chunkAlphaOffset + 2 * alphaBorder;
                        alphaTex.put(0, au).put(1, av);
                        alphaTex.put(2, au + alen).put(3, av);
                        alphaTex.put(4, au + alen).put(5, av + alen);
                        alphaTex.put(6, au).put(7, av + alen);

                        // Prepare structure texcoords for this chunk (mirror IslandGenerator)
                        float structureTexelSize = 1f / (float) Globals.STRUCTURE_SIZE;
                        float structureBorder = Globals.TEXELS_PER_CHUNK_BORDER * structureTexelSize;
                        float structureOffset = texSize * structureTexelSize - 2f * structureBorder;
                        float su = cx * structureOffset - structureBorder;
                        float sv = cy * structureOffset - structureBorder;
                        float slen = structureOffset + 2f * structureBorder;
                        structureTex.put(0, su).put(1, sv);
                        structureTex.put(2, su + slen).put(3, sv);
                        structureTex.put(4, su + slen).put(5, sv + slen);
                        structureTex.put(6, su).put(7, sv + slen);

                        // Overlay passes (now include base, materials, lighting, shadow, seabottom)
                        GL11.glEnable(GL11.GL_BLEND);
                        for (int i = 0; i < overlays.length; i++) {
                            // If this is a structure pass, ensure unit0 uses structure texcoords
                            // and unit1 contributes alpha without overriding RGB.
                            boolean isStructure = overlays[i] instanceof com.oddlabs.tt.resource.StructureBlend;

                            if (isStructure) {
                                // Switch unit0 coords to structure mapping
                                GL11.glTexCoordPointer(2, GL11.GL_FLOAT, 0, structureTex);
                                // Configure unit1 combiner: RGB = PREVIOUS (structure color), ALPHA = TEXTURE (alpha map)
                                GLState.activeTexture(GL13.GL_TEXTURE1);
                                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL13.GL_COMBINE);
                                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_COMBINE_RGB, GL13.GL_REPLACE);
                                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_SOURCE0_RGB, GL13.GL_PREVIOUS);
                                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_COMBINE_ALPHA, GL13.GL_REPLACE);
                                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_SOURCE0_ALPHA, GL13.GL_TEXTURE);
                                GLState.activeTexture(GL13.GL_TEXTURE0);
                            } else {
                                // Non-structure overlays can use base coords; reset unit1 to default for safety
                                GL11.glTexCoordPointer(2, GL11.GL_FLOAT, 0, baseTex);
                                GLState.activeTexture(GL13.GL_TEXTURE1);
                                GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_REPLACE);
                                GLState.activeTexture(GL13.GL_TEXTURE0);
                            }

                            overlays[i].setup();
                            GL11.glDrawArrays(GL11.GL_QUADS, 0, 4);
                            overlays[i].reset();

                            if (isStructure) {
                                // Restore base texcoords for unit0 after structure pass
                                GL11.glTexCoordPointer(2, GL11.GL_FLOAT, 0, baseTex);
                            }
                        }

            // Copy back to the texture at this mip level
                        offscreen.copyToTexture(
                                chunk,
                                mipLevel,
                                Globals.COMPRESSED_RGB_FORMAT,
                                0,
                                0,
                                mipSize,
                                mipSize);
                        mipScale <<= 1;
                        mipLevel++;
                        mipSize >>= 1;
                    }
                    offscreen.destroy();
                }
            }
        } catch (Throwable t) {
            // Keep editor resilient; log to stdout only
            System.err.println("EditorColormapReblender failed: " + t.getMessage());
        }
    }
    */

    /**
     * Rebuild-from-scratch ROI reblend: fully recomposes the affected chunk colormap(s)
     * using the world-gen-like blend chain (base + materials + lighting + shadow + seabottom).
     * This avoids double-stacking onto existing colors and fixes darkening.
     */
    public static void reblendROIFromScratch(
            World world,
            LandscapeRenderer renderer,
            int terrainType,
            int minGX,
            int minGY,
            int maxGX,
            int maxGY) {
        try {
            HeightMap hm = world.getHeightMap();
            int N = hm.getGridUnitsPerWorld();
            if (N <= 0) return;

            // Build dynamic channels and masks
            Channels dyn = buildDynamicChannels(world, terrainType);

            // Derive affected chunk range from GU ROI
            int metersPerChunk = hm.getMetersPerChunk();
            int chunksPer = hm.getMetersPerWorld() / metersPerChunk;
            int minMX = minGX * HeightMap.METERS_PER_UNIT_GRID;
            int minMY = minGY * HeightMap.METERS_PER_UNIT_GRID;
            int maxMX = (maxGX + 1) * HeightMap.METERS_PER_UNIT_GRID;
            int maxMY = (maxGY + 1) * HeightMap.METERS_PER_UNIT_GRID;
            int minCX = clamp((int) StrictMath.floor((double) minMX / metersPerChunk) - 1, 0, chunksPer - 1);
            int minCY = clamp((int) StrictMath.floor((double) minMY / metersPerChunk) - 1, 0, chunksPer - 1);
            int maxCX = clamp((int) StrictMath.floor((double) maxMX / metersPerChunk) + 1, 0, chunksPer - 1);
            int maxCY = clamp((int) StrictMath.floor((double) maxMY / metersPerChunk) + 1, 0, chunksPer - 1);

            // World-to-alpha texture mapping parameters (parity with IslandGenerator)
            int texelsPerGridUnit = Globals.TEXELS_PER_GRID_UNIT
                    / (int) StrictMath.pow(2, Globals.TEXTURE_MIP_SHIFT[Settings.getSettings().graphic_detail]);
            float alphaTexelSize = 1f / (N * texelsPerGridUnit);
            float alphaBorder = Globals.TEXELS_PER_CHUNK_BORDER * alphaTexelSize;

            // Build blend chain elements
        GLByteImage[] materialAlphas = buildMaterialAlphas(world, terrainType);
            GLByteImage highlight = new GLByteImage(dyn.highlight, GL11.GL_LUMINANCE); // for BlendLighting
            GLByteImage shadow = new GLByteImage(dyn.shadow); // GL_ALPHA

        BlendInfo[] overlays = buildOverlayChainFull(
            terrainType, renderer.getStructureImages(), materialAlphas, highlight, shadow, dyn.seabottom);

            OffscreenRendererFactory factory = new OffscreenRendererFactory();

            // Recompose each affected chunk at all mip levels
            for (int cy = minCY; cy <= maxCY; cy++) {
                for (int cx = minCX; cx <= maxCX; cx++) {
                    Texture chunk = renderer.getColormap(cx, cy);
                    int texSize = chunk.getWidth();
                    OffscreenRenderer offscreen = factory.createRenderer(
                            texSize,
                            texSize,
                            new PixelFormat(Globals.VIEW_BIT_DEPTH, 0, 0, 0, 0),
                            Settings.getSettings().use_copyteximage);

                    setup2D(texSize);

                    // Geometry
                    FloatBuffer coords = BufferUtils.createFloatBuffer(4 * 3);
                    coords.put(new float[] {0f, 0f, 0f, texSize, 0f, 0f, texSize, texSize, 0f, 0f, texSize, 0f});
                    coords.rewind();
                    FloatBuffer structureTex = BufferUtils.createFloatBuffer(4 * 2);
                    FloatBuffer alphaTex = BufferUtils.createFloatBuffer(4 * 2);

                    GLStateStack.switchState(
                            GLState.VERTEX_ARRAY | GLState.TEXCOORD0_ARRAY | GLState.TEXCOORD1_ARRAY);
                    GL11.glVertexPointer(3, GL11.GL_FLOAT, 0, coords);
                    GL11.glTexCoordPointer(2, GL11.GL_FLOAT, 0, structureTex);
                    GLState.clientActiveTexture(GL13.GL_TEXTURE1);
                    GL11.glTexCoordPointer(2, GL11.GL_FLOAT, 0, alphaTex);
                    GLState.clientActiveTexture(GL13.GL_TEXTURE0);

                    int mipScale = 1;
                    int mipLevel = 0;
                    int mipSize = texSize;
                    while (mipSize >= 1) {
                        GL11.glLoadIdentity();
                        GL11.glScalef(1f / mipScale, 1f / mipScale, 1f);

                        // Clear to black before composing base layer
                        GL11.glDisable(GL11.GL_TEXTURE_2D);
                        GL11.glDisable(GL11.GL_BLEND);
                        GL11.glColor4f(0f, 0f, 0f, 1f);
                        GL11.glBegin(GL11.GL_QUADS);
                        GL11.glVertex3f(0f, 0f, 0f);
                        GL11.glVertex3f(mipSize, 0f, 0f);
                        GL11.glVertex3f(mipSize, mipSize, 0f);
                        GL11.glVertex3f(0f, mipSize, 0f);
                        GL11.glEnd();
                        GL11.glEnable(GL11.GL_BLEND);
                        GL11.glEnable(GL11.GL_BLEND);

                        // Prepare texture coordinates for this chunk
                        float chunkAlphaOffset = texSize * alphaTexelSize;
                        float au = cx * chunkAlphaOffset - alphaBorder;
                        float av = cy * chunkAlphaOffset - alphaBorder;
                        float alen = chunkAlphaOffset + 2 * alphaBorder;
                        alphaTex.put(0, au).put(1, av);
                        alphaTex.put(2, au + alen).put(3, av);
                        alphaTex.put(4, au + alen).put(5, av + alen);
                        alphaTex.put(6, au).put(7, av + alen);

                        float structureTexelSize = 1f / (float) Globals.STRUCTURE_SIZE;
                        float structureBorder = Globals.TEXELS_PER_CHUNK_BORDER * structureTexelSize;
                        float structureOffset = texSize * structureTexelSize - 2f * structureBorder;
                        float su = cx * structureOffset - structureBorder;
                        float sv = cy * structureOffset - structureBorder;
                        float slen = structureOffset + 2f * structureBorder;
                        structureTex.put(0, su).put(1, sv);
                        structureTex.put(2, su + slen).put(3, sv);
                        structureTex.put(4, su + slen).put(5, sv + slen);
                        structureTex.put(6, su).put(7, sv + slen);

                        // Draw full chain from scratch
                        for (int i = 0; i < overlays.length; i++) {
                            overlays[i].setup();
                            GL11.glDrawArrays(GL11.GL_QUADS, 0, 4);
                            overlays[i].reset();
                        }

                        // Upload into the colormap texture
                        offscreen.copyToTexture(
                                chunk,
                                mipLevel,
                                Globals.COMPRESSED_RGB_FORMAT,
                                0,
                                0,
                                mipSize,
                                mipSize);
                        mipScale <<= 1;
                        mipLevel++;
                        mipSize >>= 1;
                    }
                    offscreen.destroy();
                }
            }
        } catch (Throwable t) {
            System.err.println("EditorColormapReblender (from-scratch) failed: " + t.getMessage());
        }
    }

    private static void setup2D(int size) {
        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0f, size, 0, size, -1f, 1f);
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GLState.activeTexture(GL13.GL_TEXTURE1);
        GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_REPLACE);
        GL11.glDisable(GL11.GL_TEXTURE_GEN_S);
        GL11.glDisable(GL11.GL_TEXTURE_GEN_T);
        GL11.glLoadIdentity();
        GLState.activeTexture(GL13.GL_TEXTURE0);
        GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_REPLACE);
        GL11.glDisable(GL11.GL_TEXTURE_GEN_S);
        GL11.glDisable(GL11.GL_TEXTURE_GEN_T);
        GL11.glLoadIdentity();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }

    private static int clamp(int v, int lo, int hi) {
        return StrictMath.max(lo, StrictMath.min(hi, v));
    }

    private static int to255(float c) { return (int) (StrictMath.max(0f, StrictMath.min(1f, c)) * 255f + 0.5f); }

    private static final class Channels {
        final Channel highlight;
        final Channel shadow;
        final Channel seabottom;
        Channels(Channel h, Channel s, Channel sb) { this.highlight = h; this.shadow = s; this.seabottom = sb; }
    }

    private static Channels buildDynamicChannels(World world, int terrainType) {
        HeightMap hm = world.getHeightMap();
        int N = hm.getGridUnitsPerWorld();
        float seaLevelMeters = hm.getSeaLevelMeters();
        float heightScale = seaLevelMeters / Globals.SEA_LEVEL;

        Channel height = new Channel(N, N);
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                float hNorm = hm.getWrappedHeight(x, y) / heightScale;
                if (hNorm < 0f) hNorm = 0f; else if (hNorm > 1f) hNorm = 1f;
                height.putPixel(x, y, hNorm);
            }
        }

        // Lighting (highlight/shadow) as in generator, including shadowcast sweep
        Channel highlight = new Channel(N, N);
        Channel shadow = new Channel(N, N);
        float lx = 1f, lz = 1f;
        float lnorm = 1f / (float) StrictMath.sqrt(lx * lx + lz * lz);
        lx *= lnorm; lz *= lnorm;
        float threshold = (float) StrictMath.sqrt(0.5f);
        float meters_per_height_unit = (float) hm.getMetersPerWorld() / (float) N;
        float nz = 2f * meters_per_height_unit / heightScale;
        float nzlz = nz * lz;
        float nz2 = nz * nz;
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                float nx = height.getPixelWrap(x + 1, y) - height.getPixelWrap(x - 1, y);
                float ny = height.getPixelWrap(x, y + 1) - height.getPixelWrap(x, y - 1);
                float denom = (float) StrictMath.sqrt(nx * nx + ny * ny + nz2);
                float light = (nx * lx + nzlz) / (denom == 0f ? 1f : denom);
                if (light > threshold) {
                    highlight.putPixel(x, y, light);
                    shadow.putPixel(x, y, threshold);
                } else {
                    highlight.putPixel(x, y, threshold);
                    shadow.putPixel(x, y, StrictMath.max(0f, light));
                }
            }
        }
        highlight.dynamicRange(0f, 0.25f);
        shadow.invert().dynamicRange(0f, 0.75f);
        // Shadowcasting sweep similar to Landscape (horizontal sweep)
        Channel shadowcast = new Channel(N, N);
        float peak = 0f;
        float descent = 8f / (float) N;
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                float val = height.getPixel(x, y);
                peak -= descent;
                if (peak > val) shadowcast.putPixel(x, y, 1f); else peak = val;
            }
            peak = 0f;
        }
        shadow.channelBrightest(shadowcast.smooth(1).brightness(0.67f));

        // Seabottom mask
        Channel seabottom = height.copy().invert().dynamicRange(1f - Globals.SEA_LEVEL, 1f, 0f, 1f);
        if (terrainType == com.oddlabs.tt.procedural.Landscape.NATIVE) seabottom.grow(0f, 1).gamma(0.5f);
        else seabottom.gamma(0.5f);

        // Strictly limit seabottom to water cells
        boolean[][] water = hm.getWaterGrid();
        if (water != null) {
            for (int y = 0; y < N; y++) {
                for (int x = 0; x < N; x++) {
                    if (!water[y][x]) seabottom.putPixel(x, y, 0f);
                }
            }
        }

        return new Channels(highlight, shadow, seabottom);
    }

    // ------- Material alpha + overlay helpers -------

    private static GLByteImage[] buildMaterialAlphas(World world, int terrainType) {
        HeightMap hm = world.getHeightMap();
        int N = hm.getGridUnitsPerWorld();
        float seaLevelMeters = hm.getSeaLevelMeters();
        float heightScale = seaLevelMeters / Globals.SEA_LEVEL;

        Channel height = new Channel(N, N);
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                float hNorm = hm.getWrappedHeight(x, y) / heightScale;
                if (hNorm < 0f) hNorm = 0f; else if (hNorm > 1f) hNorm = 1f;
                height.putPixel(x, y, hNorm);
            }
        }
        Channel slope = height.copy().lineart();
        int window = StrictMath.max(1, N >> 5);
        Channel relheight = height.copy().relativeIntensityNormalized(window);

        float access_threshold;
        switch (hm.getMetersPerWorld()) {
            case 256: access_threshold = 0.05f; break;
            case 512: access_threshold = 0.0375f; break;
            case 1024: access_threshold = 0.025f; break;
            case 2048: access_threshold = 0.02f; break; // match generator for largest maps
            default:
                // Fallback: align with generator’s intent. If even larger worlds are introduced,
                // prefer the most conservative (lower) threshold to avoid losing cliffs.
                access_threshold = (hm.getMetersPerWorld() > 1024) ? 0.02f : 0.0375f;
                break;
        }
        float build_threshold = access_threshold / 2f;

        GLByteImage[] out = new GLByteImage[4];

        if (terrainType == com.oddlabs.tt.procedural.Landscape.NATIVE) {
            // Dirt
            Channel a0 = height.copy().dynamicRange(1.1f * Globals.SEA_LEVEL, 2f * Globals.SEA_LEVEL, 0f, 1f);
            a0.channelSubtract(relheight.copy().invert().dynamicRange(0.5f, 0.6f, 0f, 0.5f));
            // Rubble
            Channel a1 = slope.copy().dynamicRange(build_threshold, access_threshold, 0f, 1f);
            a1.channelSubtract(height.copy().invert().dynamicRange(0.8f, 1f, 0f, 1f));
            a1.channelSubtract(relheight.copy().invert().dynamicRange(0.5f, 0.65f, 0f, 0.5f));
            // Rock
            Channel a2 = slope.copy().threshold(access_threshold, 1f);
            // Grass
            Channel grassNoise = new com.oddlabs.tt.procedural.Midpoint(N, 4, 0.45f, Globals.LANDSCAPE_SEED)
                    .toChannel()
                    .dynamicRange(1f - 0.25f - 0.75f * 0.5f, 1f, 0f, 1f)
                    .gamma2();
            Channel slopeSoft = slope.copy().dynamicRange(0f, access_threshold, 0f, 1f).invert()
                    .dynamicRange(1f - 0.25f - 0.75f * 0.5f, 1f, 0f, 1f).gamma2();
            Channel a3 = grassNoise;
            a3.channelBrightest(slopeSoft);
            a3.channelAdd(relheight.copy().invert().add(-0.5f).multiply(2f));
            a3.channelSubtract(height.copy().invert().dynamicRange(0.6f, 0.8f, 0f, 1f));
            a3.channelSubtract(slope.copy().threshold(0.75f * access_threshold, 1f).smooth(3));
            a3.channelSubtract(relheight.copy().invert().dynamicRange(0.5f, 0.7f, 0f, 0.5f));
            out[0] = new GLByteImage(a0);
            out[1] = new GLByteImage(a1);
            out[2] = new GLByteImage(a2);
            out[3] = new GLByteImage(a3);
        } else {
            // Viking: soil, cliff, grass, snow
            Channel a0 = height.copy().dynamicRange(1.1f * Globals.SEA_LEVEL, 2f * Globals.SEA_LEVEL, 0f, 1f);
            a0.channelSubtract(relheight.copy().invert().dynamicRange(0.5f, 0.6f, 0f, 0.5f));
            Channel a1 = slope.copy().threshold(access_threshold, 1f); // cliff
            Channel grassNoise = new com.oddlabs.tt.procedural.Midpoint(N, 4, 0.45f, Globals.LANDSCAPE_SEED)
                    .toChannel()
                    .dynamicRange(1f - 0.25f - 0.75f * 0.5f, 1f, 0f, 1f)
                    .gamma2();
            Channel slopeSoft = slope.copy().dynamicRange(0f, access_threshold, 0f, 1f).invert()
                    .dynamicRange(1f - 0.25f - 0.75f * 0.5f, 1f, 0f, 1f).gamma2();
            Channel a2 = grassNoise;
            a2.channelBrightest(slopeSoft);
            a2.channelAdd(relheight.copy().invert().add(-0.5f).multiply(2f));
            a2.channelSubtract(height.copy().invert().dynamicRange(0.6f, 0.8f, 0f, 1f));
            a2.channelSubtract(slope.copy().threshold(0.75f * access_threshold, 1f).smooth(3));
            a2.channelSubtract(relheight.copy().invert().dynamicRange(0.5f, 0.7f, 0f, 0.5f));
            Channel a3 = height.copy().dynamicRange(0.5f, 0.6f, 0f, 1f);
            a3.channelSubtract(a1.copy());
            a3.smooth(1).smooth(1);
            out[0] = new GLByteImage(a0);
            out[1] = new GLByteImage(a1);
            out[2] = new GLByteImage(a2);
            out[3] = new GLByteImage(a3);
        }

        // Mask materials to land only (avoid underwater material tint)
        boolean[][] water = hm.getWaterGrid();
        if (water != null) {
            for (int i = 0; i < out.length; i++) {
                if (out[i] == null) continue;
                java.nio.ByteBuffer buf = out[i].getPixels();
                for (int y = 0; y < N; y++) {
                    for (int x = 0; x < N; x++) {
                        if (water[y][x]) buf.put(y * N + x, (byte) 0);
                    }
                }
            }
        }

        return out;
    }

    // Build full overlay chain: base, materials, lighting, shadow, seabottom.
    private static BlendInfo[] buildOverlayChainFull(
            int terrainType,
            GLIntImage[] structures,
            GLByteImage[] mats,
            GLByteImage highlight,
            GLByteImage shadow,
            Channel seabottomChan) {
        // structures layout for both terrain types:
        // [0]=base (native:sand, viking:gravel), [1..4]=materials, [5]=black, [6]=seabottom
        GLIntImage baseStruct = safe(structures, 0, solid(96, 64, 32));
        GLIntImage m1 = safe(structures, 1, solid(120, 100, 80));
        GLIntImage m2 = safe(structures, 2, solid(110, 110, 110));
        GLIntImage m3 = safe(structures, 3, solid(80, 80, 85));
        GLIntImage m4 = safe(structures, 4, solid(60, 100, 40));
        GLIntImage black = safe(structures, 5, solid(0, 0, 0));
        GLIntImage seabottomStruct = safe(structures, 6, solid(30, 60, 90));

        // Alphas
        GLByteImage a1 = mats[0];
        GLByteImage a2 = mats[1];
        GLByteImage a3 = mats[2];
        GLByteImage a4 = mats[3];
        GLByteImage seabottom = new GLByteImage(seabottomChan);

        java.util.ArrayList<BlendInfo> chain = new java.util.ArrayList<>();
        // Base constant layer (alpha 1x1 white)
        GLByteImage alphaWhite = new GLByteImage(1, 1, GL11.GL_ALPHA);
        alphaWhite.putPixel(0, 0, 255);
        chain.add(new StructureBlend(baseStruct, alphaWhite));
        // Material overlays (match generator: 1..4)
        chain.add(new StructureBlend(m1, a1));
        chain.add(new StructureBlend(m2, a2));
        chain.add(new StructureBlend(m3, a3));
        chain.add(new StructureBlend(m4, a4));
        // Lighting highlight
        chain.add(new BlendLighting(highlight, 1f, 0.9f, 0.6f));
        // Shadow darken
        chain.add(new StructureBlend(black, shadow));
        // Seabottom tint
        chain.add(new StructureBlend(seabottomStruct, seabottom));
        return chain.toArray(new BlendInfo[0]);
    }

    private static GLIntImage safe(GLIntImage[] arr, int idx, GLIntImage fallback) {
        if (arr == null || idx < 0 || idx >= arr.length) return fallback;
        return arr[idx] != null ? arr[idx] : fallback;
    }

    private static GLIntImage solid(int r, int g, int b) {
        GLIntImage img = new GLIntImage(256, 256, GL11.GL_RGBA);
        int px = ((r & 0xff) << 24) | ((g & 0xff) << 16) | ((b & 0xff) << 8) | 0xff;
        for (int y = 0; y < img.getHeight(); y++)
            for (int x = 0; x < img.getWidth(); x++) img.putPixel(x, y, px);
        return img;
    }

    private static GLIntImage loadStructure(String name) {
        try {
            File f = new File("tt/" + name + ".png");
            if (!f.exists()) return null;
            BufferedImage bi = ImageIO.read(f);
            int w = bi.getWidth();
            int h = bi.getHeight();
            GLIntImage img = new GLIntImage(w, h, GL11.GL_RGBA);
            int[] argb = new int[w * h];
            bi.getRGB(0, 0, w, h, argb, 0, w);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int p = argb[y * w + x];
                    int a = (p >> 24) & 0xff;
                    int r = (p >> 16) & 0xff;
                    int g = (p >> 8) & 0xff;
                    int b = (p) & 0xff;
                    int rgba = (r << 24) | (g << 16) | (b << 8) | a;
                    img.putPixel(x, y, rgba);
                }
            }
            return img;
        } catch (Throwable t) {
            return null;
        }
    }
}
