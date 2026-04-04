package com.oddlabs.tt.landscape;

import com.oddlabs.tt.audio.AbstractAudioPlayer;
import com.oddlabs.tt.audio.AudioParameters;

@FunctionalInterface
public interface AudioImplementation {
    AbstractAudioPlayer newAudio(AudioParameters<?> params);
}
