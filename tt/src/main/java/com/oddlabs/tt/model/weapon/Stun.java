package com.oddlabs.tt.model.weapon;

import com.oddlabs.tt.audio.AbstractAudioPlayer;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.MountUnitContainer;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.particle.RandomVelocityEmitter;
import com.oddlabs.tt.pathfinder.FindOccupantFilter;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.util.StateChecksum;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.util.List;

public final class Stun implements Magic {
	private final float hit_radius;
	private final float stun_time_closest;
	private final float stun_time_farthest;
	private final @NonNull Player owner;
	private final float start_x;
	private final float start_y;
	private final @NonNull RandomVelocityEmitter emitter;
	private final AbstractAudioPlayer sound;

	private final @NonNull List<Selectable> target_list;

	public Stun(float offset_x, float offset_y, float offset_z, float hit_radius, float stun_time_closest, float stun_time_farthest, @NonNull Unit src) {
		this.hit_radius = hit_radius;
		this.stun_time_closest = stun_time_closest;
		this.stun_time_farthest = stun_time_farthest;
		this.owner = src.getOwner();

		start_x = src.getPositionX() + offset_x*src.getDirectionX() - offset_y*(-src.getDirectionY());
		start_y = src.getPositionY() + offset_x*src.getDirectionY() + offset_y*src.getDirectionX();
		float z = src.getPositionZ() + offset_z;
		float alpha = 12f;
		float energy = 4f;
		emitter = new RandomVelocityEmitter(owner.getWorld(), new Vector3f(start_x, start_y, z), 0f, 0f,
				.001f, .001f, .5f, (float)Math.PI,
				-1, 35f,
				new Vector3f(0f, 0f, 6f), new Vector3f(0f, 0f, -2f),
				new Vector4f(1f, 1f, 1f, alpha), new Vector4f(0f, 0f, 0f, -alpha/energy),
				new Vector3f(.3f, .3f, .3f), new Vector3f(.025f, .025f, .025f), energy, 1f,
				GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
				owner.getWorld().getRacesResources().getNoteTextures(),
				owner.getWorld().getAnimationManagerGameTime());
		FindOccupantFilter<Selectable> filter = new FindOccupantFilter<>(src.getPositionX(), src.getPositionY(), hit_radius, src, Selectable.class);
//		FindOccupantFilter filter = new FindOccupantFilter(src.getPositionX(), src.getPositionY(), hit_radius, src, Unit.class);
		UnitGrid unit_grid = owner.getWorld().getUnitGrid();
		unit_grid.scan(filter, UnitGrid.toGridCoordinate(src.getPositionX()), UnitGrid.toGridCoordinate(src.getPositionY()));
		target_list = filter.getResult();

		sound = owner.getWorld().getAudio().newAudio(new AudioParameters<>(owner.getWorld().getRacesResources().getStunSound(owner.getWorld().getRandom()), start_x, start_y, z,
                AudioPlayer.AUDIO_RANK_MAGIC,
                AudioPlayer.AUDIO_DISTANCE_MAGIC,
                AudioPlayer.AUDIO_GAIN_STUN_LUR,
                AudioPlayer.AUDIO_RADIUS_STUN_LUR,
                1f));
	}

        @Override
	public void animate(float t) {
            for (Selectable selectable : target_list) {
                Unit unit = null;
                if (selectable instanceof Unit) {
                    unit = (Unit) selectable;
                } else if (selectable instanceof Building) {
                    Building building = (Building) selectable;
                    if (!building.isDead() && building.getAbilities().hasAbilities(Abilities.ATTACK)) {
                        MountUnitContainer muc = (MountUnitContainer) ((Building) selectable).getUnitContainer();
                        if (muc.getNumSupplies() > 0) {
                            unit = muc.getUnit();
                        }
                    }
                }

                if (unit == null || unit.isDead())
                    continue;

                float dx = unit.getPositionX() - start_x;
                float dy = unit.getPositionY() - start_y;
                float squared_dist = dx * dx + dy * dy;
                if (owner.isEnemy(unit.getOwner()) && squared_dist < hit_radius * hit_radius) {
                    float dist = (float) Math.sqrt(squared_dist);
                    float time = calculateValueFromCurrentRadius(dist, stun_time_closest, stun_time_farthest);
                    unit.stun(time);
                }
            }
		interrupt();
	}

	private float calculateValueFromCurrentRadius(float current_radius, float max, float min) {
		float base_factor = 6f/7f;
		float error = (float)Math.pow(base_factor, hit_radius);
		float factor = (float)Math.pow(base_factor, current_radius);
		float result = (max - min + error)*factor + min - error;
		return result;
	}

        @Override
	public void updateChecksum(StateChecksum checksum) {
	}

        @Override
	public void interrupt() {
		if (emitter != null) {
			emitter.done();
		}
		if (sound != null) {
			sound.stop(.3f, Settings.getSettings().sound_gain);
		}
		owner.getWorld().getAnimationManagerGameTime().removeAnimation(this);
	}
}
