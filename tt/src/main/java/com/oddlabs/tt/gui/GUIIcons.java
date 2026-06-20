package com.oddlabs.tt.gui;

import com.oddlabs.tt.model.SupplyType;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.resource.GLImage;
import com.oddlabs.tt.resource.GLIntImage;
import com.oddlabs.tt.util.Utils;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.w3c.dom.Node;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * Fast lookups for common UI icons used throughout the game's interface.
 */
public class GUIIcons {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(Icons.class.getName());

    private @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private static final Color.Standard WATCH_MIDPOINT_COLOR = new Color.Standard(0xFFDCE202);
    private static final int WATCH_RIM_COLOR_INT = new Color.Standard(0.75f, 1.0f).toInt();
    private static final int WATCH_NUM_ICONS = 25;
    private static final int WATCH_ICON_SIZE = 64;
    private static final int WATCH_RIM_WIDTH = 2;
    private static final int WATCH_SHADOW_OFFSET = 2;

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

    private final @NonNull Map<@NonNull SupplyType, @NonNull List<@NonNull IconQuad>> tool_tip_icons;

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

        Supplier<String> tt_caption = () -> i18n("terrifying_toot", Renderer.getLocalInput().getInputManager()
                .getBindingString(GameAction.MAGIC_1));
        Supplier<String> rr_caption = () -> i18n("ravaging_roar", Renderer.getLocalInput().getInputManager()
                .getBindingString(GameAction.MAGIC_2));
        Supplier<String> ss_caption = () -> i18n("stinking_stew", Renderer.getLocalInput().getInputManager()
                .getBindingString(GameAction.MAGIC_1));
        Supplier<String> cc_caption = () -> i18n("crackling_cloud", Renderer.getLocalInput().getInputManager()
                .getBindingString(GameAction.MAGIC_2));

