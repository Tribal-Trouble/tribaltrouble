package com.oddlabs.tt.form;

import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.delegate.Menu;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUIRoot;
import org.jspecify.annotations.NonNull;

public final class TerrainMenuForm extends Form implements TerrainMenuListener {
	private final @NonNull TerrainMenu terrain;

	public TerrainMenuForm(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root, Menu main_menu) {
		terrain = new TerrainMenu(network, gui_root, main_menu, false, this);
		addChild(terrain);
		terrain.place();
		compileCanvas();
	}

	@Override
	public void setFocus() {
		terrain.getButtonOK().setFocus();
	}

	@Override
	public void terrainMenuCancel() {
		cancel();
	}

	@Override
	public void terrainMenuOK() {

	}
}
