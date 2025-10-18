package com.oddlabs.tt.render;

import com.oddlabs.tt.model.Model;

interface ModelState extends LODObject {
	void transform();
	float[] getTeamColor();
	float[] getSelectionColor();
	Model getModel();
}