        viking_icons = GUIIcons.parseRaceIcons(root, "vikings", tt_caption, rr_caption, texture);
        native_icons = GUIIcons.parseRaceIcons(root, "natives", ss_caption, cc_caption, texture);
        watch = generateWatchIcons();
        infinite = Icons.getNamedIconQuad(root, "infinite", texture);
        notify_arrow_data = GUIIcons.parseNotifyArrowData(root, texture);
        tool_tip_icons = new EnumMap<>(Map.of(
                SupplyType.WOOD, List.of(tree_status_icon),
                SupplyType.ROCK, List.of(rock_status_icon),
                SupplyType.IRON, List.of(iron_status_icon),
                SupplyType.RUBBER, List.of(rubber_status_icon)));
    }

    private static @NonNull RaceIcons parseRaceIcons(@NonNull Node n, @NonNull String head,
            @NonNull Supplier<@NonNull String> magic1_desc, @NonNull Supplier<@NonNull String> magic2_desc,
            @NonNull Texture texture) {
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

    private static @NonNull IconQuad @NonNull [] generateWatchIcons() {
        int textureSize = Utils.roundToTextureSize((int) Math.ceil(Math.sqrt(WATCH_NUM_ICONS)) * WATCH_ICON_SIZE);
        int perRow = textureSize / WATCH_ICON_SIZE;

        assert perRow * perRow >= WATCH_NUM_ICONS : "texture size too small for " + WATCH_NUM_ICONS + " icons";

        GLIntImage image = new GLIntImage(textureSize, textureSize, GL11.GL_RGBA);
        image.clearAll(Color.TRANSPARENT_INT);

        int radius = WATCH_ICON_SIZE * 3 / 8;
        int outerRadius = radius + WATCH_RIM_WIDTH;

        for (int i = 0; i < WATCH_NUM_ICONS; i++) {
            float progress = i / (float) (WATCH_NUM_ICONS - 1);

            int r;
            int g;
            int b;
            if (progress < 0.5f) {
                float t = progress * 2.0f;
                r = Math.clamp(Math.round(255.0f * (1.0f - t) + WATCH_MIDPOINT_COLOR.r() * 255.0f * t), 0, 255);
                g = Math.clamp(Math.round(WATCH_MIDPOINT_COLOR.g() * 255.0f * t), 0, 255);
                b = Math.clamp(Math.round(WATCH_MIDPOINT_COLOR.b() * 255.0f * t), 0, 255);
            } else {
                float t = (progress - 0.5f) * 2.0f;
                r = Math.clamp(Math.round(WATCH_MIDPOINT_COLOR.r() * 255.0f * (1.0f - t)), 0, 255);
                g = Math.clamp(Math.round(WATCH_MIDPOINT_COLOR.g() * 255.0f * (1.0f - t) + 255.0f * t), 0, 255);
                b = Math.clamp(Math.round(WATCH_MIDPOINT_COLOR.b() * 255.0f * (1.0f - t)), 0, 255);
            }
            // GLIntImage expects 0xAABBGGRR for GL_RGBA
            int fillColor = (255 << 24) | (b << 16) | (g << 8) | r;

            int col = i % perRow;
            int row = i / perRow;
            int startX = col * WATCH_ICON_SIZE;
            int startY = row * WATCH_ICON_SIZE;

            for (int y = 0; y < WATCH_ICON_SIZE; y++) {
                for (int x = 0; x < WATCH_ICON_SIZE; x++) {
                    int px = startX + x;
                    // GL coordinate (0 is bottom). We want to write to top.
                    // startY is from top. y is from top of icon.
                    int py = textureSize - 1 - (startY + y);

                    float dx = x - WATCH_ICON_SIZE / 2.0f + 0.5f;
                    float dy = WATCH_ICON_SIZE / 2.0f - y - 0.5f;
                    float dist = (float) Math.hypot(dx, dy);

                    float shadowDx = dx - WATCH_SHADOW_OFFSET;
                    float shadowDy = dy + WATCH_SHADOW_OFFSET; // Lower Right Shadow
                    float shadowDist = (float) Math.hypot(shadowDx, shadowDy);

                    int finalColor = 0;

                    // Shadow
                    if (shadowDist < outerRadius + 2) {
                        float alpha = 0.5f;
                        if (shadowDist > outerRadius) {
                            alpha *= (1.0f - (shadowDist - outerRadius) / 2.0f);
                        }
                        finalColor = (Math.clamp(Math.round(alpha * 255), 0, 255) << 24);
                    }

                    // Main Shape
                    if (dist < outerRadius + 1) {
                        float alpha = dist > outerRadius ? 1.0f - (dist - outerRadius) : 1.0f;

                        int pixelColor;
                        if (dist > radius) {
                            pixelColor = WATCH_RIM_COLOR_INT;
                        } else {
                            double angle = Math.atan2(dy, dx);
                            // Top (PI/2) is 0. Clockwise.
                            double normalizedAngle = Math.PI / 2 - angle;
                            if (normalizedAngle < 0) normalizedAngle += 2 * Math.PI;
                            float angleFraction = (float) (normalizedAngle / (2 * Math.PI));

                            pixelColor = angleFraction <= progress ? fillColor : Color.WHITE_INT;
                        }

                        // Blend pixelColor over finalColor (shadow)
                        int destA = (finalColor >>> 24);
                        int srcA = Math.clamp(Math.round((pixelColor >>> 24) * alpha), 0, 255);

                        float srcAf = srcA / 255.0f;
                        float destAf = destA / 255.0f;
                        float outAf = srcAf + destAf * (1.0f - srcAf);

                        if (outAf > 0) {
                            int srcR = (pixelColor >>> 16) & 0xFF;
                            int srcG = (pixelColor >>> 8) & 0xFF;
                            int srcB = pixelColor & 0xFF;

                            int outR = Math.clamp(Math.round((srcR * srcAf) / outAf), 0, 255);
                            int outG = Math.clamp(Math.round((srcG * srcAf) / outAf), 0, 255);
                            int outB = Math.clamp(Math.round((srcB * srcAf) / outAf), 0, 255);
                            int outA = Math.clamp(Math.round(outAf * 255), 0, 255);

                            finalColor = (outA << 24) | (outR << 16) | (outG << 8) | outB;
                        }
                    }
                    image.putPixel(px, py, finalColor);
                }
            }
        }

        Texture texture = new Texture(new GLImage[]{image}, GL11.GL_RGBA, GL11.GL_LINEAR, GL11.GL_LINEAR,
                GL12.GL_CLAMP_TO_EDGE, GL12.GL_CLAMP_TO_EDGE);

        return IntStream.range(0, WATCH_NUM_ICONS).mapToObj(i -> {
            int col = i % perRow;
            int row = i / perRow;
            int startX = col * WATCH_ICON_SIZE;
            int startY = row * WATCH_ICON_SIZE;

            float u1 = startX / (float) textureSize;
            float v1 = 1f - (startY + WATCH_ICON_SIZE) / (float) textureSize;
            float u2 = (startX + WATCH_ICON_SIZE) / (float) textureSize;
            float v2 = 1f - startY / (float) textureSize;

            return new IconQuad(u1, v1, u2, v2, 22, 22, texture);
        }).toArray(IconQuad[]::new);
    }

    private static @NonNull NotifyArrowData parseNotifyArrowData(@NonNull Node n, @NonNull Texture texture) {
        Node node = Icons.getNodeByName("notify_arrow", n);
        return new NotifyArrowData(Icons.getIconQuad(node, texture),
                Icons.getInt(node, "head_x"),
                Icons.getInt(node, "head_y"),
                Icons.getInt(node, "end_x"),
                Icons.getInt(node, "end_y"));
    }

    public @NonNull List<@NonNull IconQuad> getToolTipIcon(@NonNull SupplyType key) {
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

    public final @NonNull IconQuad getWatch(float progress) {
        return watch[(int) (progress * (watch.length - 1))];
    }

    public final @NonNull IconQuad getInfinite() {
        return infinite;
    }

    public final @NonNull NotifyArrowData getNotifyArrowData() {
        return notify_arrow_data;
    }
}
