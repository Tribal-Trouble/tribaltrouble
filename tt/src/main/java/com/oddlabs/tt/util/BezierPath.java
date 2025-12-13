package com.oddlabs.tt.util;

import com.oddlabs.tt.landscape.HeightMap;
import org.jspecify.annotations.NonNull;

/**
 * Cubic Bezier curve path for smooth unit movement.
 * Uses four control points to generate smooth curves between waypoints.
 * Debug visualization shows the curve as a white line (enable with UNIT_GRID mode).
 */
public final class BezierPath {
	private static final int PREVIOUS = 0;
	private static final int START = 1;
	private static final int END = 2;
	private static final int NEXT = 3;

	private static final float[] debug_point = new float[2];
	private static final float[] debug_dir = new float[2];

	private final float[][] points = new float[4][2];
	private final float[] current_point = new float[2];
	private final float[] current_dir = new float[2];
	private float dt;
	private float t;

	public BezierPath() {
		initState();
	}

	private void initState() {
		t = 1f;
	}

	public boolean isDone() {
		return t >= 1f;
	}

	public void computeCurvePoint(float speed) {
		assert !isDone();
        float old_x = current_point[0];
        float old_y = current_point[1];
        
		computeCurvePointFromTime(t, current_point);
        
        float dx = current_point[0] - old_x;
        float dy = current_point[1] - old_y;
        float len = (float)Math.sqrt(dx*dx + dy*dy);
        if (len > 0.000001f) {
            current_dir[0] = dx / len;
            current_dir[1] = dy / len;
        }
        
		t += dt*speed;
	}

	public void dumpPoints() {
            for (float[] point : points) {
				IO.println("points[i][0] = " + point[0] + " | points[i][1] = " + point[1]);
            }
	}

	private void computeCurvePointFromTime(float t, float @NonNull [] point) {
		float t2 = t*t;
		float t3 = t2*t;
		float b0 = 1 - 3*t + 3*t2 - t3;
		float b1 = 3*t3 - 6*t2 + 4;
		float b2 = -3*t3 + 3*t2 + 3*t + 1;
		float b3 = t3;
		point[0] = (1f/6f)*(points[PREVIOUS][0]*b0 + points[START][0]*b1 + points[END][0]*b2 + points[NEXT][0]*b3);
		point[1] = (1f/6f)*(points[PREVIOUS][1]*b0 + points[START][1]*b1 + points[END][1]*b2 + points[NEXT][1]*b3);
	}

	public float getCurrentDirectionX() {
		return current_dir[0];
	}

	public float getCurrentDirectionY() {
		return current_dir[1];
	}

	public float getCurrentX() {
		return current_point[0];
	}

	public float getCurrentY() {
		return current_point[1];
	}

	public void init(float inv_length, float x1, float y1, float x2, float y2) {
		assert x1 != x2 || y1 != y2: x1 + " " + y1;
		points[START][0] = x1 + (x1 - x2);
		points[START][1] = y1 + (y1 - y2);
		points[END][0] = x1;
		points[END][1] = y1;
		points[NEXT][0] = x2;
		points[NEXT][1] = y2;
		initState();
		dt = inv_length;
        
        // Initialize direction
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float)Math.sqrt(dx*dx + dy*dy);
        if (len > 0) {
            current_dir[0] = dx / len;
            current_dir[1] = dy / len;
        } else {
            // Preserve previous direction if length is 0
            // If vector is uninitialized (0,0), set to default East
            if (current_dir[0] == 0f && current_dir[1] == 0f) {
                current_dir[0] = 1f;
                current_dir[1] = 0f;
            }
        }
        current_point[0] = x1;
        current_point[1] = y1;
	}

	public void nextPoint(float inv_length, float x, float y) {
		assert x != points[NEXT][0] || y != points[NEXT][1]: x + " " + y;
		cyclePoints();
		points[NEXT][0] = x;
		points[NEXT][1] = y;
		t -= 1f;
		dt = inv_length;
	}

	public float getNextX() {
		return points[NEXT][0];
	}

	public float getNextY() {
		return points[NEXT][1];
	}

	private void cyclePoints() {
		float[] previous = points[PREVIOUS];
		points[PREVIOUS] = points[START];
		points[START] = points[END];
		points[END] = points[NEXT];
		points[NEXT] = previous;
	}

	public void endPath() {
		float next_x = points[NEXT][0] + (points[NEXT][0] - points[END][0]);
		float next_y = points[NEXT][1] + (points[NEXT][1] - points[END][1]);
		nextPoint(dt, next_x, next_y);
	}

	public void debugRender(@NonNull HeightMap heightmap) {
		float prev_x = 0, prev_y = 0, prev_z = 0;
		boolean first = true;
		for (float t = 0f; t < 1f; t += .01f) {
			computeCurvePointFromTime(t, debug_point);
			float x = debug_point[0];
			float y = debug_point[1];
			float z = heightmap.getNearestHeight(x, y) + 0.5f;
			if (!first) {
				DebugRender.drawLine(prev_x, prev_y, prev_z, x, y, z, 1f, 1f, 1f);
			}
			prev_x = x;
			prev_y = y;
			prev_z = z;
			first = false;
		}
	}
}
