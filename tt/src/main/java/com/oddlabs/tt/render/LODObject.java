package com.oddlabs.tt.render;

interface LODObject {
	void markDetailPoint();
	void markDetailPolygon(PolyDetail level);
	int getTriangleCount(PolyDetail level);
	float getEyeDistanceSquared();
}
