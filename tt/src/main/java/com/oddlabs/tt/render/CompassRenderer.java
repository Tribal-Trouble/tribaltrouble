package com.oddlabs.tt.render;

import com.oddlabs.tt.font.Font;
import com.oddlabs.tt.font.TextLineRenderer;
import com.oddlabs.util.Quad;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;

/**
 * Renders a compass indicator in the bottom-right corner of the screen
 * showing cardinal directions relative to the camera's horizontal angle.
 * <p>
 * Uses the GUIRenderer's matrix stack for rotation so that tick marks,
 * the north needle, and labels are properly oriented along radial directions.
 */
public final class CompassRenderer {

    private static final float RADIUS = 30f;
    private static final float MARGIN = 50f;
    private static final float BG_RADIUS = 32f;
    private static final int BG_SEGMENTS = 32;
    private static final float OUTLINE_RADIUS = 30f;
    private static final float OUTLINE_THICKNESS = 1.5f;
    private static final int OUTLINE_SEGMENTS = 30;
    private static final float TICK_LENGTH = 6f;
    private static final float NORTH_TICK_WIDTH = 2.5f;
    private static final float TICK_WIDTH = 1.5f;
    private static final float NEEDLE_LENGTH = 17f;
    private static final float NEEDLE_WIDTH = 4f;
    private static final float NEEDLE_START = 3f;
    private static final float LABEL_OFFSET = 14f;
    private static final float CENTER_DOT_SIZE = 3f;

    private static final Vector4fc BG_COLOR = new Vector4f(0f, 0f, 0f, 0.4f);
    private static final Vector4fc OUTLINE_COLOR = new Vector4f(0.7f, 0.7f, 0.7f, 0.6f);
    private static final Vector4fc NORTH_COLOR = new Vector4f(0.9f, 0.2f, 0.2f, 0.85f);
    private static final Vector4fc TICK_COLOR = new Vector4f(0.7f, 0.7f, 0.7f, 0.6f);
    private static final Vector4fc CENTER_COLOR = new Vector4f(0.5f, 0.5f, 0.5f, 0.5f);
    private static final Vector4fc LABEL_COLOR = new Vector4f(0.9f, 0.9f, 0.9f, 1f);
    private static final Vector4fc NORTH_LABEL_COLOR = new Vector4f(1f, 0.3f, 0.3f, 1f);

    // Cardinal direction angles in world space (North = +Y = PI/2)
    private static final float NORTH_ANGLE = (float) (Math.PI / 2);
    private static final float EAST_ANGLE = 0f;
    private static final float SOUTH_ANGLE = (float) (-Math.PI / 2);
    private static final float WEST_ANGLE = (float) Math.PI;

    private CompassRenderer() {
    }

    public static void render(@NonNull GUIRenderer renderer, @NonNull Font font,
                              float horizAngle, int screenWidth, int screenHeight) {
        float cx = screenWidth - RADIUS - MARGIN;
        float cy = RADIUS + MARGIN;

        // Background circle (approximated with wedge quads)
        drawCircle(renderer, cx, cy, BG_RADIUS, BG_SEGMENTS, BG_COLOR);

        // Circle outline (ring of thin quads)
        drawRing(renderer, cx, cy, OUTLINE_RADIUS, OUTLINE_THICKNESS, OUTLINE_SEGMENTS, OUTLINE_COLOR);

        // Center dot
        renderer.drawColoredQuad(cx - CENTER_DOT_SIZE / 2, cy - CENTER_DOT_SIZE / 2,
                CENTER_DOT_SIZE, CENTER_DOT_SIZE, CENTER_COLOR);

        // Cardinal tick marks — use matrix rotation for proper orientation
        drawTick(renderer, cx, cy, horizAngle, NORTH_ANGLE, NORTH_TICK_WIDTH, NORTH_COLOR);
        drawTick(renderer, cx, cy, horizAngle, EAST_ANGLE, TICK_WIDTH, TICK_COLOR);
        drawTick(renderer, cx, cy, horizAngle, SOUTH_ANGLE, TICK_WIDTH, TICK_COLOR);
        drawTick(renderer, cx, cy, horizAngle, WEST_ANGLE, TICK_WIDTH, TICK_COLOR);

        // North needle
        drawNeedle(renderer, cx, cy, horizAngle);

        // Cardinal labels
        drawLabel(renderer, font, cx, cy, horizAngle, NORTH_ANGLE, "N", NORTH_LABEL_COLOR);
        drawLabel(renderer, font, cx, cy, horizAngle, EAST_ANGLE, "E", LABEL_COLOR);
        drawLabel(renderer, font, cx, cy, horizAngle, SOUTH_ANGLE, "S", LABEL_COLOR);
        drawLabel(renderer, font, cx, cy, horizAngle, WEST_ANGLE, "W", LABEL_COLOR);
    }

