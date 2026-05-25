package com.oddlabs.tt.render;

import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.model.Model;
import com.oddlabs.tt.procedural.GeneratorHalos;
import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.resource.Resources;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.function.Supplier;

final class SelectableShadowRenderer extends ShadowListRenderer {
    private final @NonNull Texture @NonNull [] halos;

    private final Deque<@NonNull ModelState<?>> selection_list = new ArrayDeque<>();
    private final Deque<@NonNull Model> shadowed_list = new ArrayDeque<>();

    public SelectableShadowRenderer(@NonNull Supplier<@NonNull Texture @NonNull []> halos_desc) {
        halos = Resources.findResource(halos_desc);
        for (Texture halo : halos) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, halo.getHandle());
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        }
    }

    public void addToSelectionList(@NonNull ModelState<?> modelState) {
        if (Globals.process_shadows) {
            selection_list.add(modelState);
        }
    }

    public void addToShadowList(@NonNull ModelState<?> modelState) {
        if (Globals.process_shadows) {
            var model = modelState.getModel();
            if (null != model) {
                shadowed_list.add(model);
            }
        }
    }

    @Override
    protected void renderShadows(@NonNull RenderContext context, @NonNull LandscapeRenderer renderer,
            @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
        try (var _ = setupShadows(context, renderer, modelViewStack, projectionStack)) {
            setShadowColor(1f, 1f, 1f, 1f);
            bindShadowTexture(halos[GeneratorHalos.SHADOWED]);
            while (!shadowed_list.isEmpty()) {
                var model = shadowed_list.pop();
                renderShadow(context, renderer, model.getShadowDiameter(), model.getPositionX(), model.getPositionY());
            }

            bindShadowTexture(halos[GeneratorHalos.SELECTED]);
            while (!selection_list.isEmpty()) {
                var modelState = selection_list.pop();
                var model = Objects.requireNonNull(modelState.getModel());
                Vector4fc color = modelState.getSelectionColor();
                setShadowColor(color.x(), color.y(), color.z(), 1f);
                renderShadow(context, renderer, model.getShadowDiameter(), model.getPositionX(), model.getPositionY());
            }
        }
    }
}
