package com.oddlabs.tt.render;

import com.oddlabs.tt.model.Model;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

interface ModelState<M extends Model> extends LODObject {
	@NonNull Matrix4f getTransform(@NonNull Matrix4f dest);
	@NonNull Vector4fc getTeamColor();
	@NonNull Vector4fc getSelectionColor();
    @NonNull Vector4fc getColor();
	@Nullable M getModel();
}
