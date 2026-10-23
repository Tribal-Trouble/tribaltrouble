package com.oddlabs.converter;

import com.oddlabs.converter.xml.Animation;
import com.oddlabs.converter.xml.GeometryFile;
import com.oddlabs.converter.xml.Group;
import com.oddlabs.converter.xml.Model;
import com.oddlabs.converter.xml.SkeletonRef;
import com.oddlabs.converter.xml.Sprite;
import com.oddlabs.converter.xml.Texture;
import com.oddlabs.geometry.AnimationInfo;
import com.oddlabs.geometry.SpriteInfo;
import com.oddlabs.geometry.registry.AttachmentEntry;
import com.oddlabs.geometry.registry.DecorationEntry;
import com.oddlabs.geometry.registry.LightEntry;
import com.oddlabs.geometry.registry.Registry;
import com.oddlabs.geometry.registry.SkinEntry;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ConvertToBinary {
    // A model with no skeleton still needs a clip: one frame of one bone whose 3x4 matrix leaves the mesh where it is.
    private static final float[][] STATIC_MODEL_FRAME = {{1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0}};
    private static final String STATIC_MODEL_CLIP = "identity";

    void main(@NonNull String @NonNull... args) {
        if (args.length != 3)
            throw new IllegalArgumentException("Invalid number of arguments : <xml_file> <src_dir> <build_dir>");
        Path xml_file = Path.of(args[0]);
        Path src_dir = Path.of(args[1]);
        Path build_dir = Path.of(args[2]);
        try {
            Path registry = src_dir.resolve(xml_file);
            convert(GeometryFile.read(registry), registry, src_dir, build_dir);
        } catch (Exception e) {
            System.err.println("Error processing " + xml_file);
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static void convert(@NonNull GeometryFile geometry, @NonNull Path registry, @NonNull Path src_dir,
            @NonNull Path build_dir) throws IOException {
        List<AttachmentEntry> attachments = new ArrayList<>();
        List<SkinEntry> skins = new ArrayList<>();
        List<DecorationEntry> decorations = new ArrayList<>();
        List<LightEntry> lights = new ArrayList<>();
        for (Group group : geometry.groups()) {
            Map<String, Sprite> sprites = new LinkedHashMap<>();
            for (Sprite sprite : group.sprites()) {
                if (sprites.putIfAbsent(sprite.name(), sprite) != null)
                    throw new IllegalArgumentException(
                            "Group " + group.name() + " lists sprite " + sprite.name() + " twice");
            }
            for (Sprite sprite : sprites.values()) {
                convertSprite(sprite, sprites, registry, src_dir, build_dir.resolve(group.name()));
                String base = sprite.base();
                String slot = sprite.slot();
                String skin = sprite.skin();
                String decoration = sprite.decoration();
                // An item skin only stands in for its item, so it is never offered as an item of its own.
                if (slot != null && base != null && skin == null)
                    attachments.add(new AttachmentEntry(group.name(), base, slot, sprite.defaultOn(), sprite.name(),
                            sprite.tiers(), sprite.event()));
                if (skin != null)
                    skins.add(skin(group.name(), sprite, skin, sprites));
                if (decoration != null)
                    decorations.add(new DecorationEntry(group.name(), sprite.name(), decoration, sprite.count(),
                            sprite.event()));
                lights.addAll(lights(group.name(), sprite, src_dir));
            }
        }
        attachments.sort(AttachmentEntry.ORDER);
        skins.sort(SkinEntry.ORDER);
        decorations.sort(DecorationEntry.ORDER);
        lights.sort(LightEntry.ORDER);
        Files.createDirectories(build_dir);
        new Registry(attachments, skins, decorations, lights).write(build_dir.resolve(Registry.FILE));
    }

    private static @NonNull SkinEntry skin(@NonNull String group, @NonNull Sprite sprite, @NonNull String skin,
            @NonNull Map<String, Sprite> group_sprites) {
        String replaces = sprite.replaces();
        if (replaces == null || !group_sprites.containsKey(replaces))
            throw new IllegalArgumentException(
                    "Sprite " + sprite.name() + " is in skin " + skin + " but replaces no sprite of group " + group);
        Sprite replaced = group_sprites.get(replaces);
        if (!Objects.equals(sprite.slot(), replaced.slot()) || (sprite.slot() != null && !Objects.equals(
                sprite.base(), replaced.base())))
            throw new IllegalArgumentException(
                    "Sprite " + sprite.name() + " needs the slot and base of " + replaces + ", the item it replaces");
        return new SkinEntry(group, skin, replaces, sprite.name(), sprite.tiers(), sprite.event());
    }

    // One entry per spot where the glow of a texture of the close-up model lights its surroundings at night.
    private static @NonNull List<LightEntry> lights(@NonNull String group, @NonNull Sprite sprite,
            @NonNull Path src_dir) {
        List<LightEntry> entries = new ArrayList<>();
        Model model = sprite.models().get(0);
        List<Texture> textures = model.textures();
        for (int i = 0; i < textures.size(); i++) {
            Texture texture = textures.get(i);
            if (!texture.castsLight())
                continue;
            Path glow = src_dir.toAbsolutePath().resolveSibling("textures").resolve("models").resolve(
                    Objects.requireNonNull(texture.emissive()) + ".png");
            try {
                for (float[] spot : LightSpots.find(src_dir.resolve(model.path()), glow, sprite.scale())) {
                    entries.add(new LightEntry(group, sprite.name(), i, Objects.requireNonNull(texture.lightColor()),
                            Objects.requireNonNull(texture.lightReach()),
                            Objects.requireNonNull(texture.lightStrength()), spot[0], spot[1], spot[2]));
                }
            } catch (Exception e) {
                throw new IllegalStateException("Sprite " + sprite.name() + " has a light but its glow cannot be read",
                        e);
            }
        }
        return entries;
    }

    private static boolean isModified(@NonNull Path src, @NonNull Path dest) {
        try {
            return !Files.exists(dest) || Files.getLastModifiedTime(dest).compareTo(Files.getLastModifiedTime(
                    src)) <= 0;
        } catch (IOException e) {
            return true;
        }
    }

    private static void convertSprite(@NonNull Sprite sprite, @NonNull Map<String, Sprite> group_sprites,
            @NonNull Path registry, @NonNull Path src_dir, @NonNull Path build_dir) {
        String name = sprite.name();
        // base="peon" makes an attachment share the unit's skeleton and clip list without repeating them.
        Sprite base = sprite;
        for (int depth = 0; base.base() != null; depth++) {
            String base_name = base.base();
            base = group_sprites.get(base_name);
            if (base == null || depth > group_sprites.size())
                throw new IllegalArgumentException("Sprite " + name + " has an unknown or circular base " + base_name);
        }
        SkeletonRef skeleton = sprite.skeleton();
        List<Animation> animations = sprite.animations();
        // A sprite that brings nothing of its own refers to its base's clips instead of carrying a copy of them.
        boolean shares_clips = !base.name().equals(name) && skeleton == null && animations.isEmpty();
        if (skeleton == null)
            skeleton = base.skeleton();
        if (animations.isEmpty())
            animations = base.animations();
        Path build_file = build_dir.resolve(name + ".binsprite");

        // Texture lists, scale and clip settings live in the registry, not in the mesh files.
        boolean modified = isModified(registry, build_file)
                || (skeleton != null && isModified(src_dir.resolve(skeleton.path()), build_file));
        for (Animation animation : animations)
            modified |= isModified(src_dir.resolve(animation.path()), build_file);
        for (Model model : sprite.models())
            modified |= isModified(src_dir.resolve(model.path()), build_file);
        if (!modified)
            return;

        AnimationInfo[] clips;
        @Nullable Map<String, Bone> name_to_bone_map;
        if (skeleton != null) {
            Skeleton bones = SkeletonLoader.loadSkeleton(src_dir.resolve(skeleton.path()));
            name_to_bone_map = bones.getNameToBoneMap();
            clips = new AnimationInfo[animations.size()];
            for (int i = 0; i < clips.length; i++) {
                Animation animation = animations.get(i);
                Map<String, float[]>[] frames = AnimationLoader.loadAnimation(src_dir.resolve(animation.path()));
                clips[i] = Optimizer.convertToAnimation(bones.getBoneRoot(), bones.getInitialPose(), frames,
                        animation.type(), animation.wpc(), animation.name());
            }
        } else {
            clips = new AnimationInfo[]{new AnimationInfo(STATIC_MODEL_FRAME, AnimationInfo.AnimationType.LOOP, 1f,
                    STATIC_MODEL_CLIP)};
            name_to_bone_map = null;
        }
        SpriteInfo[] sprite_models = new SpriteInfo[sprite.models().size()];
        for (int i = 0; i < sprite_models.length; i++) {
            Model model = sprite.models().get(i);
            ModelInfo model_info = MeshLoader.loadMesh(src_dir.resolve(model.path()), name_to_bone_map,
                    sprite.scale());
            String[][] textures = model.textures().stream().map(Texture::names).toArray(String[][]::new);
            sprite_models[i] = Optimizer.convertToSprite(textures, model_info, model.clearColor());
        }
        Object clip_source = shares_clips ? build_dir.getFileName() + "/" + base.name() : clips;
        write(new Object[]{sprite_models, clip_source}, build_file);
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
