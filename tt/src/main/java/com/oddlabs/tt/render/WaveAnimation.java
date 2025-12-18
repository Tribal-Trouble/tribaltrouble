package com.oddlabs.tt.render;

import com.oddlabs.tt.util.StateChecksum;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;

public final class WaveAnimation {
	private static final float TREE_WAVE_SCALE = 2f;
	private static final float TRANSLATE_SCALE = .025f;

	private final Vector3f wave_dir = new Vector3f(0, 0, 1);
	private final Vector3f up_vec = new Vector3f(0, 0, 1);
	private final Vector3f rot_axis = new Vector3f(0, 0, 1);
	private float x;
	private float y;
	private float rot_angle = 0;
	private int time = 0;

	public void mulTranslation() {
		GL11.glTranslatef(TRANSLATE_SCALE*x, TRANSLATE_SCALE*y, 0f);
	}

	public void mulRotation(@NonNull MatrixStack stack) {
		stack.rotate(rot_angle, rot_axis.x, rot_axis.y, rot_axis.z);
	}

	public void updateChecksum(@NonNull StateChecksum checksum) {
		checksum.update(rot_angle);
	}

	public void setTime(float t) {
		time = (int)(t*1000);
		initWaveDir();
		computeRotation();
	}

	private void initWaveDir() {
		x = TREE_WAVE_SCALE*0.5f*(float)Math.cos(time*0.001f);
		y = TREE_WAVE_SCALE*(float)Math.sin(time*0.001f);
		wave_dir.set(x, y, 1);
		wave_dir.normalize();
	}

	private void computeRotation() {
		wave_dir.cross(up_vec, rot_axis);
		float length = rot_axis.length();
		rot_angle = (float)Math.asin(length);
		float inv_length = 1f/length;
		rot_axis.mul(inv_length);
	}
}