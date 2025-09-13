package com.oddlabs.tt.editor;

import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.audio.AbstractAudioPlayer;
import com.oddlabs.tt.audio.AudioManager;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.camera.Camera;
import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.camera.StaticCamera;
import com.oddlabs.tt.gui.GUI;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.KeyboardEvent;
import com.oddlabs.tt.gui.LocalInput;
import com.oddlabs.tt.input.Keyboard;
import com.oddlabs.tt.landscape.AudioImplementation;
import com.oddlabs.tt.landscape.NotificationListener;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.landscape.WorldParameters;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.player.PlayerInfo;
import com.oddlabs.tt.render.DefaultRenderer;
import com.oddlabs.tt.render.LandscapeLocation;
import com.oddlabs.tt.render.LandscapeRenderer;
import com.oddlabs.tt.render.Picker;
import com.oddlabs.tt.render.RenderQueues;
import com.oddlabs.tt.render.UIRenderer;
import com.oddlabs.tt.resource.WorldGenerator;
import com.oddlabs.tt.resource.WorldInfo;
import com.oddlabs.tt.util.StrictMatrix4f;
import com.oddlabs.tt.util.Target;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.viewer.Cheat;
import com.oddlabs.tt.viewer.Selection;
import org.lwjgl.opengl.GL11;
import com.oddlabs.tt.editor.ui.EditorState;

/**
 * Minimal editor runtime: creates a world, switches to an in-world renderer,
 * and provides a basic height brush controlled by mouse:
 * - Left mouse: raise terrain within a circular brush
 * - Right mouse: lower terrain
 * ESC returns to the main menu (pop delegate).
 */
public final class MapEditorSession {

    private MapEditorSession() {}

    // Shared editor state (forms <-> session). Simple singleton for this lightweight editor.
    private static final EditorState EDITOR_STATE = new EditorState();

    // Editor-wide pause state: gates animation driver when pause menu is open
    private static volatile boolean EDITOR_PAUSED = false;

    public static void setPaused(boolean paused) { EDITOR_PAUSED = paused; }

    public static void start(
            NetworkSelector network,
            GUI gui,
            int metersPerWorld,
            WorldGenerator generator,
            int gamespeed,
            com.oddlabs.tt.editor.ui.EditorState.EditorMode mode) {
        // Use the built-in loading progress form so all resource loading progress calls are valid
        com.oddlabs.tt.form.ProgressForm.setProgressForm(
                network,
                gui,
                new com.oddlabs.tt.form.LoadCallback() {
                    @Override
                    public UIRenderer load(GUIRoot clientRoot) {
                        // Prepare renderer queues and resources (these call ProgressForm.progress internally)
                        RenderQueues renderQueues = new RenderQueues();
                        com.oddlabs.tt.landscape.LandscapeResources landscapeResources =
                                World.loadCommon(renderQueues);
                        com.oddlabs.tt.model.RacesResources racesResources = World.loadInGame(renderQueues);

                        // Basic audio impl bound to this camera state
                        final CameraState camState = new CameraState();
                        AudioImplementation audioImpl = new AudioImplementation() {
                            public AbstractAudioPlayer newAudio(AudioParameters params) {
                                return AudioManager.getManager().newAudio(camState, params);
                            }
                        };

                        // Single-player editor setup
                        PlayerInfo[] infos = new PlayerInfo[] {
                            new PlayerInfo(0, com.oddlabs.tt.model.RacesResources.RACE_NATIVES, "Editor")
                        };
                        float[][] colors = new float[][] {Player.COLORS[0]};

                        int playersForGeneration = 6;
                        WorldInfo worldInfo =
                                generator.generate(playersForGeneration, Player.INITIAL_UNIT_COUNT, 0f);
                        WorldParameters worldParams =
                                new WorldParameters(
                                        gamespeed,
                                        "",
                                        Player.INITIAL_UNIT_COUNT,
                                        Player.DEFAULT_MAX_UNIT_COUNT);

                        final LandscapeRenderer[] lrHolder = new LandscapeRenderer[1];
                        final DefaultRenderer[] drHolder = new DefaultRenderer[1];
                        NotificationListener listener = new NotificationListener() {
                            public void gamespeedChanged(int speed) {}
                            public void playerGamespeedChanged() {}
                            public void newAttackNotification(com.oddlabs.tt.model.Selectable target) {}
                            public void newSelectableNotification(com.oddlabs.tt.model.Selectable target) {}
                            public void registerTarget(Target target) {}
                            public void unregisterTarget(Target target) {}
                            public void updateTreeLowDetail(StrictMatrix4f m, com.oddlabs.tt.landscape.TreeSupply t) {
                                if (drHolder[0] != null)
                                    drHolder[0].getTreeRenderer().getLowDetail().updateLowDetail(m, t);
                            }
                            public void patchesEdited(int x0, int y0, int x1, int y1) {
                                if (lrHolder[0] != null) lrHolder[0].patchesEdited(x0, y0, x1, y1);
                            }
                        };

                        World world =
                                World.newWorld(
                                        audioImpl,
                                        landscapeResources,
                                        racesResources,
                                        com.oddlabs.tt.landscape.LandscapeResources.loadTreeLowDetails(),
                                        listener,
                                        worldParams,
                                        worldInfo,
                                        generator.getTerrainType(),
                                        infos,
                                        colors);

                        Player local = world.getPlayers()[0];
                        Selection selection = new Selection(local);

                        // Build renderer + picker
                        LandscapeRenderer landscapeRenderer =
                                new LandscapeRenderer(
                                        world, worldInfo, clientRoot, world.getAnimationManagerRealTime());
                        lrHolder[0] = landscapeRenderer;
                        Picker picker =
                                new Picker(
                                        world.getAnimationManagerRealTime(),
                                        local,
                                        renderQueues,
                                        landscapeRenderer,
                                        selection);
                        UIRenderer uiRenderer =
                                drHolder[0] = new DefaultRenderer(
                                        new Cheat(),
                                        local,
                                        renderQueues,
                                        generator.getTerrainType(),
                                        worldInfo,
                                        landscapeRenderer,
                                        picker,
                                        selection,
                                        generator);

                        // Drive animations (LOD updates, timers) like WorldViewer does
                        final Animated animationDriver =
                                new Animated() {
                                    public void animate(float t) {
                                        if (!EDITOR_PAUSED) {
                                            world.getAnimationManagerRealTime().runAnimations(t);
                                        }
                                    }

                                    public void updateChecksum(com.oddlabs.tt.util.StateChecksum checksum) {}
                                };
                        LocalEventQueue.getQueue().getManager().registerAnimation(animationDriver);

                        // Camera + delegate
                        Camera camera = new SimpleEditorCamera(world, camState);
                        int terrainType = generator.getTerrainType();
            // Pass editor mode through
            EDITOR_STATE.setEditorMode(mode);

            EditorDelegate delegate =
                                new EditorDelegate(
                                        clientRoot,
                                        camera,
                                        world,
                                        landscapeRenderer,
                                        picker,
                                        animationDriver,
                                        terrainType);
                        clientRoot.pushDelegate(delegate);

                        // Match WorldViewer initialization for viewport sizing
                        clientRoot.displayChanged(LocalInput.getViewWidth(), LocalInput.getViewHeight());

                        return uiRenderer;
                    }
                });
    }

    // --- Minimal Editor Camera (pan/zoom with WASD/arrow keys + mouse wheel)
    // Adds middle-mouse drag look + smooth rotate/pitch keys for parity with game ---
    private static final class SimpleEditorCamera extends Camera {
        private static final int SCROLL_BUFFER = com.oddlabs.tt.camera.GameCamera.SCROLL_BUFFER;
        private static final float MAX_Z = com.oddlabs.tt.camera.GameCamera.MAX_Z;
        private static final float ZOOM_Z_DIR_MIN = -(float) StrictMath.tan(StrictMath.PI / 6);
        private static final float ZOOM_SPEED = 50f;
        private static final float SMOOTHNESS_MAP = 200f;
        private static final float MAP_THRESHOLD = .1f;
        private static final float MAP_TIME_FACTOR = 1000f;
        private static final float MAP_Z_FACTOR = 1.3f;
        private static final float ANGLE_DELTA = (float) (StrictMath.PI / 2); // match GameCamera
        private static final float DRAG_SCALE_HORIZ = .002f; // match FirstPersonCamera
        private static final float DRAG_SCALE_VERT = .002f;  // match FirstPersonCamera

        private float left_dir_x, left_dir_y;
        private float scroll_x, scroll_y;
        private float scrolling_x, scrolling_y;
        private boolean scroll_start = true;

    private float zoom_time;

        // Smooth rotate/pitch state (held keys)
        private boolean pitch_up;
        private boolean pitch_down;
        private boolean rotate_left;
        private boolean rotate_right;

        // Map mode state (mirrors MapCamera behavior)
        private static final int TO_MAP = 1;
        private static final int IN_MAP = 2;
        private static final int FROM_MAP = 3;
        private int map_mode = 0; // 0 = not in map transition/mode
        private float old_x, old_y, old_z, old_vert_angle;
        private float distance_to_landscape;

        SimpleEditorCamera(World world, CameraState state) {
            super(world.getHeightMap(), state);
            // Start near center, pitched like GameCamera
            float mid = world.getHeightMap().getMetersPerWorld() / 2f;
            float va = -45f * ((float) StrictMath.PI / 180f);
            float ha = -(float) StrictMath.PI / 2f;
            float z = world.getHeightMap().getNearestHeight(mid, mid) + 60f;
            getState().setCamera(mid, mid, z, va, ha);
            updateDirection();
            checkPosition();
        }

        protected void doAnimate(float dt) {
            doZoom(dt);
            doScroll(dt);
            doPitch(dt);
            doRotate(dt);
            doMapAnimate(dt);
            updateDirection();
        }

