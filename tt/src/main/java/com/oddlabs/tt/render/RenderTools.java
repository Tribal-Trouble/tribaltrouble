package com.oddlabs.tt.render;

import com.oddlabs.tt.global.BoundingMode;
import com.oddlabs.tt.model.Model;
import com.oddlabs.tt.util.BoundingBox;
import com.oddlabs.tt.util.DebugRender;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

public final class RenderTools {

    enum FrustumIntersection {
        ALL_OUTSIDE,
        INTERSECTING,
        ALL_INSIDE
    }

    /**
     * Translates and rotates the matrix stack to position and orient a model.
     * The rotation is calculated from the direction vector (dir_x, dir_y).
     */
    static void translateAndRotate(@NonNull Model model, @NonNull MatrixStack stack) {
        translateAndRotate(model.getPositionX(), model.getPositionY(), model.getPositionZ(), model.getDirectionX(), model.getDirectionY(), stack);
    }

	/**
	 * Translates and rotates the matrix stack to position and orient a model.
	 * The rotation is calculated from the direction vector (dir_x, dir_y).
	 */
    static void translateAndRotate(float x, float y, float z, float dir_x, float dir_y, @NonNull MatrixStack stack) {
        float angle = (float) Math.toDegrees(Math.atan2(dir_y, dir_x));
        stack.translate(x, y, z).rotate(angle, 0f, 0f, 1f);
    }

	static void translateAndRotate(@NonNull Model model) {
		translateAndRotate(model.getPositionX(), model.getPositionY(), model.getPositionZ(), model.getDirectionX(), model.getDirectionY());
	}
	
	static void translateAndRotate(float x, float y, float z, float dir_x, float dir_y) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer transform_matrix = stack.callocFloat(16);
            // Identity initialization for parts we don't touch
            transform_matrix.put(10, 1.0f);
            transform_matrix.put(15, 1.0f);
            
            // Rotation and translation
            transform_matrix.put(0, dir_x);
            transform_matrix.put(1, dir_y);
            transform_matrix.put(4, -dir_y);
            transform_matrix.put(5, dir_x);
            transform_matrix.put(12, x);
            transform_matrix.put(13, y);
            transform_matrix.put(14, z);
            
            GL11.glMultMatrixf(transform_matrix);
        }
	}

	static @NonNull FrustumIntersection inFrustum(@NonNull BoundingBox box, float[][] frustum) {
		boolean all_corners_in_all_planes = true;

		for (int f = 0; f < 6; f++) {
			boolean any_corner_in_this_plane = false;

			// Cache plane components for current plane
			float planeA = frustum[f][0];
			float planeB = frustum[f][1];
			float planeC = frustum[f][2];
			float planeD = frustum[f][3];

			// Check each corner against the current plane
			for (int corners_x = 0; corners_x <= 1; corners_x++ ) {
                float x = 0 == corners_x ? box.bmin_x : box.bmax_x;
				for (int corners_y = 0; corners_y <= 1; corners_y++ ) {
                    float y = 0 == corners_y ? box.bmin_y : box.bmax_y;
					for (int corners_z = 0; corners_z <= 1; corners_z++ ) {
                        float z = 0 == corners_z ? box.bmin_z : box.bmax_z;
						// Calculate signed distance from corner to plane
						float distance = planeA * x + planeB * y + planeC * z + planeD;

						if (distance > 0) { // If this corner is inside the plane
							any_corner_in_this_plane = true;
						} else { // If this corner is outside the plane
							all_corners_in_all_planes = false; // At least one corner is outside at least one plane
						}
					}
				}
			}

			// If no corner was inside this plane, then the entire box is outside this plane.
			// Therefore, the box is not in the frustum.
			if (!any_corner_in_this_plane) {
				return FrustumIntersection.ALL_OUTSIDE;
			}
		}

		// If we reach here, the box is not entirely outside any single frustum plane.
		// Now, determine if it's fully inside or intersecting.
        return all_corners_in_all_planes ? FrustumIntersection.ALL_INSIDE : FrustumIntersection.INTERSECTING;
	}

	static float getEyeDistanceSquared(@NonNull BoundingBox box, float camera_x, float camera_y, float camera_z) {
		float distx = camera_x - box.getCX();
		float disty = camera_y - box.getCY();
		float distz = camera_z - box.getCZ();
		float dist2 = distx*distx + disty*disty + distz*distz;
		return dist2;
	}

	static float getCameraDistanceXYSquared(@NonNull BoundingBox box, float camera_x, float camera_y) {
		float distx = camera_x - box.getCX();
		float disty = camera_y - box.getCY();
		float dist2 = distx*distx + disty*disty;
		return dist2;
	}

	static float getCameraDistanceSquared(@NonNull BoundingBox box, float camera_x, float camera_y, float camera_z) {
		float distz = camera_z - box.getCZ();
		float dist2 = getCameraDistanceXYSquared(box, camera_x, camera_y) + distz*distz;
		return dist2;
	}

	static void draw(@NonNull BoundingBox box) {
		draw(box, 1f, 1f, 1f);
	}
	
	static void draw(@NonNull BoundingBox box, float r, float g, float b) {
		DebugRender.drawBox(box.bmin_x, box.bmax_x, box.bmin_y, box.bmax_y, box.bmin_z, box.bmax_z, r, g, b);
	}

	static void draw(@NonNull BoundingBox box, @NonNull BoundingMode bound_type, float r, float g, float b) {
		draw(box, r, g, b);
	}

    private RenderTools() {
    }
}