package com.oddlabs.tt.scenery;

import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.procedural.GeneratorOcean;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.procedural.TextureGenerator;
import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.resource.Resources;
import com.oddlabs.tt.util.GLState;
import com.oddlabs.tt.util.GLStateStack;
import com.oddlabs.tt.util.GLUtils;
import com.oddlabs.tt.vbo.FloatVBO;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBBufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.nio.FloatBuffer;

public final class Water {

    private final @NonNull FloatVBO patch_vertices;
    private final Texture[] ocean;

    public Water(@NonNull HeightMap heightmap, Landscape.TerrainType terrain) {
        TextureGenerator ocean_desc = new GeneratorOcean(terrain);
        ocean = Resources.findResource(ocean_desc);
        patch_vertices = makePatchVertices(heightmap);
    }

    private void setup() {
        GLStateStack.switchState(GLState.VERTEX_ARRAY);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, ocean[0].getHandle());
        GL11.glEnable(GL11.GL_TEXTURE_GEN_S);
        GL11.glEnable(GL11.GL_TEXTURE_GEN_T);
        GLUtils.setupTexGen(Globals.WATER_REPEAT_RATE, Globals.WATER_REPEAT_RATE, 0, 0);
        if (Globals.draw_detail) {
            GLState.activeTexture(GL13.GL_TEXTURE1);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, ocean[1].getHandle());
            GL11.glEnable(GL11.GL_TEXTURE_GEN_S);
            GL11.glEnable(GL11.GL_TEXTURE_GEN_T);
            GLUtils.setupTexGen(Globals.WATER_DETAIL_REPEAT_RATE, Globals.WATER_DETAIL_REPEAT_RATE, 0, 0);
            GLState.activeTexture(GL13.GL_TEXTURE0);
        }

        GL11.glEnable(GL11.GL_BLEND);
    }

    public void render(@NonNull Sky sky) {
        setup();

        sky.getWaterVertices().vertexPointer(3, 0, 0);
        sky.getWaterIndices().drawElements(GL11.GL_TRIANGLES, sky.getWaterIndices().capacity(), 0);

        // render patches
        patch_vertices.vertexPointer(3, 0, 0);
        GL11.glDrawArrays(GL11.GL_QUADS, 0, patch_vertices.capacity() / 3);
        //patch_indices.drawElements(GL11.GL_TRIANGLES, patch_indices.capacity(), 0);
        reset();
    }

    private void reset() {
        if (Globals.draw_detail) {
            GLState.activeTexture(GL13.GL_TEXTURE1);
            GL11.glDisable(GL11.GL_TEXTURE_GEN_S);
            GL11.glDisable(GL11.GL_TEXTURE_GEN_T);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GLState.activeTexture(GL13.GL_TEXTURE0);
        }

        GL11.glDisable(GL11.GL_TEXTURE_GEN_T);
        GL11.glDisable(GL11.GL_TEXTURE_GEN_S);
        GL11.glDisable(GL11.GL_BLEND);
    }

    private static @NonNull FloatVBO makePatchVertices(@NonNull HeightMap heightmap) {
        boolean[][] water_patches = new boolean[heightmap.getPatchesPerWorld()][heightmap.getPatchesPerWorld()];
        int count = 0;
        for (int y = 0; y < water_patches.length; y++) {
            for (int x = 0; x < water_patches[y].length; x++) {
                if (heightmap.isBelowSeaLevel(x, y)) {
                    water_patches[y][x] = true;
                    count++;
                }
            }
        }
        int size = count * 4 * 3;
        FloatBuffer temp = BufferUtils.createFloatBuffer(size);

        for (int y = 0; y < water_patches.length; y++) {
            for (int x = 0; x < water_patches[y].length; x++) {
                if (water_patches[y][x]) {
                    temp.put(x * heightmap.getMetersPerPatch());
                    temp.put(y * heightmap.getMetersPerPatch());
                    temp.put(heightmap.getSeaLevelMeters());

                    temp.put((x + 1) * heightmap.getMetersPerPatch());
                    temp.put(y * heightmap.getMetersPerPatch());
                    temp.put(heightmap.getSeaLevelMeters());

                    temp.put((x + 1) * heightmap.getMetersPerPatch());
                    temp.put((y + 1) * heightmap.getMetersPerPatch());
                    temp.put(heightmap.getSeaLevelMeters());

                    temp.put(x * heightmap.getMetersPerPatch());
                    temp.put((y + 1) * heightmap.getMetersPerPatch());
                    temp.put(heightmap.getSeaLevelMeters());
                }
            }
        }
        assert !temp.hasRemaining();
        temp.flip();
        FloatVBO vertices = new FloatVBO(ARBBufferObject.GL_STATIC_DRAW_ARB, temp.remaining());
        vertices.put(temp);
        return vertices;
    }
}
