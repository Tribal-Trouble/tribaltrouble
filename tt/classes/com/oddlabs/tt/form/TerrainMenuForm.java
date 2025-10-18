package com.oddlabs.tt.form;

import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.delegate.Menu;
import com.oddlabs.tt.gui.*;

public final class TerrainMenuForm extends Form implements TerrainMenuListener {
	private final TerrainMenu terrain;

	public TerrainMenuForm(NetworkSelector network, GUIRoot gui_root, Menu main_menu) {
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
