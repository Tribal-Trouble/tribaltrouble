package com.oddlabs.tt.mapeditor;

import com.oddlabs.tt.gui.GUIObject;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.util.Color;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.nio.ByteBuffer;

/** Shows a {@link MapPreview} scaled to a square, or a note in its place when there is none. */
final class MapPreviewView extends GUIObject {
    private static final Vector4fc BACKGROUND = new Vector4f(0f, 0f, 0f, .45f);

    private final @NonNull Label label_note;
    private @Nullable MapPreview preview;
    // Made from the preview while shown, and let go when taken off the screen.
    private @Nullable Texture texture;

    MapPreviewView(int size) {
        label_note = new Label("", Skin.getSkin().getEditFont(), size, Origin.AT_MIDDLE);
        label_note.setPos(0, (size - label_note.getHeight()) / 2);
        addChild(label_note);
        setDim(size, size);
    }

    /** Shows a preview, or the note when it is null. */
    void show(@Nullable MapPreview new_preview, @NonNull String note) {
        preview = new_preview;
        closeTexture();
        label_note.set(new_preview == null ? note : "");
        // Only on the screen; otherwise it is made once the view is added.
        if (getParentGUIRoot() != null)
            upload();
    }

    private void upload() {
        MapPreview shown = preview;
        if (shown == null || texture != null)
            return;
        int size = shown.size();
        ByteBuffer pixels = BufferUtils.createByteBuffer(size * size * 4);
        byte[] rgb = shown.rgb();
        for (int i = 0; i < size * size; i++)
            pixels.put(rgb[3 * i]).put(rgb[3 * i + 1]).put(rgb[3 * i + 2]).put((byte) 0xff);
        pixels.flip();
        RenderContext context = Renderer.getRenderer().getRenderContext();
        // Making the texture binds it behind the context's back; going through unit 0 keeps the context right.
        context.setTexture(0, 0);
        Texture made = new Texture(size, size, GL11.GL_RGBA8, GL11.GL_LINEAR, GL11.GL_LINEAR,
                GL12.GL_CLAMP_TO_EDGE);
        context.setTexture(0, made);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, size, size, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
        texture = made;
    }

    private void closeTexture() {
        if (texture != null) {
            texture.close();
            texture = null;
        }
    }

    @Override
    protected void doAdd() {
        super.doAdd();
        upload();
    }

    @Override
    protected void doRemove() {
        super.doRemove();
        closeTexture();
    }

    @Override
    protected void renderGeometry(@NonNull GUIRenderer renderer) {
        renderer.drawColoredQuad(0, 0, getWidth(), getHeight(), BACKGROUND);
        if (texture != null)
            renderer.drawTexture(texture, 0, 0, getWidth(), getHeight(), 0f, 0f, 1f, 1f, Color.WHITE);
    }
}
