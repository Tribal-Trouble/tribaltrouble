package com.oddlabs.tt.model;

import com.oddlabs.geometry.registry.AttachmentEntry;
import com.oddlabs.geometry.registry.Registry;
import com.oddlabs.geometry.registry.SkinEntry;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.player.PlayerSkins;
import com.oddlabs.tt.render.RenderQueues;
import com.oddlabs.tt.render.SpriteKey;
import com.oddlabs.tt.resource.SpriteFile;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * What the geometry registry adds to the race templates: the things peons carry, the items a unit can wear in its
 * slots, and the skins players may own. All of it is render-only.
 */
public final class RegistryItems {
    public static final int DEFAULT_TEXTURE = 0;
    private static final int IRON_TEXTURE = 1;
    private static final String CARRIED_SLOT = "carried";
    private static final String CARRIED_UNIT = "peon";

    private RegistryItems() {
    }

    /** The sprite a peon of this tribe shows for each kind of thing it can carry; the simulation decides which. */
    public static @NonNull Map<Class<? extends Supply>, SpriteKey> carried(@NonNull RenderQueues queues,
            @NonNull String group) {
        List<AttachmentEntry> entries = GeometryRegistry.attachments();
        AttachmentEntry rock = carriedEntry(entries, group, "rock_resource");
        return Map.of(
                TreeSupply.class, registerItem(queues, carriedEntry(entries, group, "wood_resource"), DEFAULT_TEXTURE),
                RockSupply.class, registerItem(queues, rock, DEFAULT_TEXTURE),
                IronSupply.class, registerItem(queues, rock, IRON_TEXTURE),
                RubberSupply.class, registerItem(queues, carriedEntry(entries, group, "rubber_resource"),
                        DEFAULT_TEXTURE),
                LeftPaddle.class, registerItem(queues, carriedEntry(entries, group, "left_paddle"), DEFAULT_TEXTURE),
                RightPaddle.class, registerItem(queues, carriedEntry(entries, group, "right_paddle"),
                        DEFAULT_TEXTURE));
    }

    // During an event, <name>_<event> stands in for <name> when the registry has one.
    private static @NonNull AttachmentEntry carriedEntry(@NonNull List<AttachmentEntry> entries, @NonNull String group,
            @NonNull String name) {
        AttachmentEntry found = null;
        for (AttachmentEntry entry : entries) {
            if (!entry.group().equals(group) || !entry.base().equals(CARRIED_UNIT) || !entry.slot().equals(
                    CARRIED_SLOT))
                continue;
            if (GeometryRegistry.events().stream().anyMatch(event -> entry.name().equals(name + "_" + event))
                    || (entry.name().equals(name) && found == null))
                found = entry;
        }
        if (found == null)
            throw new IllegalStateException("No carried sprite " + group + "/" + name + " in the geometry registry");
        return found;
    }

    /** The items a unit of this tribe can show, by slot and then by item name, drawn with the unit's texture tier. */
    public static @NonNull Map<String, Map<String, SpriteKey>> slots(@NonNull RenderQueues queues,
            @NonNull String group, @NonNull String unit, int tex_index) {
        Map<String, Map<String, SpriteKey>> slots = new LinkedHashMap<>();
        for (AttachmentEntry entry : GeometryRegistry.attachments()) {
            if (!entry.group().equals(group) || !entry.base().equals(unit) || entry.slot().equals(CARRIED_SLOT))
                continue;
            slots.computeIfAbsent(entry.slot(), _ -> new LinkedHashMap<>()).put(entry.name(),
                    registerItem(queues, entry, tex_index));
        }
        return slots;
    }

    /** The slots a new unit of this tribe starts with an item in. */
    public static @NonNull Set<String> defaultSlots(@NonNull String group, @NonNull String unit) {
        Set<String> slots = new HashSet<>();
        for (AttachmentEntry entry : GeometryRegistry.attachments()) {
            if (entry.defaultOn() && entry.group().equals(group) && entry.base().equals(unit))
                slots.add(entry.slot());
        }
        return slots;
    }

    private static @NonNull SpriteKey registerItem(@NonNull RenderQueues queues, @NonNull AttachmentEntry entry,
            int tex_index) {
        String location = GeometryRegistry.spritePath(entry.group(), entry.name());
        return queues.register(new SpriteFile(location, Globals.NO_MIPMAP_CUTOFF, true, true, true, false),
                textureIndex(entry.textures(), tex_index));
    }

