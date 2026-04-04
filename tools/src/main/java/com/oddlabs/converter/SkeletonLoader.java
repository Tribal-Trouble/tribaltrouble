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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SkeletonLoader {
    private SkeletonLoader() {
    }

    public static @NonNull Skeleton loadSkeleton(@NonNull Path file) {
        try (var input_stream = Files.newInputStream(file)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new GeometryErrorHandler());
            Document document = builder.parse(input_stream);
            Element root = document.getDocumentElement();
            return parseSkeleton(root);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static @NonNull Skeleton parseSkeleton(@NonNull Node skel_node) {
        Map<String, Bone> name_to_bone_map = new HashMap<>();
        Map<String, float[]> initial_pose = AnimationLoader.parseFrame(ConvertToBinary.getNodeByName("init_pose", skel_node));
        NodeList bone_list = ConvertToBinary.getNodeByName("bones", skel_node).getChildNodes();
        Map<String, String> bone_parent_map = new HashMap<>();
        for (int i = 0; i < bone_list.getLength(); i++) {
            Node bone_node = bone_list.item(i);
            if (bone_node.getNodeName().equals("bone")) {
                String bone_name = bone_node.getAttributes().getNamedItem("name").getNodeValue();
                String bone_parent_name = bone_node.getAttributes().getNamedItem("parent").getNodeValue();
//System.out.println("bone name = " + bone_name + " parent name = " + bone_parent_name);
                bone_parent_map.put(bone_name, bone_parent_name);
            }
        }
        Map<String, List<String>> bone_children_map = new HashMap<>();
        String root = null;
        for (String name : bone_parent_map.keySet()) {
            String parent = bone_parent_map.get(name);
            if (bone_parent_map.get(parent) == null) {
                if (root != null) {
                    IO.println("WARNING: Multiple roots in skeleton, root = " + root + ", additional root = " + name);
                    parent = root;
                    bone_parent_map.put(name, parent);
                } else
                    root = name;
            }
            List<String> parent_children = bone_children_map.computeIfAbsent(parent, _ -> new ArrayList<>());
            parent_children.add(name);
        }
        Bone bone_root = buildBone((byte) 0, bone_children_map, root, name_to_bone_map);
        return new Skeleton(bone_root, initial_pose, name_to_bone_map);
    }

    private static @NonNull Bone buildBone(byte index, @NonNull Map<@NonNull String, @NonNull List<@NonNull String>> bone_children_map, @NonNull String bone_name, @NonNull Map<@NonNull String, @NonNull Bone> name_to_bone_map) {
        List<String> children_list = bone_children_map.getOrDefault(bone_name, List.of());
        Bone[] children_array = new Bone[children_list.size()];
        for (int i = 0; i < children_array.length; i++) {
            String child_name = children_list.get(i);
            Bone child_bone = buildBone(index, bone_children_map, child_name, name_to_bone_map);
            children_array[i] = child_bone;
            index = (byte) (child_bone.index() + 1);
        }
        Bone bone = new Bone(bone_name, index, children_array);
        name_to_bone_map.put(bone_name, bone);
        return bone;
    }
}
