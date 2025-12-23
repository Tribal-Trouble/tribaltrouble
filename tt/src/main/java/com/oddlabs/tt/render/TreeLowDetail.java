package com.oddlabs.tt.render;

import com.oddlabs.geometry.LowDetailModel;
import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.landscape.AbstractTreeGroup;
import com.oddlabs.tt.landscape.TreeGroup;
import com.oddlabs.tt.landscape.TreeLeaf;
import com.oddlabs.tt.landscape.TreeNodeVisitor;
import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.render.shader.TreeLowDetailShader;
import com.oddlabs.tt.resource.Resources;
import com.oddlabs.tt.resource.TextureFile;
import com.oddlabs.tt.util.GLState;
import com.oddlabs.tt.vbo.FloatVBO;
import com.oddlabs.tt.vbo.ShortVBO;
import com.oddlabs.tt.vbo.VertexArray;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;
import java.util.List;
import java.util.Map;

import static com.oddlabs.tt.landscape.AbstractTreeGroup.TreeType;

public final class TreeLowDetail {
    private static final Vector4f src = new Vector4f();
    private static final Vector4f dest = new Vector4f();

    private final @NonNull FloatVBO vertices;
    private final @NonNull FloatVBO texcoords;
    private final @NonNull ShortVBO tree_indices;
    private final @NonNull Texture @NonNull [] lowdetail_textures;
    private final @NonNull Map<@NonNull TreeType, @NonNull Tree> trees;
    private final @NonNull Map<@NonNull TreeType, @NonNull LowDetailModel> low_details;
    private final @NonNull VertexArray vao;

    private final Landscape.@NonNull TerrainType terrain;

    private int current_vertex_index;

    private static final TreeLowDetailShader shader = new TreeLowDetailShader();

    public TreeLowDetail(@NonNull World world, @NonNull Map<@NonNull TreeType, @NonNull Tree> trees, @NonNull Map<@NonNull TreeType, @NonNull LowDetailModel> tree_low_details, @NonNull List<int[]> tree_positions, @NonNull List<int[]> palm_tree_positions, Landscape.@NonNull TerrainType terrain) {
        lowdetail_textures = new Texture[]{
                Resources.findResource(new TextureFile("/textures/models/lowdetail_tree", Globals.COMPRESSED_RGBA_FORMAT)),
                Resources.findResource(new TextureFile("/textures/models/viking_lowdetail_tree", Globals.COMPRESSED_RGBA_FORMAT))};
        int[] num_trees = switch (terrain) {
            case NATIVE -> new int[]{tree_positions.size(), palm_tree_positions.size(), 0, 0};
            case VIKING -> new int[]{0, 0, tree_positions.size(), palm_tree_positions.size()};
        };

        this.low_details = tree_low_details;
        this.trees = trees;
        this.terrain = terrain;
        current_vertex_index = 0;
        int vertex_count = 0;
        int index_count = 0;
        TreeType[] ordinals = TreeType.values();
        for (int i = 0; i < num_trees.length; i++) {
            vertex_count += num_trees[i] * low_details.get(ordinals[i]).getVertices().length / 3;
            index_count += num_trees[i] * low_details.get(ordinals[i]).getIndices().length;
        }
        vertices = new FloatVBO(GL15.GL_DYNAMIC_DRAW, vertex_count * 3);
        texcoords = new FloatVBO(GL15.GL_STATIC_DRAW, vertex_count * 2);
        tree_indices = new ShortVBO(GL15.GL_STATIC_DRAW, index_count);

        vao = new VertexArray();
        vao.bind();
        
        int posLoc = shader.getAttributeLocation(TreeLowDetailShader.Attributes.POSITION);
        vertices.makeCurrent();
        GL20.glEnableVertexAttribArray(posLoc);
        GL20.glVertexAttribPointer(posLoc, 3, GL11.GL_FLOAT, false, 0, 0L);

        int texLoc = shader.getAttributeLocation(TreeLowDetailShader.Attributes.TEX_COORD);
        texcoords.makeCurrent();
        GL20.glEnableVertexAttribArray(texLoc);
        GL20.glVertexAttribPointer(texLoc, 2, GL11.GL_FLOAT, false, 0, 0L);
        
        tree_indices.makeCurrent();
        
        vao.unbind();
    }

    @NonNull Map<@NonNull TreeType, @NonNull Tree> getTrees() {
        return trees;
    }

    void build(@NonNull AbstractTreeGroup tree_root) {
        int index_count = tree_indices.capacity();

        BuildVisitor visitor = new BuildVisitor();
        tree_root.visit(visitor);
        assert visitor.end == index_count : "end index " + visitor.end + " != num coords " + index_count;
        assert current_vertex_index == vertices.capacity() / 3 : "vertex index index " + current_vertex_index + " != num coords " + vertices.capacity() / 3;
        vertices.put(visitor.vertex_array);
        texcoords.put(visitor.texcoord_array);
        tree_indices.put(visitor.tree_index_array);
    }

