package com.oddlabs.tt.editor;

import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.audio.AbstractAudioPlayer;
import com.oddlabs.tt.audio.AudioManager;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.camera.Camera;
import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.camera.StaticCamera;
import com.oddlabs.tt.camera.GameCamera;
import com.oddlabs.tt.camera.MapCamera;
import com.oddlabs.tt.camera.MapModeHost;
import com.oddlabs.tt.gui.GUI;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.KeyboardEvent;
import com.oddlabs.tt.gui.LocalInput;
import com.oddlabs.tt.gui.BackgroundLabelBox;
import com.oddlabs.tt.gui.LabelBox;
import com.oddlabs.tt.gui.Skin;
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
// import removed: com.oddlabs.procedural.Channel
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Minimal editor runtime: creates a world, switches to an in-world renderer,
 * and provides a basic height brush controlled by mouse:
 * - Left mouse: raise terrain within a circular brush
 * - Right mouse: lower terrain
 * ESC returns to the main menu (pop delegate).
 */

public final class MapEditorSession {

    // Shared editor state (forms <-> session). Simple singleton for this lightweight editor.
    private static final EditorState EDITOR_STATE = new EditorState();

    // Editor-wide pause state: gates animation driver when pause menu is open
    private static volatile boolean EDITOR_PAUSED = false;

    // Keep a reference to the network used to launch the editor so UI can return to main menu
    private static NetworkSelector EDITOR_NETWORK;

    // Prevent overlapping editor restarts while a load is in-flight
    private static final AtomicBoolean LOADING = new AtomicBoolean(false);

    public static void setPaused(boolean paused) { EDITOR_PAUSED = paused; }

    public static NetworkSelector getEditorNetwork() { return EDITOR_NETWORK; }

