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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;

public final class ConvertToBinary {
    private static final String ATTACHMENTS_FILE = "attachments.txt";
    private static final String SKINS_FILE = "skins.txt";
    private static final String DECORATIONS_FILE = "decorations.txt";
    private static final String LIGHTS_FILE = "lights.txt";
    private static final String LIGHT_COLOR_FORMAT = "#[0-9a-fA-F]{6}";
    private static final String LIGHT_NUMBER_FORMAT = "[0-9]+(\\.[0-9]+)?";
    private static final String LIGHT_EXAMPLE = "light_color=\"#ff8c26\" light_strength=\"1\" light_reach=\"8\"";
    private static final Set<String> DECORATION_GROUNDS = Set.of("beach", "dirt", "grass", "snow", "land");
    private static final int DEFAULT_DECORATION_COUNT = 20;
    private static final int MAX_DECORATION_COUNT = 1000;
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
            List<String> skins = new ArrayList<>();
            List<String> decorations = new ArrayList<>();
            List<String> lights = new ArrayList<>();
            for (int i = 0; i < nl.getLength(); i++) {
                if (nl.item(i).getNodeType() == Node.ELEMENT_NODE)
                    parseGroup(nl.item(i), registry, src_dir, build_dir, attachments, skins, decorations, lights);
            }
            Collections.sort(attachments);
            Collections.sort(skins);
            Collections.sort(decorations);
            Collections.sort(lights);
            try {
                Files.createDirectories(build_dir);
                Files.write(build_dir.resolve(ATTACHMENTS_FILE), attachments);
                Files.write(build_dir.resolve(SKINS_FILE), skins);
                Files.write(build_dir.resolve(DECORATIONS_FILE), decorations);
                Files.write(build_dir.resolve(LIGHTS_FILE), lights);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void parseGroup(@NonNull Node n, @NonNull Path registry, @NonNull Path src_dir,
            @NonNull Path build_dir,
            @NonNull List<String> attachments, @NonNull List<String> skins, @NonNull List<String> decorations,
            @NonNull List<String> lights) {
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
                Node skin = sprite.getAttributes().getNamedItem("skin");
                // An item skin only stands in for its item, so it is never offered as an item of its own.
                if (slot != null && skin == null)
                    attachments.add(attachmentLine(getName(n), sprite, slot.getNodeValue(), src_dir));
                if (skin != null)
                    skins.add(skinLine(getName(n), sprite, skin.getNodeValue(), sprites, src_dir));
                rejectEventTextures(sprite);
                Node decoration = sprite.getAttributes().getNamedItem("decoration");
                if (decoration != null)
                    decorations.add(decorationLine(getName(n), sprite, decoration.getNodeValue()));
                else if (sprite.getAttributes().getNamedItem("count") != null)
                    throw new RuntimeException("Sprite " + getName(sprite) + " has a count but no decoration");
                lights.addAll(lightLines(getName(n), sprite, src_dir));
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

    // group skin replaces name textures event: one line per sprite that stands in for another sprite of its group in a
    // skin.
    private static @NonNull String skinLine(@NonNull String group, @NonNull Node sprite, @NonNull String skin,
            @NonNull Map<String, Node> group_sprites, @NonNull Path src_dir) {
        Node replaces = sprite.getAttributes().getNamedItem("replaces");
        if (replaces == null || !group_sprites.containsKey(replaces.getNodeValue()))
            throw new RuntimeException("Sprite " + getName(
                    sprite) + " is in skin " + skin + " but replaces no sprite of group " + group);
        Node replaced = group_sprites.get(replaces.getNodeValue());
        if (!Objects.equals(attribute(sprite, "slot"), attribute(replaced, "slot")) || (attribute(sprite,
                "slot") != null && !Objects.equals(attribute(sprite, "base"), attribute(replaced, "base"))))
            throw new RuntimeException("Sprite " + getName(
                    sprite) + " needs the slot and base of " + replaces.getNodeValue() + ", the item it replaces");
        int textures = getModelObjectInfos(sprite, src_dir)[0].getTextures().length;
        Node event = sprite.getAttributes().getNamedItem("event");
        return String.join(" ", group, skin, replaces.getNodeValue(), getName(sprite), Integer.toString(textures),
                event != null ? event.getNodeValue() : NO_EVENT);
    }

    // group name texture color radius strength x y z: one line per spot where the glow of a texture of the sprite lights
    // its surroundings at night.
    private static @NonNull List<String> lightLines(@NonNull String group, @NonNull Node sprite,
            @NonNull Path src_dir) {
        List<String> lines = new ArrayList<>();
        NodeList models = ((org.w3c.dom.Element) sprite).getElementsByTagName("model");
        if (models.getLength() == 0)
            return lines;
        NodeList textures = ((org.w3c.dom.Element) models.item(0)).getElementsByTagName("texture");
        for (int i = 0; i < textures.getLength(); i++) {
            Node texture = textures.item(i);
            String emissive = attribute(texture, "emissive");
            String color = attribute(texture, "light_color");
            String strength = attribute(texture, "light_strength");
            String reach = attribute(texture, "light_reach");
            if (color == null && strength == null && reach == null)
                continue;
            if (emissive == null || color == null || !color.matches(LIGHT_COLOR_FORMAT) || strength == null
                    || !strength.matches(LIGHT_NUMBER_FORMAT) || reach == null || !reach.matches(LIGHT_NUMBER_FORMAT))
                throw new RuntimeException("Sprite " + getName(
                        sprite) + " needs a glow (emissive=) and all three of " + LIGHT_EXAMPLE + " to cast light");
            String scale = attribute(sprite, "scale");
            Path mesh = src_dir.resolve(models.item(0).getTextContent().trim());
            Path glow = src_dir.toAbsolutePath().resolveSibling("textures").resolve("models").resolve(
                    emissive + ".png");
            try {
                for (float[] spot : LightSpots.find(mesh, glow, scale != null ? Float.parseFloat(scale) : 1f)) {
                    lines.add(String.join(" ", group, getName(sprite), Integer.toString(i), color, reach, strength,
                            String.format(Locale.ROOT, "%.2f %.2f %.2f", spot[0], spot[1], spot[2])));
                }
            } catch (Exception e) {
                throw new RuntimeException("Sprite " + getName(sprite) + " has a light but its glow cannot be read", e);
            }
        }
        return lines;
    }

    private static @Nullable String attribute(@NonNull Node n, @NonNull String name) {
        Node attribute = n.getAttributes().getNamedItem(name);
        return attribute != null ? attribute.getNodeValue() : null;
    }

    // group name ground count event: one line per sprite the game scatters over the map as scenery.
    private static @NonNull String decorationLine(@NonNull String group, @NonNull Node sprite, @NonNull String ground) {
        if (!DECORATION_GROUNDS.containsAll(List.of(ground.split(",", -1))))
            throw new RuntimeException("Sprite " + getName(
                    sprite) + " has decoration=\"" + ground + "\"; use one or more of " + String.join(", ",
                            DECORATION_GROUNDS.stream().sorted().toList()) + ", comma separated");
        if (sprite.getAttributes().getNamedItem("slot") != null || sprite.getAttributes().getNamedItem("skin") != null)
            throw new RuntimeException("Sprite " + getName(
                    sprite) + " cannot be a decoration and an attachment or skin");
        Node count_node = sprite.getAttributes().getNamedItem("count");
        String count = count_node != null ? count_node.getNodeValue() : Integer.toString(DEFAULT_DECORATION_COUNT);
        if (!count.matches("[0-9]{1,4}") || Integer.parseInt(count) < 1 || Integer.parseInt(
                count) > MAX_DECORATION_COUNT)
            throw new RuntimeException("Sprite " + getName(
                    sprite) + " needs a count from 1 to " + MAX_DECORATION_COUNT + ", not " + count);
        Node event = sprite.getAttributes().getNamedItem("event");
        return String.join(" ", group, getName(sprite), ground, Integer.toString(Integer.parseInt(count)),
                event != null ? event.getNodeValue() : NO_EVENT);
    }

    private static void rejectEventTextures(@NonNull Node sprite) {
        NodeList models = sprite.getChildNodes();
        for (int i = 0; i < models.getLength(); i++) {
            NodeList textures = models.item(i).getChildNodes();
            for (int j = 0; j < textures.getLength(); j++) {
                if (textures.item(j).getNodeName().equals("texture") && textures.item(j).getAttributes().getNamedItem(
                        "event") != null)
                    throw new RuntimeException("Sprite " + getName(
                            sprite) + " has a texture with an event; make it" + " an event skin instead: a sprite with skin=, replaces= and event=");
            }
        }
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
                Node emissive_node = item.getAttributes().getNamedItem("emissive");
                if (emissive_node != null)
                    object_infos.add(new String[]{name, team_name, emissive_node.getNodeValue()});
                else
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
