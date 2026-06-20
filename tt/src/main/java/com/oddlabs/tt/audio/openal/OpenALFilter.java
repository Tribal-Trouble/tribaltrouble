package com.oddlabs.tt.audio.openal;

import com.oddlabs.tt.resource.NativeResource;
import org.jspecify.annotations.NonNull;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;

import java.util.function.Consumer;

import static com.oddlabs.tt.audio.openal.OpenALManager.checkALError;
import static org.lwjgl.openal.EXTEfx.AL_FILTER_LOWPASS;
import static org.lwjgl.openal.EXTEfx.AL_FILTER_TYPE;
import static org.lwjgl.openal.EXTEfx.AL_LOWPASS_GAIN;
import static org.lwjgl.openal.EXTEfx.AL_LOWPASS_GAINHF;
import static org.lwjgl.openal.EXTEfx.alDeleteFilters;
import static org.lwjgl.openal.EXTEfx.alFilterf;
import static org.lwjgl.openal.EXTEfx.alFilteri;
import static org.lwjgl.openal.EXTEfx.alGenFilters;

/**
 * Manages a native OpenAL filter for environmental audio effects.
 */
final class OpenALFilter extends NativeResource<OpenALFilter.FilterState> {

    static final class FilterState extends NativeResource.NativeState {
        final int filterId;

        FilterState() {
            filterId = alGenFilters();
            checkALError("alGenFilters");
            alFilteri(filterId, AL_FILTER_TYPE, AL_FILTER_LOWPASS);
            checkALError("alFilteri AL_FILTER_TYPE LOWPASS");
        }

        @Override
        public void close() {
            if (ALC10.alcGetCurrentContext() != 0) {
                AL10.alGetError(); // Clear any sticky error from previous operations
                alDeleteFilters(filterId);
                checkALError("alDeleteFilters");
            }
        }
    }

    OpenALFilter(@NonNull Consumer<@NonNull Runnable> cleanupStrategy) {
        super(new FilterState(), cleanupStrategy);
    }

    void setLowPassGain(float gain) {
        alFilterf(state.filterId, AL_LOWPASS_GAIN, gain);
        checkALError("alFilterf AL_LOWPASS_GAIN");
    }

    void setLowPassGainHF(float gainHF) {
        alFilterf(state.filterId, AL_LOWPASS_GAINHF, gainHF);
        checkALError("alFilterf AL_LOWPASS_GAINHF");
    }

    int getFilterId() {
        return state.filterId;
    }
}