        public void keyPressed(KeyboardEvent e) {
            switch (e.getKeyCode()) {
                case Keyboard.KEY_W:
                case Keyboard.KEY_UP:
                    scrolling_y = 1f; // forward
                    break;
                case Keyboard.KEY_S:
                case Keyboard.KEY_DOWN:
                    scrolling_y = -1f; // backward
                    break;
                case Keyboard.KEY_A:
                case Keyboard.KEY_LEFT:
                    scrolling_x = -1f; // left
                    break;
                case Keyboard.KEY_D:
                case Keyboard.KEY_RIGHT:
                    scrolling_x = 1f; // right
                    break;
                // Smooth rotate/pitch like GameCamera (hold for continuous)
                case Keyboard.KEY_HOME:
                case Keyboard.KEY_NUMPAD8:
                    pitch_up = true;
                    break;
                case Keyboard.KEY_END:
                case Keyboard.KEY_NUMPAD2:
                    pitch_down = true;
                    break;
                case Keyboard.KEY_INSERT:
                case Keyboard.KEY_NUMPAD6:
                    rotate_right = true;
                    break;
                case Keyboard.KEY_DELETE:
                case Keyboard.KEY_NUMPAD4:
                    rotate_left = true;
                    break;
                // PageUp/PageDown emulate zoom wheel taps
                case Keyboard.KEY_PRIOR:   // PageUp
                case Keyboard.KEY_NUMPAD9:
                    mouseScrolled(-2);
                    break;
                case Keyboard.KEY_NEXT:    // PageDown
                case Keyboard.KEY_NUMPAD3:
                    mouseScrolled(2);
                    break;
                case Keyboard.KEY_SPACE:
                case Keyboard.KEY_NUMPAD5:
                    toggleMapMode();
                    break;
                default:
                    break;
            }
        }

        public void keyReleased(KeyboardEvent e) {
            switch (e.getKeyCode()) {
                case Keyboard.KEY_W:
                case Keyboard.KEY_UP:
                case Keyboard.KEY_S:
                case Keyboard.KEY_DOWN:
                    scrolling_y = 0f;
                    break;
                case Keyboard.KEY_A:
                case Keyboard.KEY_LEFT:
                case Keyboard.KEY_D:
                case Keyboard.KEY_RIGHT:
                    scrolling_x = 0f;
                    break;
                case Keyboard.KEY_HOME:
                case Keyboard.KEY_NUMPAD8:
                    pitch_up = false;
                    break;
                case Keyboard.KEY_END:
                case Keyboard.KEY_NUMPAD2:
                    pitch_down = false;
                    break;
                case Keyboard.KEY_INSERT:
                case Keyboard.KEY_NUMPAD6:
                    rotate_right = false;
                    break;
                case Keyboard.KEY_DELETE:
                case Keyboard.KEY_NUMPAD4:
                    rotate_left = false;
                    break;
                default:
                    break;
            }
        }

        public void mouseScrolled(int amount) {
            zoom_time += amount * .05f;
            if (zoom_time > .15f) zoom_time = .15f;
            else if (zoom_time < -.15f) zoom_time = -.15f;
        }

        private void updateDirection() {
            float ha = getState().getTargetHorizAngle();
            left_dir_x = -(float) StrictMath.sin(ha);
            left_dir_y = (float) StrictMath.cos(ha);
        }

        private void doZoom(float dt) {
            float zf = zoom_time * dt * ZOOM_SPEED * getState().getTargetZ();
            if (zf == 0f) return;
            float radius = (float) StrictMath.cos(getState().getTargetVertAngle());
            float dir_x = (float) StrictMath.cos(getState().getTargetHorizAngle()) * radius;
            float dir_y = (float) StrictMath.sin(getState().getTargetHorizAngle()) * radius;
            float dir_z = (float) StrictMath.sin(getState().getTargetVertAngle());
            if (dir_z > ZOOM_Z_DIR_MIN) {
                dir_z = ZOOM_Z_DIR_MIN;
                float inv = 1f / (float) StrictMath.sqrt(dir_x * dir_x + dir_y * dir_y + dir_z * dir_z);
                dir_x *= inv; dir_y *= inv; dir_z *= inv;
            }
            float nx = getState().getTargetX() + dir_x * zf;
            float ny = getState().getTargetY() + dir_y * zf;
            float nz = getState().getTargetZ() + dir_z * zf;
            // Bounds checks similar to GameCamera
            int mid = getHeightMap().getMetersPerWorld() / 2;
            float dx = nx - mid;
            float dy = ny - mid;
            float dist2 = dx * dx + dy * dy;
            if (dist2 < getHeightMap().getMetersPerWorld() * getHeightMap().getMetersPerWorld()
                    && nz < MAX_Z) {
                float va = getState().getTargetVertAngle();
                float ha = getState().getTargetHorizAngle();
                float bx = getState().getTargetX();
                float by = getState().getTargetY();
                float bz = getState().getTargetZ();
                getState().setCamera(nx, ny, nz, va, ha);
                if (bounce(getState().getTargetX(), getState().getTargetY(), getState().getTargetZ())) {
                    getState().setCamera(bx, by, bz, va, ha);
                }
                checkPosition();
            }
            // Dampen zoom_time toward zero
            if (zoom_time < 0f) zoom_time = StrictMath.min(0f, zoom_time + dt);
            else if (zoom_time > 0f) zoom_time = StrictMath.max(0f, zoom_time - dt);
        }

        private void doScroll(float dt) {
            // Edge scroll vector from mouse
            int x = LocalInput.getMouseX();
            int y = LocalInput.getMouseY();
            int lw = LocalInput.getViewWidth();
            int lh = LocalInput.getViewHeight();
            int left = SCROLL_BUFFER, top = SCROLL_BUFFER;
            int right = lw - 1 - SCROLL_BUFFER, bottom = lh - 1 - SCROLL_BUFFER;
            if (x < left || y < top || x > right || y > bottom) {
                if (scroll_start) scroll_start = false;
                scroll_x = (float) (x - lw / 2);
                scroll_y = (float) (y - lh / 2);
                float inv = 1f / (float) StrictMath.sqrt(scroll_x * scroll_x + scroll_y * scroll_y);
                scroll_x *= inv; scroll_y *= inv;
            } else {
                scroll_start = true;
                scroll_x = 0f; scroll_y = 0f;
            }

            // Prefer keyboard input if pressed; otherwise fall back to edge scroll
            boolean blocked = false;
            if (LocalInput.isKeyDown(Keyboard.KEY_LEFT)
                    && !LocalInput.isKeyDown(Keyboard.KEY_RIGHT)
                    && !blocked) scrolling_x = -1f;
            else if (LocalInput.isKeyDown(Keyboard.KEY_RIGHT)
                    && !LocalInput.isKeyDown(Keyboard.KEY_LEFT)
                    && !blocked) scrolling_x = 1f;
            else scrolling_x = scroll_x;

            if (LocalInput.isKeyDown(Keyboard.KEY_DOWN)
                    && !LocalInput.isKeyDown(Keyboard.KEY_UP)
                    && !blocked) scrolling_y = -1f;
            else if (LocalInput.isKeyDown(Keyboard.KEY_UP)
                    && !LocalInput.isKeyDown(Keyboard.KEY_DOWN)
                    && !blocked) scrolling_y = 1f;
            else scrolling_y = scroll_y;

            if (scrolling_x == 0f && scrolling_y == 0f) return;
            float scroll_speed = 60f;
            float sf = dt * scroll_speed;
            float nx = getState().getTargetX()
                    - (scrolling_x * left_dir_x + scrolling_y * -left_dir_y) * sf;
            float ny = getState().getTargetY()
                    - (scrolling_x * left_dir_y + scrolling_y * left_dir_x) * sf;
            float nz = getState().getTargetZ();
            float va = getState().getTargetVertAngle();
            float ha = getState().getTargetHorizAngle();
            getState().setCamera(nx, ny, nz, va, ha);
            checkPosition();
        }

        private void doPitch(float dt) {
            float x = getState().getTargetX();
            float y = getState().getTargetY();
            float z = getState().getTargetZ();
            float va = getState().getTargetVertAngle();
            float ha = getState().getTargetHorizAngle();
            boolean changed = false;
            if ((pitch_down && !com.oddlabs.tt.global.Settings.getSettings().invert_camera_pitch)
                    || (pitch_up && com.oddlabs.tt.global.Settings.getSettings().invert_camera_pitch)) {
                va -= dt * ANGLE_DELTA;
                changed = true;
            }
            if ((pitch_up && !com.oddlabs.tt.global.Settings.getSettings().invert_camera_pitch)
                    || (pitch_down && com.oddlabs.tt.global.Settings.getSettings().invert_camera_pitch)) {
                va += dt * ANGLE_DELTA;
                changed = true;
            }
            if (changed) {
                getState().setCamera(x, y, z, va, ha);
                checkPosition();
            }
        }

        private void doRotate(float dt) {
            if (!(rotate_left || rotate_right)) return;
            float x = getState().getTargetX();
            float y = getState().getTargetY();
            float z = getState().getTargetZ();
            float va = getState().getTargetVertAngle();
            float ha = getState().getTargetHorizAngle();
            float da = rotate_left ? -dt * ANGLE_DELTA : dt * ANGLE_DELTA;
            ha += da;
            getState().setCamera(x, y, z, va, ha);
            checkPosition();
        }

        // Support middle-mouse drag look, given relative mouse deltas from delegate
        public void rotateByMouseDelta(int rel_x, int rel_y) {
            if (rel_x == 0 && rel_y == 0) return;
            float x = getState().getTargetX();
            float y = getState().getTargetY();
            float z = getState().getTargetZ();
            float va = getState().getTargetVertAngle();
            float ha = getState().getTargetHorizAngle();
            ha -= rel_x * DRAG_SCALE_HORIZ;
            if (com.oddlabs.tt.global.Settings.getSettings().invert_camera_pitch)
                va -= rel_y * DRAG_SCALE_VERT;
            else
                va += rel_y * DRAG_SCALE_VERT;
            getState().setCamera(x, y, z, va, ha);
            checkPosition();
        }