    public static void start(
            NetworkSelector network,
            GUI gui,
            int metersPerWorld,
            WorldGenerator generator,
            int gamespeed,
            com.oddlabs.tt.editor.ui.EditorState.EditorMode mode) {
        // Remember network selector for returning to the main menu from pause menu
        EDITOR_NETWORK = network;

        // Guard against re-entrant starts while a previous load is in-flight
        if (!LOADING.compareAndSet(false, true)) {
            System.err.println("[EditorStart] Ignoring re-entrant start; load already in progress.");
            try {
                if (gui != null && gui.getGUIRoot() != null && gui.getGUIRoot().getInfoPrinter() != null) {
                    gui.getGUIRoot().getInfoPrinter().print("Load already in progress...");
                }
            } catch (Throwable ignore) {}
            return;
        }

    // Use the built-in loading progress form so all resource loading progress calls are valid
    try {
        com.oddlabs.tt.form.ProgressForm.setProgressForm(
            network,
            gui,
            new com.oddlabs.tt.form.LoadCallback() {
                    @Override
                    public UIRenderer load(GUIRoot clientRoot) {
                        try {
                            // Prepare renderer queues and resources (these call ProgressForm.progress internally)
                            System.err.println("[EditorStart] Begin load: create RenderQueues");
                            RenderQueues renderQueues = new RenderQueues();
                            System.err.println("[EditorStart] Load common resources");
                            com.oddlabs.tt.landscape.LandscapeResources landscapeResources =
                                    World.loadCommon(renderQueues);
                            System.err.println("[EditorStart] Load in-game resources");
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
                            System.err.println("[EditorStart] Generate world info via generator=" + generator.getClass().getSimpleName());
                            WorldInfo worldInfo =
                                    generator.generate(playersForGeneration, Player.INITIAL_UNIT_COUNT, 0f);
                            System.err.println("[EditorStart] WorldInfo ready. Build WorldParameters (gamespeed=" + gamespeed + ")");
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
                                    // Debounce water rebuilds: mark dirty, rebuild at most 10Hz in DefaultRenderer.render()
                                    if (drHolder[0] != null) drHolder[0].markWaterDirty();
                                }
                            };

                            System.err.println("[EditorStart] Create World");
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
                            System.err.println("[EditorStart] Create LandscapeRenderer + Picker + DefaultRenderer");
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
                            System.err.println("[EditorStart] Push EditorDelegate");
                            // Use the in-game GameCamera in the editor via a tiny host adapter
                            final com.oddlabs.tt.camera.CameraHost host = new com.oddlabs.tt.camera.CameraHost() {
                                public com.oddlabs.tt.landscape.World getWorld() { return world; }
                                public com.oddlabs.tt.render.Picker getPicker() { return picker; }
                                public com.oddlabs.tt.gui.GUIRoot getGUIRoot() { return clientRoot; }
                            };
                            Camera camera = new com.oddlabs.tt.camera.GameCamera(host, camState);
                            // Start centered with a sensible pitch like in-game
                            try {
                                float mid = world.getHeightMap().getMetersPerWorld() / 2f;
                                ((com.oddlabs.tt.camera.GameCamera) camera).reset(mid, mid);
                                try {
                                    com.oddlabs.tt.global.Globals.force_landscape_visible = true;
                                    com.oddlabs.tt.camera.CameraState dbg = camera.getState();
                                    if (Float.isNaN(dbg.getCurrentX()) || Float.isNaN(dbg.getCurrentY())) {
                                        // Fallback: place camera looking at mid from an offset
                                        float fallbackX = mid + 50f;
                                        float fallbackY = mid + 50f;
                                        ((com.oddlabs.tt.camera.GameCamera) camera).setPos(fallbackX, fallbackY);
                                        dbg = camera.getState();
                                        System.err.println("[EditorStart] Camera NaN detected; applied fallback pos=(" + dbg.getCurrentX() + "," + dbg.getCurrentY() + "," + dbg.getCurrentZ() + ")");
                                    }
                                    System.err.println("[EditorStart] Camera reset to world mid=" + mid +
                                            " -> camPos=(" + dbg.getCurrentX() + "," + dbg.getCurrentY() + "," + dbg.getCurrentZ() + ")");
                                } catch (Throwable logIgnore) {}
                            } catch (Throwable ignore) {}
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
                                            terrainType,
                                            drHolder[0]);
                            clientRoot.pushDelegate(delegate);

                            // Enable color buffer clearing during editor frames to avoid
                            // trailing artifacts if the 3D world doesn't draw for any reason.
                            try {
                                com.oddlabs.tt.global.Globals.clear_frame_buffer = true;
                                System.err.println("[EditorStart] Enabled clear_frame_buffer for debugging");
                            } catch (Throwable ignore) {}

                            // Match WorldViewer initialization for viewport sizing
                            clientRoot.displayChanged(LocalInput.getViewWidth(), LocalInput.getViewHeight());

                            // Auto-reblend textures across the full world when loading a .ttmap
                            // so colormaps match the newly loaded height/water/build grids
                            try {
                                if (generator instanceof com.oddlabs.tt.mapio.LoadedMapGenerator) {
                                    if (clientRoot != null && clientRoot.getInfoPrinter() != null) {
                                        clientRoot.getInfoPrinter().print("Reblending terrain textures...");
                                    }
                                    int N = world.getHeightMap().getGridUnitsPerWorld();
                                    EditorColormapReblender.reblendROIFromScratch(
                                            world,
                                            landscapeRenderer,
                                            terrainType,
                                            0,
                                            0,
                                            N - 1,
                                            N - 1);
                                }
                            } catch (Throwable ignore) {}

                            System.err.println("[EditorStart] Load done, returning UIRenderer");
                            LOADING.set(false);
                            return uiRenderer;
                        } catch (Throwable t) {
                            try {
                                System.err.println("[EditorStart][ERROR] Load failed: " + t);
                                t.printStackTrace();
                                if (clientRoot != null && clientRoot.getInfoPrinter() != null) {
                                    clientRoot.getInfoPrinter().print("Editor load failed: " + t.getMessage());
                                }
                            } catch (Throwable ignore) {}
                            LOADING.set(false);
                            // Propagate to show behavior upstream as well
                            throw new RuntimeException("MapEditorSession.start load failed", t);
                        }
                    }
                    });
        } catch (Throwable t) {
            // If something fails before the callback runs, clear the loading flag
            LOADING.set(false);
            throw t;
        }
    }

    // Legacy SimpleEditorCamera removed: editor now uses the in-game GameCamera via a host adapter.

    // --- Delegate with a simple height brush ---
    private static final class EditorDelegate extends com.oddlabs.tt.delegate.CameraDelegate implements Animated, MapModeHost {
        private final World world;
        private final LandscapeRenderer landscapeRenderer;
    private final Picker picker;
        private final Animated extraAnimationDriver;
        private final int terrainType;
    private final DefaultRenderer defaultRenderer;
        // Cached overlay renderer (per-patch display lists)
        private final EditorOverlayRenderer overlayRenderer;
        // Toolbar UI (non-modal)
        private com.oddlabs.tt.editor.ui.EditorToolbar toolbar;
        // Binding surface for brush sliders
        private final com.oddlabs.tt.editor.ui.BrushBinding brushBinding = new com.oddlabs.tt.editor.ui.BrushBinding() {
            public float getRadiusMeters() { return (brushRadiusXM + brushRadiusYM) * 0.5f; }
            public void setRadiusMeters(float meters) {
                brushRadiusXM = clamp(meters, MIN_RADIUS, MAX_RADIUS);
                brushRadiusYM = clamp(meters, MIN_RADIUS, MAX_RADIUS);
                info("Size: " + (int) brushRadiusXM + "m");
            }
            public float getIntensity() { return brushStrengthM; }
            public void setIntensity(float strength) {
                float minS = 0.1f, maxS = 10f;
                brushStrengthM = clamp(strength, minS, maxS);
                info("Intensity: " + fmt(brushStrengthM));
            }
        };

    private boolean leftDown = false;
        private boolean rightDown = false;

        // Elliptical brush parameters (meters)
        private float brushRadiusXM = 6f;
        private float brushRadiusYM = 6f;
        private float brushAngleRad = 0f; // rotation of ellipse in radians

    // Brush strength (meters per tick) and hardness falloff exponent
    private float brushStrengthM = 5.0f;  // doubled default intensity
        private float hardnessExp = 0.5f;     // 0.2 very soft .. 2.0 very hard

        private static final float MIN_RADIUS = 1f;
        private static final float MAX_RADIUS = 200f;
    // hardness bounds not used directly; omit to keep build clean

    // Mode handling
    private enum BrushMode { RAISE_LOWER, FLATTEN, SOFTEN, SMOOTH, RIVER, RAMP, ROUGH, RANDOM }
    private BrushMode brushMode = BrushMode.RAISE_LOWER;
        private Float flattenHeightRef = null; // captured on stroke start for FLATTEN

    // Random brush now uses per-cell jitter; no persistent noise fields required

    // Polyline tools (Ramp/Path/River)
    private final java.util.ArrayList<float[]> polylinePts = new java.util.ArrayList<float[]>();

        // Stroke accumulation (no feedback loop until release)
        private java.util.HashMap<Long, Float> strokeAccum = new java.util.HashMap<Long, Float>();
        private java.util.HashMap<Long, Float> strokeBaseline =
                new java.util.HashMap<Long, Float>();
        private boolean strokeActive = false;
        private float strokeDir = 0f;

        // Track an overall bounding box (in grid units) of edited area during the stroke
        private boolean strokeHasBounds = false;
        private int strokeMinGX, strokeMinGY, strokeMaxGX, strokeMaxGY;

    // Resource/Entities brush state
    private enum ActiveTool { TERRAIN, RESOURCE, ENTITIES }
    private ActiveTool activeTool = ActiveTool.TERRAIN;
    // Resource placement types available in the editor. Plants and rubber are intentionally
    // disabled for placement (trees remain placeable). Plants will still snap/remove on terrain edits.
    private enum ResourceType { ROCK, IRON, TREE_JUNGLE, TREE_PALM, TREE_OAK, TREE_PINE }
        private ResourceType resourceType = ResourceType.ROCK;
    // Track resource brush cells processed in the current stroke (place/erase once per cell)
    private final java.util.HashSet<Long> resourceStrokeVisited = new java.util.HashSet<Long>();
    // Entities tool UI state
    private int entitiesType = 0; // 0=Buildings, 1=Units
    // Buildings: 0 Quarters, 1 Armory, 2 Tower, 3 Ship
    // Units: 0 Peon, 1 Warrior Rock, 2 Warrior Iron, 3 Warrior Chicken (rubber), 4 Chieftain
    private int entitiesKind = 0;
    // Team index: 0 = Neutral, else 1..8 map to team 0..7
    private int entitiesTeam = 1; // default Team 0
    private int entitiesRace = com.oddlabs.tt.model.RacesResources.RACE_NATIVES;

        // ------- Map mode state (zoom-to-fit like game modes) -------
        private boolean mapMode = false;
        private GameCamera gameCameraRef; // original game camera the editor uses
        private boolean mapDrag = false;  // true if user dragged while in map mode
        private int mapDownX = 0, mapDownY = 0;
        private static final int MAP_CLICK_TOL = 4; // pixels
    // Track which button is pending for potential drag-edit while in map mode
    private boolean mapLeftPending = false;
    private boolean mapRightPending = false;

    // ------- Overlay tool state -------
    private enum OverlayLayer { WATER, DOCK, ACCESS, BUILD, RESOURCE, SLOPE }
    private boolean overlayActiveHeld = false; // true while T is held (for cycling)
    private boolean overlayTPressed = false;   // true between T down and up
    private boolean overlayTScrollUsed = false; // true if user scrolled while holding T
    private OverlayLayer overlayLayer = OverlayLayer.WATER;

        // Options binding for toolbar selectors
        private final com.oddlabs.tt.editor.ui.EditorOptionsBinding optionsBinding =
                new com.oddlabs.tt.editor.ui.EditorOptionsBinding() {
                    private final String[] modeNames = new String[] {
                        "Raise/Lower", "Flatten", "Soften", "Smooth", "River", "Ramp", "Rough", "Random"
                    };
                    private final String[] resNames = new String[] {
                        "Rock", "Iron", "Tree Jungle", "Tree Palm", "Tree Oak", "Tree Pine"
                    };
                    private final String[] overlayNames = new String[] {
                        "Water", "Dock", "Access", "Build", "Resource", "Slope"
                    };
                    public int getActiveToolIndex() { return activeTool.ordinal(); }
                    public void setActiveToolIndex(int idx) {
                        ActiveTool[] vals = ActiveTool.values();
                        if (idx >= 0 && idx < vals.length) {
                            // Sandbox-gate Entities tool selection
                            if (vals[idx] == ActiveTool.ENTITIES
                                    && EDITOR_STATE.getEditorMode() != com.oddlabs.tt.editor.ui.EditorState.EditorMode.Sandbox) {
                                getGUIRoot().getInfoPrinter().print("Entities tool is Sandbox-only");
                                if (toolbar != null) toolbar.syncOptionsFromBinding();
                                return;
                            }
                            activeTool = vals[idx];
                            // UI tool switches should apply immediately without waiting for mouse actions
                            cancelActiveStrokeAndButtons();
                            if (toolbar != null) toolbar.syncOptionsFromBinding();
                        }
                    }
                    public String[] getBrushModeNames() { return modeNames; }
                    public int getBrushModeIndex() { return brushMode.ordinal(); }
                    public void setBrushModeIndex(int idx) {
                        BrushMode[] vals = BrushMode.values();
                        if (idx >= 0 && idx < vals.length) { brushMode = vals[idx]; info("Mode = " + brushMode); if (toolbar != null) toolbar.syncOptionsFromBinding(); }
                    }
                    public String[] getResourceTypeNames() { return resNames; }
                    public int getResourceTypeIndex() { return resourceType.ordinal(); }
                    public void setResourceTypeIndex(int idx) {
                        ResourceType[] vals = ResourceType.values();
                        if (idx >= 0 && idx < vals.length) { resourceType = vals[idx]; info("Resource = " + resourceType); if (toolbar != null) toolbar.syncOptionsFromBinding(); }
                    }
                    public String[] getOverlayLayerNames() { return overlayNames; }
                    public int getOverlayLayerIndex() { return overlayLayer.ordinal(); }
                    public void setOverlayLayerIndex(int idx) {
                        OverlayLayer[] vals = OverlayLayer.values();
                        if (idx >= 0 && idx < vals.length) { overlayLayer = vals[idx]; if (toolbar != null) toolbar.syncOptionsFromBinding(); }
                        // Selecting an overlay layer implies the user wants overlays visible;
                        // latch the master toggle on so overlays display continuously.
                        try {
                            EDITOR_STATE.setOverlayMaster(true);
                            if (toolbar != null) toolbar.syncOptionsFromBinding();
                        } catch (Throwable ignore) {}
                    }
                    public boolean isOverlayMaster() { return EDITOR_STATE.isOverlayMaster(); }
                    public void setOverlayMaster(boolean v) { EDITOR_STATE.setOverlayMaster(v); if (toolbar != null) toolbar.syncOptionsFromBinding(); }

                    // Entities selectors
                    public String[] getEntitiesTypeNames() { return new String[] {"Buildings", "Units"}; }
                    public int getEntitiesTypeIndex() { return entitiesType; }
                    public void setEntitiesTypeIndex(int idx) {
                        if (idx < 0 || idx > 1) return;
                        entitiesType = idx;
                        // Reset kind to first item of the chosen type to avoid out-of-range
                        entitiesKind = 0;
                        if (toolbar != null) toolbar.syncOptionsFromBinding();
                    }
                    public String[] getEntitiesKindNames() {
                        if (entitiesType == 0) {
                            return new String[] {"Quarters", "Armory", "Tower", "Ship"};
                        } else {
                            return new String[] {"Peon", "Warrior Rock", "Warrior Iron", "Warrior Chicken", "Chieftain"};
                        }
                    }
                    public int getEntitiesKindIndex() { return entitiesKind; }
                    public void setEntitiesKindIndex(int idx) {
                        int max = (entitiesType == 0) ? 3 : 4;
                        if (idx < 0 || idx > max) return;
                        entitiesKind = idx;
                        if (toolbar != null) toolbar.syncOptionsFromBinding();
                    }
                    public String[] getEntitiesTeamNames() {
                        String[] names = new String[1 + 8];
                        names[0] = "Neutral";
                        for (int i=0;i<8;i++) names[1+i] = "Team " + i;
                        return names;
                    }
                    public int getEntitiesTeamIndex() { return entitiesTeam; }
                    public void setEntitiesTeamIndex(int idx) {
                        if (idx < 0 || idx > 8) return; // 0..8 (0 neutral, 1..8 => team 0..7)
                        entitiesTeam = idx;
                        if (toolbar != null) toolbar.syncOptionsFromBinding();
                    }
                    public String[] getEntitiesRaceNames() { return new String[] {"Natives", "Vikings"}; }
                    public int getEntitiesRaceIndex() { return entitiesRace == com.oddlabs.tt.model.RacesResources.RACE_VIKINGS ? 1 : 0; }
                    public void setEntitiesRaceIndex(int idx) {
                        if (idx < 0 || idx > 1) return;
                        entitiesRace = (idx == 1) ? com.oddlabs.tt.model.RacesResources.RACE_VIKINGS
                                                  : com.oddlabs.tt.model.RacesResources.RACE_NATIVES;
                        if (toolbar != null) toolbar.syncOptionsFromBinding();
                    }
                };

        EditorDelegate(
                GUIRoot root,
                Camera camera,
                World world,
                LandscapeRenderer lr,
                Picker picker,
                Animated extraAnimationDriver,
                int terrainType,
                DefaultRenderer defaultRenderer) {
            super(root, camera);
            this.world = world;
            this.landscapeRenderer = lr;
            this.picker = picker;
            this.extraAnimationDriver = extraAnimationDriver;
            this.terrainType = terrainType;
            this.defaultRenderer = defaultRenderer;
            this.overlayRenderer = new EditorOverlayRenderer(world);
            // Register for real-time ticks to drive brush application cadence
            world.getAnimationManagerRealTime().registerAnimation(this);
            getGUIRoot()
                    .getInfoPrinter()
                    .print(
                "Welcome to the Map Editor! Press f1 for help");

            // Add toolbar docked at bottom-left (default)
            try {
                toolbar = new com.oddlabs.tt.editor.ui.EditorToolbar(getGUIRoot(), world, landscapeRenderer, defaultRenderer, terrainType, brushBinding, optionsBinding);
                // Place with a small margin at the bottom of the screen
                toolbar.dockBottomLeft(8, 8);
                addChild(toolbar);
                info("Editor toolbar ready (` to toggle)");
            } catch (Throwable t) {
                try { getGUIRoot().getInfoPrinter().print("Toolbar init failed: " + t.getMessage()); } catch (Throwable ignore) {}
            }
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
    private boolean lookModeActive = false; // editor first-person style look while MMB held
    private int lookBaseX, lookBaseY; // screen center for recentering pointer during look mode
        private LabelBox helpBox = null; // toggled with F1

        private void toggleHelp() {
            if (helpBox == null) {
        String helpText =
                        "Map Editor Help\n\n"
                                + "Camera:\n"
                                + "  - Mouse wheel: Zoom\n"
                                + "  - WASD / Arrow keys: Pan\n"
                                + "  - Home/End: Pitch, Insert/Delete: Rotate\n"
                                + "  - PageUp/PageDown: Zoom (tap)\n"
                                + "  - Space: Toggle map view\n\n"
                                + "Height Tool (Q):\n"
                                + "  - LMB: Raise, RMB: Lower\n"
                                + "  - Ctrl+Wheel: Size, Alt+Wheel: Intensity\n"
                                + "  - Hold Q + Wheel: Cycle modes (Raise/Lower, Flatten, Soften, Smooth, River, Ramp, Rough, Random)\n"
                                + "  - Ramp/River: LMB add point, RMB undo last, Enter apply, Backspace clear\n\n"
                + "Resource Tool (W):\n"
                + "  - LMB: Place, RMB: Erase\n"
                + "  - Hold W + Wheel: Cycle resource type\n"
                + "  - Available types: Rock, Iron, Trees (Jungle, Palm, Oak, Pine)\n"
                + "  - Note: Plant and Rubber placement are disabled in this build\n\n"
                + "Entities Tool (E) [Sandbox only]:\n"
                + "  - LMB: Place, RMB: Erase\n"
                + "  - Type: Buildings (Quarters, Armory, Tower, Ship) or Units (Peon, Warriors Rock/Iron/Chicken, Chieftain)\n"
                + "  - Team/Race selectors available in toolbar; currently spawns under local owner\n\n"
                                + "Other:\n"
                                + "  - ESC: Pause menu\n"
                                + "  - F1: Toggle this help\n"
                                + "  - Ctrl+P: Test Map (configure players)";
                helpBox = new BackgroundLabelBox(helpText, Skin.getSkin().getEditFont(), 640);
                // center on screen
                int x = (getWidth() - helpBox.getWidth()) / 2;
                int y = (getHeight() - helpBox.getHeight()) / 2;
                helpBox.setPos(x, y);
                addChild(helpBox);
            } else {
                helpBox.remove();
                helpBox = null;
            }
        }

        public void animate(float t) {
            // While in map mode, allow edits only if a stroke is active (drag-edit)
            if (mapMode && !strokeActive) return;
            if (!strokeActive) return;
            if (mmbDown) return;
            // Sandbox gating: auto-switch away from Entities if mode is not Sandbox
            if (activeTool == ActiveTool.ENTITIES
                && EDITOR_STATE.getEditorMode() != com.oddlabs.tt.editor.ui.EditorState.EditorMode.Sandbox) {
                activeTool = ActiveTool.TERRAIN;
                cancelActiveStrokeAndButtons();
                try { getGUIRoot().getInfoPrinter().print("Entities tool is Sandbox-only"); } catch (Throwable ignore) {}
                if (toolbar != null) toolbar.syncOptionsFromBinding();
                return;
            }
            if (activeTool == ActiveTool.TERRAIN) applyBrush(strokeDir, t);
            else if (activeTool == ActiveTool.RESOURCE) applyResourceBrush(t);
            else /* ENTITIES */ applyEntitiesBrush(t);
        }

        public void updateChecksum(com.oddlabs.tt.util.StateChecksum checksum) {}

        public void mousePressed(int button, int x, int y) {
            if (mapMode) {
                // In map mode, record a potential click or drag-edit start; do not start a stroke yet.
                if (button == 0 /*LMB*/ || button == 1 /*RMB*/) {
                    mapDrag = false;
                    mapDownX = x;
                    mapDownY = y;
                    mapLeftPending = (button == 0);
                    mapRightPending = (button == 1);
                }
                // Swallow input from tools until we decide (click vs drag) in mouseDragged/mouseReleased
                return;
            }
            // Activate look mode (first-person style) on MMB press
            if (button == LocalInput.MIDDLE_BUTTON) {
                mmbDown = true; // retain existing gating for brush application
                lookModeActive = true;
                lookBaseX = com.oddlabs.tt.render.Display.getWidth() / 2;
                lookBaseY = com.oddlabs.tt.render.Display.getHeight() / 2;
                try { com.oddlabs.tt.input.PointerInput.setCursorPosition(lookBaseX, lookBaseY); } catch (Throwable ignore) {}
                return; // do not start strokes when entering look mode
            }
            // Resource tool supports LMB (paint) and RMB (erase)
            if (button == 0) leftDown = true;
            if (button == 1) rightDown = true;
            // Polyline tools (Ramp/Path/River): LMB add, RMB undo. Don't start a stroke.
            if (activeTool == ActiveTool.TERRAIN && (brushMode == BrushMode.RAMP || brushMode == BrushMode.RIVER)) {
                LandscapeLocation hit = new LandscapeLocation();
                if (picker.pickLocation(getCamera().getState(), hit)) {
                    if (button == 0) {
                        polylinePts.add(new float[] { hit.x, hit.y });
                        info("Point + (#" + polylinePts.size() + ")");
                    } else if (button == 1) {
                        if (!polylinePts.isEmpty()) {
                            polylinePts.remove(polylinePts.size() - 1);
                            info("Undo point (#" + polylinePts.size() + ")");
                        }
                    }
                }
                return;
            }
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
                } else {
                    // Resource stroke: reset per-stroke visited set
                    resourceStrokeVisited.clear();
                }
            }
        }

        public void mouseReleased(int button, int x, int y) {
            if (mapMode) {
                if (button == 0 /*LMB*/ || button == 1 /*RMB*/) {
                    int dx = StrictMath.abs(x - mapDownX);
                    int dy = StrictMath.abs(y - mapDownY);
                    boolean click = (dx <= MAP_CLICK_TOL && dy <= MAP_CLICK_TOL) && !mapDrag;
                    if (click && button == 0 /*LMB*/ && getCamera() instanceof MapCamera) {
                        // Clean click: jump back to previous height at clicked area; no edits
                        picker.pickMapGoto(x, y, (MapCamera) getCamera());
                        // MapCamera will animate back and call exitMapMode() when done.
                    } else {
                        // Drag-release while in map mode: finalize any active stroke but stay in map view
                        if (strokeActive) {
                            if (activeTool == ActiveTool.TERRAIN) applyStroke();
                            strokeActive = false;
                            flattenHeightRef = null;
                            resourceStrokeVisited.clear();
                            if (activeTool == ActiveTool.TERRAIN && strokeHasBounds) {
                                finalizeTerrainRect(strokeMinGX, strokeMinGY, strokeMaxGX, strokeMaxGY);
                                strokeHasBounds = false;
                            }
                            landscapeRenderer.endEdit();
                        }
                    }
                }
                // Reset pending/drag state on release in map mode
                leftDown = false;
                rightDown = false;
                mapLeftPending = false;
                mapRightPending = false;
                mapDrag = false;
                return; // swallow
            }
            if (button == 0) leftDown = false;
            if (button == 1) rightDown = false;
            if (button == LocalInput.MIDDLE_BUTTON) {
                mmbDown = false;
                lookModeActive = false; // exit look mode
            }
            if (!leftDown && !rightDown) {
                // Apply accumulated stroke in one go
                if (strokeActive && activeTool == ActiveTool.TERRAIN) applyStroke();
                strokeActive = false;
                flattenHeightRef = null;
                // Clear resource stroke visited cells at end of stroke
                resourceStrokeVisited.clear();
                // Snap resources (trees, rocks, iron, rubber) inside the edited region to terrain height
                if (activeTool == ActiveTool.TERRAIN && strokeHasBounds) {
                    finalizeTerrainRect(strokeMinGX, strokeMinGY, strokeMaxGX, strokeMaxGY);
                    strokeHasBounds = false;
                }
            }
            landscapeRenderer.endEdit();
        }

        public void mouseHeld(int button, int x, int y) {
            // In map mode, we defer to click/drag logic; do not auto-start strokes from held events
            if (mapMode) return;
            // If the user switched tools while holding the mouse button, InputState will keep
            // generating mouseHeld() events but no new mousePressed() will arrive. To honor the
            // "no mouse gating" contract, re-arm a fresh stroke here when none is active yet.
            if (!strokeActive) {
                if (button == 0) {
                    // Treat as a new left-press
                    leftDown = true;
                    rightDown = false;
                    beginStrokeForCurrentTool(/*left*/ true);
                } else if (button == 1) {
                    // Treat as a new right-press
                    rightDown = true;
                    leftDown = false;
                    beginStrokeForCurrentTool(/*left*/ false);
                }
            }
            // Continuous apply via animate()
        }

        public void mouseScrolled(int amount) {
            // Editor scroll precedence (editor-only, does not affect game/engine globals):
            // 1) Ctrl + Wheel -> adjust brush radius
            // 2) Alt  + Wheel -> adjust brush intensity
            // 3) Q    + Wheel -> cycle height tool mode
            // 4) W    + Wheel -> cycle resource type
            // 5) T    + Wheel -> cycle overlay layer (temporary while held)
            // else: camera zoom
            boolean ctrl = LocalInput.isControlDownCurrently();
            boolean alt = LocalInput.isMenuDownCurrently();

            // 1) Ctrl: radius (and only radius)
            if (ctrl) {
                float scale = 1f + 0.1f * StrictMath.signum(amount);
                if (scale <= 0f) scale = 0.1f;
                brushRadiusXM = clamp(brushRadiusXM * scale, MIN_RADIUS, MAX_RADIUS);
                brushRadiusYM = clamp(brushRadiusYM * scale, MIN_RADIUS, MAX_RADIUS);
                info("Size: " + (int) brushRadiusXM + "x" + (int) brushRadiusYM + "m");
                if (toolbar != null) toolbar.syncFromBinding();
                return;
            }

            // 2) Alt: intensity (and only intensity)
            if (alt) {
                float minS = 0.1f, maxS = 10f;
                brushStrengthM = clamp(brushStrengthM + 0.1f * StrictMath.signum(amount), minS, maxS);
                info("Intensity: " + fmt(brushStrengthM));
                if (toolbar != null) toolbar.syncFromBinding();
                return;
            }

            // 3) Terrain tool key held: cycle height tool mode
            if (LocalInput.isKeyDown(
                    com.oddlabs.tt.global.Settings.getSettings().getKeybind(
                            com.oddlabs.tt.global.Globals.KB_EDITOR_SET_TERRAIN_TOOL))) {
                if (amount > 0) nextMode(); else if (amount < 0) prevMode();
                if (toolbar != null) toolbar.syncOptionsFromBinding();
                return;
            }

            // 4) Resource tool key held: cycle resource type
            if (LocalInput.isKeyDown(
                    com.oddlabs.tt.global.Settings.getSettings().getKeybind(
                            com.oddlabs.tt.global.Globals.KB_EDITOR_SET_RESOURCE_TOOL))) {
                if (amount > 0) cycleResourceType(1); else if (amount < 0) cycleResourceType(-1);
                if (toolbar != null) toolbar.syncOptionsFromBinding();
                return;
            }

            // 5) Overlay key held: overlay layer peek/cycle (does not modify modes)
            if (LocalInput.isKeyDown(
                    com.oddlabs.tt.global.Settings.getSettings().getKeybind(
                            com.oddlabs.tt.global.Globals.KB_EDITOR_OVERLAY_MODE))) {
                overlayTScrollUsed = true;
                if (amount > 0) nextOverlayLayer(); else if (amount < 0) prevOverlayLayer();
                if (toolbar != null) toolbar.syncOptionsFromBinding();
                return;
            }

            // Default: camera zoom
            getCamera().mouseScrolled(amount);
        }

        // 3D halo ring showing current brush footprint
        public void render3D(LandscapeRenderer renderer, RenderQueues queues) {
            LandscapeLocation hit = new LandscapeLocation();
            boolean hasHit = picker.pickLocation(getCamera().getState(), hit);

            if (hasHit) {
                float cx = hit.x;
                float cy = hit.y;
                float rx = brushRadiusXM;
                float ry = brushRadiusYM;
                float cosA = (float) StrictMath.cos(brushAngleRad);
                float sinA = (float) StrictMath.sin(brushAngleRad);

                // Choose color by mode
                float r = 1f, g = 1f, b = 0f, a = 0.85f; // default raise/lower
                switch (brushMode) {
                    case SOFTEN: r = 0.6f; g = 1f; b = 0.6f; break; // light green
                    case SMOOTH: r = 0f; g = 1f; b = 1f; break;
                    case ROUGH: r = 1f; g = 0.5f; b = 0f; break; // orange
                    case RANDOM: r = 0.4f; g = 0.8f; b = 1f; break; // light cyan
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

                // Polyline preview for Ramp/Path/River
                if (brushMode == BrushMode.RAMP || brushMode == BrushMode.RIVER) {
                    if (polylinePts.size() > 0) {
                        GL11.glDisable(GL11.GL_TEXTURE_2D);
                        GL11.glEnable(GL11.GL_BLEND);
                        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                        float pr=1f,pg=1f,pb=1f,pa=0.9f;
                        if (brushMode == BrushMode.RAMP) { pr=0.9f; pg=0.8f; pb=0.1f; } // yellow
                        else { pr=0.2f; pg=0.6f; pb=1f; } // river blue
                        GL11.glColor4f(pr, pg, pb, pa);
                        GL11.glLineWidth(3f);
                        // Draw connected line at terrain height
                        GL11.glBegin(GL11.GL_LINE_STRIP);
                        for (int i=0;i<polylinePts.size();i++) {
                            float[] p = polylinePts.get(i);
                            float z = renderer.getHeightMap().getNearestHeight(p[0], p[1]) + 0.05f;
                            GL11.glVertex3f(p[0], p[1], z);
                        }
                        GL11.glEnd();
                        // Draw points
                        GL11.glPointSize(6f);
                        GL11.glBegin(GL11.GL_POINTS);
                        for (int i=0;i<polylinePts.size();i++) {
                            float[] p = polylinePts.get(i);
                            float z = renderer.getHeightMap().getNearestHeight(p[0], p[1]) + 0.07f;
                            GL11.glVertex3f(p[0], p[1], z);
                        }
                        GL11.glEnd();
                        GL11.glDisable(GL11.GL_BLEND);
                        GL11.glEnable(GL11.GL_TEXTURE_2D);
                    }
                }
            }

            // --- Debug overlays (drawn in world space as semi-transparent quads) ---
            if (shouldRenderOverlays()) {
                drawOverlay(renderer);
            }
        }

        // (Removed legacy fpLookDelegate block; lookMode is handled in the primary mousePressed/mouseReleased implementation earlier.)

        public void mouseDragged(
                int button,
                int x,
                int y,
                int relative_x,
                int relative_y,
                int absolute_x,
                int absolute_y) {
            if (mapMode) {
                if (button == 0 /*LMB*/ || button == 1 /*RMB*/) {
                    int dx = x - mapDownX;
                    int dy = y - mapDownY;
                    if (!mapDrag && (StrictMath.abs(dx) > MAP_CLICK_TOL || StrictMath.abs(dy) > MAP_CLICK_TOL)) {
                        mapDrag = true; // transition to drag-edit in map mode
                        // Start a stroke now that we've crossed the drag threshold
                        if (!strokeActive) {
                            leftDown = mapLeftPending;
                            rightDown = mapRightPending;
                            beginStrokeForCurrentTool(leftDown);
                        }
                    }
                }
                // Do not forward camera drags to keep map overview static during edits
                return;
            }
            if (lookModeActive && button == LocalInput.MIDDLE_BUTTON) {
                // Emulate FirstPersonCamera mouse look using center recentering
                int dx = x - lookBaseX;
                int dy = y - lookBaseY;
                if (dx != 0 || dy != 0) {
                    final float SCALE = .002f; // same as FirstPersonCamera constants
                    float ha = getCamera().getState().getTargetHorizAngle() - dx * SCALE;
                    float va = getCamera().getState().getTargetVertAngle();
                    if (com.oddlabs.tt.global.Settings.getSettings().invert_camera_pitch)
                        va -= dy * SCALE;
                    else
                        va += dy * SCALE;
                    getCamera().getState().setCamera(
                            getCamera().getState().getTargetX(),
                            getCamera().getState().getTargetY(),
                            getCamera().getState().getTargetZ(),
                            va,
                            ha);
                    try { com.oddlabs.tt.input.PointerInput.setCursorPosition(lookBaseX, lookBaseY); } catch (Throwable ignore) {}
                }
                return; // consume
            }
            super.mouseDragged(button, x, y, relative_x, relative_y, absolute_x, absolute_y);
        }

        public void mouseMoved(int x, int y) {
            if (mapMode) return; // no edge scroll updates while in map mode
            if (lookModeActive) {
                // Treat passive movement (in case some platforms deliver moved not dragged)
                int dx = x - lookBaseX;
                int dy = y - lookBaseY;
                if (dx != 0 || dy != 0) {
                    final float SCALE = .002f;
                    float ha = getCamera().getState().getTargetHorizAngle() - dx * SCALE;
                    float va = getCamera().getState().getTargetVertAngle();
                    if (com.oddlabs.tt.global.Settings.getSettings().invert_camera_pitch)
                        va -= dy * SCALE;
                    else
                        va += dy * SCALE;
                    getCamera().getState().setCamera(
                            getCamera().getState().getTargetX(),
                            getCamera().getState().getTargetY(),
                            getCamera().getState().getTargetZ(),
                            va,
                            ha);
                    try { com.oddlabs.tt.input.PointerInput.setCursorPosition(lookBaseX, lookBaseY); } catch (Throwable ignore) {}
                }
                return;
            }
            // Forward to camera for edge scrolling when not in look mode
            getCamera().mouseMoved(x, y);
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
        if (event.getKeyCode() == com.oddlabs.tt.global.Settings.getSettings().getKeybind(
            com.oddlabs.tt.global.Globals.KB_EDITOR_TOGGLE_HELP)) {
                toggleHelp();
                return;
            }
        // Map mode toggle (reuse in-game behavior): use configured key or NumPad5
        if (event.getKeyCode() == com.oddlabs.tt.global.Settings.getSettings().getKeybind(
                com.oddlabs.tt.global.Globals.KB_TOGGLE_MAP_MODE)
            || event.getKeyCode() == Keyboard.KEY_NUMPAD5) {
                if (!mapMode) {
                    // Enter map mode: remember game camera and switch to MapCamera using shared impl
                    if (!(getCamera() instanceof GameCamera)) return; // safety
                    gameCameraRef = (GameCamera) getCamera();
                    // Set rotation pick point like in game for nicer zoom-out path
                    try { picker.pickRotate(gameCameraRef); } catch (Throwable ignore) {}
                    getCamera().disable();
                    setCamera(new MapCamera(this, gameCameraRef));
                    getCamera().enable();
                    mapMode = true;
                } else {
                    // Already in map mode: forward to MapCamera to toggle back (it will call exitMapMode)
                    getCamera().keyPressed(event);
                }
                return;
            }
            // Save (Ctrl + configured key)
            if (event.isControlDown()
                    && !event.isShiftDown()
                    && event.getKeyCode()
                            == com.oddlabs.tt.global.Settings.getSettings().getKeybind(
                                    com.oddlabs.tt.global.Globals.KB_EDITOR_SAVE)) {
                getGUIRoot().addModalForm(new com.oddlabs.tt.form.EditorMapDialogs.SaveDialog(
                        getGUIRoot(), world, terrainType));
                return;
            }
            // Test Map (Ctrl + configured key)
            if (event.isControlDown()
                    && !event.isShiftDown()
                    && event.getKeyCode()
                            == com.oddlabs.tt.global.Settings.getSettings().getKeybind(
                                    com.oddlabs.tt.global.Globals.KB_EDITOR_TEST_MAP)) {
                try {
                    getGUIRoot().addModalForm(new com.oddlabs.tt.form.TestMapForm(
                            getGUIRoot(),
                            com.oddlabs.tt.editor.MapEditorSession.getEditorNetwork(),
                            world,
                            terrainType));
                } catch (Throwable t) {
                    info("Open Test Map failed: " + t.getMessage());
                }
                return;
            }
            // Toolbar toggle key (toggles the toolbar visibility or recreates)
            if (!event.isControlDown()
                    && !event.isShiftDown()
                    && event.getKeyCode()
                            == com.oddlabs.tt.global.Settings.getSettings().getKeybind(
                                    com.oddlabs.tt.global.Globals.KB_EDITOR_TOGGLE_TOOLBAR)) {
                // If toolbar object is null or was closed (removed from parent), recreate it
                if (toolbar == null || toolbar.getParent() == null) {
                    try {
                        toolbar = new com.oddlabs.tt.editor.ui.EditorToolbar(getGUIRoot(), world, landscapeRenderer, defaultRenderer, terrainType, brushBinding, optionsBinding);
                        toolbar.dockBottomLeft(8, 8);
                        addChild(toolbar);
                        info("Editor toolbar restored");
                    } catch (Throwable t) {
                        info("Toolbar create failed: " + t.getMessage());
                    }
                } else {
                    // Toggle visibility without manipulating focus
                    toolbar.toggleVisible();
                }
                return;
            }
            // Removed: F5 quick save (debug-only)
        // Load (Ctrl + configured key)
        if (event.isControlDown()
            && !event.isShiftDown()
            && event.getKeyCode()
                == com.oddlabs.tt.global.Settings.getSettings().getKeybind(
                    com.oddlabs.tt.global.Globals.KB_EDITOR_LOAD)) {
                getGUIRoot().addModalForm(new com.oddlabs.tt.form.EditorMapDialogs.LoadDialog(
                        getGUIRoot(), world, landscapeRenderer, defaultRenderer, terrainType));
                return;
            }
            // Removed: F9 quick load (debug-only)
            // Polyline tool hotkeys
            if (activeTool == ActiveTool.TERRAIN && (brushMode == BrushMode.RAMP || brushMode == BrushMode.RIVER)) {
                if (event.getKeyCode() == com.oddlabs.tt.global.Settings.getSettings().getKeybind(
                                com.oddlabs.tt.global.Globals.KB_EDITOR_POLY_APPLY)
                        || event.getKeyCode() == Keyboard.KEY_NUMPADENTER) {
                    if (polylinePts.size() >= 2) {
                        applyPolylineEdit();
                        polylinePts.clear();
                    } else {
                        info("Need at least 2 points to apply");
                    }
                    return;
                } else if (event.getKeyCode()
                        == com.oddlabs.tt.global.Settings.getSettings().getKeybind(
                                com.oddlabs.tt.global.Globals.KB_EDITOR_POLY_UNDO_POINT)) { // Backspace by default
                    if (!polylinePts.isEmpty()) {
                        polylinePts.remove(polylinePts.size() - 1);
                        info("Undo point (#" + polylinePts.size() + ")");
                    }
                    return;
                }
            }
        // Tool toggles: Terrain (tap switches immediately; hold to cycle with wheel), Resource, Entities.
            if (event.getKeyCode()
                    == com.oddlabs.tt.global.Settings.getSettings().getKeybind(
                            com.oddlabs.tt.global.Globals.KB_EDITOR_SET_TERRAIN_TOOL)) {
                activeTool = ActiveTool.TERRAIN;
                info("Tool = HEIGHT");
                // Clear any in-progress stroke so tool switches never wait for a mouse action
                cancelActiveStrokeAndButtons();
                if (toolbar != null) toolbar.syncOptionsFromBinding();
            } else if (event.getKeyCode()
                    == com.oddlabs.tt.global.Settings.getSettings().getKeybind(
                            com.oddlabs.tt.global.Globals.KB_EDITOR_SET_RESOURCE_TOOL)) {
                activeTool = ActiveTool.RESOURCE;
                info("Tool = RESOURCE, Type = " + resourceType);
                // Don't forward to camera to avoid conflicting with camera forward movement
                // Clear stroke state to avoid any gating
                cancelActiveStrokeAndButtons();
                if (toolbar != null) toolbar.syncOptionsFromBinding();
                return;
        } else if (event.getKeyCode()
            == com.oddlabs.tt.global.Settings.getSettings().getKeybind(
                com.oddlabs.tt.global.Globals.KB_EDITOR_SET_ENTITIES_TOOL)) {
        // Sandbox-gated
        if (EDITOR_STATE.getEditorMode() != com.oddlabs.tt.editor.ui.EditorState.EditorMode.Sandbox) {
            getGUIRoot().getInfoPrinter().print("Entities tool is Sandbox-only");
            return;
        }
        activeTool = ActiveTool.ENTITIES;
        info("Tool = ENTITIES");
        cancelActiveStrokeAndButtons();
        if (toolbar != null) toolbar.syncOptionsFromBinding();
        return;
            } else if (event.getKeyCode()
                    == com.oddlabs.tt.global.Settings.getSettings().getKeybind(
                            com.oddlabs.tt.global.Globals.KB_EDITOR_OVERLAY_MODE)) {
                // Start temporary overlay display; defer toggling until release unless scrolled
                overlayActiveHeld = true;
                overlayTPressed = true;
                overlayTScrollUsed = false;
            }
            getCamera().keyPressed(event);
        }

        protected void keyReleased(KeyboardEvent event) {
            if (event.getKeyCode()
                    == com.oddlabs.tt.global.Settings.getSettings().getKeybind(
                            com.oddlabs.tt.global.Globals.KB_EDITOR_OVERLAY_MODE)) {
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
                        + overlayLayer);
                    if (toolbar != null) toolbar.syncOptionsFromBinding();
                }
                overlayTPressed = false;
                overlayTScrollUsed = false;
            }
            getCamera().keyReleased(event);
        }

        // MapModeHost: called by MapCamera when returning from map view
        public void exitMapMode() {
            if (!mapMode) return;
            mapMode = false;
            // Restore the original game camera instance
            if (gameCameraRef != null) {
                getCamera().disable();
                setCamera(gameCameraRef);
                getCamera().enable();
            }
            // Clear any pending drag/edit state to avoid phantom edits after exit
            mapDrag = false;
            mapLeftPending = false;
            mapRightPending = false;
            cancelActiveStrokeAndButtons();
            try { landscapeRenderer.endEdit(); } catch (Throwable ignore) {}
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
                // Skip any out-of-bounds entries to avoid wrap-around at edges
                if (!hm.isGridInside(gx, gy)) continue;
                float base = strokeBaseline.getOrDefault(k, hm.getHeight(gx, gy));
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
                    // Ignore out-of-bounds cells so the halo doesn't wrap to the other side
                    if (!hm.isGridInside(gx, gy)) continue;

                    long key = keyOf(gx, gy);

                    if (brushMode == BrushMode.RAISE_LOWER) {
                        // Accumulate deltas relative to stroke baseline; apply on release
                        float base = strokeBaseline.containsKey(key)
                                ? strokeBaseline.get(key)
                                : hm.getHeight(gx, gy);
                        if (!strokeBaseline.containsKey(key)) strokeBaseline.put(key, base);
                        float delta = brushStrengthM * dir * falloff * dt;
                        float acc = strokeAccum.getOrDefault(key, 0f);
                        strokeAccum.put(key, acc + delta);
                    } else {
                        // Live updates: compute from current height and apply immediately
                        float curr = hm.getHeight(gx, gy);
                        float target;
                        switch (brushMode) {
                            case SMOOTH: {
                                float avg = neighborhoodAverageCurrent(hm, gx, gy);
                                target = avg;
                                break;
                            }
                            case SOFTEN: {
                                float avg = neighborhoodAverageCurrent(hm, gx, gy);
                                target = avg;
                                break;
                            }
                            case ROUGH: {
                                float avg = neighborhoodAverageCurrent(hm, gx, gy);
                                float diff = curr - avg;
                                target = curr + diff; // push away from mean
                                break;
                            }
                                case RANDOM: {
                                    // Per-cell random jitter each update
                                    java.util.Random rnd = world.getRandom();
                                    float jitter = (rnd.nextFloat() * 2f - 1f) * brushStrengthM * falloff;
                                    float nh = curr + jitter;
                                    hm.editHeight(gx, gy, clampHeightForEdit(hm, gx, gy, nh));
                                    continue; // already applied
                                }
                            case FLATTEN: {
                                target = (flattenHeightRef != null) ? flattenHeightRef : curr;
                                break;
                            }
                            case RAMP:
                            case RIVER:
                                // Polyline tools are applied on Enter commit, not continuously here.
                                target = curr;
                                break;
                            default:
                                target = curr;
                        }
                        float modeScale = 1f;
                        if (brushMode == BrushMode.SMOOTH) modeScale = 0.5f; // gentle
                        else if (brushMode == BrushMode.SOFTEN) modeScale = 0.2f; // extra gentle
                        float delta = (target - curr) * brushStrengthM * modeScale * falloff * dt;
                        // Early-out tiny changes to avoid notification churn
                        final float EPS = 1e-4f;
                        if (delta > -EPS && delta < EPS) continue;
                        float nh = curr + delta;
                        // Enforce editor height constraints on live edits
                        hm.editHeight(gx, gy, clampHeightForEdit(hm, gx, gy, nh));
                    }
                }
            }
        }

        // Apply Ramp/Path/River polyline edit in one commit
        private void applyPolylineEdit() {
            if (polylinePts.size() < 2) return;
            com.oddlabs.tt.landscape.HeightMap hm = world.getHeightMap();
            int gridSize = hm.getGridUnitsPerWorld();
            float cell = com.oddlabs.tt.landscape.HeightMap.METERS_PER_UNIT_GRID;

            // Compute world-space bounding box expanded by brush radius
            float r = StrictMath.max(brushRadiusXM, brushRadiusYM);
            float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY;
            float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
            for (int i=0;i<polylinePts.size();i++) {
                float[] p = polylinePts.get(i);
                if (p[0] < minX) minX = p[0];
                if (p[1] < minY) minY = p[1];
                if (p[0] > maxX) maxX = p[0];
                if (p[1] > maxY) maxY = p[1];
            }
            minX -= r; minY -= r; maxX += r; maxY += r;
            int minGX = StrictMath.max(0, com.oddlabs.tt.pathfinder.UnitGrid.toGridCoordinate(minX));
            int minGY = StrictMath.max(0, com.oddlabs.tt.pathfinder.UnitGrid.toGridCoordinate(minY));
            int maxGX = StrictMath.min(gridSize - 1, com.oddlabs.tt.pathfinder.UnitGrid.toGridCoordinate(maxX));
            int maxGY = StrictMath.min(gridSize - 1, com.oddlabs.tt.pathfinder.UnitGrid.toGridCoordinate(maxY));

            // Precompute vertex baseline heights and optionally adjust per mode
            int nPts = polylinePts.size();
            float[] vertH = new float[nPts];
            for (int i=0;i<nPts;i++) {
                float[] p = polylinePts.get(i);
                vertH[i] = hm.getNearestHeight(p[0], p[1]);
            }
            if (brushMode == BrushMode.RIVER) {
                // Enforce non-increasing downstream baseline
                for (int i=1;i<nPts;i++) {
                    if (vertH[i] > vertH[i-1]) vertH[i] = vertH[i-1];
                }
            }

            // Iterate ROI and apply
            for (int gy=minGY; gy<=maxGY; gy++) {
                for (int gx=minGX; gx<=maxGX; gx++) {
                    float wx = gx * cell + 0.5f * cell;
                    float wy = gy * cell + 0.5f * cell;

                    // Find closest segment and projection
                    float bestD2 = Float.POSITIVE_INFINITY;
                    int bestIdx = -1;
                    float bestT = 0f;
                    for (int i=0;i<nPts-1;i++) {
                        float[] p0 = polylinePts.get(i);
                        float[] p1 = polylinePts.get(i+1);
                        float dx = p1[0]-p0[0];
                        float dy = p1[1]-p0[1];
                        float len2 = dx*dx + dy*dy;
                        if (len2 < 1e-6f) continue;
                        float tx = wx - p0[0];
                        float ty = wy - p0[1];
                        float t = (tx*dx + ty*dy) / len2;
                        if (t < 0f) t = 0f; else if (t > 1f) t = 1f;
                        float qx = p0[0] + t*dx;
                        float qy = p0[1] + t*dy;
                        float ox = wx - qx;
                        float oy = wy - qy;
                        float d2 = ox*ox + oy*oy;
                        if (d2 < bestD2) { bestD2 = d2; bestIdx = i; bestT = t; }
                    }
                    if (bestIdx < 0) continue;
                    float d = (float) StrictMath.sqrt(bestD2);
                    if (d > r) continue; // outside influence

                    // Baseline target height along the closest segment
                    float baseH0 = vertH[bestIdx];
                    float baseH1 = vertH[bestIdx+1];
                    float Ht = baseH0 + bestT * (baseH1 - baseH0);
                    // RIVER: add channel carve that is deepest at centerline
                    if (brushMode == BrushMode.RIVER) {
                        float centerFalloff = 1f - (d / r);
                        if (centerFalloff < 0f) centerFalloff = 0f;
                        float channelDepth = 0.5f * brushStrengthM; // scale with intensity
                        Ht -= channelDepth * centerFalloff;
                    }

                    float curr = hm.getHeight(gx, gy);
                    float falloff = (float) StrictMath.pow(StrictMath.max(0f, 1f - (d / r)), hardnessExp);
                    // Ramp semantics: flat ramp blend toward Ht using intensity as blend factor
                    float blend = clamp(brushStrengthM * falloff, 0f, 1f);
                    float nh = curr + (Ht - curr) * blend;
                    hm.editHeight(gx, gy, clampHeightForEdit(hm, gx, gy, nh));
                }
            }

            // Post-edit processing for ROI
            finalizeTerrainRect(minGX, minGY, maxGX, maxGY);
        }

        // Common post-terrain-edit housekeeping for a grid rectangle
        private void finalizeTerrainRect(int minGX, int minGY, int maxGX, int maxGY) {
            // Clamp ROI to valid grid bounds to avoid wrap-around and OOB access
            int N = world.getHeightMap().getGridUnitsPerWorld();
            int cminGX = StrictMath.max(0, minGX);
            int cminGY = StrictMath.max(0, minGY);
            int cmaxGX = StrictMath.min(N - 1, maxGX);
            int cmaxGY = StrictMath.min(N - 1, maxGY);
            if (cminGX > cmaxGX || cminGY > cmaxGY) return; // nothing valid
            // Snap resources
            snapResourcesInGridRect(cminGX, cminGY, cmaxGX, cmaxGY);
            // Snap decorative plants within the edited ROI
            try {
                float cell = com.oddlabs.tt.landscape.HeightMap.METERS_PER_UNIT_GRID;
                float minWX = cminGX * cell;
                float minWY = cminGY * cell;
                float maxWX = (cmaxGX + 1) * cell;
                float maxWY = (cmaxGY + 1) * cell;
                snapPlantsInWorldRect(minWX, minWY, maxWX, maxWY);
            } catch (Throwable ignore) {}
            // Recompute grids and placement validity
            if (EDITOR_STATE.isAutoUpdatePlacementGrids()) {
                try {
                    EditorGridRecalculator.recomputeROI(
                            world, terrainType, cminGX, cminGY, cmaxGX, cmaxGY);
                } catch (Throwable ignore) {}
                try {
                    com.oddlabs.tt.editor.EditorResourceValidity.recomputeROI(
                            world, cminGX, cminGY, cmaxGX, cmaxGY);
                } catch (Throwable ignore) {}
            }
            // Remove no-longer-valid resources/plants and refresh placement grid
            removeInvalidResourcesInGridRect(cminGX, cminGY, cmaxGX, cmaxGY);
            removeInvalidPlantsInGridRect(cminGX, cminGY, cmaxGX, cmaxGY);
            if (EDITOR_STATE.isAutoUpdatePlacementGrids()) {
                try {
                    com.oddlabs.tt.editor.EditorResourceValidity.recomputeROI(
                            world, cminGX, cminGY, cmaxGX, cmaxGY);
                } catch (Throwable ignore) {}
            }
            // Invalidate cached overlays for the edited region so they rebuild next frame
            try { overlayRenderer.markDirtyROI(cminGX, cminGY, cmaxGX, cmaxGY); } catch (Throwable ignore) {}
            // Reblend colormap for ROI
            try {
                EditorColormapReblender.reblendROIFromScratch(
                        world, landscapeRenderer, terrainType, cminGX, cminGY, cmaxGX, cmaxGY);
            } catch (Throwable ignore) {}
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

        // Snap decorative plants by forcing a reinsert at their current XY within a world-space rectangle
        private void snapPlantsInWorldRect(final float minWX, final float minWY, final float maxWX, final float maxWY) {
            com.oddlabs.tt.model.ElementNodeVisitor visitor = new com.oddlabs.tt.model.ElementNodeVisitor() {
                private boolean intersects(float bminX, float bmaxX, float bminY, float bmaxY) {
                    return !(bmaxX < minWX || bminX > maxWX || bmaxY < minWY || bminY > maxWY);
                }
                public void visitNode(com.oddlabs.tt.model.ElementNode node) {
                    if (intersects(node.bmin_x, node.bmax_x, node.bmin_y, node.bmax_y)) node.visitChildren(this);
                }
                public void visitLeaf(com.oddlabs.tt.model.ElementLeaf leaf) {
                    if (intersects(leaf.bmin_x, leaf.bmax_x, leaf.bmin_y, leaf.bmax_y)) leaf.visitElements(this);
                }
                public void visit(com.oddlabs.tt.model.Element element) {
                    if (element instanceof com.oddlabs.tt.model.Plants) {
                        com.oddlabs.tt.model.Plants p = (com.oddlabs.tt.model.Plants) element;
                        // trigger Model.reinsert() by setting same XY
                        p.setPosition(p.getPositionX(), p.getPositionY());
                    }
                }
            };
            try { world.getElementRoot().visit(visitor); } catch (Throwable ignore) {}
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

        // Remove decorative plants found within invalid grid cells (water, dock, or inaccessible)
    private void removeInvalidPlantsInGridRect(int minGX, int minGY, int maxGX, int maxGY) {
            com.oddlabs.tt.landscape.HeightMap hm = world.getHeightMap();
            int n = hm.getGridUnitsPerWorld();
            int x0 = StrictMath.max(0, minGX);
            int y0 = StrictMath.max(0, minGY);
            int x1 = StrictMath.min(n - 1, maxGX);
            int y1 = StrictMath.min(n - 1, maxGY);
            final boolean[][] water = hm.getWaterGrid();
            final boolean[][] dock = hm.getDockGrid();
            final boolean[][] access = hm.getAccessGrid();
            final float cell = com.oddlabs.tt.landscape.HeightMap.METERS_PER_UNIT_GRID;

            final float minWX = x0 * cell;
            final float minWY = y0 * cell;
            final float maxWX = (x1 + 1) * cell;
            final float maxWY = (y1 + 1) * cell;

            final int[] removed = new int[] {0};
            com.oddlabs.tt.model.ElementNodeVisitor visitor = new com.oddlabs.tt.model.ElementNodeVisitor() {
                private boolean intersects(float bminX, float bmaxX, float bminY, float bmaxY) {
                    return !(bmaxX < minWX || bminX > maxWX || bmaxY < minWY || bminY > maxWY);
                }
                public void visitNode(com.oddlabs.tt.model.ElementNode node) {
                    if (intersects(node.bmin_x, node.bmax_x, node.bmin_y, node.bmax_y)) node.visitChildren(this);
                }
                public void visitLeaf(com.oddlabs.tt.model.ElementLeaf leaf) {
                    if (intersects(leaf.bmin_x, leaf.bmax_x, leaf.bmin_y, leaf.bmax_y)) leaf.visitElements(this);
                }
                public void visit(com.oddlabs.tt.model.Element element) {
                    if (element instanceof com.oddlabs.tt.model.Plants) {
                        com.oddlabs.tt.model.Plants p = (com.oddlabs.tt.model.Plants) element;
                        int gx = p.getGridX();
                        int gy = p.getGridY();
                        if (gx < x0 || gy < y0 || gx > x1 || gy > y1) return;
                        boolean isWater = (water != null && water[gy][gx]);
                        boolean isDock = (dock != null && dock[gy][gx]);
                        boolean isAccessible = (access == null) || access[gy][gx];
                        if (isWater || isDock || !isAccessible) {
                            try { p.remove(); removed[0]++; } catch (Throwable ignore) {}
                        }
                    }
                }
            };
            try { world.getElementRoot().visit(visitor); } catch (Throwable ignore) {}
            if (removed[0] > 0) {
                System.out.println("[Editor] Removed " + removed[0] + " invalid plant(s) after terrain edit.");
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
                    if (!hm.isGridInside(x, y)) continue;
                    sum += hm.getHeight(x, y);
                    count++;
                }
            }
            return count > 0 ? (sum / count) : hm.getHeight(gx, gy);
        }

        private float clamp(float v, float lo, float hi) {
            return StrictMath.max(lo, StrictMath.min(hi, v));
        }

        private String fmt(float f) {
            return String.format(java.util.Locale.ROOT, "%.2f", f);
        }

        private void info(String s) { getGUIRoot().getInfoPrinter().print("Brush: " + s); }

        // Ensure tool switching never waits for mouse release: clear stroke/buttons immediately
        private void cancelActiveStrokeAndButtons() {
            leftDown = false;
            rightDown = false;
            mmbDown = false;
            if (strokeActive) {
                // Abort current stroke without applying (only used when switching tools)
                strokeActive = false;
                strokeAccum.clear();
                strokeBaseline.clear();
                flattenHeightRef = null;
                resourceStrokeVisited.clear();
                try { landscapeRenderer.endEdit(); } catch (Throwable ignore) {}
            }
        }

        // Called when we need to (re)start a stroke without an explicit mousePressed event,
        // e.g., after switching tools while holding a mouse button.
        private void beginStrokeForCurrentTool(boolean left) {
            if (activeTool == ActiveTool.TERRAIN) {
                // In polyline modes (Ramp/River), strokes are built by discrete clicks only.
                if (brushMode == BrushMode.RAMP || brushMode == BrushMode.RIVER) return;
                strokeActive = true;
                strokeDir = left ? 1f : -1f;
                strokeAccum.clear();
                strokeBaseline.clear();
                strokeHasBounds = false;
                // Capture flatten ref height at stroke start if needed
                if (brushMode == BrushMode.FLATTEN) {
                    LandscapeLocation hit = new LandscapeLocation();
                    if (picker.pickLocation(getCamera().getState(), hit)) {
                        int cx = com.oddlabs.tt.pathfinder.UnitGrid.toGridCoordinate(hit.x);
                        int cy = com.oddlabs.tt.pathfinder.UnitGrid.toGridCoordinate(hit.y);
                        flattenHeightRef = world.getHeightMap().getWrappedHeight(cx, cy);
                    }
                }
            } else {
                // Resource tool: start a new placement/erase sweep for this hold
                strokeActive = true;
                resourceStrokeVisited.clear();
            }
        }

        private void nextMode() {
            switch (brushMode) {
                case RAISE_LOWER: brushMode = BrushMode.FLATTEN; break;
                case FLATTEN: brushMode = BrushMode.SOFTEN; break;
                case SOFTEN: brushMode = BrushMode.SMOOTH; break;
                case SMOOTH: brushMode = BrushMode.RIVER; break;
                case RIVER: brushMode = BrushMode.RAMP; break;
                case RAMP: brushMode = BrushMode.ROUGH; break;
                case ROUGH: brushMode = BrushMode.RANDOM; break;
                case RANDOM: brushMode = BrushMode.RAISE_LOWER; break;
            }
            info("Mode = " + brushMode);
        }

        private void prevMode() {
            switch (brushMode) {
                case RAISE_LOWER: brushMode = BrushMode.RANDOM; break;
                case RANDOM: brushMode = BrushMode.ROUGH; break;
                case ROUGH: brushMode = BrushMode.RAMP; break;
                case RAMP: brushMode = BrushMode.RIVER; break;
                case RIVER: brushMode = BrushMode.SMOOTH; break;
                case SMOOTH: brushMode = BrushMode.SOFTEN; break;
                case SOFTEN: brushMode = BrushMode.FLATTEN; break;
                case FLATTEN: brushMode = BrushMode.RAISE_LOWER; break;
            }
            info("Mode = " + brushMode);
        }

        // random noise helpers removed (RANDOM uses per-cell jitter)

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
                    // Only process each cell once per stroke to avoid repeated placement/erase while stationary
                    long key = keyOf(gx, gy);
                    if (resourceStrokeVisited.contains(key)) continue;
                    resourceStrokeVisited.add(key);
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
                                try { overlayRenderer.markDirtyROI(gx, gy, gx, gy); } catch (Throwable ignore) {}
                            }
                            // Also remove decorative plants in this cell
                            try { removePlantsInCell(gx, gy); } catch (Throwable ignore) {}
                        } else if (occ instanceof com.oddlabs.tt.model.SupplyModel) {
                            ((com.oddlabs.tt.model.SupplyModel) occ).editorRemoveNow();
                            if (EDITOR_STATE.isAutoUpdatePlacementGrids()) {
                                try { com.oddlabs.tt.editor.EditorResourceValidity.recomputeROI(world, gx, gy, gx, gy); } catch (Throwable ignore) {}
                                try { overlayRenderer.markDirtyROI(gx, gy, gx, gy); } catch (Throwable ignore) {}
                            }
                            try { removePlantsInCell(gx, gy); } catch (Throwable ignore) {}
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
                            // Regardless, remove any decorative plants in this cell
                            try { removePlantsInCell(gx, gy); } catch (Throwable ignore) {}
                        } else {
                            // No grid occupant: try deleting decorative plants in this cell
                            int deleted = removePlantsInCell(gx, gy);
                            if (deleted > 0 && !debugPrintedThisStroke && gx == cx && gy == cy) {
                                getGUIRoot().getInfoPrinter().print("Erased " + deleted + " plant(s) at gx=" + gx + ", gy=" + gy);
                                System.out.println("[Editor] Erased " + deleted + " plant(s) at gx=" + gx + ", gy=" + gy);
                                debugPrintedThisStroke = true;
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
                                try { overlayRenderer.markDirtyROI(gx, gy, gx, gy); } catch (Throwable ignore) {}
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

        // Remove plants whose centers fall within the given grid cell. Returns number removed.
        private int removePlantsInCell(final int gx, final int gy) {
            final float cell = com.oddlabs.tt.landscape.HeightMap.METERS_PER_UNIT_GRID;
            final float minWX = gx * cell;
            final float minWY = gy * cell;
            final float maxWX = (gx + 1) * cell;
            final float maxWY = (gy + 1) * cell;
            final int[] removed = new int[] {0};
            com.oddlabs.tt.model.ElementNodeVisitor visitor = new com.oddlabs.tt.model.ElementNodeVisitor() {
                private boolean intersects(float bminX, float bmaxX, float bminY, float bmaxY) {
                    return !(bmaxX < minWX || bminX > maxWX || bmaxY < minWY || bminY > maxWY);
                }
                public void visitNode(com.oddlabs.tt.model.ElementNode node) {
                    if (intersects(node.bmin_x, node.bmax_x, node.bmin_y, node.bmax_y)) node.visitChildren(this);
                }
                public void visitLeaf(com.oddlabs.tt.model.ElementLeaf leaf) {
                    if (intersects(leaf.bmin_x, leaf.bmax_x, leaf.bmin_y, leaf.bmax_y)) leaf.visitElements(this);
                }
                public void visit(com.oddlabs.tt.model.Element element) {
                    if (element instanceof com.oddlabs.tt.model.Plants) {
                        com.oddlabs.tt.model.Plants p = (com.oddlabs.tt.model.Plants) element;
                        int egx = p.getGridX();
                        int egy = p.getGridY();
                        if (egx == gx && egy == gy) {
                            try { p.remove(); removed[0]++; } catch (Throwable ignore) {}
                        }
                    }
                }
            };
            try { world.getElementRoot().visit(visitor); } catch (Throwable ignore) {}
            return removed[0];
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

        // Rubber and decorative plant placement intentionally removed from the editor.

        // Returns true if any decorative plant exists fully within the given grid cell
        // Duplicate plant checks removed per request; placement is now gated to once-per-cell-per-stroke

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

        // -------- Entities Brush Implementation --------
        private void applyEntitiesBrush(float dt) {
            // Same spatial semantics as resource brush: once-per-cell-per-stroke with coverage
            LandscapeLocation hit = new LandscapeLocation();
            if (!picker.pickLocation(getCamera().getState(), hit)) return;
            com.oddlabs.tt.landscape.HeightMap hm = world.getHeightMap();
            com.oddlabs.tt.pathfinder.UnitGrid ug = world.getUnitGrid();
            int cx = com.oddlabs.tt.pathfinder.UnitGrid.toGridCoordinate(hit.x);
            int cy = com.oddlabs.tt.pathfinder.UnitGrid.toGridCoordinate(hit.y);

            int rGU = (int) StrictMath.ceil(
                    StrictMath.max(brushRadiusXM, brushRadiusYM)
                            / com.oddlabs.tt.landscape.HeightMap.METERS_PER_UNIT_GRID);
            float rx = brushRadiusXM;
            float ry = brushRadiusYM;
            float cosA = (float) StrictMath.cos(brushAngleRad);
            float sinA = (float) StrictMath.sin(brushAngleRad);

            // Intensity -> coverage fraction
            float coverage = (float) StrictMath.max(0f, StrictMath.min(1f, 0.2f * brushStrengthM));
            java.util.Random rnd = world.getRandom();
            boolean erase = rightDown && !leftDown;

            boolean debugPrintedThisStroke = false;
            for (int gy = cy - rGU; gy <= cy + rGU; gy++) {
                for (int gx = cx - rGU; gx <= cx + rGU; gx++) {
                    if (gx < 0 || gy < 0 || gx >= ug.getGridSize() || gy >= ug.getGridSize()) continue;
                    float dxm = (gx - cx) * com.oddlabs.tt.landscape.HeightMap.METERS_PER_UNIT_GRID;
                    float dym = (gy - cy) * com.oddlabs.tt.landscape.HeightMap.METERS_PER_UNIT_GRID;
                    float ldx = cosA * dxm + sinA * dym;
                    float ldy = -sinA * dxm + cosA * dym;
                    float norm = (ldx * ldx) / (rx * rx) + (ldy * ldy) / (ry * ry);
                    if (norm > 1f) continue;
                    long key = keyOf(gx, gy);
                    if (resourceStrokeVisited.contains(key)) continue;
                    resourceStrokeVisited.add(key);

                    if (erase) {
                        com.oddlabs.tt.pathfinder.Occupant occ = ug.getOccupant(gx, gy, com.oddlabs.tt.pathfinder.UnitGrid.LAND);
                        boolean changed = false;
                        if (occ instanceof com.oddlabs.tt.model.Unit) {
                            try { ((com.oddlabs.tt.model.Unit) occ).remove(); changed = true; } catch (Throwable ignore) {}
                        } else if (occ instanceof com.oddlabs.tt.model.Building) {
                            try { ((com.oddlabs.tt.model.Building) occ).remove(); changed = true; } catch (Throwable ignore) {}
                        }
                        if (changed && EDITOR_STATE.isAutoUpdatePlacementGrids()) {
                            try { com.oddlabs.tt.editor.EditorResourceValidity.recomputeROI(world, gx, gy, gx, gy); } catch (Throwable ignore) {}
                            try { overlayRenderer.markDirtyROI(gx, gy, gx, gy); } catch (Throwable ignore) {}
                        }
                        continue;
                    }

                    if (rnd.nextFloat() > coverage) continue;

                    if (entitiesType == 0) {
                        // Buildings
                        int buildingIndex = entitiesKindToBuildingIndex(entitiesKind);
                        com.oddlabs.tt.model.Race raceSel = world.getRacesResources().getRace(entitiesRace);
                        com.oddlabs.tt.model.BuildingTemplate tmpl = raceSel.getBuildingTemplate(buildingIndex);
                        com.oddlabs.tt.player.BuildingSiteScanFilter filter = new com.oddlabs.tt.player.BuildingSiteScanFilter(
                                ug, tmpl, 0, true);
                        ug.scan(filter, gx, gy, com.oddlabs.tt.pathfinder.UnitGrid.LAND);
                        java.util.List targets = filter.getResult();
                        if (targets.size() > 0) {
                            com.oddlabs.tt.util.Target t = (com.oddlabs.tt.util.Target) targets.get(0);
                            com.oddlabs.tt.player.Player owner = world.getPlayers()[0];
                            try {
                                com.oddlabs.tt.model.Building b = new com.oddlabs.tt.model.Building(owner, tmpl, t.getGridX(), t.getGridY());
                                b.place();
                                b.repair(1000);
                                if (!debugPrintedThisStroke && gx == cx && gy == cy) {
                                    getGUIRoot().getInfoPrinter().print("Placed building at gx=" + t.getGridX() + ", gy=" + t.getGridY());
                                    debugPrintedThisStroke = true;
                                }
                                if (EDITOR_STATE.isAutoUpdatePlacementGrids()) {
                                    int size = tmpl.getPlacingSize();
                                    int r = StrictMath.max(0, size / 2);
                                    int minGX = t.getGridX() - r, minGY = t.getGridY() - r, maxGX = t.getGridX() + r, maxGY = t.getGridY() + r;
                                    try { com.oddlabs.tt.editor.EditorResourceValidity.recomputeROI(world, minGX, minGY, maxGX, maxGY); } catch (Throwable ignore) {}
                                    try { overlayRenderer.markDirtyROI(minGX, minGY, maxGX, maxGY); } catch (Throwable ignore) {}
                                }
                            } catch (Throwable ex) {
                                // Drop placement on any failure
                            }
                        } else if (!debugPrintedThisStroke && gx == cx && gy == cy) {
                            getGUIRoot().getInfoPrinter().print("Place FAIL: illegal building site");
                            debugPrintedThisStroke = true;
                        }
                    } else {
                        // Units
                        boolean[][] access = hm.getAccessGrid();
                        boolean okAccess = (access == null) || access[gy][gx];
                        com.oddlabs.tt.pathfinder.Occupant occ = ug.getOccupant(gx, gy, com.oddlabs.tt.pathfinder.UnitGrid.LAND);
                        boolean free = (occ == null) || (occ instanceof com.oddlabs.tt.pathfinder.StaticOccupant);
                        if (okAccess && free) {
                            float xw = com.oddlabs.tt.pathfinder.UnitGrid.coordinateFromGrid(gx) + (world.getRandom().nextFloat() - .5f);
                            float yw = com.oddlabs.tt.pathfinder.UnitGrid.coordinateFromGrid(gy) + (world.getRandom().nextFloat() - .5f);
                            com.oddlabs.tt.player.Player owner = world.getPlayers()[0];
                            com.oddlabs.tt.model.Race raceSel = world.getRacesResources().getRace(entitiesRace);
                            int unitIndex = entitiesKindToUnitIndex(entitiesKind);
                            if (unitIndex >= 0) {
                                try {
                                    new com.oddlabs.tt.model.Unit(owner, xw, yw, null, raceSel.getUnitTemplate(unitIndex));
                                    if (!debugPrintedThisStroke && gx == cx && gy == cy) {
                                        getGUIRoot().getInfoPrinter().print("Spawned unit at gx=" + gx + ", gy=" + gy);
                                        debugPrintedThisStroke = true;
                                    }
                                    if (EDITOR_STATE.isAutoUpdatePlacementGrids()) {
                                        try { com.oddlabs.tt.editor.EditorResourceValidity.recomputeROI(world, gx, gy, gx, gy); } catch (Throwable ignore) {}
                                        try { overlayRenderer.markDirtyROI(gx, gy, gx, gy); } catch (Throwable ignore) {}
                                    }
                                } catch (Throwable ignore) {}
                            }
                        } else if (!debugPrintedThisStroke && gx == cx && gy == cy) {
                            String occStr = (occ == null) ? "null" : occ.getClass().getSimpleName();
                            getGUIRoot().getInfoPrinter().print("Spawn FAIL: access=" + okAccess + ", occ=" + occStr);
                            debugPrintedThisStroke = true;
                        }
                    }
                }
            }
        }

        private int entitiesKindToBuildingIndex(int kind) {
            // kind 0..3 => quarters, armory, tower, ship
            switch (kind) {
                case 0: return com.oddlabs.tt.model.Race.BUILDING_QUARTERS;
                case 1: return com.oddlabs.tt.model.Race.BUILDING_ARMORY;
                case 2: return com.oddlabs.tt.model.Race.BUILDING_TOWER;
                case 3: return com.oddlabs.tt.model.Race.BUILDING_SHIP;
                default: return com.oddlabs.tt.model.Race.BUILDING_QUARTERS;
            }
        }

        private int entitiesKindToUnitIndex(int kind) {
            // When type==Units, our kind indices are: 0 Peon, 1 Rock, 2 Iron, 3 Chicken, 4 Chieftain
            // Map to Race constants
            switch (kind) {
                case 0: return com.oddlabs.tt.model.Race.UNIT_PEON;
                case 1: return com.oddlabs.tt.model.Race.UNIT_WARRIOR_ROCK;
                case 2: return com.oddlabs.tt.model.Race.UNIT_WARRIOR_IRON;
                case 3: return com.oddlabs.tt.model.Race.UNIT_WARRIOR_RUBBER;
                case 4: return com.oddlabs.tt.model.Race.UNIT_CHIEFTAIN;
                default: return -1;
            }
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

        // Overlay modes removed; only default threshold visualization remains.

        private void drawOverlay(LandscapeRenderer renderer) {
            // Render using cached per-patch display lists.
            // Important: keep depth test ON so overlay tiles hidden behind terrain don't bleed through.
            // Also keep depth writes OFF and use alpha blend so overlays composite on visible terrain.
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glDepthMask(false);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            // Dynamic polygon offset tied to camera height above ground.
            // Negative values bias fragments toward the camera for GL_LEQUAL depth testing.
            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            float cx = getCamera().getState().getCurrentX();
            float cy = getCamera().getState().getCurrentY();
            float cz = getCamera().getState().getCurrentZ();
            float ground = 0f;
            try { ground = world.getHeightMap().getNearestHeight(cx, cy); } catch (Throwable ignore) {}
            float above = Math.max(0f, cz - ground);
            // Emphasize constant units term for coplanar surfaces, scale with distance; clamp for safety
            float factor = -1.1f;
            float units  = -(float) clamp(3.0f + above * 0.05f, 5.0f, 48.0f);
            GL11.glPolygonOffset(factor, units);

            float cr=0f,cg=0f,cb=0f,ca=0.35f;
            switch (overlayLayer) {
                case WATER:    cr=0f;   cg=0.4f; cb=1f;   break;
                case DOCK:     cr=0f;   cg=1f;   cb=1f;   break;
                case ACCESS:   cr=0.1f; cg=1f;   cb=0.1f; break;
                case BUILD:    cr=1f;   cg=1f;   cb=0f;   break;
                case RESOURCE: cr=1f;   cg=0.5f; cb=0f;   break;
                case SLOPE:    cr=1f;   cg=0f;   cb=0f;   break;
            }
            GL11.glColor4f(cr, cg, cb, ca);

            EditorOverlayRenderer.Layer layer;
            switch (overlayLayer) {
                case WATER: layer = EditorOverlayRenderer.Layer.WATER; break;
                case DOCK: layer = EditorOverlayRenderer.Layer.DOCK; break;
                case ACCESS: layer = EditorOverlayRenderer.Layer.ACCESS; break;
                case BUILD: layer = EditorOverlayRenderer.Layer.BUILD; break;
                case RESOURCE: layer = EditorOverlayRenderer.Layer.RESOURCE; break;
                case SLOPE: default: layer = EditorOverlayRenderer.Layer.SLOPE; break;
            }
            overlayRenderer.draw(layer);

            // Restore state
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonOffset(0f, 0f);
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
