package com.oddlabs.converter2;

import org.jspecify.annotations.NonNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class SkeletonLoader {
	private SkeletonLoader() {
	}

	public static @NonNull Skeleton loadSkeleton(@NonNull File file) {
		try (FileInputStream input_stream = new FileInputStream(file)) {
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
		Map<String,Bone> name_to_bone_map = new HashMap<>();
		Map<String,float[]> initial_pose = AnimationLoader.parseFrame(ConvertToBinary.getNodeByName("init_pose", skel_node));
		NodeList bone_list = ConvertToBinary.getNodeByName("bones", skel_node).getChildNodes();
		Map<String,String> bone_parent_map = new HashMap<>();
		for (int i = 0; i < bone_list.getLength(); i++) {
			Node bone_node = bone_list.item(i);
			if (bone_node.getNodeName().equals("bone")) {
				String bone_name = bone_node.getAttributes().getNamedItem("name").getNodeValue();
				String bone_parent_name = bone_node.getAttributes().getNamedItem("parent").getNodeValue();
//System.out.println("bone name = " + bone_name + " parent name = " + bone_parent_name);
				bone_parent_map.put(bone_name, bone_parent_name);
			}
		}
		Map<String,List<String>> bone_children_map = new HashMap<>();
		Iterator<String> it = bone_parent_map.keySet().iterator();
		String root = null;
		while (it.hasNext()) {
			String name = it.next();
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
		Bone bone_root = buildBone((byte)0, bone_children_map, root, name_to_bone_map);
		return new Skeleton(bone_root, initial_pose, name_to_bone_map);
	}

	private static @NonNull Bone buildBone(byte index, @NonNull Map<String,List<String>> bone_children_map, String bone_name, @NonNull Map<String,Bone> name_to_bone_map) {
		List<String> children_list = bone_children_map.get(bone_name);
		Bone[] children_array;
		if (children_list != null) {
			children_array = new Bone[children_list.size()];
			for (int i = 0; i < children_array.length; i++) {
				String child_name = children_list.get(i);
				Bone child_bone = buildBone(index, bone_children_map, child_name, name_to_bone_map);
				children_array[i] = child_bone;
				index = (byte)(child_bone.getIndex() + 1);
			}
		} else
			children_array = new Bone[0];
		Bone bone = new Bone(bone_name, index, children_array);
		name_to_bone_map.put(bone_name, bone);
		return bone;
	}
}