    /**
     * Approximate a filled circle using wedge-shaped quads radiating from center.
     */
    private static void drawCircle(@NonNull GUIRenderer renderer, float cx, float cy,
                                   float radius, int segments, @NonNull Vector4fc color) {
        MatrixStack stack = renderer.getMatrixStack();
        float angleStep = 360f / segments;
        for (int i = 0; i < segments; i++) {
            stack.push();
            stack.translate(cx, cy, 0);
            stack.rotate(i * angleStep, 0, 0, 1);
            // Draw a tall thin quad that covers one wedge segment
            float halfWidth = radius * (float) Math.sin(Math.toRadians(angleStep / 2));
            renderer.drawColoredQuad(-halfWidth, 0, halfWidth * 2, radius, color);
            stack.pop();
        }
    }

    /**
     * Draw a circle outline as a ring of small rotated quads.
     */
    private static void drawRing(@NonNull GUIRenderer renderer, float cx, float cy,
                                 float radius, float thickness, int segments, @NonNull Vector4fc color) {
        MatrixStack stack = renderer.getMatrixStack();
        float angleStep = 360f / segments;
        float segmentLength = 2f * radius * (float) Math.sin(Math.toRadians(angleStep / 2));
        for (int i = 0; i < segments; i++) {
            stack.push();
            stack.translate(cx, cy, 0);
            stack.rotate(i * angleStep, 0, 0, 1);
            // Place a small quad at the rim, oriented tangentially
            renderer.drawColoredQuad(-segmentLength / 2, radius - thickness / 2,
                    segmentLength, thickness, color);
            stack.pop();
        }
    }

    /**
     * Draw a tick mark at the rim, properly rotated to point radially inward.
     */
    private static void drawTick(@NonNull GUIRenderer renderer, float cx, float cy,
                                 float horizAngle, float cardinalAngle, float width,
                                 @NonNull Vector4fc color) {
        float delta = cardinalAngle - horizAngle;
        float angleDeg = (float) Math.toDegrees(delta);

        MatrixStack stack = renderer.getMatrixStack();
        stack.push();
        stack.translate(cx, cy, 0);
        stack.rotate(angleDeg, 0, 0, 1);
        // Draw tick along the +Y axis (radially outward from center)
        float innerEdge = RADIUS - TICK_LENGTH;
        renderer.drawColoredQuad(-width / 2, innerEdge, width, TICK_LENGTH, color);
        stack.pop();
    }

    /**
     * Draw the north needle from near center outward toward north.
     */
    private static void drawNeedle(@NonNull GUIRenderer renderer, float cx, float cy,
                                   float horizAngle) {
        float delta = NORTH_ANGLE - horizAngle;
        float angleDeg = (float) Math.toDegrees(delta);

        MatrixStack stack = renderer.getMatrixStack();
        stack.push();
        stack.translate(cx, cy, 0);
        stack.rotate(angleDeg, 0, 0, 1);
        // Needle along +Y from NEEDLE_START to NEEDLE_START + NEEDLE_LENGTH
        renderer.drawColoredQuad(-NEEDLE_WIDTH / 2, NEEDLE_START, NEEDLE_WIDTH, NEEDLE_LENGTH, NORTH_COLOR);
        stack.pop();
    }

    /**
     * Draw a cardinal label at the correct position outside the compass circle.
     */
    private static void drawLabel(@NonNull GUIRenderer renderer, @NonNull Font font,
                                  float cx, float cy, float horizAngle,
                                  float cardinalAngle, @NonNull String label,
                                  @NonNull Vector4fc color) {
        float delta = cardinalAngle - horizAngle;
        float dx = (float) Math.sin(delta);
        float dy = (float) Math.cos(delta);

        float labelDist = RADIUS + LABEL_OFFSET;
        float labelX = cx + dx * labelDist;
        float labelY = cy + dy * labelDist;

        Quad glyph = font.getQuad(label.charAt(0));
        if (glyph != null) {
            float charWidth = glyph.getWidth();
            float charHeight = glyph.getHeight();
            TextLineRenderer.render(renderer, font, label,
                    labelX - charWidth / 2, labelY - charHeight / 2,
                    Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, color);
        }
    }
}
