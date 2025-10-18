package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.model.Element;
import com.oddlabs.tt.model.ElementLeaf;
import com.oddlabs.tt.model.ElementNode;
import com.oddlabs.tt.model.ElementNodeVisitor;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.viewer.Selection;

final class ElementRenderer implements ElementNodeVisitor {

    private final RenderState render_state;
    private final boolean picking;
    private CameraState camera;

    private boolean visible_override;

    ElementRenderer(Player local_player, LandscapeRenderer renderer, RenderQueues render_queues, Picker picker, boolean picking, SpriteSorter sprite_sorter, Selection selection) {
        this.picking = picking;
        this.render_state = new RenderState(local_player, renderer, sprite_sorter, render_queues, picker, selection);
    }

    RenderState getRenderState() {
        return render_state;
    }

    void setup(CameraState camera_state) {
        this.camera = camera_state;
        render_state.setup(picking, camera);
    }

    @Override
    public void visitNode(ElementNode node) {
        int frustum_state = RenderTools.NOT_IN_FRUSTUM;
        if (visible_override || (frustum_state = RenderTools.inFrustum(node, camera.getFrustum())) >= RenderTools.IN_FRUSTUM) {
            boolean old_override = visible_override;
            visible_override = visible_override || frustum_state == RenderTools.ALL_IN_FRUSTUM;
            node.visitChildren(this);
            node.visitElements(this);
            visible_override = old_override;
        }
    }

    @Override
    public void visitLeaf(ElementLeaf leaf) {
        int frustum_state = RenderTools.NOT_IN_FRUSTUM;
        if (visible_override || (frustum_state = RenderTools.inFrustum(leaf, camera.getFrustum())) >= RenderTools.IN_FRUSTUM) {
            boolean old_override = visible_override;
            visible_override = visible_override || frustum_state == RenderTools.ALL_IN_FRUSTUM;
            leaf.visitElements(this);
            visible_override = old_override;
        }
    }

    @Override
    public void visit(Element<?> element) {
        int frustum_state = RenderTools.NOT_IN_FRUSTUM;
        if (visible_override || (frustum_state = RenderTools.inFrustum(element, camera.getFrustum())) >= RenderTools.IN_FRUSTUM) {
            boolean old_override = visible_override;
            visible_override = visible_override || frustum_state == RenderTools.ALL_IN_FRUSTUM;
            render_state.setVisibleOverride(visible_override);
            element.visit(render_state);
            visible_override = old_override;
        }
    }
}
