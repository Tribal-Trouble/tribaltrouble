package com.oddlabs.tt.render;

import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.procedural.GeneratorHalos;
import com.oddlabs.tt.resource.Resources;
import java.util.function.Supplier;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.opengl.GL11;

final class SelectableShadowRenderer extends ShadowListRenderer {
	private final Texture[] halos;

	private final List<ModelState> selection_list = new ArrayList<>();
	private final List<ModelState> shadowed_list = new ArrayList<>();

	public SelectableShadowRenderer(Supplier<Texture[]> halos_desc) {
		halos = Resources.findResource(halos_desc);
	}

	public void addToSelectionList(ModelState model) {
		if (Globals.process_shadows)
			selection_list.add(model);
	}

	public void addToShadowList(ModelState model) {
		if (Globals.process_shadows)
			shadowed_list.add(model);
	}

        @Override
	protected void renderShadows(LandscapeRenderer renderer) {
		setupShadows();
		GL11.glColor3f(1f, 1f, 1f);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, halos[GeneratorHalos.SHADOWED].getHandle());
		for (int i = 0; i < shadowed_list.size(); i++) {
			ModelState model = shadowed_list.get(i);
			shadowed_list.set(i, null);
			renderShadow(renderer, model.getModel().getShadowDiameter(), model.getModel().getPositionX(), model.getModel().getPositionY());
		}
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, halos[GeneratorHalos.SELECTED].getHandle());
		for (int i = 0; i < selection_list.size(); i++) {
			ModelState model = selection_list.get(i);
			selection_list.set(i, null);
			float[] color = model.getSelectionColor();
			GL11.glColor3f(color[0], color[1], color[2]);
			renderShadow(renderer, model.getModel().getShadowDiameter(), model.getModel().getPositionX(), model.getModel().getPositionY());
		}
		resetShadows();
		selection_list.clear();
		shadowed_list.clear();
	}
}
