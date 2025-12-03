package com.oddlabs.tt.viewer;

import com.oddlabs.tt.audio.AbstractAudioPlayer;
import com.oddlabs.tt.audio.Audio;
import com.oddlabs.tt.audio.AudioFile;
import com.oddlabs.tt.audio.AudioManager;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.camera.GameCamera;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.landscape.AudioImplementation;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.resource.Resources;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

public final class AmbientAudio {
	private static final FloatBuffer orientation_buffer = BufferUtils.createFloatBuffer(3*2);

	private final @NonNull Audio ambient_forest_buffer;
	private final @NonNull Audio ambient_beach_buffer;
	private final @NonNull Audio ambient_wind_buffer;

	private final AbstractAudioPlayer ambient_forest;
	private final AbstractAudioPlayer ambient_beach;
	private final AbstractAudioPlayer ambient_wind;

	private final Vector3f f = new Vector3f();
	private final Vector3f s = new Vector3f();
	private final Vector3f u = new Vector3f();

	public AmbientAudio(@NonNull AudioImplementation audio_implementation) {
		ambient_forest_buffer = Resources.findResource(new AudioFile("/sfx/ambient_forest.ogg"));
		ambient_beach_buffer = Resources.findResource(new AudioFile("/sfx/ambient_beach.ogg"));
		ambient_wind_buffer = Resources.findResource(new AudioFile("/sfx/ambient_wind.ogg"));
		ambient_forest = audio_implementation.newAudio(new AudioParameters<>(ambient_forest_buffer, 10000f, 10000f, 10000f, AudioPlayer.AUDIO_RANK_AMBIENT, AudioPlayer.AUDIO_DISTANCE_AMBIENT, AudioPlayer.AUDIO_GAIN_AMBIENT_FOREST, AudioPlayer.AUDIO_RADIUS_AMBIENT_FOREST, 1f, true, true, false));
		ambient_beach = audio_implementation.newAudio(new AudioParameters<>(ambient_beach_buffer, 10000f, 10000f, 10000f, AudioPlayer.AUDIO_RANK_AMBIENT, AudioPlayer.AUDIO_DISTANCE_AMBIENT, AudioPlayer.AUDIO_GAIN_AMBIENT_BEACH, AudioPlayer.AUDIO_RADIUS_AMBIENT_BEACH, 1f, true, true, false));
		ambient_wind = audio_implementation.newAudio(new AudioParameters<>(ambient_wind_buffer, 10000f, 10000f, 10000f, AudioPlayer.AUDIO_RANK_AMBIENT, AudioPlayer.AUDIO_DISTANCE_AMBIENT, AudioPlayer.AUDIO_GAIN_AMBIENT_WIND, AudioPlayer.AUDIO_RADIUS_AMBIENT_WIND, 1f, true, true, false));
		ambient_forest.registerAmbient();
		ambient_beach.registerAmbient();
		ambient_wind.registerAmbient();
	}

	public void stop() {
		ambient_forest.stop();
		ambient_beach.stop();
		ambient_wind.stop();
		ambient_forest.removeAmbient();
		ambient_beach.removeAmbient();
		ambient_wind.removeAmbient();
	}

	public void updateSoundListener(@NonNull CameraState camera, @NonNull HeightMap heightmap) {
		if (Settings.getSettings().play_sfx) {
			camera.updateDirectionAndNormal(f, u, s);
			orientation_buffer.put(0, f.x);
			orientation_buffer.put(1, f.y);
			orientation_buffer.put(2, f.z);
			orientation_buffer.put(3, u.x);
			orientation_buffer.put(4, u.y);
			orientation_buffer.put(5, u.z);
            AudioManager.getManager()
                    .updatePosition(camera.getCurrentX(), camera.getCurrentY(), camera.getCurrentZ())
                    .updateOrientation(orientation_buffer);

			int meters_per_world = heightmap.getMetersPerWorld();
			float dx = Math.abs(camera.getCurrentX() - meters_per_world/2);
			float dy = Math.abs(camera.getCurrentY() - meters_per_world/2);
			float dr = 2f*(float)Math.sqrt(dx*dx + dy*dy)/meters_per_world; //can use Math here - not gamestate affecting

			// update placement and gain of ambient forest source
			ambient_forest.setPos(0f, 0f, heightmap.getNearestHeight(camera.getCurrentX(), camera.getCurrentY()) - camera.getCurrentZ() + 8f);
			ambient_forest.setGain(AudioPlayer.AUDIO_GAIN_AMBIENT_FOREST * Math.clamp(1f - dr + 0.5f, 0f, 1f));

			// update placement and gain of ambient beach source
			float factor = 1f;
			if (dr != 0)
				factor = 1f/dr - 1f;
			float beach_x = camera.getCurrentX()*factor;
			float beach_y = camera.getCurrentY()*factor;
			float beach_z = heightmap.getNearestHeight(camera.getCurrentX(), camera.getCurrentY()) - camera.getCurrentZ();
			float beach_gain = AudioPlayer.AUDIO_GAIN_AMBIENT_BEACH * Math.clamp(1f - Math.abs(4f*dr - 3.75f), 0f, 1f);
			ambient_beach.setPos(beach_x, beach_y, beach_z);
			ambient_beach.setGain(beach_gain);

			// update placement of ambient wind source
			ambient_wind.setPos(0f, 0f, Math.max(0f, 50f + GameCamera.MAX_Z - camera.getCurrentZ()));
			ambient_wind.setGain(AudioPlayer.AUDIO_GAIN_AMBIENT_WIND);
		}
	}
}
