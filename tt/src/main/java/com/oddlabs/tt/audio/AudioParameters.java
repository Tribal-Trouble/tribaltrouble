package com.oddlabs.tt.audio;

import com.oddlabs.tt.global.Settings;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

public final class AudioParameters<S> {
	public final float x;
	public final float y;
	public final float z;
	public final @NonNull S sound;
	public final int rank;
	public final float distance;
	public final float gain;
	public final float radius;
	public final float pitch;
	public final boolean looping;
	public final boolean relative;
	public final boolean music;

	public AudioParameters(@NonNull S music_path) {
		this(music_path,0f,0f,0f,AudioPlayer.AUDIO_RANK_MUSIC,AudioPlayer.AUDIO_DISTANCE_MUSIC,Settings.getSettings().music_gain,1f,1f,true,true,true);
	}

	public AudioParameters(@NonNull S sound, float x, float y, float z, int rank, float distance) {
		this(sound, x, y, z, rank, distance, 1f, .5f);
	}

	public AudioParameters(@NonNull S sound, float x, float y, float z, int rank, float distance, float gain, float radius) {
		this(sound, x, y, z, rank, distance, gain, radius, 1f);
	}

	public AudioParameters(@NonNull S sound, float x, float y, float z, int rank, float distance, float gain, float radius, float pitch) {
		this(sound, x, y, z, rank, distance, gain, radius, pitch, false, false);
	}

	public AudioParameters(@NonNull S sound, float x, float y, float z, int rank, float distance, float gain, float radius, float pitch, boolean looping, boolean relative) {
		this(sound, x, y, z, rank, distance, gain, radius, pitch, looping, relative, false);
	}

	public AudioParameters(@NonNull S sound, float x, float y, float z, int rank, float distance, float gain, float radius, float pitch, boolean looping, boolean relative, boolean music) {
		this.sound = Objects.requireNonNull(sound, "sound");
		this.x = x;
		this.y = y;
		this.z = z;
		this.rank = rank;
		this.distance = distance;
		this.gain = gain;
		this.radius = radius;
		this.pitch = pitch;
		this.looping = looping;
		this.relative = relative;
		this.music = music;
	}
}
