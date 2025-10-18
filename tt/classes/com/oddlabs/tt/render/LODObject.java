package com.oddlabs.tt.render;

interface LODObject {
	void markDetailPoint();
	void markDetailPolygon(int level);
	int getTriangleCount(int level);
	float getEyeDistanceSquared();
}
