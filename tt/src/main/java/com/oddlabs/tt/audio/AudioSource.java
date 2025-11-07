package com.oddlabs.tt.audio;

import com.oddlabs.tt.resource.NativeResource;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;

import java.nio.IntBuffer;

public final class AudioSource extends NativeResource<AudioSource.Source> {

    static final class Source extends NativeResource.NativeState {
        private final IntBuffer source;

        Source() {
            source = BufferUtils.createIntBuffer(1);
        }

        @Override
        public void close() {
            if (source != null && AL.isCreated()) {
                assert AL10.alGetSourcei(source.get(0), AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING;
                AL10.alDeleteSources(source);
            }
        }
    }

	private AbstractAudioPlayer audio_player;

	public AudioSource() {
        super(new Source());
		if (AL.isCreated())
			AL10.alGenSources(state.source);
	}

	public int getSource() {
		return state.source.get(0);
	}

	public int getRank() {
		if (audio_player == null)
			return AudioPlayer.AUDIO_RANK_NOT_INITIALIZED;
		return audio_player.getParameters().rank;
	}

	public AbstractAudioPlayer getAudioPlayer() {
		return audio_player;
	}

	public void setAudioPlayer(AbstractAudioPlayer audio_player) {
		if (this.audio_player != null)
			this.audio_player.stop();
		this.audio_player = audio_player;
	}
}
