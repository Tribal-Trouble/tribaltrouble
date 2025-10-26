package com.oddlabs.tt.render;

import com.oddlabs.tt.util.StateChecksum;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;

public final class WaveAnimation {
	private final static float TREE_WAVE_SCALE = 2f;
	private final static float TRANSLATE_SCALE = .025f;

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

	public void mulRotation() {
		GL11.glRotatef(rot_angle, rot_axis.x, rot_axis.y, rot_axis.z);
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
		wave_dir.normalise();
	}

	private void computeRotation() {
		Vector3f.cross(wave_dir, up_vec, rot_axis);
		float length = rot_axis.length();
		rot_angle = (float)Math.asin(length);
		float inv_length = 1f/length;
		rot_axis.scale(inv_length);
	}
}
