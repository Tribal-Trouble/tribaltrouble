package com.oddlabs.tt.render;

import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.util.GLState;
import com.oddlabs.tt.util.GLStateStack;
import com.oddlabs.tt.util.Target;
import com.oddlabs.tt.vbo.FloatVBO;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.ARBBufferObject;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public final class SpriteRenderer {
	private final @NonNull SpriteList sprite_list;
	private final @NonNull SpriteListRenderer sprite_list_renderer;
	private final int tex_index;
	private final List<ModelState> no_detail_render_list = new ArrayList<>();

	private static FloatVBO quad_vbo;

	public SpriteRenderer(@NonNull SpriteList sprite_list, int tex_index) {
		this.sprite_list = sprite_list;
		this.tex_index = tex_index;
		sprite_list_renderer = new SpriteListRenderer(sprite_list);

		if (quad_vbo == null) {
			float[] quad_vertices = {
				-1.0f, -1.0f, 0.0f,
				 1.0f, -1.0f, 0.0f,
				 1.0f,  1.0f, 0.0f,
				-1.0f,  1.0f, 0.0f
			};
			quad_vbo = new FloatVBO(ARBBufferObject.GL_STATIC_DRAW_ARB, quad_vertices);
		}
	}

	void renderNoDetail(@NonNull ModelState model) {
		float[] color = model.getTeamColor();
		GL11.glColor3f(color[0], color[1], color[2]);

		GL11.glPushMatrix();
		float x = model.getModel().getPositionX();
		float y = model.getModel().getPositionY();
		float z = model.getModel().getPositionZ();
		float r = model.getModel().getNoDetailSize();

		GL11.glTranslatef(x, y, z);
		GL11.glScalef(r, r, 1.0f);

		quad_vbo.vertexPointer(3, 0, 0);
		GL11.glDrawArrays(GL11.GL_QUADS, 0, 4);
		GL11.glPopMatrix();
	}

	public @NonNull SpriteList getSpriteList() {
		return sprite_list;
	}

	public void setupWithColor(int index, @NonNull FloatBuffer material_color, boolean respond, boolean modulate_tex1) {
		getSpriteList().getSprite(index).setupWithColor(material_color, tex_index, respond, modulate_tex1, getSpriteList());
	}

	public void setup(int index, boolean respond) {
		getSpriteList().getSprite(index).setup(tex_index, respond, getSpriteList());
	}

	void addToNoDetailList(ModelState model) {
		no_detail_render_list.add(model);
	}

	void addToRenderList(@NonNull PolyDetail detail, ModelState model, boolean respond) {
        int index = detail.ordinal();
		index = Math.min(sprite_list.getNumSprites() - 1, index);
		if (respond) {
			sprite_list_renderer.addToRespondRenderList(model, index, tex_index);
		} else {
			sprite_list_renderer.addToRenderList(model, index, tex_index);
		}
	}

	int getTriangleCount(@NonNull PolyDetail detail) {
        int index = detail.ordinal();
		index = Math.min(sprite_list.getNumSprites() - 1, index);
		return sprite_list.getSprite(index).getTriangleCount();
	}


	private void clearRenderLists() {
		no_detail_render_list.clear();
	}

	public void getAllPicks(@NonNull List<Target> pick_list) {
		for (int i = 0; i < sprite_list.getNumSprites(); i++) {
            sprite_list_renderer.getAllPicks(pick_list, i, tex_index);
        }
		for (int i = 0; i < no_detail_render_list.size(); i++) {
			ModelState model = no_detail_render_list.get(i);
			no_detail_render_list.set(i, null);
			pick_list.add((Target)model.getModel());
		}
		clearRenderLists();
	}

	public void renderAll() {
		for (int i = 0; i < sprite_list.getNumSprites(); i++) {
			sprite_list_renderer.renderAll(i, tex_index);
		}
		setupNoDetail();
		for (int i = 0; i < no_detail_render_list.size(); i++) {
			ModelState model = no_detail_render_list.get(i);
			no_detail_render_list.set(i, null);
			if (Globals.draw_misc)
				renderNoDetail(model);
		}
		finishNoDetail();
		clearRenderLists();
	}
	
	private static void setupNoDetail() {
		GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GLStateStack.pushState();
		GLStateStack.switchState(GLState.VERTEX_ARRAY);
	}

	private static void finishNoDetail() {
		GLStateStack.popState();
		GL11.glPopAttrib();
	}
}
