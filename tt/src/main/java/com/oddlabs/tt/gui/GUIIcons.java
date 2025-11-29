package com.oddlabs.tt.gui;

import com.oddlabs.tt.render.Texture;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.IntStream;

public class GUIIcons {
    private static final GUIIcons ICONS = new GUIIcons("/gui/icons.xml");

    private final @NonNull ModeIconQuads harvest_icon;
    private final @NonNull ModeIconQuads tree_icon;
    private final @NonNull ModeIconQuads rock_icon;
    private final @NonNull ModeIconQuads iron_icon;
    private final @NonNull ModeIconQuads rubber_icon;
    private final @NonNull IconQuad tree_status_icon;
    private final @NonNull IconQuad rock_status_icon;
    private final @NonNull IconQuad iron_status_icon;
    private final @NonNull IconQuad rubber_status_icon;
    private final @NonNull IconQuad cheat_icon;
    private final @NonNull RaceIcons native_icons;
    private final @NonNull RaceIcons viking_icons;
    private final @NonNull IconQuad @NonNull [] watch;
    private final @NonNull IconQuad infinite;
    private final @NonNull NotifyArrowData notify_arrow_data;

    private final @NonNull Map<@NonNull Class<?>, @NonNull IconQuad @NonNull []> tool_tip_icons;

    public static GUIIcons getIcons() {
        return ICONS;
    }

    private GUIIcons(@NonNull String xml_file) {
        Node root = Icons.loadFile(xml_file, new GUIErrorHandler());
        Texture texture = Icons.loadTexture(root);

        harvest_icon = Icons.getNamedIconQuads(root, "harvest_icon", texture);
        tree_icon = Icons.getNamedIconQuads(root, "tree_icon", texture);
        rock_icon = Icons.getNamedIconQuads(root, "rock_icon", texture);
        iron_icon = Icons.getNamedIconQuads(root, "iron_icon", texture);
        rubber_icon = Icons.getNamedIconQuads(root, "rubber_icon", texture);
        tree_status_icon = Icons.getNamedIconQuad(root, "tree_status_icon", texture);
        rock_status_icon = Icons.getNamedIconQuad(root, "rock_status_icon", texture);
        iron_status_icon = Icons.getNamedIconQuad(root, "iron_status_icon", texture);
        rubber_status_icon = Icons.getNamedIconQuad(root, "rubber_status_icon", texture);
        cheat_icon = Icons.getNamedIconQuad(root, "cheat_icon", texture);
        ResourceBundle bundle = ResourceBundle.getBundle(Icons.class.getName());
        String tt_caption = com.oddlabs.tt.util.Utils.getBundleString(bundle, "terrifying_toot", "S");
        String rr_caption = com.oddlabs.tt.util.Utils.getBundleString(bundle, "ravaging_roar", "C");
        String ss_caption = com.oddlabs.tt.util.Utils.getBundleString(bundle, "stinking_stew", "S");
        String cc_caption = com.oddlabs.tt.util.Utils.getBundleString(bundle, "crackling_cloud", "C");
        viking_icons = GUIIcons.parseRaceIcons(root, "vikings", tt_caption, rr_caption, texture);
        native_icons = GUIIcons.parseRaceIcons(root, "natives", ss_caption, cc_caption, texture);
        watch = GUIIcons.parseWatch(root, texture);
        infinite = Icons.getNamedIconQuad(root, "infinite", texture);
        notify_arrow_data = GUIIcons.parseNotifyArrowData(root, texture);
        tool_tip_icons = Map.of(
                com.oddlabs.tt.landscape.TreeSupply.class, new IconQuad[]{tree_status_icon},
                com.oddlabs.tt.model.RockSupply.class, new IconQuad[]{rock_status_icon},
                com.oddlabs.tt.model.IronSupply.class, new IconQuad[]{iron_status_icon},
                com.oddlabs.tt.model.RubberSupply.class, new IconQuad[]{rubber_status_icon});
    }