    void loadMatrix(@NonNull Matrix4f matrix, @NonNull MatrixStack stack) {
        stack.multiply(matrix);
    }

    private int putCoordinate(int index, float x, float y, float z, float u, float v, float @NonNull [] vertice_array, float @NonNull [] texcoord_array) {
        vertice_array[index * 3] = x;
        vertice_array[index * 3 + 1] = y;
        vertice_array[index * 3 + 2] = z;

        texcoord_array[index * 2] = u;
        texcoord_array[index * 2 + 1] = v;
        return index + 1;
    }

    private int putIndex(int index, int tree_index, short @NonNull [] tree_indice_array) {
        assert tree_index <= Character.MAX_VALUE;
        short tree_char_index = (short) tree_index;
        tree_indice_array[index] = tree_char_index;
        return index + 1;
    }

    private int[] putLowDetail(int start_index, @NonNull Matrix4f matrix, @NonNull LowDetailModel low_detail_model, float @NonNull [] vertice_array, float @NonNull [] texcoord_array, short @NonNull [] tree_indice_array) {
        float[] vertices = low_detail_model.getVertices();
        float[] tex_coords = low_detail_model.getTexCoords();
        short[] indices = low_detail_model.getIndices();
        int end = start_index;
        int start_vertex_index = current_vertex_index;
        for (short index : indices) {
            end = putIndex(end, index + current_vertex_index, tree_indice_array);
        }
        for (int i = 0; i < vertices.length / 3; i++) {
            src.set(vertices[i * 3], vertices[i * 3 + 1], vertices[i * 3 + 2], 1f);
            matrix.transform(src, dest);
            float u = tex_coords[i * 2];
            float v = tex_coords[i * 2 + 1];
            current_vertex_index = putCoordinate(current_vertex_index, dest.x, dest.y, dest.z, u, v, vertice_array, texcoord_array);
        }
        return new int[]{end, start_vertex_index};
    }

    public void updateLowDetail(@NonNull Matrix4f matrix, @NonNull TreeSupply tree) {
        int start_vertex_index = tree.getLowDetailStartIndex();
        TreeType tree_type = tree.getTreeType();
        LowDetailModel low_detail_model = low_details.get(tree_type);
        float[] vertex_array = low_detail_model.getVertices();
        FloatBuffer update_buffer = BufferUtils.createFloatBuffer(vertex_array.length);
        for (int i = 0; i < vertex_array.length / 3; i++) {
            src.set(vertex_array[i * 3], vertex_array[i * 3 + 1], vertex_array[i * 3 + 2], 1f);
            matrix.transform(src, dest);
            update_buffer.put(i * 3, dest.x);
            update_buffer.put(i * 3 + 1, dest.y);
            update_buffer.put(i * 3 + 2, dest.z);
        }
        vertices.putSubData(start_vertex_index * 3, update_buffer);
    }

    @NonNull GLState setupTrees(@NonNull CameraState state, @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
        GLState shaderState = shader.use();
        GLState fogState = state.getFog().setup(shader, state.getCurrentZ());

        Matrix4f mvpMatrix = new Matrix4f(projectionStack.current()).mul(modelViewStack.current());
        shader.setUniformMatrix4("u_mvpMatrix", false, mvpMatrix);
        shader.setUniformMatrix4("u_modelViewMatrix", false, modelViewStack.current());

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, lowdetail_textures[terrain.ordinal()].getHandle());
        shader.setUniform(TreeLowDetailShader.Uniforms.TEXTURE_0, 0);

        vao.bind();

        return () -> {
            vao.unbind();
            fogState.close();
            shaderState.close();
        };
    }

    void renderLowDetail(int start, int count) {
        tree_indices.drawElements(GL11.GL_TRIANGLES, count, start);
    }

    private final class BuildVisitor implements TreeNodeVisitor {
        private int end = 0;
        private final float[] vertex_array = new float[vertices.capacity()];
        private final float[] texcoord_array = new float[texcoords.capacity()];
        private final short[] tree_index_array = new short[tree_indices.capacity()];

        @Override
        public void visitLeaf(@NonNull TreeLeaf tree_leaf) {
            int start = end;
            tree_leaf.visitTrees(this);
            tree_leaf.initLowDetailBuffer(start, end);
        }

        @Override
        public void visitNode(@NonNull TreeGroup tree_group) {
            int start = end;
            tree_group.visitChildren(this);
            tree_group.initLowDetailBuffer(start, end);
        }

        @Override
        public void visitTree(@NonNull TreeSupply tree_supply) {
            int start_index = end;
            TreeType tree_type = tree_supply.getTreeType();
            int[] values = putLowDetail(end, tree_supply.getMatrix(), low_details.get(tree_type), vertex_array, texcoord_array, tree_index_array);
            int end_index = values[0];
            tree_supply.setLowDetailStartIndex(values[1]);
            tree_supply.initLowDetailBuffer(start_index, end_index);
            end = end_index;
        }
    }
}