        // --- Map mode helpers ---
        private void toggleMapMode() {
            // Lazily init map state snapshot
            if (map_mode == 0) {
                // entering TO_MAP
                float cx = getState().getTargetX();
                float cy = getState().getTargetY();
                float cz = getState().getTargetZ();
                old_x = cx;
                old_y = cy;
                old_z = cz;
                old_vert_angle = getState().getTargetVertAngle();
                // Distance to ground along current forward dir
                float[] target = new float[] {cx, cy};
                float dz = getHeightMap().getNearestHeight(target[0], target[1]) - cz;
                float dx = 0f;
                float dy = 0f;
                distance_to_landscape = (float) StrictMath.sqrt(dx * dx + dy * dy + dz * dz);
                setSmoothnessFactor(SMOOTHNESS_MAP);
                map_mode = TO_MAP;
            } else if (map_mode == TO_MAP || map_mode == IN_MAP) {
                map_mode = FROM_MAP;
            } else if (map_mode == FROM_MAP) {
                // interrupt return and go back to TO_MAP
                map_mode = TO_MAP;
            }
        }

        private void doMapAnimate(float t) {
            if (map_mode == 0) return;
            float factor =
                    t * 1000f
                            / StrictMath.max(
                                    t * 1000f,
                                    com.oddlabs.tt.global.Settings.getSettings().mapmode_delay
                                            * MAP_TIME_FACTOR);
            float dx, dy, dz, da;
            float map_x = getHeightMap().getMetersPerWorld() / 2f;
            float map_y = getHeightMap().getMetersPerWorld() / 2f;
            float map_z = getHeightMap().getMetersPerWorld() * MAP_Z_FACTOR;
            final float MIN_VERT_ANGLE = -(float) StrictMath.PI / 2f;

            switch (map_mode) {
                case TO_MAP:
                    dx = map_x - old_x;
                    dy = map_y - old_y;
                    dz = map_z - old_z;
                    {
                        float nx = getState().getTargetX() + dx * factor;
                        float ny = getState().getTargetY() + dy * factor;
                        float nz = getState().getTargetZ() + dz * factor;
                        float va = getState().getTargetVertAngle();
                        float ha = getState().getTargetHorizAngle();
                        getState().setCamera(nx, ny, nz, va, ha);
                    }
                    if (getState().getTargetZ() > map_z - MAP_THRESHOLD) {
                        float ha = getState().getTargetHorizAngle();
                        getState().setCamera(map_x, map_y, map_z, getState().getTargetVertAngle(), ha);
                        map_mode = IN_MAP;
                    }
                    da = MIN_VERT_ANGLE - old_vert_angle;
                    {
                        float x = getState().getTargetX();
                        float y = getState().getTargetY();
                        float z = getState().getTargetZ();
                        float va = getState().getTargetVertAngle() + da * factor;
                        float ha = getState().getTargetHorizAngle();
                        getState().setCamera(x, y, z, va, ha);
                    }
                    break;
                case IN_MAP:
                    // hold
                    break;
                case FROM_MAP:
                    dx = old_x - map_x;
                    dy = old_y - map_y;
                    dz = old_z - map_z;
                    {
                        float nx = getState().getTargetX() + dx * factor;
                        float ny = getState().getTargetY() + dy * factor;
                        float nz = getState().getTargetZ() + dz * factor;
                        float va = getState().getTargetVertAngle();
                        float ha = getState().getTargetHorizAngle();
                        getState().setCamera(nx, ny, nz, va, ha);
                    }
                    if (getState().getTargetZ() <= old_z) {
                        float ha = getState().getTargetHorizAngle();
                        getState().setCamera(old_x, old_y, old_z, old_vert_angle, ha);
                        checkPosition();
                        map_mode = 0; // done
                        break;
                    }
                    da = old_vert_angle - MIN_VERT_ANGLE;
                    {
                        float x = getState().getTargetX();
                        float y = getState().getTargetY();
                        float z = getState().getTargetZ();
                        float va = getState().getTargetVertAngle() + da * factor;
                        float ha = getState().getTargetHorizAngle();
                        getState().setCamera(x, y, z, va, ha);
                    }
                    break;
            }
        }
    }

    // --- Delegate with a simple height brush ---
    private static final class EditorDelegate extends com.oddlabs.tt.delegate.CameraDelegate implements Animated {
        private final World world;
        private final LandscapeRenderer landscapeRenderer;
        private final Picker picker;
        private final Animated extraAnimationDriver;
        private final int terrainType;

        private boolean leftDown = false;
        private boolean rightDown = false;

        // Elliptical brush parameters (meters)
        private float brushRadiusXM = 6f;
        private float brushRadiusYM = 6f;
        private float brushAngleRad = 0f; // rotation of ellipse in radians

        // Brush strength (meters per tick) and hardness falloff exponent
        private float brushStrengthM = 2.5f;  // increased default for stronger edits
        private float hardnessExp = 0.5f;     // 0.2 very soft .. 2.0 very hard

        private static final float MIN_RADIUS = 1f;
        private static final float MAX_RADIUS = 200f;
    // hardness bounds not used directly; omit to keep build clean

        // Mode handling
        private enum BrushMode { RAISE_LOWER, SMOOTH, FLATTEN }
        private BrushMode brushMode = BrushMode.RAISE_LOWER;
        private boolean qHeld = false;
        private Float flattenHeightRef = null; // captured on stroke start for FLATTEN

        // Stroke accumulation (no feedback loop until release)
        private java.util.HashMap<Long, Float> strokeAccum = new java.util.HashMap<Long, Float>();
        private java.util.HashMap<Long, Float> strokeBaseline =
                new java.util.HashMap<Long, Float>();
        private boolean strokeActive = false;
        private float strokeDir = 0f;

        // Track an overall bounding box (in grid units) of edited area during the stroke
        private boolean strokeHasBounds = false;
        private int strokeMinGX, strokeMinGY, strokeMaxGX, strokeMaxGY;

        // Resource brush state
        private enum ActiveTool { TERRAIN, RESOURCE }
        private ActiveTool activeTool = ActiveTool.TERRAIN;
        private enum ResourceType { ROCK, IRON, RUBBER, TREE_JUNGLE, TREE_PALM, TREE_OAK, TREE_PINE }
        private ResourceType resourceType = ResourceType.ROCK;

    // ------- Overlay tool state -------
    private enum OverlayLayer { WATER, DOCK, ACCESS, BUILD, RESOURCE, SLOPE }
    private enum OverlayMode { THRESHOLD, GRAYSCALE, HEAT }
    private boolean overlayActiveHeld = false; // true while T is held (for cycling)
    private boolean overlayTPressed = false;   // true between T down and up
    private boolean overlayTScrollUsed = false; // true if user scrolled while holding T
    private OverlayLayer overlayLayer = OverlayLayer.WATER;
    private OverlayMode overlayMode = OverlayMode.THRESHOLD;

        EditorDelegate(
                GUIRoot root,
                Camera camera,
                World world,
                LandscapeRenderer lr,
                Picker picker,
                Animated extraAnimationDriver,
                int terrainType) {
            super(root, camera);
            this.world = world;
            this.landscapeRenderer = lr;
            this.picker = picker;
            this.extraAnimationDriver = extraAnimationDriver;
            this.terrainType = terrainType;
            // Register for real-time ticks to drive brush application cadence
            world.getAnimationManagerRealTime().registerAnimation(this);
            getGUIRoot()
                    .getInfoPrinter()
                    .print(
                "Editor: Height[LMB/RMB]. Wheel: zoom. Ctrl+Wheel: size. Alt+Wheel: intensity. Q: Height tool (hold Q+Wheel cycles mode). W: Resource tool (hold W+Wheel cycles type). T: Toggle overlays (hold T+Wheel layer, Alt+T+Wheel mode). Space: map view toggle. RMB: Erase resources.");
        }

        public boolean canScroll() { return true; }

        // When this delegate is re-added (e.g., after closing pause menu),
        // re-register animations so input/brush updates resume.
        protected void doAdd() {
            super.doAdd();
            // Resume receiving world real-time animation ticks for the brush
            world.getAnimationManagerRealTime().registerAnimation(this);
            // Re-register the external animation driver (drives world animations)
            if (extraAnimationDriver != null)
                com.oddlabs.tt.event.LocalEventQueue.getQueue().getManager().registerAnimation(extraAnimationDriver);
        }

        protected void doRemove() {
            super.doRemove();
            world.getAnimationManagerRealTime().removeAnimation(this);
            if (extraAnimationDriver != null)
                LocalEventQueue.getQueue().getManager().removeAnimation(extraAnimationDriver);
        }

        private boolean mmbDown = false; // don't apply brush while middle mouse is down

        public void animate(float t) {
            if (!strokeActive) return;
            if (mmbDown) return;
            if (activeTool == ActiveTool.TERRAIN) applyBrush(strokeDir, t);
            else applyResourceBrush(t);
        }

        public void updateChecksum(com.oddlabs.tt.util.StateChecksum checksum) {}

        public void mousePressed(int button, int x, int y) {
            // Resource tool supports LMB (paint) and RMB (erase)
            if (button == 0) leftDown = true;
            if (button == 1) rightDown = true;
            if (button == LocalInput.MIDDLE_BUTTON) mmbDown = true;
            if (!strokeActive && (leftDown || rightDown)) {
                strokeActive = true;
                if (activeTool == ActiveTool.TERRAIN) {
                    strokeDir = leftDown ? 1f : -1f;
                    strokeAccum.clear();
                    strokeBaseline.clear();
                    strokeHasBounds = false;
                    // Capture flatten ref height at stroke start
                    if (brushMode == BrushMode.FLATTEN) {
                        LandscapeLocation hit = new LandscapeLocation();
                        if (picker.pickLocation(getCamera().getState(), hit)) {
                            int cx = com.oddlabs.tt.pathfinder.UnitGrid.toGridCoordinate(hit.x);
                            int cy = com.oddlabs.tt.pathfinder.UnitGrid.toGridCoordinate(hit.y);
                            flattenHeightRef = world.getHeightMap().getWrappedHeight(cx, cy);
                        }
                    }
                }
            }
        }

