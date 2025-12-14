package com.oddlabs.tt.model;

import com.oddlabs.tt.audio.AbstractAudioPlayer;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.particle.LinearEmitter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

public class WeaponsProducer {
	private static final float MAX_BREAK_TIME = .25f;
	private static final float BREAK_PROBABILITY = .2f;

	private static final Deque<@NonNull BuildProductionContainer> build_list = new ArrayDeque<>();

	private final @NonNull Building building;
	private final @NonNull WorkerUnitContainer unit_container;
	private final @NonNull BuildProductionContainer @NonNull [] production_containers;
	private final @NonNull LinearEmitter emitter;

	private float break_time = 0f;
	private @Nullable AbstractAudioPlayer production_player;

	public WeaponsProducer(@NonNull Building building, @NonNull WorkerUnitContainer unit_container, @NonNull  BuildProductionContainer @NonNull [] production_containers, @NonNull LinearEmitter emitter) {
		this.building = building;
		this.unit_container = unit_container;
		this.production_containers = production_containers;
		this.emitter = emitter;
	}

	public final void animate(float t) {
		for (var production_container : production_containers) {
			if (production_container.getNumSupplies() > 0 && production_container.hasEnoughSupplies()) {
				build_list.add(production_container);
			}
		}

		if (!build_list.isEmpty()) {
			if (break_time <= 0) {
				if (building.getOwner().getWorld().getRandom().nextFloat() < BREAK_PROBABILITY) {
					break_time = building.getOwner().getWorld().getRandom().nextFloat()*MAX_BREAK_TIME;
					emitter.stop();
				} else {
					emitter.start();
				}
			}
			startSound();
			float man_seconds_per_container = unit_container.getNumSupplies()*t/build_list.size();
			while (!build_list.isEmpty()) {
				build_list.pop().build(man_seconds_per_container);
            }
		} else {
			emitter.stop();
			stopSound();
		}
		break_time -= t;
	}

	private void startSound() {
		if (production_player == null) {
			production_player = building.getOwner().getWorld().getAudio().newAudio(new AudioParameters<>(building.getOwner().getWorld().getRacesResources().getArmorySound(), building.getPositionX(), building.getPositionY(), building.getPositionZ(),
					AudioPlayer.AUDIO_RANK_ARMORY,
					AudioPlayer.AUDIO_DISTANCE_ARMORY,
					AudioPlayer.AUDIO_GAIN_ARMORY,
					AudioPlayer.AUDIO_RADIUS_ARMORY,
					1f, true, false));
		}
	}

	public final void stopSound() {
		if (production_player != null) {
			production_player.stop();
			production_player = null;
		}
	}
}
