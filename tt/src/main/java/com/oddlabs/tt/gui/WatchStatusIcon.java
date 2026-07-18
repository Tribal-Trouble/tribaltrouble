package com.oddlabs.tt.gui;

import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.ReproduceUnitContainer;
import com.oddlabs.tt.render.GUIRenderer;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;

public final class WatchStatusIcon extends StatusIcon {
    private static final Vector4fc COLOR = new Vector4f(1f, 1f, 1f, .75f);
    private Building building;

    public WatchStatusIcon(int label_width, @NonNull IconQuad icon, @NonNull String tooltip) {
        super(label_width, icon, tooltip);
    }

    public void setUnitContainerBuilding(Building building) {
        this.building = building;
    }

    @Override
    protected void renderGeometry(@NonNull GUIRenderer renderer) {
        super.renderGeometry(renderer);
        if (!building.isDead() && !building.getChieftainContainer().isTraining()
                && building.getOwner().getUnitCountContainer().getNumSupplies() < building.getOwner().getWorld().getMaxUnitCount()) {
            float progress = ((ReproduceUnitContainer) (building.getUnitContainer())).getBuildProgress();
            var watch = GUIIcons.getIcons().getWatch(progress);
            int x = getWidth() - watch.getWidth();
            int y = (getHeight() - watch.getHeight()) / 2;
            x -= 5; // visual HAX
            renderer.drawTexture(watch.getTexture(), x, y, watch.getWidth(), watch.getHeight(),
                    watch.getU1(), watch.getV1(), watch.getU2(), watch.getV2(), COLOR);
        }
    }
}
