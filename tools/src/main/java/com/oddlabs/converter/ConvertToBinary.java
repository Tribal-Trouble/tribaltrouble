package com.oddlabs.converter;

import com.oddlabs.geometry.AnimationInfo;
import com.oddlabs.geometry.SpriteInfo;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public final class ConvertToBinary {
    private static final String ATTACHMENTS_FILE = "attachments.txt";
    private static final String EVENT_TEXTURES_FILE = "event_textures.txt";
    private static final String NO_EVENT = "-";

    void main(@NonNull String @NonNull... args) {
        if (args.length != 3)
            throw new IllegalArgumentException("Invalid number of arguments : <xml_file> <src_dir> <build_dir>");
        Path xml_file = Path.of(args[0]);
        Path src_dir = Path.of(args[1]);
        Path build_dir = Path.of(args[2]);

        try (var input_stream = Files.newInputStream(src_dir.resolve(xml_file))) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new GeometryErrorHandler());
            Document document = builder.parse(input_stream);
            org.w3c.dom.Element root = document.getDocumentElement();
            parseGeometry(root, src_dir.resolve(xml_file), src_dir, build_dir);
        } catch (Exception e) {
            System.err.println("Error processing " + xml_file);
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static void parseGeometry(@NonNull Node n, @NonNull Path registry, @NonNull Path src_dir,
            @NonNull Path build_dir) {
        if (n.hasChildNodes()) {
            NodeList nl = n.getChildNodes();
            List<String> attachments = new ArrayList<>();
            List<String> event_textures = new ArrayList<>();
            for (int i = 0; i < nl.getLength(); i++) {
                if (nl.item(i).getNodeType() == Node.ELEMENT_NODE)
                    parseGroup(nl.item(i), registry, src_dir, build_dir, attachments, event_textures);
            }
            Collections.sort(attachments);
            Collections.sort(event_textures);
            try {
                Files.createDirectories(build_dir);
                Files.write(build_dir.resolve(ATTACHMENTS_FILE), attachments);
                Files.write(build_dir.resolve(EVENT_TEXTURES_FILE), event_textures);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void parseGroup(@NonNull Node n, @NonNull Path registry, @NonNull Path src_dir,
            @NonNull Path build_dir,
            @NonNull List<String> attachments, @NonNull List<String> event_textures) {
        if (n.hasChildNodes()) {
            Path new_build_dir = build_dir.resolve(getName(n));
            NodeList nl = n.getChildNodes();
            Map<String, Node> sprites = new HashMap<>();
            for (int i = 0; i < nl.getLength(); i++) {
                Node child = nl.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals("sprite"))
                    sprites.put(getName(child), child);
            }
            for (Node sprite : sprites.values()) {
                parseSprite(sprite, sprites, registry, src_dir, new_build_dir);
                Node slot = sprite.getAttributes().getNamedItem("slot");
                if (slot != null)
                    attachments.add(attachmentLine(getName(n), sprite, slot.getNodeValue(), src_dir));
                eventTextureLines(getName(n), sprite, event_textures);
            }
        }
    }

    // group base slot order name textures event: one line per sprite with a slot, default-on sprites sorted first in
    // their slot.
    private static @NonNull String attachmentLine(@NonNull String group, @NonNull Node sprite, @NonNull String slot,
            @NonNull Path src_dir) {
        Node base = sprite.getAttributes().getNamedItem("base");
        if (base == null)
            throw new RuntimeException("Sprite " + getName(sprite) + " has a slot but no base");
        Node default_on = sprite.getAttributes().getNamedItem("default");
        int order = default_on != null && Boolean.parseBoolean(default_on.getNodeValue()) ? 0 : 1;
        int textures = getModelObjectInfos(sprite, src_dir)[0].getTextures().length;
        Node event = sprite.getAttributes().getNamedItem("event");
        return String.join(" ", group, base.getNodeValue(), slot, Integer.toString(order), getName(sprite),
                Integer.toString(textures), event != null ? event.getNodeValue() : NO_EVENT);
    }

    // group sprite event index: the texture index is shared by every detail level, so each model must agree on it.
    private static void eventTextureLines(@NonNull String group, @NonNull Node sprite, @NonNull List<String> lines) {
        Map<String, Integer> indices = null;
        NodeList nl = sprite.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            if (!nl.item(i).getNodeName().equals("model"))
                continue;
            Map<String, Integer> model_indices = new HashMap<>();
            NodeList textures = nl.item(i).getChildNodes();
            int index = 0;
            for (int j = 0; j < textures.getLength(); j++) {
                if (!textures.item(j).getNodeName().equals("texture"))
                    continue;
                Node event = textures.item(j).getAttributes().getNamedItem("event");
                if (event != null)
                    model_indices.put(event.getNodeValue(), index);
                index++;
            }
            if (indices != null && !indices.equals(model_indices))
                throw new RuntimeException("Sprite " + getName(
                        sprite) + " needs the same event textures, in the same place, on every model");
            indices = model_indices;
        }
        if (indices != null)
            indices.forEach((event, index) -> lines.add(String.join(" ", group, getName(sprite), event,
                    Integer.toString(index))));
    }

    private static boolean isModified(@NonNull Path src, @NonNull Path dest) {
        try {
            return !Files.exists(dest) || Files.getLastModifiedTime(dest).compareTo(Files.getLastModifiedTime(
                    src)) <= 0;
        } catch (IOException e) {
            return true;
        }
    }

    private static ModelObjectInfo @NonNull [] getModelObjectInfos(@NonNull Node n, @NonNull Path src_dir) {
        NodeList nl = n.getChildNodes();
        List<ModelObjectInfo> object_infos = new ArrayList<>();
        for (int i = 0; i < nl.getLength(); i++) {
            Node item = nl.item(i);
            if (item.getNodeName().equals("model")) {
                float r = getInt(item, "r") / 255f;
                float g = getInt(item, "g") / 255f;
                float b = getInt(item, "b") / 255f;
                String[][] textures = getTextureInfos(item, src_dir);
                object_infos.add(new ModelObjectInfo(src_dir.resolve(getText(item)), textures, new float[]{r, g, b}));
            }
        }
        ModelObjectInfo[] infos = new ModelObjectInfo[object_infos.size()];
        return object_infos.toArray(infos);
    }

    private static @NonNull AnimObjectInfo @NonNull [] getAnimObjectInfos(@NonNull Node n, @NonNull Path src_dir) {
        NodeList nl = n.getChildNodes();
        List<AnimObjectInfo> object_infos = new ArrayList<>();
        for (int i = 0; i < nl.getLength(); i++) {
            Node item = nl.item(i);
            if (item.getNodeName().equals("animation")) {
                float wpc = Float.parseFloat(item.getAttributes().getNamedItem("wpc").getNodeValue());
                assert wpc != 0f;
                String type_str = item.getAttributes().getNamedItem("type").getNodeValue();
                AnimationInfo.AnimationType type = getTypeFromString(type_str);
                String animName = item.getAttributes().getNamedItem("name").getNodeValue();
                object_infos.add(new AnimObjectInfo(src_dir.resolve(getText(item)), wpc, type, animName));
            }
        }
        AnimObjectInfo[] infos = new AnimObjectInfo[object_infos.size()];
        return object_infos.toArray(infos);
    }

    private static String @NonNull [] @NonNull [] getTextureInfos(@NonNull Node n, @NonNull Path src_dir) {
        NodeList nl = n.getChildNodes();
        List<String[]> object_infos = new ArrayList<>();
        for (int i = 0; i < nl.getLength(); i++) {
            Node item = nl.item(i);
            if (item.getNodeName().equals("texture")) {
                String name = item.getAttributes().getNamedItem("name").getNodeValue();
                Node team_node = item.getAttributes().getNamedItem("team");
                String team_name;
                if (team_node != null)
                    team_name = team_node.getNodeValue();
                else
                    team_name = null;
                object_infos.add(new String[]{name, team_name});
            }
        }
        String[][] infos = new String[object_infos.size()][];
        return object_infos.toArray(infos);
    }

    private static void parseSprite(@NonNull Node n, @NonNull Map<String, Node> group_sprites, @NonNull Path registry,
            @NonNull Path src_dir, @NonNull Path build_dir) {
        String name = getName(n);
        // base="peon" makes an attachment share the unit's skeleton and clip list without repeating them.
        Node base = n;
        for (int depth = 0; base.getAttributes().getNamedItem("base") != null; depth++) {
            String base_name = base.getAttributes().getNamedItem("base").getNodeValue();
            base = group_sprites.get(base_name);
            if (base == null || depth > group_sprites.size())
                throw new RuntimeException("Sprite " + name + " has an unknown or circular base " + base_name);
        }
        ObjectInfo skeleton_info = getSkeletonObjectInfo(n, src_dir);
        AnimObjectInfo[] anim_object_infos = getAnimObjectInfos(n, src_dir);
        // A sprite that brings nothing of its own refers to its base's clips instead of carrying a copy of them.
        boolean shares_clips = base != n && skeleton_info == null && anim_object_infos.length == 0;
        if (skeleton_info == null)
            skeleton_info = getSkeletonObjectInfo(base, src_dir);
        if (anim_object_infos.length == 0)
            anim_object_infos = getAnimObjectInfos(base, src_dir);
        ModelObjectInfo[] model_object_infos = getModelObjectInfos(n, src_dir);
        Path build_file = build_dir.resolve(name + ".binsprite");

        // Texture lists, scale and clip settings live in the registry, not in the mesh files.
        boolean modified = isModified(registry, build_file)
                || (skeleton_info != null && isModified(skeleton_info.getFile(), build_file));
        for (AnimObjectInfo anim_object_info : anim_object_infos) {
            if (isModified(anim_object_info.getFile(), build_file)) {
                modified = true;
                break;
            }
        }
        for (ModelObjectInfo model_object_info : model_object_infos) {
            if (isModified(model_object_info.getFile(), build_file)) {
                modified = true;
                break;
            }
        }
        if (modified) {
            float scale;
            Node scale_node = n.getAttributes().getNamedItem("scale");
            if (scale_node != null)
                scale = Float.parseFloat(scale_node.getNodeValue());
            else
                scale = 1f;
            AnimationInfo[] animations;
            Map<String, Bone> name_to_bone_map;
            if (skeleton_info != null) {
                Skeleton skeleton = SkeletonLoader.loadSkeleton(skeleton_info.getFile());
                name_to_bone_map = skeleton.getNameToBoneMap();
                animations = new AnimationInfo[anim_object_infos.length];
                for (int i = 0; i < anim_object_infos.length; i++) {
                    AnimObjectInfo current = anim_object_infos[i];
                    Map<String, float[]>[] animation_map = AnimationLoader.loadAnimation(current.getFile());
                    assert animations[i] == null;
                    animations[i] = Optimizer.convertToAnimation(skeleton.getBoneRoot(), skeleton.getInitialPose(),
                            animation_map, current.getType(), current.getWPC(), current.getName());
                }
            } else {
                float[][] identity_frame = {{1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0}};
                animations = new AnimationInfo[]{new AnimationInfo(identity_frame, AnimationInfo.AnimationType.LOOP, 1f,
                        "identity")};
                name_to_bone_map = null;
            }
            SpriteInfo[] sprite_models = new SpriteInfo[model_object_infos.length];
            for (int i = 0; i < model_object_infos.length; i++) {
                ModelObjectInfo current = model_object_infos[i];
                ModelInfo model_info = MeshLoader.loadMesh(current.getFile(), name_to_bone_map, scale);
                assert sprite_models[i] == null;
                sprite_models[i] = Optimizer.convertToSprite(current.getTextures(), model_info,
                        current.getClearColor());
            }
            Object clips = shares_clips ? build_dir.getFileName() + "/" + getName(base) : animations;
            write(new Object[]{sprite_models, clips}, build_file);
        }
    }

    private static @Nullable ObjectInfo getSkeletonObjectInfo(@NonNull Node n, @NonNull Path src_dir) {
        NodeList nl = n.getChildNodes();
        return IntStream.range(0, nl.getLength()).mapToObj(nl::item).filter(item -> item.getNodeName().equals(
                "skeleton")).findFirst().map(item -> new ObjectInfo(src_dir.resolve(getText(item)))).orElse(null);
    }

    public static Node getNodeByName(String name, @NonNull Node n) {
        NodeList nl = n.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            if (nl.item(i).getNodeName().equals(name))
                return nl.item(i);
        }
        throw new RuntimeException("Missing node: " + name);
    }

    private static String getName(@NonNull Node n) {
        return n.getAttributes().getNamedItem("name").getNodeValue();
    }

    private static int getInt(@NonNull Node n, String key) {
        String string = n.getAttributes().getNamedItem(key).getNodeValue();
        return Integer.parseInt(string);
    }

    private static AnimationInfo.@NonNull AnimationType getTypeFromString(@NonNull String str) {
        return switch (str) {
            case "loop" -> AnimationInfo.AnimationType.LOOP;
            case "plain" -> AnimationInfo.AnimationType.PLAIN;
            default -> throw new RuntimeException("Unknown animation type: " + str);
        };
    }

    private static @NonNull String getText(@NonNull Node n) {
        return n.getFirstChild().getNodeValue().trim();
    }

    private static void write(Object output, @NonNull Path file) {
        System.err.println("Saving to " + file);

        try {
            Files.createDirectories(file.getParent());
            try (var obj_stream = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(file)))) {
                obj_stream.writeObject(output);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