        public void mouseReleased(int button, int x, int y) {
            if (button == 0) leftDown = false;
            if (button == 1) rightDown = false;
            if (button == LocalInput.MIDDLE_BUTTON) mmbDown = false;
            if (!leftDown && !rightDown) {
                // Apply accumulated stroke in one go
                if (strokeActive && activeTool == ActiveTool.TERRAIN) applyStroke();
                strokeActive = false;
                flattenHeightRef = null;
                // Snap resources (trees, rocks, iron, rubber) inside the edited region to terrain height
                if (activeTool == ActiveTool.TERRAIN && strokeHasBounds) {
                    snapResourcesInGridRect(strokeMinGX, strokeMinGY, strokeMaxGX, strokeMaxGY);
                    // IMPORTANT: recompute grids (water/dock/access/build) for consistency with textures and placement
                    if (EDITOR_STATE.isAutoUpdatePlacementGrids()) {
                        try {
                            EditorGridRecalculator.recomputeROI(
                                    world,
                                    terrainType,
                                    strokeMinGX,
                                    strokeMinGY,
                                    strokeMaxGX,
                                    strokeMaxGY);
                        } catch (Throwable ignore) {}
                        // Also recompute resource placement validity for the edited region
                        try {
                            com.oddlabs.tt.editor.EditorResourceValidity.recomputeROI(
                                    world,
                                    strokeMinGX,
                                    strokeMinGY,
                                    strokeMaxGX,
                                    strokeMaxGY);
                        } catch (Throwable ignore) {}
                    }
                    // Remove any existing resources within ROI that are no longer valid
                    // according to the updated water/dock/access rules
                    removeInvalidResourcesInGridRect(strokeMinGX, strokeMinGY, strokeMaxGX, strokeMaxGY);
                    // After removals, refresh the placement validity grid once to reflect occupancy changes
                    if (EDITOR_STATE.isAutoUpdatePlacementGrids()) {
                        try {
                            com.oddlabs.tt.editor.EditorResourceValidity.recomputeROI(
                                    world,
                                    strokeMinGX,
                                    strokeMinGY,
                                    strokeMaxGX,
                                    strokeMaxGY);
                        } catch (Throwable ignore) {}
                    }
                    // Rebuild-from-scratch colormap reblend for edited ROI (base+materials+lighting+shadow+seabottom)
                    try {
                        EditorColormapReblender.reblendROIFromScratch(
                                world,
                                landscapeRenderer,
                                terrainType,
                                strokeMinGX,
                                strokeMinGY,
                                strokeMaxGX,
                                strokeMaxGY);
                    } catch (Throwable ignore) {}
                    strokeHasBounds = false;
                }
            }
            landscapeRenderer.endEdit();
        }

        public void mouseHeld(int button, int x, int y) {
            // Continuous apply via animate()
        }

        public void mouseScrolled(int amount) {
            // Brush parameter adjustments via modifier keys + optional Q/W hold
            boolean ctrl = LocalInput.isControlDownCurrently();
            boolean alt = LocalInput.isMenuDownCurrently();

            // Overlay cycling when holding T
            if (LocalInput.isKeyDown(Keyboard.KEY_T)) {
                overlayTScrollUsed = true;
                if (alt) {
                    // Cycle overlay mode
                    if (amount > 0) nextOverlayMode(); else if (amount < 0) prevOverlayMode();
                } else {
                    // Cycle overlay layer
                    if (amount > 0) nextOverlayLayer(); else if (amount < 0) prevOverlayLayer();
                }
                return;
            }

            // Cycle resource types while holding W (consistent with Q+Wheel for terrain modes)
            if (LocalInput.isKeyDown(Keyboard.KEY_W)) {
                if (amount > 0) cycleResourceType(1);
                else if (amount < 0) cycleResourceType(-1);
                return;
            }

            if (qHeld) {
                // Cycle brush modes with scroll while Q is held
                if (amount > 0) nextMode();
                else if (amount < 0) prevMode();
                return;
            }

            if (ctrl) {
                // Adjust overall size (both radii)
                float scale = 1f + 0.1f * StrictMath.signum(amount);
                if (scale <= 0f) scale = 0.1f;
                brushRadiusXM = clamp(brushRadiusXM * scale, MIN_RADIUS, MAX_RADIUS);
                brushRadiusYM = clamp(brushRadiusYM * scale, MIN_RADIUS, MAX_RADIUS);
                info("Size: " + (int) brushRadiusXM + "x" + (int) brushRadiusYM + "m");
                return;
            }

            if (alt) {
                // Adjust intensity/strength
                float minS = 0.05f, maxS = 5f;
                brushStrengthM = clamp(brushStrengthM + 0.05f * StrictMath.signum(amount), minS, maxS);
                info("Intensity: " + fmt(brushStrengthM));
                return;
            }

            // Default: delegate to camera for zoom
            getCamera().mouseScrolled(amount);
        }

        // 3D halo ring showing current brush footprint
        public void render3D(LandscapeRenderer renderer, RenderQueues queues) {
            LandscapeLocation hit = new LandscapeLocation();
            if (!picker.pickLocation(getCamera().getState(), hit)) return;

            float cx = hit.x;
            float cy = hit.y;
            float rx = brushRadiusXM;
            float ry = brushRadiusYM;
            float cosA = (float) StrictMath.cos(brushAngleRad);
            float sinA = (float) StrictMath.sin(brushAngleRad);

            // Choose color by mode
            float r = 1f, g = 1f, b = 0f, a = 0.85f; // default raise/lower
            switch (brushMode) {
                case SMOOTH: r = 0f; g = 1f; b = 1f; break;
                case FLATTEN: r = 1f; g = 0f; b = 1f; break;
                default: break;
            }

            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(r, g, b, a);
            GL11.glLineWidth(2f);

            int steps = 64;
            GL11.glBegin(GL11.GL_LINE_LOOP);
            for (int i = 0; i < steps; i++) {
                float tAng = (float) (2.0 * StrictMath.PI * i / steps);
                float ex = (float) StrictMath.cos(tAng) * rx;
                float ey = (float) StrictMath.sin(tAng) * ry;
                float dx = cosA * ex + sinA * ey;
                float dy = -sinA * ex + cosA * ey;
                float x = cx + dx;
                float y = cy + dy;
                float z = renderer.getHeightMap().getNearestHeight(x, y) + 0.03f; // slight lift
                GL11.glVertex3f(x, y, z);
            }
            GL11.glEnd();

            // Restore state
            // Draw pending stroke plus marks (preview) only for RAISE/LOWER
            if (brushMode == BrushMode.RAISE_LOWER && strokeActive && !strokeAccum.isEmpty()) {
                GL11.glLineWidth(2f);
                GL11.glBegin(GL11.GL_LINES);
                for (java.util.Map.Entry<Long, Float> e : strokeAccum.entrySet()) {
                    long k = e.getKey();
                    int gx = (int) (k >> 32);
                    int gy = (int) k;
                    float delta = e.getValue();
                    // cell center in world coords
                    float wx = gx * com.oddlabs.tt.landscape.HeightMap.METERS_PER_UNIT_GRID;
                    float wy = gy * com.oddlabs.tt.landscape.HeightMap.METERS_PER_UNIT_GRID;
                    // Predict resulting height using stroke baseline + accumulated delta
                    float base = strokeBaseline.containsKey(k)
                            ? strokeBaseline.get(k)
                            : renderer.getHeightMap().getNearestHeight(wx, wy);
                    float wz = base + delta + 0.05f;
                    // color by sign
                    if (delta >= 0f) GL11.glColor4f(0f, 1f, 0f, a);
                    else GL11.glColor4f(1f, 0f, 0f, a);
                    float len = 0.6f; // half-length of the plus arms
                    // horizontal arm
                    GL11.glVertex3f(wx - len, wy, wz);
                    GL11.glVertex3f(wx + len, wy, wz);
                    // vertical arm
                    GL11.glVertex3f(wx, wy - len, wz);
                    GL11.glVertex3f(wx, wy + len, wz);
                }
                GL11.glEnd();
            }

            // Restore state
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_TEXTURE_2D);

            // --- Debug overlays (drawn in world space as semi-transparent quads) ---
            if (shouldRenderOverlays()) {
                drawOverlay(renderer);
            }
        }

        // Enable middle-mouse drag camera look for editor
        public void mouseDragged(
                int button,
                int x,
                int y,
                int relative_x,
                int relative_y,
                int absolute_x,
                int absolute_y) {
            if (button == LocalInput.MIDDLE_BUTTON && getCamera() instanceof SimpleEditorCamera) {
                ((SimpleEditorCamera) getCamera()).rotateByMouseDelta(relative_x, relative_y);
                return;
            }
            super.mouseDragged(button, x, y, relative_x, relative_y, absolute_x, absolute_y);
        }

        protected void keyPressed(KeyboardEvent event) {
            if (event.getKeyCode() == Keyboard.KEY_ESCAPE) {
                // Open pause menu analogous to in-game: static camera snapshot + pause gating
        getGUIRoot()
            .pushDelegate(
                new EditorPauseMenu(
                    getGUIRoot(),
                    new StaticCamera(getCamera().getState())));
                return;
            }
            // Tool toggles: Q = Terrain (also hold to cycle modes), W = Resource. No H/F.
            if (event.getKeyCode() == Keyboard.KEY_Q) {
                qHeld = true;
                activeTool = ActiveTool.TERRAIN;
                info("Tool = HEIGHT");
            } else if (event.getKeyCode() == Keyboard.KEY_W) {
                activeTool = ActiveTool.RESOURCE;
                info("Tool = RESOURCE, Type = " + resourceType);
                // Don't forward to camera to avoid conflicting with camera forward movement
                return;
            } else if (event.getKeyCode() == Keyboard.KEY_T) {
                // Start temporary overlay display; defer toggling until release unless scrolled
                overlayActiveHeld = true;
                overlayTPressed = true;
                overlayTScrollUsed = false;
            }
            getCamera().keyPressed(event);
        }

