package com.oddlabs.tt.gui;

import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.resource.Resources;
import com.oddlabs.tt.resource.TextureFile;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

/**
 * Displays a texture or a region of a texture.
 */
public final class GUIImage extends GUIIcon {
    private final boolean clickable;

    /**
     * Creates a new GUIImage from a texture file.
     *
     * @param width        The width of the image or negative value to use texture width and height.
     * @param height       The height of the image or negative value to use texture width and height.
     * @param u1           The u-coordinate of the top-left corner of the texture region.
     * @param v1           The v-coordinate of the top-left corner of the texture region.
     * @param u2           The u-coordinate of the bottom-right corner of the texture region.
     * @param v2           The v-coordinate of the bottom-right corner of the texture region.
     * @param texture_name The name of the texture file to load.
     */
    public GUIImage(int width, int height, float u1, float v1, float u2, float v2, String texture_name) {
        this(width, height, u1, v1, u2, v2, Resources.findResource(new TextureFile(texture_name, GL11.GL_RGBA, GL11.GL_LINEAR, GL11.GL_LINEAR, GL11.GL_REPEAT, GL11.GL_REPEAT)), false);
    }

    /**
     * Creates a clickable GUIImage. Focusable so it receives mouse pick events, shows target cursor on hover.
     */
    public GUIImage(int width, int height, float u1, float v1, float u2, float v2, String texture_name, boolean clickable) {
        this(width, height, u1, v1, u2, v2, Resources.findResource(new TextureFile(texture_name, GL11.GL_RGBA, GL11.GL_LINEAR, GL11.GL_LINEAR, GL11.GL_REPEAT, GL11.GL_REPEAT)), clickable);
    }

    /**
     * Creates a new GUIImage from an existing texture.
     *
     * @param width     The width of the image or negative value to use texture width and height.
     * @param height    The height of the image or negative value to use texture width and height.
     * @param u1        The u-coordinate of the top-left corner of the texture region.
     * @param v1        The v-coordinate of the top-left corner of the texture region.
     * @param u2        The u-coordinate of the bottom-right corner of the texture region.
     * @param v2        The v-coordinate of the bottom-right corner of the texture region.
     * @param texture   The texture to display.
     * @param clickable Whether this image should be clickable (focusable + target cursor).
     */
    private GUIImage(int width, int height, float u1, float v1, float u2, float v2, @NonNull Texture texture, boolean clickable) {
        boolean useTexture = width < 0 || height < 0;
        int useHeight = useTexture ? texture.getHeight() : height;
        int useWidth = useTexture ? texture.getWidth() : width;
        super(new IconQuad(u1, v1, u2, v2, useWidth, useHeight, texture));
        this.clickable = clickable;
        if (clickable) setCanFocus(true);
    }

    @Override
    protected @NonNull CursorType getCursorType() {
        return clickable ? CursorType.TARGET : CursorType.NORMAL;
    }
}
