package com.oddlabs.tt.model;

import com.oddlabs.tt.audio.AbstractAudioPlayer;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.particle.LinearEmitter;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class WeaponsProducer {
	private final static float MAX_BREAK_TIME = .25f;
	private final static float BREAK_PROBABILITY = .2f;

	private final static List<BuildProductionContainer> build_list = new ArrayList<>();

	private final Building building;
	private final WorkerUnitContainer unit_container;
	private final BuildProductionContainer[] production_containers;
	private final LinearEmitter emitter;

	private float break_time = 0f;
	private @Nullable AbstractAudioPlayer production_player;

	public WeaponsProducer(Building building, WorkerUnitContainer unit_container, BuildProductionContainer[] production_containers, LinearEmitter emitter) {
		this.building = building;
		this.unit_container = unit_container;
		this.production_containers = production_containers;
		this.emitter = emitter;
	}

	public final void animate(float t) {
            for (BuildProductionContainer production_container : production_containers) {
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
            for (BuildProductionContainer current : build_list) {
                current.build(man_seconds_per_container);
            }
		} else {
			emitter.stop();
			stopSound();
		}
		build_list.clear();
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