    private static int textureIndex(int textures, int tex_index) {
        return tex_index < textures ? tex_index : DEFAULT_TEXTURE;
    }

    /**
     * Every owned skin by name: for each, the sprite that stands in for a unit, a building stage or an item, plus the
     * props the skin brings to a building stage.
     */
    public static @NonNull Map<String, PlayerSkins> skins(@NonNull RenderQueues queues, @NonNull Race @NonNull [] races,
            @NonNull Set<SpriteKey> carried) {
        Map<String, Map<UnitTemplate, SpriteKey>> units = new HashMap<>();
        Map<String, Map<BuildingTemplate, Map<Building.BuildState, SpriteKey>>> buildings = new HashMap<>();
        Map<String, Map<BuildingTemplate, Map<Building.BuildState, List<SpriteKey>>>> props = new HashMap<>();
        Map<String, Map<SpriteKey, SpriteKey>> items = new HashMap<>();
        Set<SpriteKey> item_keys = new HashSet<>(carried);
        for (Race race : races) {
            for (int i = 0; i < Race.NUM_UNITS; i++)
                race.getUnitTemplate(i).getAttachments().values().forEach(slot -> item_keys.addAll(slot.values()));
        }
        for (SkinEntry entry : GeometryRegistry.skins()) {
            if (!entry.event().equals(Registry.NO_EVENT))
                continue;
            for (SpriteKey item : item_keys) {
                SpriteKey skin = reskin(queues, item, entry);
                if (skin != null)
                    items.computeIfAbsent(entry.skin(), _ -> new HashMap<>()).put(item, skin);
            }
            for (Race race : races) {
                for (int i = 0; i < Race.NUM_UNITS; i++) {
                    UnitTemplate unit = race.getUnitTemplate(i);
                    SpriteKey skin = reskin(queues, unit.getSpriteRenderer(), entry);
                    if (skin != null)
                        units.computeIfAbsent(entry.skin(), _ -> new HashMap<>()).put(unit, skin);
                }
                for (int i = 0; i < Race.NUM_BUILDINGS; i++) {
                    BuildingTemplate building = race.getBuildingTemplate(i);
                    for (Building.BuildState stage : Building.BuildState.values()) {
                        SpriteKey skin = reskin(queues, building.getRenderer(stage), entry);
                        if (skin == null)
                            continue;
                        buildings.computeIfAbsent(entry.skin(), _ -> new HashMap<>()).computeIfAbsent(building,
                                _ -> new EnumMap<>(Building.BuildState.class)).put(stage, skin);
                        props.computeIfAbsent(entry.skin(), _ -> new HashMap<>()).computeIfAbsent(building,
                                _ -> new EnumMap<>(Building.BuildState.class)).put(stage, Stream.concat(
                                        building.getProps(stage).stream(), queues.getProps(skin).stream()).toList());
                    }
                }
            }
        }
        Set<String> names = new HashSet<>(units.keySet());
        names.addAll(buildings.keySet());
        names.addAll(items.keySet());
        Map<String, PlayerSkins> skins = new HashMap<>();
        for (String name : names)
            skins.put(name, new PlayerSkins(units.getOrDefault(name, Map.of()), buildings.getOrDefault(name, Map.of()),
                    props.getOrDefault(name, Map.of()), items.getOrDefault(name, Map.of())));
        return skins;
    }

    // Drawn with the replaced sprite's flags and texture, or the first texture when the skin has fewer.
    private static @Nullable SpriteKey reskin(@NonNull RenderQueues queues, @NonNull SpriteKey stock,
            @NonNull SkinEntry entry) {
        SpriteFile stock_file = queues.getSpriteFile(stock);
        String replaced = GeometryRegistry.spritePath(entry.group(), entry.replaces());
        if (!stock_file.equals(stock_file.withLocation(replaced)))
            return null;
        String location = GeometryRegistry.spritePath(entry.group(), entry.name());
        SpriteKey skin = queues.register(stock_file.withLocation(location),
                textureIndex(entry.textures(), queues.getTexIndex(stock)));
        if (queues.getRenderer(skin).getSpriteList().getAnimationTypes().length != queues.getRenderer(
                stock).getSpriteList().getAnimationTypes().length)
            throw new IllegalStateException(
                    "Skin sprite " + location + " has other clips than " + entry.replaces() + "; give it base=\"" + entry.replaces() + "\"");
        return skin;
    }
}
