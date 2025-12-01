package com.oddlabs.tt.render;

import com.oddlabs.tt.model.Model;
import org.joml.Vector4f;

interface ModelState extends LODObject {
	void transform();
	float[] getTeamColor();
	float[] getSelectionColor();
    Vector4f getColor();
	Model getModel();
}
