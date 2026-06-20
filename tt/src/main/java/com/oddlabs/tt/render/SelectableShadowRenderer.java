package com.oddlabs.tt.render;

import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Shadowable;
import com.oddlabs.tt.procedural.GeneratorHalos;
import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.resource.Resources;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.EnumMap;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Specialized renderer that handles drawing halos for selected units and regular shadows for other entities.
 */
final class SelectableShadowRenderer extends ShadowListRenderer {
    private final @NonNull EnumMap<GeneratorHalos.HaloType, Texture> halos = new EnumMap<>(
            GeneratorHalos.HaloType.class);

    private final Deque<@NonNull ModelState<?>> selection_list = new ArrayDeque<>();
    private final Deque<@NonNull Shadowable> shadowed_list = new ArrayDeque<>();

    SelectableShadowRenderer(@NonNull Supplier<@NonNull Texture @NonNull []> halos_desc) {
        Texture[] textures = Resources.findResource(halos_desc);
        for (int i = 0; i < textures.length; i++) {
            Texture halo = textures[i];
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, halo.getHandle());
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            halos.put(GeneratorHalos.HaloType.values()[i], halo);
        }
        setRadial(true);
    }

    void addToSelectionList(@NonNull ModelState<?> modelState) {
        if (Globals.process_shadows) {
            selection_list.add(modelState);
        }
    }

    void addToShadowList(@NonNull ModelState<?> modelState) {
        if (Globals.process_shadows) {
            var model = modelState.getModel();
            if (null != model) {
                shadowed_list.add(model);
            }
        }
    }

    void addToShadowList(@NonNull Collection<? extends @NonNull Shadowable> shadowable) {
        if (Globals.process_shadows) {
            shadowed_list.addAll(shadowable);
        }
    }

    @Override
    protected void renderShadows(@NonNull RenderContext context, @NonNull RenderQueues queues,
            @NonNull LandscapeRenderer renderer, @NonNull MatrixStack modelViewStack,
            @NonNull MatrixStack projectionStack) {
        try (var _ = setupShadows(context, queues, renderer, modelViewStack, projectionStack)) {
            setShadowColor(Color.Linear.WHITE);
            setPattern(Selectable.VisualPattern.NONE);
            bindShadowTexture(halos.get(GeneratorHalos.HaloType.SHADOWED));
            while (!shadowed_list.isEmpty()) {
                var model = shadowed_list.pop();
                renderShadow(context, renderer, model);
            }

            bindShadowTexture(halos.get(GeneratorHalos.HaloType.SELECTED));
            while (!selection_list.isEmpty()) {
                var modelState = selection_list.pop();
                var model = Objects.requireNonNull(modelState.getModel());
                setShadowColor(modelState.getSelectionColor());
                setPattern(modelState.getPattern());

                renderShadow(context, renderer, model);
            }
        }
    }
}
