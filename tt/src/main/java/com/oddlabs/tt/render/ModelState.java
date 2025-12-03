package com.oddlabs.tt.render;

import com.oddlabs.tt.model.Model;
import org.joml.Vector4f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

interface ModelState<M extends Model> extends LODObject {
	void transform();
	float @NonNull [] getTeamColor();
	float @NonNull [] getSelectionColor();
    @NonNull Vector4f getColor();
	@Nullable M getModel();
}
