package com.oddlabs.texturegenerator;

import com.oddlabs.geometry.LowDetailModel;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.render.BillboardPainter;
import com.oddlabs.tt.render.SpriteList;
import com.oddlabs.tt.resource.Resources;
import com.oddlabs.tt.resource.SpriteFile;
import com.oddlabs.tt.util.FBORenderer;
import com.oddlabs.tt.util.GLStateStack;
import com.oddlabs.tt.util.OffscreenRenderer;
import com.oddlabs.util.Utils;
import org.jspecify.annotations.NonNull;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.PixelFormat;

import java.io.File;

public final class TextureGenerator {
	private final static int LOW_DETAIL_TEX_SIZE = 256;
	private final static int LOWDETAIL_MIPMAP_CUTOFF = Globals.NO_MIPMAP_CUTOFF;
	private final static int CROWN_MIPMAP_CUTOFF = Globals.NO_MIPMAP_CUTOFF;

	public static void main(String @NonNull [] args) throws LWJGLException {
		assert args.length == 1;
		new TextureGenerator(args[0]);
	}

	public TextureGenerator(@NonNull String dest) throws LWJGLException {
		Settings.setSettings(new Settings());
		File path = new File(dest);
		path.mkdirs();

		GLStateStack display_state_stack = new GLStateStack();
		GLStateStack.setCurrent(display_state_stack);

		Display.setDisplayMode(new DisplayMode(LOW_DETAIL_TEX_SIZE, LOW_DETAIL_TEX_SIZE));
		Display.create(new PixelFormat(Globals.VIEW_BIT_DEPTH, 1, 16, 0, 0));

		// Generate Native/Jungle tree textures
		generateTextureSet(
				new String[]{"/geometry/misc/jungle_tree_crown.binsprite", "/geometry/misc/palm_crown.binsprite"},
				new String[]{"/geometry/misc/jungle_tree_trunk.binsprite", "/geometry/misc/palm_trunk.binsprite"},
				new String[]{"/geometry/misc/tree_low.binlowdetail", "/geometry/misc/palm_low.binlowdetail"},
				dest + "/lowdetail_tree"
		);

		// Generate Viking tree textures
		generateTextureSet(
				new String[]{"/geometry/misc/oak_tree_crown.binsprite", "/geometry/misc/pine_tree_crown.binsprite"},
				new String[]{"/geometry/misc/oak_tree_trunk.binsprite", "/geometry/misc/pine_tree_trunk.binsprite"},
				new String[]{"/geometry/misc/oak_tree_low.binlowdetail", "/geometry/misc/pine_tree_low.binlowdetail"},
				dest + "/viking_lowdetail_tree"
		);

		Display.destroy();
	}

	private void generateTextureSet(String[] crownFiles, String[] trunkFiles, String[] lowDetailFiles, String dest) {
		OffscreenRenderer buffer = new FBORenderer(LOW_DETAIL_TEX_SIZE, LOW_DETAIL_TEX_SIZE);

		SpriteList[] crowns = new SpriteList[crownFiles.length];
		SpriteList[] trunks = new SpriteList[trunkFiles.length];
		LowDetailModel[] models = new LowDetailModel[lowDetailFiles.length];

		for (int i = 0; i < crownFiles.length; i++) {
			crowns[i] = Resources.findResource(new SpriteFile(crownFiles[i], CROWN_MIPMAP_CUTOFF, false, false, true, false));
			trunks[i] = Resources.findResource(new SpriteFile(trunkFiles[i], CROWN_MIPMAP_CUTOFF, true, true, false, false));
			models[i] = Utils.loadObject(Utils.makeURL(lowDetailFiles[i]));
		}

		int[] indices = new int[models.length];
		for (int i = 0; i < models.length; i++) {
			indices[i] = i;
		}
        drawBillboardsToBuffer(models, this, indices, buffer, crowns, trunks, 0);
        buffer.dumpToFile(dest);
		buffer.destroy();
	}

	private static void generateBillboardMip(@NonNull LowDetailModel lowdetail, @NonNull TextureGenerator renderer, int mode, float ortho_size, SpriteList[] crowns, SpriteList[] trunks, int tex_index) {
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();
		GL11.glOrtho(0.0f, ortho_size, 0.0f, ortho_size, -50.0f, 50.0f);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		BillboardPainter.init();
		for (int i = 0; i < lowdetail.getIndices().length/3; i++) {
			BillboardPainter.loadFaceMatrixAndClipPlanes(i, lowdetail.getIndices(), lowdetail.getVertices(), lowdetail.getTexCoords());
			renderer.renderModel(mode, crowns, trunks, tex_index);
//buffer.dumpToFile("test_bill" + i + ".image");
//GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		}
		BillboardPainter.finish();
	}

	private static void drawBillboardsToBuffer(LowDetailModel @NonNull [] lowdetails, @NonNull TextureGenerator renderer, int[] modes, OffscreenRenderer buffer, SpriteList[] crowns, SpriteList[] trunks, int tex_index) {
		int ortho_size = 1;
		float[] clear_color = trunks[0].getClearColor();
		GL11.glClearColor(clear_color[0], clear_color[1], clear_color[2], 0f);
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		for (int i = 0; i < lowdetails.length; i++) {
            generateBillboardMip(lowdetails[i], renderer, modes[i], ortho_size, crowns, trunks, tex_index);
        }
	}

	public void renderModel(int mode, SpriteList[] crowns, SpriteList[] trunks, int tex_index) {
		trunks[mode].renderModel(tex_index);
		crowns[mode].renderModel(tex_index);
	}

}
