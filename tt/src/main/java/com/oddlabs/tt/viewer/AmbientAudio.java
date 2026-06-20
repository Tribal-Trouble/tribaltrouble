package com.oddlabs.tt.viewer;

import com.oddlabs.tt.audio.AudioManager;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.audio.ReverbType;
import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.camera.GameCamera;
import com.oddlabs.tt.landscape.AbstractTreeGroup;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.landscape.TreeGroup;
import com.oddlabs.tt.landscape.TreeLeaf;
import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.resource.AudioAssets;
import com.oddlabs.tt.model.BoundingBox;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;

import java.util.logging.Logger;

/**
 * Coordinates the playback of ambient environmental sounds, providing
 * realistic reverb and attenuation based on camera proximity and terrain features.
 */
public final class AmbientAudio implements AutoCloseable {
    private static final Logger logger = Logger.getLogger(AmbientAudio.class.getSimpleName());
    /** How many trees make a forest */
    private static final int TREES_FOREST_THRESHOLD = 10;
    private static final float CANYON_PROXIMITY_DISTANCE = 30f;


    private final @NonNull AudioManager audioManager;
    private final @NonNull AudioPlayer ambient_forest;
    private final @NonNull AudioPlayer ambient_beach;
    private final @NonNull AudioPlayer ambient_wind;

    private final Vector3f f = new Vector3f();
    private final Vector3f s = new Vector3f();
    private final Vector3f u = new Vector3f();

    public AmbientAudio(@NonNull AudioManager audioManager) {
        this.audioManager = audioManager;
        ambient_forest = audioManager.newAudio(10000f, 10000f, 10000f, AudioAssets.AMBIENT_FOREST);
        ambient_beach = audioManager.newAudio(10000f, 10000f, 10000f, AudioAssets.AMBIENT_BEACH);
        ambient_wind = audioManager.newAudio(10000f, 10000f, 10000f, AudioAssets.AMBIENT_WIND);
    }

    private static int countTrees(@NonNull AbstractTreeGroup node, float x, float y, float radiusSq, int threshold,
            int currentCount) {
        if (currentCount >= threshold) return currentCount;

        if (intersects(node, x, y, radiusSq)) {
            switch (node) {
                case TreeGroup group -> {
                    for (AbstractTreeGroup child : group.children()) {
                        currentCount = countTrees(child, x, y, radiusSq, threshold, currentCount);
                        if (currentCount >= threshold) break;
                    }
                }
                case TreeLeaf leaf -> {
                    for (TreeSupply tree : leaf.getTrees()) {
                        if (currentCount >= threshold) break;
                        if (!tree.isHidden()) {
                            float ddx = tree.getCX() - x;
                            float ddy = tree.getCY() - y;
                            if (ddx * ddx + ddy * ddy < radiusSq) {
                                currentCount++;
                            }
                        }
                    }
                }
                case TreeSupply tree -> {
                    if (!tree.isHidden()) {
                        float ddx = tree.getCX() - x;
                        float ddy = tree.getCY() - y;
                        if (ddx * ddx + ddy * ddy < radiusSq) {
                            currentCount++;
                        }
                    }
                }
            }
        }
        return currentCount;
    }

    @SuppressWarnings("MathClampMigration")
    private static boolean intersects(@NonNull BoundingBox box, float x, float y, float radiusSq) {
        float dx = x - Math.max(box.bmin_x, Math.min(x, box.bmax_x));
        float dy = y - Math.max(box.bmin_y, Math.min(y, box.bmax_y));
        return (dx * dx + dy * dy) < radiusSq;
    }

    @Override
    public void close() {
        ambient_forest.stop();
        ambient_beach.stop();
        ambient_wind.stop();
    }

