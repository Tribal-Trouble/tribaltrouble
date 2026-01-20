package com.oddlabs.tt.audio;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.global.Settings;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.openal.AL10;

public abstract class AbstractAudioPlayer implements Animated {
	protected static final float ROLLOFF_FACTOR = 0.5f;

	protected final @Nullable AudioSource source;
	private final @NonNull AudioParameters<?> parameters;
	private boolean playing = false;

	private float fadeout_time;
	private float end_gain;
	private float fadeout_gain;

	protected AbstractAudioPlayer(@Nullable AudioSource source, @NonNull AudioParameters<?> params) {
		this.parameters = params;
        if (source == null || (!params.music && !Settings.getSettings().play_sfx)) {
            this.source = null;
            return;
        }
        this.source = source;
        source.setAudioPlayer(this);
		playing = true;
	}

	protected final boolean isPlaying() {
		return playing;
	}

	public final @NonNull AudioParameters<?> getParameters() {
		return parameters;
	}

	public final void setGain(float gain) {
		if (playing && source != null) {
            var settings = Settings.getSettings();
			source.setGain(gain * (parameters.music ? settings.music_gain : settings.sound_gain));
		}
	}

	public final void setPos(float x, float y, float z) {
		if (playing && source != null)
			source.setPosition(x, y, z);
	}

	public void stop() {
		if (playing && source != null) {
			source.stop();
			source.setBuffer(AL10.AL_NONE); // AL10.AL_NONE is still needed
			source.rewind();
			playing = false;
		}
	}

	public final void registerAmbient() {
		if (source != null)
			AudioManager.getManager().registerAmbient(source);
	}

	public final void removeAmbient() {
		if (source != null)
			AudioManager.getManager().removeAmbient(source);
	}

	public final void stop(float delay, float end_gain) {
		this.end_gain = end_gain;
		fadeout_gain = end_gain;
		fadeout_time = delay;
		LocalEventQueue.getQueue().getManager().registerAnimation(this);
	}

	@Override
	public final void animate(float t) {
		fadeout_gain -= t*(end_gain/fadeout_time);
		if (fadeout_gain <= 0) {
			stop();
			LocalEventQueue.getQueue().getManager().removeAnimation(this);
		} else {
			setGain(fadeout_gain);
		}
	}

}
