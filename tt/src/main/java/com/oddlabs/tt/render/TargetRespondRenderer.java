package com.oddlabs.tt.render;

import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.landscape.LandscapeTargetRespond;
import com.oddlabs.tt.resource.Resources;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class TargetRespondRenderer extends ShadowListRenderer {
	private final static float SHADOW_SIZE = 1.6f;
	private final Texture ring;

	private final List<LandscapeTargetRespond> target_list = new ArrayList<>();

	public TargetRespondRenderer(@NonNull Supplier<Texture[]> desc) {
		ring = Resources.findResource(desc)[0];
	}

	public void addToTargetList(LandscapeTargetRespond target) {
		if (Globals.process_shadows)
			target_list.add(target);
	}

	@Override
	public void renderShadows(@NonNull LandscapeRenderer renderer) {
		setupShadows();
		GL11.glColor3f(0f, 1f, 0f);
		for (int i = 0; i < target_list.size(); i++) {
			LandscapeTargetRespond target = target_list.get(i);
			target_list.set(i, null);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, ring.getHandle());
			renderShadow(renderer, SHADOW_SIZE*target.getProgress(), target.getPositionX(), target.getPositionY());
		}
		resetShadows();
		target_list.clear();
	}
}