        protected void keyReleased(KeyboardEvent event) {
            if (event.getKeyCode() == Keyboard.KEY_Q) qHeld = false;
            if (event.getKeyCode() == Keyboard.KEY_T) {
                overlayActiveHeld = false;
                if (overlayTPressed) {
                    if (overlayTScrollUsed) {
                        // User cycled while holding T: keep overlays enabled persistently
                        EDITOR_STATE.setOverlayMaster(true);
                    } else {
                        // Clean tap: toggle overlays on/off
                        boolean now = !EDITOR_STATE.isOverlayMaster();
                        EDITOR_STATE.setOverlayMaster(now);
                    }
                    getGUIRoot()
                            .getInfoPrinter()
                            .print(
                                    "Overlays: "
                                            + (EDITOR_STATE.isOverlayMaster() ? "ON" : "OFF")
                                            + " | Layer="
                                            + overlayLayer
                                            + " | Mode="
                                            + overlayMode);
                }
                overlayTPressed = false;
                overlayTScrollUsed = false;
            }
            getCamera().keyReleased(event);
        }

        private static long keyOf(int gx, int gy) {
            return (((long) gx) << 32) ^ (long) (gy & 0xffffffffL);
        }

        private void applyStroke() {
            com.oddlabs.tt.landscape.HeightMap hm = world.getHeightMap();
            for (java.util.Map.Entry<Long, Float> e : strokeAccum.entrySet()) {
                long k = e.getKey();
                int gx = (int) (k >> 32);
                int gy = (int) k;
                float base = strokeBaseline.getOrDefault(k, hm.getWrappedHeight(gx, gy));
                float nh = base + e.getValue();
                // Enforce editor height constraints on stroke apply
                hm.editHeight(gx, gy, clampHeightForEdit(hm, gx, gy, nh));
            }
            strokeAccum.clear();
            strokeBaseline.clear();
        }

        private void applyBrush(float dir, float dt) {
            LandscapeLocation hit = new LandscapeLocation();
            if (!picker.pickLocation(getCamera().getState(), hit)) return;
            com.oddlabs.tt.landscape.HeightMap hm = world.getHeightMap();
            int cx = com.oddlabs.tt.pathfinder.UnitGrid.toGridCoordinate(hit.x);
            int cy = com.oddlabs.tt.pathfinder.UnitGrid.toGridCoordinate(hit.y);

            int rGU = (int)
                    StrictMath.ceil(
                            StrictMath.max(brushRadiusXM, brushRadiusYM)
                                    / com.oddlabs.tt.landscape.HeightMap.METERS_PER_UNIT_GRID);
            // Expand stroke bounds to include this brush footprint rectangle
            expandStrokeBounds(cx - rGU, cy - rGU, cx + rGU, cy + rGU);
            float rx = brushRadiusXM;
            float ry = brushRadiusYM;
            float cosA = (float) StrictMath.cos(brushAngleRad);
            float sinA = (float) StrictMath.sin(brushAngleRad);
            for (int gy = cy - rGU; gy <= cy + rGU; gy++) {
                for (int gx = cx - rGU; gx <= cx + rGU; gx++) {
                    float dxm = (gx - cx) * com.oddlabs.tt.landscape.HeightMap.METERS_PER_UNIT_GRID;
                    float dym = (gy - cy) * com.oddlabs.tt.landscape.HeightMap.METERS_PER_UNIT_GRID;
                    // Rotate into brush-local space
                    float ldx = cosA * dxm + sinA * dym;
                    float ldy = -sinA * dxm + cosA * dym;
                    float norm = (ldx * ldx) / (rx * rx) + (ldy * ldy) / (ry * ry);
                    if (norm > 1f) continue;
                    float falloff = (float) StrictMath.pow(Math.max(0f, 1f - norm), hardnessExp);

                    long key = keyOf(gx, gy);

                    if (brushMode == BrushMode.RAISE_LOWER) {
                        // Accumulate deltas relative to stroke baseline; apply on release
                        float base = strokeBaseline.containsKey(key)
                                ? strokeBaseline.get(key)
                                : hm.getWrappedHeight(gx, gy);
                        if (!strokeBaseline.containsKey(key)) strokeBaseline.put(key, base);
                        float delta = brushStrengthM * dir * falloff * dt;
                        float acc = strokeAccum.getOrDefault(key, 0f);
                        strokeAccum.put(key, acc + delta);
                    } else {
                        // Live updates: compute from current height and apply immediately
                        float curr = hm.getWrappedHeight(gx, gy);
                        float target;
                        switch (brushMode) {
                            case SMOOTH: {
                                float avg = neighborhoodAverageCurrent(hm, gx, gy);
                                target = avg;
                                break;
                            }
                            case FLATTEN: {
                                target = (flattenHeightRef != null) ? flattenHeightRef : curr;
                                break;
                            }
                            default:
                                target = curr;
                        }
                        float delta = (target - curr) * brushStrengthM * (brushMode == BrushMode.SMOOTH ? 0.5f : 1f) * falloff * dt;
                        float nh = curr + delta;
                        // Enforce editor height constraints on live edits
                        hm.editHeight(gx, gy, clampHeightForEdit(hm, gx, gy, nh));
                    }
                }
            }
        }

        // --- Editor constraints helpers ---
        // Contract:
        // - Inputs: grid coords (possibly out-of-range), proposed height in meters
        // - Output: clamped height within [sea floor, MAX_HEIGHT], with edge cells forced to sea floor
        // - Notes: respects toroidal wrapping of the heightmap grid
        private static final float MAX_TERRAIN_HEIGHT_M = 50f; // user max
        private static final float SEA_FLOOR_HEIGHT_M = 0f;    // user min (seafloor)

        private float clampHeightForEdit(com.oddlabs.tt.landscape.HeightMap hm, int gx, int gy, float proposed) {
            // Wrap to valid grid index space for edge detection
            int n = hm.getGridUnitsPerWorld();
            int wx = (gx + n) & (n - 1);
            int wy = (gy + n) & (n - 1);

            // If on world edge, force to sea floor to avoid seams with seafloor geometry
            if (wx == 0 || wy == 0 || wx == n - 1 || wy == n - 1) {
                return SEA_FLOOR_HEIGHT_M;
            }

            // Clamp general terrain edits between seafloor and max
            if (proposed < SEA_FLOOR_HEIGHT_M) proposed = SEA_FLOOR_HEIGHT_M;
            if (proposed > MAX_TERRAIN_HEIGHT_M) proposed = MAX_TERRAIN_HEIGHT_M;
            return proposed;
        }

        private void expandStrokeBounds(int minGX, int minGY, int maxGX, int maxGY) {
            if (!strokeHasBounds) {
                strokeMinGX = minGX;
                strokeMinGY = minGY;
                strokeMaxGX = maxGX;
                strokeMaxGY = maxGY;
                strokeHasBounds = true;
            } else {
                if (minGX < strokeMinGX) strokeMinGX = minGX;
                if (minGY < strokeMinGY) strokeMinGY = minGY;
                if (maxGX > strokeMaxGX) strokeMaxGX = maxGX;
                if (maxGY > strokeMaxGY) strokeMaxGY = maxGY;
            }
        }

        // Vertically snap resources within the given grid-rect to the current terrain height
        private void snapResourcesInGridRect(int minGX, int minGY, int maxGX, int maxGY) {
            com.oddlabs.tt.pathfinder.UnitGrid ug = world.getUnitGrid();
            com.oddlabs.tt.landscape.HeightMap hm = world.getHeightMap();
            int gridSize = ug.getGridSize();
            // Clamp to valid grid indices
            int x0 = StrictMath.max(0, minGX);
            int y0 = StrictMath.max(0, minGY);
            int x1 = StrictMath.min(gridSize - 1, maxGX);
            int y1 = StrictMath.min(gridSize - 1, maxGY);

            for (int gy = y0; gy <= y1; gy++) {
                for (int gx = x0; gx <= x1; gx++) {
                    com.oddlabs.tt.pathfinder.Occupant occ = ug.getOccupant(gx, gy, com.oddlabs.tt.pathfinder.UnitGrid.LAND);
                    if (occ == null) continue;
                    try {
                        // Snap trees
                        if (occ instanceof com.oddlabs.tt.landscape.TreeSupply) {
                            com.oddlabs.tt.landscape.TreeSupply tree = (com.oddlabs.tt.landscape.TreeSupply) occ;
                            float nx = tree.getPositionX();
                            float ny = tree.getPositionY();
                            float nz = hm.getNearestHeight(nx, ny);
                            com.oddlabs.tt.util.StrictMatrix4f m = tree.getMatrix();
                            m.m32 = nz; // update world-space Z translation
                            // Update low-detail VBO to reflect new base transform
                            world.getNotificationListener().updateTreeLowDetail(m, tree);
                        }
                        // Snap sprite-based supplies (rocks, iron, rubber)
                        if (occ instanceof com.oddlabs.tt.model.Model) {
                            com.oddlabs.tt.model.Model model = (com.oddlabs.tt.model.Model) occ;
                            // Re-setting the same XY triggers reinsert() which recomputes Z
                            model.setPosition(model.getPositionX(), model.getPositionY());
                        }
                    } catch (Throwable t) {
                        // Be resilient: never let an editor snap failure break the session
                    }
                }
            }
        }

