package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.model.Element;
import com.oddlabs.tt.model.ElementLeaf;
import com.oddlabs.tt.model.ElementNode;
import com.oddlabs.tt.model.ElementNodeVisitor;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.viewer.Selection;
import org.jspecify.annotations.NonNull;

final class ElementRenderer implements ElementNodeVisitor<Element<?>> {

    private final @NonNull RenderState render_state;
    private final boolean picking;
    private CameraState camera;

    private boolean visible_override;

    ElementRenderer(Player local_player, LandscapeRenderer renderer, @NonNull RenderQueues render_queues, Picker picker, boolean picking, SpriteSorter sprite_sorter, Selection selection) {
        this.picking = picking;
        this.render_state = new RenderState(local_player, renderer, sprite_sorter, render_queues, picker, selection);
    }

    @NonNull RenderState getRenderState() {
        return render_state;
    }

    void setup(CameraState camera_state) {
        this.camera = camera_state;
        render_state.setup(picking, camera);
    }

    @Override
    public void visitNode(@NonNull ElementNode<Element<?>> node) {
        RenderTools.FrustumIntersection frustum_state;
        if (camera.inNoDetailMode()) {
            frustum_state = RenderTools.FrustumIntersection.ALL_INSIDE; // Force all in frustum for map mode
        } else {
            frustum_state = RenderTools.inFrustum(node, camera.getFrustum());
        }

        if (visible_override || frustum_state != RenderTools.FrustumIntersection.ALL_OUTSIDE) {
            boolean old_override = visible_override;
            visible_override = visible_override || frustum_state == RenderTools.FrustumIntersection.ALL_INSIDE;
            node.visitChildren(this);
            node.visitElements(this);
            visible_override = old_override;
        }
    }

    @Override
    public void visitLeaf(@NonNull ElementLeaf<Element<?>> leaf) {
        RenderTools.FrustumIntersection frustum_state;
        if (camera.inNoDetailMode()) {
            frustum_state = RenderTools.FrustumIntersection.ALL_INSIDE; // Force all in frustum for map mode
        } else {
            frustum_state = RenderTools.inFrustum(leaf, camera.getFrustum());
        }

        if (visible_override || frustum_state != RenderTools.FrustumIntersection.ALL_OUTSIDE) {
            boolean old_override = visible_override;
            visible_override = visible_override || frustum_state == RenderTools.FrustumIntersection.ALL_INSIDE;
            leaf.visitElements(this);
            visible_override = old_override;
        }
    }

    @Override
    public void visit(@NonNull Element<Element<?>> element) {
        RenderTools.FrustumIntersection frustum_state;
        if (camera.inNoDetailMode()) {
            frustum_state = RenderTools.FrustumIntersection.ALL_INSIDE; // Force all in frustum for map mode
        } else {
            frustum_state = RenderTools.inFrustum(element, camera.getFrustum());
        }

        if (visible_override || frustum_state != RenderTools.FrustumIntersection.ALL_OUTSIDE) {
            boolean old_override = visible_override;
            visible_override = visible_override || frustum_state == RenderTools.FrustumIntersection.ALL_INSIDE;
            render_state.setVisibleOverride(visible_override);
            element.visit(render_state);
            visible_override = old_override;
        }
    }
}
