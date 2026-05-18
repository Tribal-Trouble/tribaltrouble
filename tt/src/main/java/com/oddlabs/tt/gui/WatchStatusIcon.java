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
            IconQuad[] watch = GUIIcons.getIcons().getWatch();
            float progress = ((ReproduceUnitContainer) (building.getUnitContainer())).getBuildProgress();
            int index = (int) (progress * (watch.length - 1));
            int x = getWidth() - watch[0].getWidth();
            int y = (getHeight() - watch[0].getHeight()) / 2;
            x -= 5; // visual HAX
            IconQuad icon = watch[index];
            renderer.drawTexture(icon.getTexture(), x, y, icon.getWidth(), icon.getHeight(), icon.getU1(), icon.getV1(),
                    icon.getU2(), icon.getV2(), COLOR);
        }
    }
}
