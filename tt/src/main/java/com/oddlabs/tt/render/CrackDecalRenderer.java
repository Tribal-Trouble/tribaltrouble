package com.oddlabs.tt.render;

import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Shadowable;
import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.resource.Resources;
import org.jspecify.annotations.NonNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

/**
 * Renders ground-crack decals for supply impact events using the decal instancing pipeline.
 * Crack texture and per-instance colour are driven by the owning {@link com.oddlabs.tt.model.SupplyModel}.
 */
public final class CrackDecalRenderer extends ShadowListRenderer {
    private final @NonNull Texture crackTexture;
    private final Deque<@NonNull Shadowable> crack_list = new ArrayDeque<>();

    public CrackDecalRenderer(@NonNull Supplier<@NonNull Texture @NonNull []> desc) {
        crackTexture = Resources.findResource(desc)[0];
        setRadial(false);
    }

    public void addToCrackList(@NonNull Shadowable shadowable) {
        if (Globals.process_shadows) {
            crack_list.add(shadowable);
        }
    }

    @Override
    protected void renderShadows(@NonNull RenderContext context, @NonNull RenderQueues queues,
            @NonNull LandscapeRenderer renderer, @NonNull MatrixStack modelViewStack,
            @NonNull MatrixStack projectionStack) {
        if (crack_list.isEmpty()) {
            return;
        }

        try (var _ = setupShadows(context, queues, renderer, modelViewStack, projectionStack)) {
            setPattern(Selectable.VisualPattern.NONE);
            bindShadowTexture(crackTexture);
            while (!crack_list.isEmpty()) {
                var model = crack_list.pop();
                setShadowColor(model.getShadowColor());
                setPatternVal(model.getShadowPattern());
                renderShadow(context, renderer, model);
            }
        }
    }
}