        // Remove any resources within the given grid-rect that are no longer valid
        // by editor rules after terrain changes (water/dock/access). This intentionally
        // ignores the occupancy component of placement validity to avoid self-invalidation.
        private void removeInvalidResourcesInGridRect(int minGX, int minGY, int maxGX, int maxGY) {
            com.oddlabs.tt.pathfinder.UnitGrid ug = world.getUnitGrid();
            com.oddlabs.tt.landscape.HeightMap hm = world.getHeightMap();
            int gridSize = ug.getGridSize();
            // Clamp to valid grid indices
            int x0 = StrictMath.max(0, minGX);
            int y0 = StrictMath.max(0, minGY);
            int x1 = StrictMath.min(gridSize - 1, maxGX);
            int y1 = StrictMath.min(gridSize - 1, maxGY);

            boolean[][] water = hm.getWaterGrid();
            boolean[][] dock = hm.getDockGrid();
            boolean[][] access = hm.getAccessGrid();

            int removedCount = 0;
            for (int gy = y0; gy <= y1; gy++) {
                for (int gx = x0; gx <= x1; gx++) {
                    boolean isWater = (water != null && water[gy][gx]);
                    boolean isDock = (dock != null && dock[gy][gx]);
                    boolean isAccessible = (access == null) || access[gy][gx];
                    boolean invalid = isWater || isDock || !isAccessible;
                    if (!invalid) continue;

                    com.oddlabs.tt.pathfinder.Occupant occ = ug.getOccupant(gx, gy, com.oddlabs.tt.pathfinder.UnitGrid.LAND);
                    if (occ == null) {
                        // Also handle legacy trees that aren't registered as real occupants
                        // when the grid cell was unreachable (StaticOccupant placeholder)
                        // by scanning the tree quadtree at this location.
                        float xw = com.oddlabs.tt.pathfinder.UnitGrid.coordinateFromGrid(gx);
                        float yw = com.oddlabs.tt.pathfinder.UnitGrid.coordinateFromGrid(gy);
                        com.oddlabs.tt.landscape.TreeLeaf leaf = findLeafForPosition(world.getTreeRoot(), xw, yw);
                        final com.oddlabs.tt.landscape.TreeSupply[] found = new com.oddlabs.tt.landscape.TreeSupply[1];
                        final int egx = gx;
                        final int egy = gy;
                        if (leaf != null) {
                            leaf.visitTrees(new com.oddlabs.tt.landscape.TreeNodeVisitor() {
                                public void visitLeaf(com.oddlabs.tt.landscape.TreeLeaf l) {}
                                public void visitNode(com.oddlabs.tt.landscape.TreeGroup g) {}
                                public void visitTree(com.oddlabs.tt.landscape.TreeSupply t) {
                                    if (!t.isHidden() && t.getGridX() == egx && t.getGridY() == egy) found[0] = t;
                                }
                            });
                        }
                        if (found[0] != null) {
                            found[0].editorHideOnly();
                            removedCount++;
                        }
                        continue;
                    }

                    try {
                        if (occ instanceof com.oddlabs.tt.landscape.TreeSupply) {
                            ((com.oddlabs.tt.landscape.TreeSupply) occ).editorHideAndUnoccupy();
                            removedCount++;
                        } else if (occ instanceof com.oddlabs.tt.model.SupplyModel) {
                            ((com.oddlabs.tt.model.SupplyModel) occ).editorRemoveNow();
                            removedCount++;
                        }
                        // Ignore StaticOccupant and unrelated occupants
                    } catch (Throwable t) {
                        // Keep editing session stable even if one removal fails
                    }
                }
            }
            if (removedCount > 0) {
                System.out.println("[Editor] Removed " + removedCount + " invalid resource(s) after terrain edit.");
            }
        }

        // baseline averaging removed (unused)

        private float neighborhoodAverageCurrent(
                com.oddlabs.tt.landscape.HeightMap hm, int gx, int gy) {
            int radius = 2;
            float sum = 0f;
            int count = 0;
            for (int y = gy - radius; y <= gy + radius; y++) {
                for (int x = gx - radius; x <= gx + radius; x++) {
                    sum += hm.getWrappedHeight(x, y);
                    count++;
                }
            }
            return count > 0 ? (sum / count) : hm.getWrappedHeight(gx, gy);
        }

        private float clamp(float v, float lo, float hi) {
            return StrictMath.max(lo, StrictMath.min(hi, v));
        }

        private String fmt(float f) {
            return String.format(java.util.Locale.ROOT, "%.2f", f);
        }

        private void info(String s) { getGUIRoot().getInfoPrinter().print("Brush: " + s); }

        private void nextMode() {
            switch (brushMode) {
                case RAISE_LOWER: brushMode = BrushMode.SMOOTH; break;
                case SMOOTH: brushMode = BrushMode.FLATTEN; break;
                case FLATTEN: brushMode = BrushMode.RAISE_LOWER; break;
            }
            info("Mode = " + brushMode);
        }

        private void prevMode() {
            switch (brushMode) {
                case RAISE_LOWER: brushMode = BrushMode.FLATTEN; break;
                case FLATTEN: brushMode = BrushMode.SMOOTH; break;
                case SMOOTH: brushMode = BrushMode.RAISE_LOWER; break;
            }
            info("Mode = " + brushMode);
        }

        // -------- Resource Brush Implementation --------
        private void applyResourceBrush(float dt) {
            LandscapeLocation hit = new LandscapeLocation();
            if (!picker.pickLocation(getCamera().getState(), hit)) return;
            com.oddlabs.tt.landscape.HeightMap hm = world.getHeightMap();
            com.oddlabs.tt.pathfinder.UnitGrid ug = world.getUnitGrid();
            int cx = com.oddlabs.tt.pathfinder.UnitGrid.toGridCoordinate(hit.x);
            int cy = com.oddlabs.tt.pathfinder.UnitGrid.toGridCoordinate(hit.y);

            int rGU = (int)
                    StrictMath.ceil(
                            StrictMath.max(brushRadiusXM, brushRadiusYM)
                                    / com.oddlabs.tt.landscape.HeightMap.METERS_PER_UNIT_GRID);
            float rx = brushRadiusXM;
            float ry = brushRadiusYM;
            float cosA = (float) StrictMath.cos(brushAngleRad);
            float sinA = (float) StrictMath.sin(brushAngleRad);

            // Intensity controls coverage: 1 -> 20%, 2 -> 40%, ..., 4 -> 80% (clamped)
            float coverage = (float) StrictMath.max(0f, StrictMath.min(1f, 0.2f * brushStrengthM));
            java.util.Random rnd = world.getRandom();
            boolean erase = rightDown && !leftDown;

            boolean debugPrintedThisStroke = false;
            for (int gy = cy - rGU; gy <= cy + rGU; gy++) {
                for (int gx = cx - rGU; gx <= cx + rGU; gx++) {
                    // Bounds check to avoid crashes at world edges
                    if (gx < 0 || gy < 0 || gx >= ug.getGridSize() || gy >= ug.getGridSize()) continue;
                    float dxm = (gx - cx) * com.oddlabs.tt.landscape.HeightMap.METERS_PER_UNIT_GRID;
                    float dym = (gy - cy) * com.oddlabs.tt.landscape.HeightMap.METERS_PER_UNIT_GRID;
                    float ldx = cosA * dxm + sinA * dym;
                    float ldy = -sinA * dxm + cosA * dym;
                    float norm = (ldx * ldx) / (rx * rx) + (ldy * ldy) / (ry * ry);
                    if (norm > 1f) continue;
                    // Erasing should still work anywhere; placement uses live validity grid
                    if (!erase) {
                        if (rnd.nextFloat() > coverage) continue;
                        // Check the editor placement validity grid (not water/dock, accessible, unoccupied by real obj)
                        boolean valid = com.oddlabs.tt.editor.EditorResourceValidity.isValid(world, gx, gy);
                        if (!valid) {
                            // For the center cell only, print diagnostics once per stroke
                            if (!debugPrintedThisStroke && gx == cx && gy == cy) {
                                boolean[][] water = hm.getWaterGrid();
                                boolean[][] dock = hm.getDockGrid();
                                boolean[][] access = hm.getAccessGrid();
                                boolean w = water != null && water[gy][gx];
                                boolean d = dock != null && dock[gy][gx];
                                boolean a = access != null && access[gy][gx];
                                com.oddlabs.tt.pathfinder.Occupant occDbg = ug.getOccupant(gx, gy, com.oddlabs.tt.pathfinder.UnitGrid.LAND);
                                String occStr = (occDbg == null) ? "null" : occDbg.getClass().getSimpleName();
                                getGUIRoot().getInfoPrinter().print(
                                        "Place FAIL: invalid cell gx=" + gx + ", gy=" + gy
                                                + " | water=" + w + ", dock=" + d + ", access=" + a
                                                + ", occ=" + occStr);
                                System.out.println("[Editor] Placement invalid: gx=" + gx + ", gy=" + gy
                                        + ", water=" + w + ", dock=" + d + ", access=" + a + ", occ=" + occStr);
                                debugPrintedThisStroke = true;
                            }
                            continue;
                        }
                        // Trees no longer apply an extra slope filter; placement validity already
                        // encodes the editor's rules (water/dock/access/occupancy).
                    }
                    if (erase) {
                        com.oddlabs.tt.pathfinder.Occupant occ =
                                ug.getOccupant(gx, gy, com.oddlabs.tt.pathfinder.UnitGrid.LAND);
                        if (occ instanceof com.oddlabs.tt.landscape.TreeSupply) {
                            ((com.oddlabs.tt.landscape.TreeSupply) occ).editorHideAndUnoccupy();
                            // Update placement validity for this tile now that occupancy changed
                            if (EDITOR_STATE.isAutoUpdatePlacementGrids()) {
                                try { com.oddlabs.tt.editor.EditorResourceValidity.recomputeROI(world, gx, gy, gx, gy); } catch (Throwable ignore) {}
                            }
                        } else if (occ instanceof com.oddlabs.tt.model.SupplyModel) {
                            ((com.oddlabs.tt.model.SupplyModel) occ).editorRemoveNow();
                            if (EDITOR_STATE.isAutoUpdatePlacementGrids()) {
                                try { com.oddlabs.tt.editor.EditorResourceValidity.recomputeROI(world, gx, gy, gx, gy); } catch (Throwable ignore) {}
                            }
                        } else if (occ instanceof com.oddlabs.tt.pathfinder.StaticOccupant) {
                            // Legacy case: tree was inserted when cell was marked unreachable and
                            // never registered as grid occupant. Try to find and hide it.
                            float xw = com.oddlabs.tt.pathfinder.UnitGrid.coordinateFromGrid(gx);
                            float yw = com.oddlabs.tt.pathfinder.UnitGrid.coordinateFromGrid(gy);
                            com.oddlabs.tt.landscape.TreeLeaf leaf = findLeafForPosition(world.getTreeRoot(), xw, yw);
                            final com.oddlabs.tt.landscape.TreeSupply[] found = new com.oddlabs.tt.landscape.TreeSupply[1];
                            final int egx = gx;
                            final int egy = gy;
                            if (leaf != null) {
                                leaf.visitTrees(new com.oddlabs.tt.landscape.TreeNodeVisitor() {
                                    public void visitLeaf(com.oddlabs.tt.landscape.TreeLeaf l) {}
                                    public void visitNode(com.oddlabs.tt.landscape.TreeGroup g) {}
                                    public void visitTree(com.oddlabs.tt.landscape.TreeSupply t) {
                                        if (!t.isHidden() && t.getGridX() == egx && t.getGridY() == egy) found[0] = t;
                                    }
                                });
                            }
                            if (found[0] != null) {
                                found[0].editorHideOnly();
                                if (!debugPrintedThisStroke && gx == cx && gy == cy) {
                                    getGUIRoot().getInfoPrinter().print("Erased legacy tree at gx=" + gx + ", gy=" + gy);
                                    System.out.println("[Editor] Erased legacy tree at gx=" + gx + ", gy=" + gy);
                                    debugPrintedThisStroke = true;
                                }
                            } else {
                                if (!debugPrintedThisStroke && gx == cx && gy == cy) {
                                    getGUIRoot().getInfoPrinter().print("Erase FAIL: legacy static occupant, no tree found at gx=" + gx + ", gy=" + gy);
                                    System.out.println("[Editor] Erase FAIL: static occupant, no tree found at gx=" + gx + ", gy=" + gy);
                                    debugPrintedThisStroke = true;
                                }
                            }
                        }
                    } else {
                        // Only block if occupied by a real object; ignore RegionBuilder's StaticOccupant
                        com.oddlabs.tt.pathfinder.Occupant occHere =
                                ug.getOccupant(gx, gy, com.oddlabs.tt.pathfinder.UnitGrid.LAND);
                        if (occHere == null || occHere instanceof com.oddlabs.tt.pathfinder.StaticOccupant) {
                            placeResourceAt(gx, gy);
                            if (!debugPrintedThisStroke && gx == cx && gy == cy) {
                                getGUIRoot().getInfoPrinter().print("Placed " + resourceType + " at gx=" + gx + ", gy=" + gy);
                                System.out.println("[Editor] Placed " + resourceType + " at gx=" + gx + ", gy=" + gy);
                                debugPrintedThisStroke = true;
                            }
                            // Update placement validity to reflect new occupancy
                            if (EDITOR_STATE.isAutoUpdatePlacementGrids()) {
                                try { com.oddlabs.tt.editor.EditorResourceValidity.recomputeROI(world, gx, gy, gx, gy); } catch (Throwable ignore) {}
                            }
                        }
                        else {
                            if (!debugPrintedThisStroke && gx == cx && gy == cy) {
                                getGUIRoot().getInfoPrinter().print(
                                        "Place FAIL: occupied by " + occHere.getClass().getSimpleName()
                                                + " at gx=" + gx + ", gy=" + gy);
                                System.out.println("[Editor] Placement fail: occupied by " + occHere.getClass().getSimpleName()
                                        + " at gx=" + gx + ", gy=" + gy);
                                debugPrintedThisStroke = true;
                            }
                        }
                    }
                }
            }
        }

