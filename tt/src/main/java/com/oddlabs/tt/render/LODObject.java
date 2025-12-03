package com.oddlabs.tt.render;

import org.jspecify.annotations.NonNull;

interface LODObject {
	void markDetailPoint();
	void markDetailPolygon(@NonNull PolyDetail level);
	int getTriangleCount(@NonNull PolyDetail level);
	float getEyeDistanceSquared();
}