    private static @NonNull RaceIcons parseRaceIcons(@NonNull Node n, @NonNull String head, @NonNull String magic1_desc, @NonNull String magic2_desc, @NonNull Texture texture) {
        return new RaceIcons(Icons.getNamedIconQuad(n, head + "_unit_status_icon", texture),
                Icons.getNamedIconQuad(n, head + "_weapon_rock_status_icon", texture),
                Icons.getNamedIconQuad(n, head + "_weapon_iron_status_icon", texture),
                Icons.getNamedIconQuad(n, head + "_weapon_rubber_status_icon", texture),
                Icons.getNamedIconQuads(n, head + "_build_weapons_icon", texture),
                Icons.getNamedIconQuads(n, head + "_build_weapon_rock_icon", texture),
                Icons.getNamedIconQuads(n, head + "_build_weapon_iron_icon", texture),
                Icons.getNamedIconQuads(n, head + "_build_weapon_rubber_icon", texture),
                Icons.getNamedIconQuads(n, head + "_army_icon", texture),
                Icons.getNamedIconQuads(n, head + "_warrior_rock_icon", texture),
                Icons.getNamedIconQuads(n, head + "_warrior_iron_icon", texture),
                Icons.getNamedIconQuads(n, head + "_warrior_rubber_icon", texture),
                Icons.getNamedIconQuads(n, head + "_peon_icon", texture),
                Icons.getNamedIconQuads(n, head + "_chieftain_icon", texture),
                Icons.getNamedIconQuads(n, head + "_transport_icon", texture),
                Icons.getNamedIconQuads(n, head + "_attack_icon", texture),
                Icons.getNamedIconQuads(n, head + "_move_icon", texture),
                Icons.getNamedIconQuads(n, head + "_gather_repair_icon", texture),
                Icons.getNamedIconQuads(n, head + "_quarters_icon", texture),
                Icons.getNamedIconQuads(n, head + "_armory_icon", texture),
                Icons.getNamedIconQuads(n, head + "_tower_icon", texture),
                Icons.getNamedIconQuads(n, head + "_tower_exit_icon", texture),
                Icons.getNamedIconQuads(n, head + "_rally_point_icon", texture),
                Icons.getNamedIconQuads(n, head + "_magic1_icon", texture),
                magic1_desc,
                Icons.getNamedIconQuads(n, head + "_magic2_icon", texture),
                magic2_desc);
    }

    private static @NonNull IconQuad @NonNull [] parseWatch(@NonNull Node n, @NonNull Texture texture) {
        Node node = Icons.getNodeByName("watch", n);
        NodeList nl = node.getChildNodes();
        IconQuad[] result = IntStream.range(0, nl.getLength())
                .mapToObj(nl::item)
                .filter(item -> "quad".equals(item.getNodeName()))
                .map(item -> Icons.parseIconQuad(item, texture))
                .toArray(IconQuad[]::new);
        return result;
    }

    private static @NonNull NotifyArrowData parseNotifyArrowData(@NonNull Node n, @NonNull Texture texture) {
        Node node = Icons.getNodeByName("notify_arrow", n);
        return new NotifyArrowData(Icons.getIconQuad(node, texture),
                Icons.getInt(node, "head_x"),
                Icons.getInt(node, "head_y"),
                Icons.getInt(node, "end_x"),
                Icons.getInt(node, "end_y"));
    }

    public @NonNull IconQuad @Nullable [] getToolTipIcon(@NonNull Class<?> key) {
        return tool_tip_icons.get(key);
    }

    public final @NonNull RaceIcons getVikingIcons() {
        return viking_icons;
    }

    public final @NonNull RaceIcons getNativeIcons() {
        return native_icons;
    }

    public final @NonNull ModeIconQuads getHarvestIcon() {
        return harvest_icon;
    }

    public final @NonNull IconQuad getTreeStatusIcon() {
        return tree_status_icon;
    }

    public final @NonNull IconQuad getRockStatusIcon() {
        return rock_status_icon;
    }

    public final @NonNull IconQuad getIronStatusIcon() {
        return iron_status_icon;
    }

    public final @NonNull IconQuad getRubberStatusIcon() {
        return rubber_status_icon;
    }

    public final @NonNull IconQuad getCheatIcon() {
        return cheat_icon;
    }

    public final @NonNull ModeIconQuads getTreeIcon() {
        return tree_icon;
    }

    public final @NonNull ModeIconQuads getRockIcon() {
        return rock_icon;
    }

    public final @NonNull ModeIconQuads getIronIcon() {
        return iron_icon;
    }

    public final @NonNull ModeIconQuads getRubberIcon() {
        return rubber_icon;
    }

    public final @NonNull IconQuad @NonNull [] getWatch() {
        return watch;
    }

    public final @NonNull IconQuad getInfinite() {
        return infinite;
    }

    public final @NonNull NotifyArrowData getNotifyArrowData() {
        return notify_arrow_data;
    }
}