        // Trees follow the same placement validity as other resources; no extra slope filter.

        private void placeResourceAt(int grid_x, int grid_y) {
            switch (resourceType) {
                case ROCK:
                    placeRock(grid_x, grid_y);
                    break;
                case IRON:
                    placeIron(grid_x, grid_y);
                    break;
                case RUBBER:
                    placeRubber(grid_x, grid_y);
                    break;
                case TREE_JUNGLE:
                    placeTree(grid_x, grid_y, com.oddlabs.tt.landscape.AbstractTreeGroup.TREE_INDEX);
                    break;
                case TREE_PALM:
                    placeTree(grid_x, grid_y, com.oddlabs.tt.landscape.AbstractTreeGroup.PALMTREE_INDEX);
                    break;
                case TREE_OAK:
                    placeTree(grid_x, grid_y, com.oddlabs.tt.landscape.AbstractTreeGroup.OAKTREE_INDEX);
                    break;
                case TREE_PINE:
                    placeTree(grid_x, grid_y, com.oddlabs.tt.landscape.AbstractTreeGroup.PINETREE_INDEX);
                    break;
            }
        }

        private void placeRock(int grid_x, int grid_y) {
            com.oddlabs.tt.render.SpriteKey[] sprites = world.getLandscapeResources().getRockFragments();
            int idx = world.getRandom().nextInt(sprites.length);
            float x = com.oddlabs.tt.pathfinder.UnitGrid.coordinateFromGrid(grid_x) + (world.getRandom().nextFloat() - .5f);
            float y = com.oddlabs.tt.pathfinder.UnitGrid.coordinateFromGrid(grid_y) + (world.getRandom().nextFloat() - .5f);
            float rot = world.getRandom().nextFloat() * 360f;
            new com.oddlabs.tt.model.RockSupply(world, sprites[idx], 2f, grid_x, grid_y, x, y, rot, true);
        }

        private void placeIron(int grid_x, int grid_y) {
            com.oddlabs.tt.render.SpriteKey[] sprites = world.getLandscapeResources().getIronFragments();
            int idx = world.getRandom().nextInt(sprites.length);
            float x = com.oddlabs.tt.pathfinder.UnitGrid.coordinateFromGrid(grid_x) + (world.getRandom().nextFloat() - .5f);
            float y = com.oddlabs.tt.pathfinder.UnitGrid.coordinateFromGrid(grid_y) + (world.getRandom().nextFloat() - .5f);
            float rot = world.getRandom().nextFloat() * 360f;
            new com.oddlabs.tt.model.IronSupply(world, sprites[idx], 2f, grid_x, grid_y, x, y, rot, true);
        }

        private void placeRubber(int grid_x, int grid_y) {
            // Place a single rubber supply (chicken) without a group
            com.oddlabs.tt.render.SpriteKey sprite = world.getLandscapeResources().getChicken();
            float x = com.oddlabs.tt.pathfinder.UnitGrid.coordinateFromGrid(grid_x);
            float y = com.oddlabs.tt.pathfinder.UnitGrid.coordinateFromGrid(grid_y);
            com.oddlabs.tt.model.RubberSupply s =
                    new com.oddlabs.tt.model.RubberSupply(
                            world, sprite, 2f, grid_x, grid_y, x, y, 0f, null, x, y);
            s.setStationary(true);
            new com.oddlabs.tt.model.SupplySpawnAnimation(s, 2f);
        }

        private void placeTree(int grid_x, int grid_y, int tree_type_index) {
            float tree_x = com.oddlabs.tt.pathfinder.UnitGrid.coordinateFromGrid(grid_x)
                    + (world.getRandom().nextFloat() - .5f);
            float tree_y = com.oddlabs.tt.pathfinder.UnitGrid.coordinateFromGrid(grid_y)
                    + (world.getRandom().nextFloat() - .5f);
            // Build transform similar to world generation
            com.oddlabs.tt.util.StrictMatrix4f m = new com.oddlabs.tt.util.StrictMatrix4f();
            com.oddlabs.tt.util.StrictMatrix4f m2 = new com.oddlabs.tt.util.StrictMatrix4f();
            com.oddlabs.tt.util.StrictVector3f v = new com.oddlabs.tt.util.StrictVector3f();
            m.setIdentity();
            // Match worldgen ranges: jungle ~[0.75..1.0], others ~[1.0..1.5]
            float scale_factor = (tree_type_index == com.oddlabs.tt.landscape.AbstractTreeGroup.TREE_INDEX) ? 0.25f : 0.5f;
            float min_size = (tree_type_index == com.oddlabs.tt.landscape.AbstractTreeGroup.TREE_INDEX) ? 0.75f : 1.0f;
            float scale_base = world.getRandom().nextFloat() * scale_factor + min_size;
            float sx = scale_base + world.getRandom().nextFloat() * 0.2f - 0.1f;
            float sy = scale_base + world.getRandom().nextFloat() * 0.2f - 0.1f;
            float sz = scale_base + world.getRandom().nextFloat() * 0.2f - 0.1f;
            v.set(sx, sy, sz);
            m.scale(v);
            v.set(0f, 0f, 1f);
            m.rotate(world.getRandom().nextFloat() * 360f, v);
            m2.setIdentity();
            v.set(tree_x, tree_y, world.getHeightMap().getNearestHeight(tree_x, tree_y));
            m2.translate(v);
            com.oddlabs.tt.util.StrictMatrix4f.mul(m2, m, m);

            // Choose tree grid footprint and radius (match worldgen defaults)
            int grid_size = (tree_type_index == com.oddlabs.tt.landscape.AbstractTreeGroup.PALMTREE_INDEX
                                || tree_type_index == com.oddlabs.tt.landscape.AbstractTreeGroup.PINETREE_INDEX)
                                    ? 1 : 3;
            float radius = (tree_type_index == com.oddlabs.tt.landscape.AbstractTreeGroup.PALMTREE_INDEX
                                || tree_type_index == com.oddlabs.tt.landscape.AbstractTreeGroup.PINETREE_INDEX)
                                    ? 1.6f : 2.3f;

            // Only require the center tile not be occupied by a real object
            // Ignore RegionBuilder's StaticOccupant markers
            com.oddlabs.tt.pathfinder.UnitGrid ug = world.getUnitGrid();
            com.oddlabs.tt.pathfinder.Occupant occ =
                    ug.getOccupant(grid_x, grid_y, com.oddlabs.tt.pathfinder.UnitGrid.LAND);
            if (occ != null && !(occ instanceof com.oddlabs.tt.pathfinder.StaticOccupant)) {
                getGUIRoot().getInfoPrinter().print(
                        "Tree place FAIL: occupied by " + occ.getClass().getSimpleName()
                                + " at gx=" + grid_x + ", gy=" + grid_y);
                System.out.println("[Editor] Tree place fail: occupied by " + occ.getClass().getSimpleName()
                        + " at gx=" + grid_x + ", gy=" + grid_y);
                return;
            }

            // Load low-detail vertices just for initial bounds computation
            com.oddlabs.geometry.LowDetailModel[] lows = com.oddlabs.tt.landscape.LandscapeResources.loadTreeLowDetails();
            float[] verts = lows[tree_type_index].getVertices();

            // Find destination leaf and insert
            com.oddlabs.tt.landscape.TreeLeaf leaf = findLeafForPosition(world.getTreeRoot(), tree_x, tree_y);
            if (leaf == null) {
                getGUIRoot().getInfoPrinter().print(
                        "Tree place FAIL: no tree leaf found at x=" + tree_x + ", y=" + tree_y);
                System.out.println("[Editor] Tree place fail: no leaf for x=" + tree_x + ", y=" + tree_y);
                return;
            }
            com.oddlabs.tt.landscape.TreeSupply t = new com.oddlabs.tt.landscape.TreeSupply(
                    world,
                    leaf,
                    tree_x,
                    tree_y,
                    grid_x,
                    grid_y,
                    grid_size,
                    radius,
                    m,
                    tree_type_index,
                    verts);
            leaf.insertTree(t);
            // Optional: immediate render state is fine; low-detail is not used for editor
        }

