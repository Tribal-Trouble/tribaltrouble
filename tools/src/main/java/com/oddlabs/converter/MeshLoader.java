package com.oddlabs.converter;

import com.oddlabs.converter.xml.MeshFile;
import com.oddlabs.converter.xml.Polygon;
import com.oddlabs.converter.xml.SkinWeight;
import com.oddlabs.converter.xml.Vertex;
import com.oddlabs.converter.xml.Xml;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class MeshLoader {
    private static final byte STATIC_BONE = 0;

    private MeshLoader() {
    }

    public static @NonNull ModelInfo loadMesh(@NonNull Path file,
            @Nullable Map<@NonNull String, @NonNull Bone> name_to_bone_map, float scale) {
        try {
            return createModelInfo(Xml.read(file, MeshFile.class), name_to_bone_map, scale);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // The optimizer takes flat arrays, three corners per triangle in file order.
    private static @NonNull ModelInfo createModelInfo(@NonNull MeshFile mesh,
            @Nullable Map<String, Bone> name_to_bone_map, float scale) {
        int num_vertices = mesh.polygons().triangles().size() * Polygon.CORNERS;
        float[] vertices = new float[num_vertices * 3];
        float[] colors = new float[num_vertices * 3];
        float[] normals = new float[num_vertices * 3];
        float[] uvs = new float[num_vertices * 2];
        float[] uvs2 = new float[num_vertices * 2];
        byte[][] skin_names = new byte[num_vertices][];
        float[][] skin_weights = new float[num_vertices][];

        int corner = 0;
        for (Polygon polygon : mesh.polygons().triangles()) {
            for (Vertex vertex : polygon.vertices()) {
                vertices[corner * 3] = vertex.x() * scale;
                vertices[corner * 3 + 1] = vertex.y() * scale;
                vertices[corner * 3 + 2] = vertex.z() * scale;
                colors[corner * 3] = vertex.r();
                colors[corner * 3 + 1] = vertex.g();
                colors[corner * 3 + 2] = vertex.b();
                float len_sq = vertex.nx() * vertex.nx() + vertex.ny() * vertex.ny() + vertex.nz() * vertex.nz();
                if (len_sq > 0f) {
                    float vec_len_inv = 1f / (float) Math.sqrt(len_sq);
                    normals[corner * 3] = vertex.nx() * vec_len_inv;
                    normals[corner * 3 + 1] = vertex.ny() * vec_len_inv;
                    normals[corner * 3 + 2] = vertex.nz() * vec_len_inv;
                } else {
                    normals[corner * 3] = 0f;
                    normals[corner * 3 + 1] = 0f;
                    normals[corner * 3 + 2] = 1f;
                }
                uvs[corner * 2] = vertex.u();
                uvs[corner * 2 + 1] = vertex.v();
                uvs2[corner * 2] = vertex.u2();
                uvs2[corner * 2 + 1] = vertex.v2();

                List<SkinWeight> skins = vertex.skins();
                byte[] vertex_skin_names = new byte[skins.size()];
                float[] vertex_skin_weights = new float[skins.size()];
                for (int i = 0; i < skins.size(); i++) {
                    SkinWeight skin = skins.get(i);
                    vertex_skin_names[i] = name_to_bone_map != null ? name_to_bone_map.get(
                            skin.bone()).index() : STATIC_BONE;
                    vertex_skin_weights[i] = skin.weight();
                }
                skin_names[corner] = vertex_skin_names;
                skin_weights[corner] = vertex_skin_weights;
                corner++;
            }
        }
        return Optimizer.optimize(num_vertices, vertices, normals, colors, uvs, uvs2, skin_names, skin_weights);
    }
}
