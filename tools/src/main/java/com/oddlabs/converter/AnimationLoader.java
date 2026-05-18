package com.oddlabs.converter;

import org.jspecify.annotations.NonNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

public final class AnimationLoader {
    private AnimationLoader() {
    }

    public static @NonNull Map<@NonNull String, float @NonNull []> @NonNull [] loadAnimation(@NonNull Path file) {
        try (var input_stream = Files.newInputStream(file)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new GeometryErrorHandler());
            Document document = builder.parse(input_stream);
            Element root = document.getDocumentElement();
            return parseAnimation(root);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static @NonNull Map<@NonNull String, float @NonNull []> @NonNull [] parseAnimation(@NonNull Node node) {
        NodeList frames = node.getChildNodes();
        Map<Integer, Map<String, float[]>> anim_infos_map = new HashMap<>();
        IntStream.range(0, frames.getLength()).mapToObj(frames::item).filter(frame -> frame.getNodeName().equals(
                "frame")).forEach(frame -> {
                    int frame_index = getAttrInt(frame, "index");
                    assert frame_index >= 0;
                    anim_infos_map.put(frame_index, parseFrame(frame));
                });
        @SuppressWarnings("unchecked") var anim_infos = (Map<String, float[]>[]) new Map[anim_infos_map.size()];
        anim_infos_map.keySet().forEach(frame_index_obj -> {
            Map<String, float[]> frame = anim_infos_map.get(frame_index_obj);
            int index = frame_index_obj;
            assert anim_infos[index] == null;
            anim_infos[index] = frame;
        });
        return anim_infos;
    }

    public static @NonNull Map<@NonNull String, float @NonNull []> parseFrame(@NonNull Node node) {
        NodeList bones = node.getChildNodes();
        Map<String, float[]> bone_infos = new HashMap<>();
        IntStream.range(0, bones.getLength()).mapToObj(bones::item).filter(bone -> bone.getNodeName().equals(
                "transform")).forEach(bone -> {
                    String name = bone.getAttributes().getNamedItem("name").getNodeValue();
                    float[] matrix = new float[16];
                    matrix[0 * 4 + 0] = getAttrFloat(bone, "m00");
                    matrix[0 * 4 + 1] = getAttrFloat(bone, "m01");
                    matrix[0 * 4 + 2] = getAttrFloat(bone, "m02");
                    matrix[0 * 4 + 3] = getAttrFloat(bone, "m03");
                    matrix[1 * 4 + 0] = getAttrFloat(bone, "m10");
                    matrix[1 * 4 + 1] = getAttrFloat(bone, "m11");
                    matrix[1 * 4 + 2] = getAttrFloat(bone, "m12");
                    matrix[1 * 4 + 3] = getAttrFloat(bone, "m13");
                    matrix[2 * 4 + 0] = getAttrFloat(bone, "m20");
                    matrix[2 * 4 + 1] = getAttrFloat(bone, "m21");
                    matrix[2 * 4 + 2] = getAttrFloat(bone, "m22");
                    matrix[2 * 4 + 3] = getAttrFloat(bone, "m23");
                    matrix[3 * 4 + 0] = getAttrFloat(bone, "m30");
                    matrix[3 * 4 + 1] = getAttrFloat(bone, "m31");
                    matrix[3 * 4 + 2] = getAttrFloat(bone, "m32");
                    matrix[3 * 4 + 3] = getAttrFloat(bone, "m33");
                    bone_infos.put(name, matrix);
                });
        return bone_infos;
    }

    private static int getAttrInt(@NonNull Node node, @NonNull String name) {
        return Integer.parseInt(node.getAttributes().getNamedItem(name).getNodeValue());
    }

    private static float getAttrFloat(@NonNull Node node, @NonNull String name) {
        return Float.parseFloat(node.getAttributes().getNamedItem(name).getNodeValue());
    }
}