        private com.oddlabs.tt.landscape.TreeLeaf findLeafForPosition(com.oddlabs.tt.landscape.AbstractTreeGroup root, float x, float y) {
            int size = world.getHeightMap().getMetersPerWorld();
            int cx = 0, cy = 0;
            com.oddlabs.tt.landscape.AbstractTreeGroup node = root;
            while (node instanceof com.oddlabs.tt.landscape.TreeGroup) {
                size >>= 1;
                com.oddlabs.tt.landscape.TreeGroup g = (com.oddlabs.tt.landscape.TreeGroup) node;
                if (x < cx + size) {
                    if (y < cy + size) {
                        node = g.getChild0();
                    } else {
                        cy += size;
                        node = g.getChild2();
                    }
                } else {
                    if (y < cy + size) {
                        cx += size;
                        node = g.getChild1();
                    } else {
                        cx += size;
                        cy += size;
                        node = g.getChild3();
                    }
                }
            }
            return (node instanceof com.oddlabs.tt.landscape.TreeLeaf) ? (com.oddlabs.tt.landscape.TreeLeaf) node : null;
        }

        private void cycleResourceType(int dir) {
            ResourceType[] vals = ResourceType.values();
            int idx = resourceType.ordinal();
            idx = (idx + dir + vals.length) % vals.length;
            resourceType = vals[idx];
            info("Resource = " + resourceType);
        }

        private boolean shouldRenderOverlays() {
            // Master toggle enables overlays. Holding T also temporarily enables for quick peek.
            boolean master = EDITOR_STATE.isOverlayMaster();
            if (!master && !overlayActiveHeld) return false;
            // Must have at least one layer enabled
            return EDITOR_STATE.isOverlayWater()
                    || EDITOR_STATE.isOverlayAccess()
                    || EDITOR_STATE.isOverlaySlope()
                    || EDITOR_STATE.isOverlayResource();
        }

        private void nextOverlayLayer() {
            OverlayLayer[] v = OverlayLayer.values();
            int idx = overlayLayer.ordinal();
            for (int i = 1; i <= v.length; i++) {
                OverlayLayer candidate = v[(idx + i) % v.length];
                if (isLayerEnabled(candidate)) { overlayLayer = candidate; break; }
            }
            getGUIRoot().getInfoPrinter().print("Overlay Layer: " + overlayLayer);
        }

        private void prevOverlayLayer() {
            OverlayLayer[] v = OverlayLayer.values();
            int idx = overlayLayer.ordinal();
            for (int i = 1; i <= v.length; i++) {
                OverlayLayer candidate = v[(idx - i + v.length) % v.length];
                if (isLayerEnabled(candidate)) { overlayLayer = candidate; break; }
            }
            getGUIRoot().getInfoPrinter().print("Overlay Layer: " + overlayLayer);
        }

        private boolean isLayerEnabled(OverlayLayer l) {
            switch (l) {
                case WATER: return EDITOR_STATE.isOverlayWater();
                case ACCESS: return EDITOR_STATE.isOverlayAccess();
                case SLOPE: return EDITOR_STATE.isOverlaySlope();
                case DOCK: return true; // no dedicated toggle yet
                case BUILD: return true; // no dedicated toggle yet
                case RESOURCE: return EDITOR_STATE.isOverlayResource();
            }
            return true;
        }

        private void nextOverlayMode() {
            switch (overlayMode) {
                case THRESHOLD:
                    overlayMode = OverlayMode.GRAYSCALE;
                    break;
                case GRAYSCALE:
                    overlayMode = OverlayMode.HEAT; // placeholder visual
                    break;
                case HEAT:
                    overlayMode = OverlayMode.THRESHOLD;
                    break;
            }
            getGUIRoot().getInfoPrinter().print("Overlay Mode: " + overlayMode);
        }

        private void prevOverlayMode() {
            // reverse cycle
            switch (overlayMode) {
                case THRESHOLD:
                    overlayMode = OverlayMode.HEAT;
                    break;
                case HEAT:
                    overlayMode = OverlayMode.GRAYSCALE;
                    break;
                case GRAYSCALE:
                    overlayMode = OverlayMode.THRESHOLD;
                    break;
            }
            getGUIRoot().getInfoPrinter().print("Overlay Mode: " + overlayMode);
        }

        private void drawOverlay(LandscapeRenderer renderer) {
            com.oddlabs.tt.landscape.HeightMap hm = renderer.getHeightMap();
            int size = hm.getGridUnitsPerWorld();
            float cell = com.oddlabs.tt.landscape.HeightMap.METERS_PER_UNIT_GRID;

            // Conservative coverage: draw overlay for the whole map while enabled.
            // This avoids view-frustum underestimation artifacts and ensures all visible tiles render.
            int x0 = 0;
            int y0 = 0;
            int x1 = size - 1;
            int y1 = size - 1;

            boolean[][] water = hm.getWaterGrid();
            boolean[][] dock = hm.getDockGrid();
            boolean[][] access = hm.getAccessGrid();
            byte[][] build = hm.getBuildGrid();
            boolean[][] place = com.oddlabs.tt.editor.EditorResourceValidity.getPlacementGrid(world);

            GL11.glDisable(GL11.GL_TEXTURE_2D);
            // Draw overlays on top of terrain to avoid z-fighting gaps between tiles
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            for (int gy = y0; gy <= y1; gy++) {
                for (int gx = x0; gx <= x1; gx++) {
                    float wx = gx * cell;
                    float wy = gy * cell;
                    float h = hm.getWrappedHeight(gx, gy);
                    // choose value based on layer
                    float value = 0f;
                    float r=0f,g=0f,b=0f,a=0.35f;
                    switch (overlayLayer) {
                        case WATER:
                            value = (water != null && water[gy][gx]) ? 1f : 0f;
                            r=0f; g=0.4f; b=1f; break;
                        case DOCK:
                            value = (dock != null && dock[gy][gx]) ? 1f : 0f;
                            r=0f; g=1f; b=1f; break;
                        case ACCESS:
                            value = (access != null && access[gy][gx]) ? 1f : 0f;
                            r=0.1f; g=1f; b=0.1f; break;
                        case BUILD:
                            value = (build != null && build[gy][gx] != 0) ? 1f : 0f;
                            r=1f; g=1f; b=0f; break;
                        case RESOURCE:
                            value = (place != null && place[gy][gx]) ? 1f : 0f;
                            r=1f; g=0.5f; b=0f; break;
                        case SLOPE:
                            // simple normalized slope approximation using neighbors
                            float hR = hm.getWrappedHeight(gx+1, gy);
                            float hU = hm.getWrappedHeight(gx, gy+1);
                            float sx = StrictMath.abs(hR - h) / cell;
                            float sy = StrictMath.abs(hU - h) / cell;
                            value = (float) StrictMath.min(1f, StrictMath.hypot(sx, sy) * 0.5f);
                            r=1f; g=0f; b=0f; break;
                    }
                    float alpha = a;
                    switch (overlayMode) {
                        case GRAYSCALE:
                            r = g = b = value;
                            alpha = 0.5f;
                            break;
                        case THRESHOLD:
                            if (value < 0.5f) continue;
                            break;
                        case HEAT:
                            // Placeholder: same as grayscale until heatmap is implemented
                            r = g = b = value;
                            alpha = 0.5f;
                            break;
                    }
                    // Draw as a filled quad at cell corners, sampling each corner height to avoid gaps
                    float z00 = hm.getNearestHeight(wx, wy) + 0.02f;
                    float z10 = hm.getNearestHeight(wx + cell, wy) + 0.02f;
                    float z11 = hm.getNearestHeight(wx + cell, wy + cell) + 0.02f;
                    float z01 = hm.getNearestHeight(wx, wy + cell) + 0.02f;
                    GL11.glColor4f(r, g, b, alpha);
                    GL11.glBegin(GL11.GL_QUADS);
                    GL11.glVertex3f(wx, wy, z00);
                    GL11.glVertex3f(wx+cell, wy, z10);
                    GL11.glVertex3f(wx+cell, wy+cell, z11);
                    GL11.glVertex3f(wx, wy+cell, z01);
                    GL11.glEnd();
                }
            }

            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDepthMask(true);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
        }
    }

    // -------- Overlay API for forms --------
    public EditorState getEditorState() { return EDITOR_STATE; }

    public void applyOverlaySelection() {
        // For now, nothing to precompute; drawing reads flags directly.
        // Status message will be shown on next overlay interaction.
    }
}
