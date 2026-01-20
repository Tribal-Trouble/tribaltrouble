package com.oddlabs.tt.viewer;

import com.oddlabs.tt.audio.AbstractAudioPlayer;
import com.oddlabs.tt.audio.Audio;
import com.oddlabs.tt.audio.AudioFile;
import com.oddlabs.tt.audio.AudioManager;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.audio.EFXManager;
import com.oddlabs.tt.audio.openal.OpenALManager;
import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.camera.GameCamera;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.landscape.AudioImplementation;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.landscape.TreeNodeVisitor;
import com.oddlabs.tt.landscape.TreeGroup;
import com.oddlabs.tt.landscape.TreeLeaf;
import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.resource.Resources;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.logging.Logger;

public final class AmbientAudio {
    private static final Logger logger = Logger.getLogger(AmbientAudio.class.getSimpleName());
    /** How many trees make a forest */
    private static final int TREES_FOREST_THRESHOLD = 10;
    private static final float CANYON_PROXIMITY_DISTANCE = 30f;

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

    private static final class TreeCounterVisitor implements TreeNodeVisitor {
        private final float x, y, radiusSq;
        private final int threshold;
        private int count;

        TreeCounterVisitor(float x, float y, float radius, int threshold) {
            this.x = x;
            this.y = y;
            this.radiusSq = radius * radius;
            this.threshold = threshold;
        }

        private boolean intersects(com.oddlabs.tt.util.@NonNull BoundingBox box) {
            float dx = x - Math.max(box.bmin_x, Math.min(x, box.bmax_x));
            float dy = y - Math.max(box.bmin_y, Math.min(y, box.bmax_y));
            return (dx * dx + dy * dy) < radiusSq;
        }

        @Override
        public void visitLeaf(TreeLeaf tree_leaf) {
            if (count >= threshold) return;
            if (intersects(tree_leaf)) {
                tree_leaf.visitTrees(this);
            }
        }

        @Override
        public void visitNode(TreeGroup tree_group) {
            if (count >= threshold) return;
            if (intersects(tree_group)) {
                tree_group.visitChildren(this);
            }
        }

        @Override
        public void visitTree(TreeSupply tree_supply) {
            if (count >= threshold) return;
            if (!tree_supply.isHidden()) {
                float dx = tree_supply.getPositionX() - x;
                float dy = tree_supply.getPositionY() - y;
                if (dx * dx + dy * dy < radiusSq) {
                    count++;
                }
            }
        }

        public int getCount() {
            return count;
        }
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
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer orientation_buffer = stack.mallocFloat(6);
                f.get(orientation_buffer);
                u.get(3, orientation_buffer);
                AudioManager.getManager()
                        .updatePosition(camera.getCurrentX(), camera.getCurrentY(), camera.getCurrentZ())
                        .updateOrientation(orientation_buffer);
            }

			int meters_per_world = heightmap.getMetersPerWorld();
			float dx = Math.abs(camera.getCurrentX() - meters_per_world/2);
			float dy = Math.abs(camera.getCurrentY() - meters_per_world/2);
			float dr = 2f*(float)Math.sqrt(dx*dx + dy*dy)/meters_per_world;

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

            if (AudioManager.getManager() instanceof OpenALManager alManager) {
                EFXManager efx = alManager.getEfxManager();
                if (efx.isSupported()) {
                    float camZ = camera.getCurrentZ();
                    
                    if (camZ < heightmap.getSeaLevelMeters()) {
                        efx.setReverb(EFXManager.ReverbType.UNDERWATER);
                    } else {
                        float camX = camera.getCurrentX();
                        float camY = camera.getCurrentY();

                        // Check for forest density
                        World world = heightmap.getWorld();
                        TreeCounterVisitor treeVisitor = new TreeCounterVisitor(camX, camY, 25f, TREES_FOREST_THRESHOLD);
                        world.getTreeRoot().visit(treeVisitor);
                        
                        if (treeVisitor.getCount() >= TREES_FOREST_THRESHOLD) {
                            efx.setReverb(EFXManager.ReverbType.FOREST);
                        } else {
                            // Check for valley/enclosure by sampling terrain height around camera
                            float hCurrent = heightmap.getNearestHeight(camX, camY);
                            float hN = heightmap.getNearestHeight(camX, camY + CANYON_PROXIMITY_DISTANCE);
                            float hS = heightmap.getNearestHeight(camX, camY - CANYON_PROXIMITY_DISTANCE);
                            float hE = heightmap.getNearestHeight(camX + CANYON_PROXIMITY_DISTANCE, camY);
                            float hW = heightmap.getNearestHeight(camX - CANYON_PROXIMITY_DISTANCE, camY);
                            
                            float avgSurround = (hN + hS + hE + hW) * 0.25f;
                            
                            // If average surrounding height is significantly higher than current ground position,
                            // we are likely in a valley or depression.
                            efx.setReverb(avgSurround > hCurrent + 8.0f ? EFXManager.ReverbType.VALLEY : EFXManager.ReverbType.GENERIC);
                        }
                    }
                }
            }
		}
	}
}
