package com.oddlabs.tt.gui;

import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.resource.GLImage;
import com.oddlabs.tt.resource.GLIntImage;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.w3c.dom.Node;

import java.util.Map;
import java.util.ResourceBundle;

public class GUIIcons {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(Icons.class.getName());

    private @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull ... args) {
        return Utils.getBundleString(bundle, key, args);
    }

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
        String tt_caption = i18n("terrifying_toot", "S");
        String rr_caption = i18n("ravaging_roar", "C");
        String ss_caption = i18n("stinking_stew", "S");
        String cc_caption = i18n("crackling_cloud", "C");
        viking_icons = GUIIcons.parseRaceIcons(root, "vikings", tt_caption, rr_caption, texture);
        native_icons = GUIIcons.parseRaceIcons(root, "natives", ss_caption, cc_caption, texture);
        watch = generateWatchIcons();
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

    private static @NonNull IconQuad @NonNull [] generateWatchIcons() {
        int numIcons = 25;
        int iconSize = 64;
        int textureSize = 512;

        GLIntImage image = new GLIntImage(textureSize, textureSize, GL11.GL_RGBA);
        image.clearAll(0);

        int radius = 24;
        int rimWidth = 2;
        int outerRadius = radius + rimWidth;
        int shadowOffset = 2;

        for (int i = 0; i < numIcons; i++) {
            float progress = i / (float) (numIcons - 1);

            int r, g, b;
            if (progress < 0.5f) {
                r = 255;
                g = (int) (255 * (progress * 2));
                b = 0;
            } else {
                r = (int) (255 * (1.0f - (progress - 0.5f) * 2));
                g = 255;
                b = 0;
            }
            // GLIntImage expects 0xAABBGGRR for GL_RGBA
            int fillColor = (255 << 24) | (b << 16) | (g << 8) | r;
            int whiteColor = 0xFFFFFFFF;
            int rimColor = 0xFFC0C0C0;

            int col = i % 8;
            int row = i / 8;
            int startX = col * iconSize;
            int startY = row * iconSize;

            for (int y = 0; y < iconSize; y++) {
                for (int x = 0; x < iconSize; x++) {
                    int px = startX + x;
                    // GL coordinate (0 is bottom). We want to write to top.
                    // startY is from top. y is from top of icon.
                    int py = textureSize - 1 - (startY + y);

                    float dx = x - iconSize / 2.0f + 0.5f;
                    float dy = iconSize / 2.0f - y - 0.5f;
                    float dist = (float) Math.sqrt(dx * dx + dy * dy);

                    float shadowDx = dx - shadowOffset;
                    float shadowDy = dy + shadowOffset; // Lower Right Shadow
                    float shadowDist = (float) Math.sqrt(shadowDx * shadowDx + shadowDy * shadowDy);

                    int finalColor = 0;

                    // Shadow
                    if (shadowDist < outerRadius + 2) {
                        float alpha = 0.5f;
                        if (shadowDist > outerRadius) {
                            alpha *= (1.0f - (shadowDist - outerRadius) / 2.0f);
                        }
                        finalColor = ((int) (alpha * 255) << 24);
                    }

                    // Main Shape
                    if (dist < outerRadius + 1) {
                        float alpha = dist > outerRadius ? 1.0f - (dist - outerRadius) : 1.0f;

                        int pixelColor;
                        if (dist > radius) {
                            pixelColor = rimColor;
                        } else {
                            double angle = Math.atan2(dy, dx);
                            // Top (PI/2) is 0. Clockwise.
                            double normalizedAngle = Math.PI / 2 - angle;
                            if (normalizedAngle < 0) normalizedAngle += 2 * Math.PI;
                            float angleFraction = (float) (normalizedAngle / (2 * Math.PI));

                            pixelColor = angleFraction <= progress ? fillColor : whiteColor;
                        }

                        // Blend pixelColor over finalColor (shadow)
                        int destA = (finalColor >>> 24);
                        int srcA = (int) ((pixelColor >>> 24) * alpha);

                        float srcAf = srcA / 255.0f;
                        float destAf = destA / 255.0f;
                        float outAf = srcAf + destAf * (1.0f - srcAf);

                        if (outAf > 0) {
                            int srcR = (pixelColor >>> 16) & 0xFF;
                            int srcG = (pixelColor >>> 8) & 0xFF;
                            int srcB = pixelColor & 0xFF;

                            int outR = (int) ((srcR * srcAf) / outAf);
                            int outG = (int) ((srcG * srcAf) / outAf);
                            int outB = (int) ((srcB * srcAf) / outAf);
                            int outA = (int) (outAf * 255);

                            finalColor = (outA << 24) | (outR << 16) | (outG << 8) | outB;
                        }
                    }
                    image.putPixel(px, py, finalColor);
                }
            }
        }

        Texture texture = new Texture(new GLImage[]{image}, GL11.GL_RGBA, GL11.GL_LINEAR, GL11.GL_LINEAR, GL12.GL_CLAMP_TO_EDGE, GL12.GL_CLAMP_TO_EDGE);

        IconQuad[] icons = new IconQuad[numIcons];
        for (int i = 0; i < numIcons; i++) {
            int col = i % 8;
            int row = i / 8;
            int startX = col * iconSize;
            int startY = row * iconSize;

            float u1 = startX / (float) textureSize;
            float v1 = 1f - (startY + iconSize) / (float) textureSize;
            float u2 = (startX + iconSize) / (float) textureSize;
            float v2 = 1f - startY / (float) textureSize;

            icons[i] = new IconQuad(u1, v1, u2, v2, 22, 22, texture);
        }
        return icons;
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
