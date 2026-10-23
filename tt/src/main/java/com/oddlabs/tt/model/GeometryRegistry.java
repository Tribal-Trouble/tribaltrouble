package com.oddlabs.tt.model;

import com.oddlabs.geometry.registry.AttachmentEntry;
import com.oddlabs.geometry.registry.DecorationEntry;
import com.oddlabs.geometry.registry.LightEntry;
import com.oddlabs.geometry.registry.Registry;
import com.oddlabs.geometry.registry.SkinEntry;
import com.oddlabs.tt.render.GlowLights;
import com.oddlabs.tt.resource.SpriteFile;
import com.oddlabs.util.Utils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The registry the geometry converter writes next to the binary sprites, with the entries of events that are off left
 * out. Everything here is render-only: which event is on comes from the local date, so players in one game may differ.
 */
public final class GeometryRegistry {
    public static final String AUTUMN = "autumn";
    public static final String HALLOWEEN = "halloween";
    private static final int LAST_AUTUMN_ONLY_DAY = 15;
    private static final String PROP_SLOT = "prop";
    private static final String GEOMETRY_DIR = "/geometry/";
    private static final Pattern SPRITE_PATH = Pattern.compile("/geometry/\\w+/\\w+\\.binsprite$");
    private static final int COLOR_RADIX = 16;
    private static final float COLOR_SCALE = 255f;

    private static final Set<String> EVENTS = activeEvents();
    private static Registry registry;
    private static List<AttachmentEntry> attachments;
    private static List<SkinEntry> skins;
    private static Map<String, String> event_skins;
    private static Map<String, List<GlowLights.Light>> lights;

    private GeometryRegistry() {
    }

    // Autumn through October, Halloween from the 16th; com.oddlabs.tt.event=a,b overrides the date.
    private static @NonNull Set<String> activeEvents() {
        String forced = System.getProperty("com.oddlabs.tt.event");
        if (forced != null)
            return Set.copyOf(List.of(forced.split(",")));
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        if (today.getMonth() != Month.OCTOBER)
            return Set.of();
        return today.getDayOfMonth() <= LAST_AUTUMN_ONLY_DAY ? Set.of(AUTUMN) : Set.of(AUTUMN, HALLOWEEN);
    }

    public static @NonNull Set<String> events() {
        return EVENTS;
    }

    public static boolean isEventActive(@NonNull String event) {
        return event.equals(Registry.NO_EVENT) || EVENTS.contains(event);
    }

    public static @NonNull List<AttachmentEntry> attachments() {
        if (attachments == null)
            attachments = registry().attachments().stream().filter(entry -> isEventActive(entry.event())).sorted(
                    AttachmentEntry.ORDER).toList();
        return attachments;
    }

    public static @NonNull List<SkinEntry> skins() {
        if (skins == null)
            skins = registry().skins().stream().filter(entry -> isEventActive(entry.event())).toList();
        return skins;
    }

    public static @NonNull List<DecorationEntry> decorations() {
        return registry().decorations().stream().filter(entry -> isEventActive(entry.event())).toList();
    }

    /** The registry path of every prop drawn with the sprite at location; there is nothing to toggle. */
    public static @NonNull List<String> getProps(@NonNull String location) {
        String base = registryPath(location);
        return attachments().stream().filter(entry -> entry.slot().equals(PROP_SLOT) && spritePath(entry.group(),
                entry.base()).equals(base)).map(entry -> spritePath(entry.group(), entry.name())).toList();
    }

    /** Where the glow of the sprite at location drawn with texture tex_index lights from, in the sprite's space. */
    public static @NonNull List<GlowLights.Light> getLights(@NonNull String location, int tex_index) {
        if (lights == null) {
            lights = new HashMap<>();
            for (LightEntry entry : registry().lights()) {
                int rgb = Integer.parseInt(entry.color().substring(1), COLOR_RADIX);
                float strength = entry.strength() / COLOR_SCALE;
                lights.computeIfAbsent(spritePath(entry.group(), entry.name()) + " " + entry.texture(),
                        key -> new ArrayList<>()).add(new GlowLights.Light(entry.x(), entry.y(), entry.z(),
                                entry.reach(), (rgb >> 16 & 0xff) * strength, (rgb >> 8 & 0xff) * strength,
                                (rgb & 0xff) * strength));
            }
        }
        return lights.getOrDefault(registryPath(location) + " " + tex_index, List.of());
    }

    /** What the active event draws for everyone in place of sprite_file; owned skins still draw over it. */
    public static @NonNull SpriteFile eventSkin(@NonNull SpriteFile sprite_file) {
        if (event_skins == null) {
            event_skins = new HashMap<>();
            skins().stream().filter(entry -> !entry.event().equals(Registry.NO_EVENT)).forEach(
                    entry -> event_skins.put(spritePath(entry.group(), entry.replaces()),
                            spritePath(entry.group(), entry.name())));
        }
        String location = event_skins.get(registryPath(sprite_file.getLocation()));
        return location != null ? sprite_file.withLocation(location) : sprite_file;
    }

    public static @NonNull String spritePath(@NonNull String group, @NonNull String name) {
        return GEOMETRY_DIR + group + "/" + name + ".binsprite";
    }

    // Resource locations are URLs, while the registry names a sprite by its path under /geometry.
    private static @Nullable String registryPath(@NonNull String location) {
        Matcher path = SPRITE_PATH.matcher(location);
        return path.find() ? path.group() : null;
    }

    private static @NonNull Registry registry() {
        if (registry == null) {
            try (InputStream in = Utils.makeURL(GEOMETRY_DIR + Registry.FILE).openStream()) {
                registry = Registry.read(in);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return registry;
    }
}