    public void updateSoundListener(@NonNull CameraState camera, @NonNull HeightMap heightmap) {
        if (!audioManager.isSfxEnabled()) {
            return;
        }

        camera.updateDirectionAndNormal(f, u, s);
        audioManager
                .setListenerPosition(camera.getCurrentX(), camera.getCurrentY(), camera.getCurrentZ())
                .setListenerOrientation(f, u);

        int meters_per_world = heightmap.getMetersPerWorld();
        float dx = Math.abs(camera.getCurrentX() - meters_per_world / 2f);
        float dy = Math.abs(camera.getCurrentY() - meters_per_world / 2f);
        float dr = 2f * (float) Math.sqrt(dx * dx + dy * dy) / meters_per_world;

        // update placement and gain of ambient forest source
        ambient_forest.setPosition(0f, 0f, heightmap.getNearestHeight(camera.getCurrentX(), camera.getCurrentY())
                - camera.getCurrentZ() + 8f);
        ambient_forest.setGain(AudioAssets.AUDIO_GAIN_AMBIENT_FOREST * Math.clamp(1f - dr + 0.5f, 0f, 1f));

        // update placement and gain of ambient beach source
        float factor = 1f;
        if (dr != 0)
            factor = 1f / dr - 1f;
        float beach_x = (camera.getCurrentX() - meters_per_world / 2f) * factor;
        float beach_y = (camera.getCurrentY() - meters_per_world / 2f) * factor;
        float beach_z = heightmap.getNearestHeight(camera.getCurrentX(), camera.getCurrentY()) - camera.getCurrentZ();
        float beach_gain = AudioAssets.AUDIO_GAIN_AMBIENT_BEACH * Math.clamp(1f - Math.abs(4f * dr - 3.75f), 0f, 1f);
        ambient_beach.setPosition(beach_x, beach_y, beach_z);
        ambient_beach.setGain(beach_gain);

        // update placement of ambient wind source
        ambient_wind.setPosition(0f, 0f, Math.max(0f, 50f + GameCamera.MAX_Z - camera.getCurrentZ()));
        ambient_wind.setGain(AudioAssets.AUDIO_GAIN_AMBIENT_WIND);

        if (audioManager.isEFXSupported()) {
            float camZ = camera.getCurrentZ();
            float camX = camera.getCurrentX();
            float camY = camera.getCurrentY();
            float hCurrent = heightmap.getNearestHeight(camX, camY);

            if (camZ < heightmap.getSeaLevelMeters()) {
                audioManager.setReverb(ReverbType.UNDERWATER);
            } else {
                float heightAboveGround = camZ - hCurrent;

                // Check for forest density
                World world = heightmap.getWorld();
                int treeCount = countTrees(world.getTreeRoot(), camX, camY, 25f * 25f, TREES_FOREST_THRESHOLD, 0);

                if (treeCount >= TREES_FOREST_THRESHOLD) {
                    // Forest reverb: Blend from FOREST (fully active at 15m) to NONE (fully silent at 30m)
                    float blend = Math.clamp((30.0f - heightAboveGround) / 15.0f, 0f, 1f);
                    audioManager.setReverb(ReverbType.NONE, ReverbType.FOREST, blend);
                } else {
                    // Check for valley/enclosure by sampling terrain height around camera
                    float hN = heightmap.getNearestHeight(camX, camY + CANYON_PROXIMITY_DISTANCE);
                    float hS = heightmap.getNearestHeight(camX, camY - CANYON_PROXIMITY_DISTANCE);
                    float hE = heightmap.getNearestHeight(camX + CANYON_PROXIMITY_DISTANCE, camY);
                    float hW = heightmap.getNearestHeight(camX - CANYON_PROXIMITY_DISTANCE, camY);

                    float avgSurround = (hN + hS + hE + hW) * 0.25f;
                    float valleyDepth = avgSurround - hCurrent;

                    if (valleyDepth > 8.0f) {
                        // Valley reverb: Blend from VALLEY (fully active at 8m) to NONE (fully silent at 16m)
                        float blend = Math.clamp((16.0f - heightAboveGround) / 8.0f, 0f, 1f);
                        audioManager.setReverb(ReverbType.NONE, ReverbType.VALLEY, blend);
                    } else {
                        // Open Plains (Generic) reverb: Blend from GENERIC (fully active at 10m) to NONE (fully silent at 20m)
                        float blend = Math.clamp((20.0f - heightAboveGround) / 10.0f, 0f, 1f);
                        audioManager.setReverb(ReverbType.NONE, ReverbType.GENERIC, blend);
                    }
                }
            }
        }
    }
}
