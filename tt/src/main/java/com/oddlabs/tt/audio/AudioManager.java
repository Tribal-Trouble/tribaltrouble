package com.oddlabs.tt.audio;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.landscape.AudioImplementation;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.OpenALException;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public final class AudioManager implements AudioImplementation {
	private final static int MAX_NUM_SOURCES = 32;

	private final static AudioManager SINGLETON = new AudioManager();

	private final List<AudioSource> ambients = new ArrayList<>();
	private final RefillerList queued_players = new RefillerList();
	private AudioSource[] sources;

	private int sound_play_counter = Settings.getSettings().play_sfx ? 1 : 0;

	public static AudioManager getManager() {
		return SINGLETON;
	}

	private AudioManager() {
		generateSources(MAX_NUM_SOURCES);
	}

	private void generateSources(int max) {
		List<AudioSource> list = new ArrayList<>();
		for (int i = 0; i < max; i++) {
			try {
				AudioSource source = new AudioSource();
				list.add(source);
			} catch (OpenALException _) {
				break;
			}
		}
		sources = new AudioSource[list.size()];
		list.toArray(sources);
	}

	public void stopSources() {
		sound_play_counter--;
		if (sound_play_counter == 0) {
                    for (AudioSource source : sources) {
                        int rank = source.getRank();
                        switch (rank) {
                            case AudioPlayer.AUDIO_RANK_MUSIC:
                                continue;
                            case AudioPlayer.AUDIO_RANK_AMBIENT:
                                AL10.alSourcePause(source.getSource());
                                break;
                            default:
                                AL10.alSourceStop(source.getSource());
                                break;
                        }
                    }
		}
	}

	public @NonNull AbstractAudioPlayer newAudio(@NonNull CameraState camera_state, @NonNull AudioParameters<?> params) {
		AudioSource source = getSource(camera_state, params);
		return doNewAudio(source, params);
	}

        @Override
	public @NonNull AbstractAudioPlayer newAudio(@NonNull AudioParameters<?> params) {
		AudioSource source = getSource(params);
		return doNewAudio(source, params);
	}

	private static @NonNull AbstractAudioPlayer doNewAudio(@NonNull AudioSource source, @NonNull AudioParameters<?> params) {
		if (params.sound instanceof Audio)
			return new AudioPlayer(source, (AudioParameters<Audio>) params);
        else if (params.sound instanceof String) try {
            return new QueuedAudioPlayer(source, (AudioParameters<String>) params);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Could not load " + params.sound, ex);
        } else {
            throw new IllegalArgumentException("Unrecognized audio parameters : " + params.sound.getClass().getSimpleName());
        }
	}

	public void startSources() {
		if (sound_play_counter == 0) {
            for (AudioSource ambient : ambients) {
                AL10.alSourcePlay(ambient.getSource());
            }
		}
		sound_play_counter++;
	}

	public void registerAmbient(AudioSource source) {
		ambients.add(source);
	}

	public void removeAmbient(AudioSource source) {
		ambients.remove(source);
	}

	boolean startPlaying() {
		return sound_play_counter > 0;
	}

	private @Nullable AudioSource findSource(@NonNull AudioParameters<?> params) {
		// Check for free sources
		int worst_rank = Integer.MAX_VALUE;
            for (AudioSource source1 : sources) {
                int source_index = source1.getSource();
                if ((AL10.alGetSourcei(source_index, AL10.AL_SOURCE_STATE) == AL10.AL_STOPPED
                        || AL10.alGetSourcei(source_index, AL10.AL_SOURCE_STATE) == AL10.AL_INITIAL) && source1.getRank() < AudioPlayer.AUDIO_RANK_AMBIENT) {
                    if (source1.getAudioPlayer() != null)
                        source1.getAudioPlayer().stop();
                    return source1;
                }
                if (worst_rank > source1.getRank())
                    worst_rank = source1.getRank();
            }

		if (params.rank > worst_rank) {
			FloatBuffer position = BufferUtils.createFloatBuffer(3);
                for (AudioSource source1 : sources) {
                    if (source1.getRank() == worst_rank) {
                        return source1;
                    }
                }
		}
		return null;
	}

	private @Nullable AudioSource getSource(@NonNull AudioParameters<?> params) {
		if (!AL.isCreated())
			return null;
		AudioSource best_source = findSource(params);
		stopSource(best_source);
		return best_source;
	}

	private @Nullable AudioSource getSource(@NonNull CameraState camera_state, @NonNull AudioParameters<?> params) {
		if (!AL.isCreated())
			return null;
		float this_dist_squared;
		if (params.relative)
			this_dist_squared = params.x*params.x + params.y*params.y + params.z*params.z;
		else
			this_dist_squared = getCamDistSquared(camera_state, params.x, params.y, params.z);

		if (this_dist_squared > params.distance*params.distance) {
		   return null;
		}

		AudioSource best_source = findSource(params);

		if (best_source == null) {
			float max_dist_squared = this_dist_squared;
			FloatBuffer position = BufferUtils.createFloatBuffer(3);
                    for (AudioSource source1 : sources) {
                        if (source1.getRank() == params.rank) {
                            int source_index = source1.getSource();
                            AL10.alGetSource(source_index, AL10.AL_POSITION, position);

                            float dist_squared = getCamDistSquared(camera_state, position.get(0), position.get(1), position.get(2));
                            if (dist_squared > max_dist_squared) {
                                max_dist_squared = dist_squared;
                                best_source = source1;
                            }
                        }
                    }
		}
		stopSource(best_source);
		return best_source;
	}

	private static void stopSource(@Nullable AudioSource source) {
		if (source != null && source.getAudioPlayer() != null) {
			source.getAudioPlayer().stop();
		}
	}

	private static float getCamDistSquared(@NonNull CameraState camera_state, float x, float y, float z) {
		float dx = x - camera_state.getCurrentX();
		float dy = y - camera_state.getCurrentY();
		float dz = z - camera_state.getCurrentZ();
		return dx*dx + dy*dy + dz*dz;
	}

        void registerQueuedPlayer(QueuedAudioPlayer q) {
		queued_players.registerQueuedPlayer(q);
	}

	void removeQueuedPlayer(QueuedAudioPlayer q) {
		queued_players.removeQueuedPlayer(q);
	}

	public void destroy() {
		queued_players.destroy();
	}
}
