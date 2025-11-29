package com.oddlabs.tt.gui;

import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.ReproduceUnitContainer;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

public final class WatchStatusIcon extends StatusIcon {
	private Building building;

	public WatchStatusIcon(int label_width, @NonNull IconQuad icon, @NonNull String tooltip) {
		super(label_width, icon, tooltip);
	}

	public void setUnitContainerBuilding(Building building) {
		this.building = building;
	}

	@Override
	protected void renderGeometry() {
		super.renderGeometry();
		if (!building.isDead() && !building.getChieftainContainer().isTraining() && building.getOwner().getUnitCountContainer().getNumSupplies() < building.getOwner().getWorld().getMaxUnitCount()) {
			IconQuad[] watch = GUIIcons.getIcons().getWatch();
			float progress = ((ReproduceUnitContainer)(building.getUnitContainer())).getBuildProgress();
			int index = (int)(progress*(watch.length - 1));
			int x = getWidth() - watch[0].getWidth();
			int y = (getHeight() - watch[0].getHeight())/2;
			x -= 5; // visual HAX
			float[] c = Color.argb4f(Color.argbi(1f, 1f, 1f, .75f));
			GL11.glColor4f(c[0], c[1], c[2], c[3]);
			IconQuad icon = watch[index];
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, icon.getTexture().getHandle());
			GL11.glBegin(GL11.GL_QUADS);
			icon.render(x, y);
			GL11.glEnd();
			GL11.glColor4f(1f, 1f, 1f, 1f);
		}
	}
}
