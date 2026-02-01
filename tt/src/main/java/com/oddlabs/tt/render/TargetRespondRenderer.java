package com.oddlabs.tt.render;

import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.landscape.LandscapeTargetRespond;
import com.oddlabs.tt.render.state.RenderContext;
import com.oddlabs.tt.resource.Resources;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

public final class TargetRespondRenderer extends ShadowListRenderer {
    private static final float SHADOW_SIZE = 1.6f;
    private final Texture ring;

    private final Deque<@NonNull LandscapeTargetRespond> target_list = new ArrayDeque<>();

    public TargetRespondRenderer(@NonNull Supplier<@NonNull Texture @NonNull []> desc) {
        ring = Resources.findResource(desc)[0];
    }

    public void addToTargetList(@NonNull LandscapeTargetRespond target) {
        if (Globals.process_shadows)
            target_list.push(target);
    }

    @Override
    public void renderShadows(@NonNull RenderContext context, @NonNull LandscapeRenderer renderer, @NotNull MatrixStack modelViewStack, @NotNull MatrixStack projectionStack) {
        if (target_list.isEmpty()) return;
        
        try (var _ = setupShadows(context, renderer, modelViewStack, projectionStack)) {
            setShadowColor(0f, 1f, 0f, 1f);
            bindShadowTexture(ring);
            while (!target_list.isEmpty()) {
                var target = target_list.pop();
                renderShadow(context, renderer, SHADOW_SIZE*target.getProgress(), target.getPositionX(), target.getPositionY());
            }
        }
    }
}
