package com.oddlabs.tt.mapeditor;

import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.camera.Camera;
import com.oddlabs.tt.camera.CameraHost;
import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.camera.FirstPersonCamera;
import com.oddlabs.tt.camera.GameCamera;
import com.oddlabs.tt.camera.MapCamera;
import com.oddlabs.tt.camera.MapCameraOwner;
import com.oddlabs.tt.delegate.CameraDelegate;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.form.MessageForm;
import com.oddlabs.tt.form.QuestionForm;
import com.oddlabs.tt.form.SelectGameMenu;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.gui.CheckBox;
import com.oddlabs.tt.gui.CursorType;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUIObject;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.gui.PulldownButton;
import com.oddlabs.tt.gui.PulldownItem;
import com.oddlabs.tt.gui.PulldownMenu;
import com.oddlabs.tt.gui.RadioButton;
import com.oddlabs.tt.gui.RadioButtonGroup;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import com.oddlabs.tt.input.Key;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.render.LandscapeLocation;
import com.oddlabs.tt.render.LandscapeRenderer;
import com.oddlabs.tt.render.MatrixStack;
import com.oddlabs.tt.render.Picker;
import com.oddlabs.tt.render.RenderQueues;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.scenery.Water;
import com.oddlabs.tt.viewer.Cheat;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.RIGHT_MID;

/**
 * The editor screen: the island seen through the game camera, a toolbar along the top with a dropdown each for
 * terrain and resource brushes, and the mouse painting the terrain.
 *
 * <p>Left button paints with the positive side of a brush and right button with the negative side. Ctrl plus the
 * wheel sizes the brush, Shift plus the wheel sets its intensity, and the plain wheel zooms like in a game. The
 * middle button turns the view, as it does in a game.
 *
 * <p>Paths, rivers and ridges follow a course: each left click adds a point to it, and a right click or Enter lays
 * the course, Backspace takes back its last point and Escape drops it. A path's legs too steep to walk show red. The
 * copy brush copies the area dragged over, then pastes it at each left click; R turns the copy and M mirrors it, and
 * a right click or Escape drops it.
 *
 * <p>The spawn tool puts the chosen player's spawn where the left button clicks, on playable ground or the sea, and the
 * right button takes away the spawn under the cursor. Every spawn shows as a ring in its player's colour with the
 * player's number, floating on the sea where it lies in it, and crossed out in red while it is on neither.
 *
 * <p>The map mode key (Space by default) flies up to the game's island overview. Another press of it flies back, and
 * a left click flies down to the clicked spot. Nothing else works while there.
 *
 * <p>In a shared session the island is edited by several players at once. Each one's edits go to the others a few
 * times a second, and each sees the others' cameras, brushes and edits in their colours. Undo takes back this
 * player's own edits only. Enter opens the session's chat.
 */
final class MapEditorDelegate extends CameraDelegate<Camera> implements CameraHost, MapCameraOwner {
    private static final float MIN_RADIUS = 4f;
    private static final float MAX_RADIUS = 512f;
    private static final float RADIUS_STEP = 1.15f;
    private static final int MIN_INTENSITY = 0;
    /** Seconds between resource brush dabs while the button is held. */
    private static final float RESOURCE_DAB_INTERVAL = .05f;
    /** The middle mouse button's number for the input provider. */
    private static final int MIDDLE_BUTTON = 2;
    private static final int MAX_INTENSITY = 100;
    private static final int INTENSITY_STEP = 5;
    private static final int LABEL_WIDTH = 150;
    private static final int PULLDOWN_WIDTH = 150;
    private static final int OVERLAY_PULLDOWN_WIDTH = 190;
    private static final int SPAWN_PULLDOWN_WIDTH = 110;
    /** The radius of a spawn's ring, in meters, and how far from one a right click takes it away, in cells. */
    private static final float SPAWN_RADIUS = 9f;
    private static final float SPAWN_PICK_CELLS = 6f;
    // The seven segments of each digit drawn on a spawn: top, top right, bottom right, bottom, bottom left, top left,
    // middle, from the lowest bit. The digits' size and the gap between them, and the dots' spacing along a segment,
    // close enough for the dots to run together, in meters.
    private static final int[] SEGMENTS = {0x3F, 0x06, 0x5B, 0x4F, 0x66, 0x6D, 0x7D, 0x07, 0x7F, 0x6F};
    private static final float DIGIT_WIDTH = 4f;
    private static final float DIGIT_HEIGHT = 7f;
    private static final float DIGIT_GAP = 2f;
    private static final float DIGIT_DOT_SPACING = .6f;
    private static final int HINT_WIDTH = 760;
    /** Seconds between edits sent to a shared session, at least, and the bytes a second they may take at most. */
    private static final float SEND_INTERVAL = .1f;
    private static final float SEND_RATE = 400_000f;
    /** Seconds between telling the session where the camera and brush are, and between telling it when idle. */
    private static final float PRESENCE_INTERVAL = .1f;
    private static final float PRESENCE_KEEPALIVE = 1f;
    /** Seconds after another player's edit before the ground settles, as after a stroke of this player's. */
    private static final float REMOTE_QUIET = .5f;

    private final @NonNull NetworkSelector network;
    private final @NonNull World world;
    private final @NonNull AnimationManager manager;
    private final @NonNull Picker picker;
    private static final float GROUND_UPDATE_INTERVAL = .1f;

    private final @NonNull TerrainEditor editor;
    private final @Nullable GroundTextures ground;
    private static final float ACCESS_UPDATE_INTERVAL = .25f;
    private final @NonNull AccessMap access_map;
    private final @NonNull PlantLayer plants;
    private final @NonNull Water water;
    private final @NonNull TintOverlay tint;
    private final @NonNull EdgeOverlay edges;
    // What the overlay shows, explained below the toolbar.
    private @Nullable Label label_legend;
    private static final float OVERLAY_UPDATE_INTERVAL = .25f;
    // Seconds since the overlay last followed a stroke in progress.
    private float overlay_timer;
    // Seconds since the playable area last followed a stroke in progress.
    private float access_timer;
    // Seconds since the ground texture last followed a stroke in progress.
    private float ground_timer;
    // An edit finished and the ground texture should settle, possibly rebuilding the whole island.
    private boolean ground_settle;
    private final @NonNull MapSettings settings;
    private final @NonNull ResourceLayer layer;
    // Terrain steps and resource strokes, newest first, so undo takes them back in the order they were made.
    private final Deque<Object> history = new ArrayDeque<>();
    // What undo took back, newest first, as steps whose undo puts it back; emptied by the next edit.
    private final Deque<Object> redo_history = new ArrayDeque<>();
    private ResourceLayer.@Nullable Stroke resource_stroke;
    // Resources the latest terrain edit left off the playable area; kept with it in the history until the next edit
    // or an undo, as the playable area catches up with an edit after the stroke ends.
    private ResourceLayer.@Nullable Stroke pruned;
    private float resource_timer;
    // Whether the resources differ from the generated ones, and whether they changed since the last save.
    private boolean resources_edited;
    private boolean resources_modified;
    // Where each player starts, the player the spawn tool puts down, and whether the spawns changed since the last
    // save.
    private @NonNull Spawns spawns;
    private int spawn_player;
    private boolean spawns_modified;
    private final @NonNull PulldownButton<Integer> pulldown_spawn;
    private final @NonNull BrushRenderer brush_renderer = new BrushRenderer();
    private final @NonNull Animated ticker = this::tick;
    private final @NonNull LandscapeLocation location = new LandscapeLocation();
    private final @NonNull Random random = new Random(LocalEventQueue.getQueue().getHighPrecisionManager().getTick());
    // Fixed for the session, so painting cliffs over cliffs raises the same cells further.
    private final int cliff_seed = random.nextInt();

    private final @NonNull EditorCamera game_camera;
    private final @NonNull Toolbar toolbar;
    private final @NonNull Label label_radius;
    private final @NonNull Label label_intensity;
    private final @NonNull Label label_hint;

    private @Nullable String map_name;
    // What the map's maker wrote about it, saved with it.
    private @NonNull String description;
    // Whether the heights differ from what the settings generate, so saving must keep them.
    private boolean edited;

    private @NonNull Brush brush = Brush.HEIGHT;
    private float radius = 16f;
    private int intensity = 50;
    // Whether resource brushes spread in clumps rather than evenly.
    private boolean natural_spread;

    // The points of the course clicked so far: x and y in meters, and the ground's height there when clicked.
    private final List<float @NonNull []> course = new ArrayList<>();
    // What the copy brush last copied, which each left click pastes.
    private @Nullable Clipboard clipboard;

    // The ground under the cursor, in meters.
    private boolean has_cursor;
    private float cursor_x;
    private float cursor_y;

    // The stroke in progress: 0 when none, else 1 for the left button and -1 for the right.
    private int stroke_sign;
    // Ground height where the stroke began: the flatten target, and the level the cursor is held to while painting.
    private float stroke_z;
    private int random_seed;
    // Where a drag began, or where the stretch brush grabbed the ground, in meters.
    private float ramp_x;
    private float ramp_y;
    // Where the ground the stretch brush drags along was on the last frame, in meters.
    private float stretch_x;
    private float stretch_y;

    private boolean map_mode;

    // The middle button view turn in progress. Drags and the release keep arriving here, like in a game.
    private @Nullable LookDelegate look;

    // The shared session this island is edited in, if any, and what keeps it in step and shows the others.
    private final SessionSync.@NonNull Link link;
    private @Nullable EditorSession session;
    private @Nullable SessionSync sync;
    // What keeps the spawns in step.
    private @Nullable SpawnSync spawn_sync;
    private @Nullable RemoteEditors remotes;
    private float send_timer;
    private float send_interval = SEND_INTERVAL;
    private float presence_timer;
    private float presence_idle;
    private EditorSession.@Nullable Presence last_presence;
    // Seconds left before another player's latest edit counts as finished.
    private float remote_busy;
    // Whether the session is under way, past taking in who was there already.
    private boolean session_live;
    // The session's chat, while open, and whether the chat key went down here, to open it once it comes up.
    private @Nullable EditorChatForm chat_form;
    private boolean chat_key_down;

    MapEditorDelegate(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root, @NonNull World world,
            @NonNull AnimationManager manager, @NonNull Picker picker, @NonNull Cheat view,
            @NonNull CameraState camera_state, @NonNull TerrainEditor editor, @Nullable GroundTextures ground,
            @NonNull AccessMap access_map, @NonNull TintOverlay tint, @NonNull ResourceLayer layer,
            @NonNull PlantLayer plants, @NonNull Water water, @NonNull MapSettings settings,
            @Nullable String map_name, @NonNull String description, boolean edited, boolean resources_edited,
            @NonNull Spawns spawns, SessionSync.@NonNull Link link) {
        super(gui_root, null);
        this.link = link;
        this.network = network;
        this.world = world;
        this.manager = manager;
        this.picker = picker;
        this.editor = editor;
        this.ground = ground;
        this.access_map = access_map;
        this.plants = plants;
        this.water = water;
        this.tint = tint;
        this.settings = settings;
        this.map_name = map_name;
        this.description = description;
        this.edited = edited;
        this.layer = layer;
        this.resources_edited = resources_edited;
        this.spawns = spawns;

        game_camera = new EditorCamera(this, camera_state);
        setCamera(game_camera);
        // Start south of the middle, looking north over the island.
        float center = world.getHeightMap().getMetersPerWorld() / 2f;
        game_camera.reset(center, center * .75f);

        toolbar = new Toolbar();
        // One dropdown of terrain brushes and one of resource brushes, each with a button beside it that picks
        // whichever brush its dropdown shows.
        Landscape.TerrainType terrain = Landscape.TerrainType.values()[settings.terrain()];
        PulldownMenu<Brush> menu_terrain = new PulldownMenu<>();
        PulldownMenu<Brush> menu_resource = new PulldownMenu<>();
        for (Brush b : Brush.values()) {
            if (b != Brush.SPAWN)
                (b.isResourceBrush() ? menu_resource : menu_terrain).addItem(new PulldownItem<>(b.getName(terrain),
                        b));
        }
        RadioButtonGroup tools = new RadioButtonGroup();
        RadioButton radio_terrain = new RadioButton(true, tools, MapEditor.i18n("tool_terrain"));
        RadioButton radio_resource = new RadioButton(false, tools, MapEditor.i18n("tool_resources"));
        PulldownButton<Brush> pulldown_terrain = new PulldownButton<>(gui_root, menu_terrain, 0, PULLDOWN_WIDTH);
        PulldownButton<Brush> pulldown_resource = new PulldownButton<>(gui_root, menu_resource, 0, PULLDOWN_WIDTH);
        radio_terrain.addMouseClickListener((_, _, _, _) -> {
            selectBrush(chosen(menu_terrain));
            setFocus();
        });
        radio_resource.addMouseClickListener((_, _, _, _) -> {
            selectBrush(chosen(menu_resource));
            setFocus();
        });
        // Choosing from a dropdown also switches to it.
        menu_terrain.addItemChosenListener((menu, _) -> {
            tools.mark(radio_terrain);
            selectBrush(chosen(menu));
            setFocus();
        });
        menu_resource.addItemChosenListener((menu, _) -> {
            tools.mark(radio_resource);
            selectBrush(chosen(menu));
            setFocus();
        });
        Group group_terrain = tool(radio_terrain, pulldown_terrain);
        Group group_resource = tool(radio_resource, pulldown_resource);
        // The spawn tool, with a dropdown of the players whose spawn a click puts down.
        RadioButton radio_spawns = new RadioButton(false, tools, MapEditor.i18n("tool_spawns"));
        PulldownMenu<Integer> menu_spawn = new PulldownMenu<>();
        for (int i = 0; i < Spawns.COUNT; i++)
            menu_spawn.addItem(new PulldownItem<>(MapEditor.i18n("spawn_player", i + 1), i));
        pulldown_spawn = new PulldownButton<>(gui_root, menu_spawn, 0, SPAWN_PULLDOWN_WIDTH);
        colourSpawnPlayer();
        radio_spawns.addMouseClickListener((_, _, _, _) -> {
            selectBrush(Brush.SPAWN);
            setFocus();
        });
        menu_spawn.addItemChosenListener((menu, _) -> {
            tools.mark(radio_spawns);
            spawn_player = chosen(menu);
            colourSpawnPlayer();
            selectBrush(Brush.SPAWN);
            setFocus();
        });
        Group group_spawns = tool(radio_spawns, pulldown_spawn);
        toolbar.addChild(group_terrain);
        toolbar.addChild(group_resource);
        group_terrain.place();
        group_resource.place(group_terrain, RIGHT_MID, 20);
        CheckBox check_natural = new CheckBox(natural_spread, MapEditor.i18n("natural_spread"));
        check_natural.addCheckBoxListener(marked -> {
            natural_spread = marked;
            setFocus();
        });
        label_radius = new Label("", Skin.getSkin().getEditFont(), LABEL_WIDTH);
        label_intensity = new Label("", Skin.getSkin().getEditFont(), LABEL_WIDTH);
        CheckBox check_trees = new CheckBox(view.draw_trees, MapEditor.i18n("show_trees"));
        check_trees.addCheckBoxListener(marked -> {
            view.draw_trees = marked;
            setFocus();
        });
        edges = new EdgeOverlay(editor, world.getHeightMap().getSeaLevelMeters());
        // The overlay box turns the overlay its dropdown shows on and off; choosing from the dropdown turns it on.
        PulldownMenu<Overlay> menu_overlay = new PulldownMenu<>();
        for (Overlay o : Overlay.values())
            menu_overlay.addItem(new PulldownItem<>(o.getName(), o));
        CheckBox check_overlay = new CheckBox(false, MapEditor.i18n("overlay"));
        PulldownButton<Overlay> pulldown_overlay = new PulldownButton<>(gui_root, menu_overlay, 0,
                OVERLAY_PULLDOWN_WIDTH);
        check_overlay.addCheckBoxListener(marked -> {
            showOverlay(marked ? chosen(menu_overlay) : null);
            setFocus();
        });
        menu_overlay.addItemChosenListener((menu, _) -> {
            if (check_overlay.isMarked())
                showOverlay(chosen(menu));
            else
                check_overlay.setMarked(true);
            setFocus();
        });
        Group group_overlay = tool(check_overlay, pulldown_overlay);
        CheckBox check_wireframe = new CheckBox(view.line_mode, MapEditor.i18n("wireframe"));
        check_wireframe.addCheckBoxListener(marked -> {
            view.line_mode = marked;
            setFocus();
        });
        HorizButton button_undo = new HorizButton(MapEditor.i18n("undo"), 80);
        button_undo.addMouseClickListener((_, _, _, _) -> {
            undo();
            setFocus();
        });
        HorizButton button_redo = new HorizButton(MapEditor.i18n("redo"), 80);
        button_redo.addMouseClickListener((_, _, _, _) -> {
            redo();
            setFocus();
        });
        HorizButton button_menu = new HorizButton(MapEditor.i18n("menu"), 80);
        button_menu.addMouseClickListener((_, _, _, _) -> openMenu());
        // Wide enough for the longest hint, as a label clips what does not fit.
        int hint_width = Math.max(HINT_WIDTH, Skin.getSkin().getEditFont().getWidth(MapEditor.i18n("hint_paste")));
        for (Brush b : Brush.values())
            hint_width = Math.max(hint_width, Skin.getSkin().getEditFont().getWidth(b.getHint()));
        label_hint = new Label("", Skin.getSkin().getEditFont(), hint_width + 10);
        Label label_controls = new Label(MapEditor.i18n("hint_controls"), Skin.getSkin().getEditFont(),
                hint_width + 10);
        toolbar.addChild(check_natural);
        toolbar.addChild(label_radius);
        toolbar.addChild(label_intensity);
        toolbar.addChild(check_trees);
        toolbar.addChild(check_wireframe);
        toolbar.addChild(group_overlay);
        toolbar.addChild(button_undo);
        toolbar.addChild(button_redo);
        toolbar.addChild(button_menu);
        toolbar.addChild(label_hint);
        toolbar.addChild(label_controls);
        toolbar.addChild(group_spawns);
        // The brushes and their settings along the top, what to show beside the hint below.
        check_natural.place(group_resource, RIGHT_MID);
        label_radius.place(check_natural, RIGHT_MID, 20);
        label_intensity.place(label_radius, RIGHT_MID);
        button_undo.place(label_intensity, RIGHT_MID, 20);
        button_redo.place(button_undo, RIGHT_MID);
        button_menu.place(button_redo, RIGHT_MID);
        label_hint.place(group_terrain, BOTTOM_LEFT);
        label_controls.place(label_hint, BOTTOM_LEFT);
        group_spawns.place(label_controls, RIGHT_MID);
        check_trees.place(label_hint, RIGHT_MID);
        check_wireframe.place(check_trees, RIGHT_MID);
        group_overlay.place(check_wireframe, RIGHT_MID, 20);
        toolbar.compileCanvas();
        addChild(toolbar);

        refreshLabels();
    }

    private void colourSpawnPlayer() {
        Vector4f[] colours = Settings.getSettings().team_colours;
        pulldown_spawn.setLabelColor(colours[spawn_player % colours.length]);
    }

    /** A tool's button with its dropdown beside it, kept together so the row stays as tall as the dropdown. */
    private static @NonNull Group tool(@NonNull GUIObject button, @NonNull PulldownButton<?> pulldown) {
        Group group = new Group();
        group.addChild(button);
        group.addChild(pulldown);
        button.place();
        pulldown.place(button, RIGHT_MID);
        group.compileCanvas();
        return group;
    }

    /**
     * The game camera, able to zoom out twice as far as in a game (200 m instead of 100 m) so more of the island
     * fits in view while editing. The unlocked cinematic camera still goes higher.
     */
    private static final class EditorCamera extends GameCamera {
        private static final float EDITOR_MAX_Z = 2 * GameCamera.MAX_Z;

        EditorCamera(@NonNull CameraHost host, @NonNull CameraState state) {
            super(host, state);
        }

        @Override
        protected float getMaxZ() {
            return Math.max(EDITOR_MAX_Z, super.getMaxZ());
        }

        float maxZ() {
            return getMaxZ();
        }
    }

    /**
     * A terrain stroke in the undo history, with the resources it left off the playable area. The terrain editor
     * keeps what it changed in the heights.
     */
    private record TerrainStep(ResourceLayer.@NonNull Stroke pruned) {
    }

    /**
     * A paste in the undo history: the resources it cleared from its area and placed, and its heights, if it changed
     * any, as a terrain step.
     */
    private record PasteStep(@Nullable TerrainStep terrain, ResourceLayer.@NonNull Stroke resources) {
    }

    /** A spawn moved, in the undo history: where the player's spawn was before, which undo puts it back to. */
    private record SpawnStep(int player, int @Nullable [] cell) {
    }

    /** The toolbar looks like a window but Escape on it opens the editor menu instead of closing it. */
    private final class Toolbar extends Form {
        @Override
        public void cancel() {
            openMenu();
        }
    }

    // ---- CameraHost ----

    @Override
    public @NonNull World getWorld() {
        return world;
    }

    @Override
    public @NonNull Picker getPicker() {
        return picker;
    }

    /** The editor camera's ceiling, so turning the view or jumping from map mode keeps the zoom. */
    @Override
    public float getMaxCameraZ() {
        return game_camera.maxZ();
    }

    // ---- Lifecycle ----

    @Override
    protected void doAdd() {
        super.doAdd();
        LocalEventQueue.getQueue().getHighPrecisionManager().registerAnimation(ticker);
    }

    @Override
    protected void doRemove() {
        super.doRemove();
        LocalEventQueue.getQueue().getHighPrecisionManager().removeAnimation(ticker);
        cancelStroke();
    }

    @Override
    public void displayChangedNotify(int width, int height) {
        super.displayChangedNotify(width, height);
        toolbar.setPos((width - toolbar.getWidth()) / 2, height - toolbar.getHeight());
        placeLegend();
        if (remotes != null)
            remotes.setRosterTop(rosterTop());
    }

    /** The list of who is in a shared session goes at the left, below the toolbar. */
    private int rosterTop() {
        return toolbar.getY() - 10;
    }

    /** Just below the toolbar, centred. */
    private void placeLegend() {
        Label legend = label_legend;
        if (legend != null)
            legend.setPos((getWidth() - legend.getWidth()) / 2, toolbar.getY() - legend.getHeight() - 4);
    }

    /** Lays an overlay over the island with its legend below the toolbar, or takes it away when null. */
    private void showOverlay(@Nullable Overlay overlay) {
        tint.show(overlay);
        edges.setVisible(overlay == Overlay.EDGES);
        if (label_legend != null)
            label_legend.remove();
        label_legend = null;
        if (overlay != null) {
            Label legend = new Label(overlay.getLegend(), Skin.getSkin().getEditFont());
            addChild(legend);
            label_legend = legend;
            placeLegend();
        }
    }

    @Override
    public boolean canScroll() {
        // Keep edge scrolling in step with the cursor, as the game's camera delegates do.
        var input = Renderer.getLocalInput();
        float scale = getGUIRoot().getGlobalScale();
        game_camera.mouseMoved(Math.round(input.getMouseX() / scale), Math.round(input.getMouseY() / scale));
        return !map_mode && getGUIRoot().getModalDelegate() == null;
    }

    /** Runs every frame: the world's animations, painting while a button is held, and pushing edits to the GPU. */
    private void tick(float t) {
        // A release can go astray, as when it happens outside the window, and the cursor would stay hidden for good.
        if (look != null && !Renderer.getLocalInput().getInputProvider().isButtonDown(MIDDLE_BUTTON))
            endLook();
        world.tick(t);
        manager.runAnimations(t);
        has_cursor = !map_mode && pickCursor();
        if (stroke_sign != 0 && has_cursor && !brush.isDragShape())
            paint(t);
        editor.flush();
        updateSession(t);
        updateGround(t);
        updateAccess(t);
    }

    /**
     * Finds the ground under the mouse. While painting, the ground under the brush keeps moving, and picking it
     * would pull the brush along the view ray towards or away from the camera, which then changes the ground again.
     * So during a stroke the mouse ray is followed to the level the stroke began at instead, which only moves when
     * the mouse or the camera does.
     */
    private boolean pickCursor() {
        if (!picker.pickLocation(game_camera.getState(), location))
            return false;
        cursor_x = location.x;
        cursor_y = location.y;
        if (stroke_sign == 0 || brush.isDragShape())
            return true;
        CameraState state = game_camera.getState();
        float eye_x = state.getCurrentX();
        float eye_y = state.getCurrentY();
        float eye_z = state.getCurrentZ();
        // The picked point is on the mouse ray, so the ray runs from the eye through it.
        float dz = editor.getHeight(toGrid(location.x), toGrid(location.y)) - eye_z;
        float t = (stroke_z - eye_z) / dz;
        if (dz < 0f && t > 0f) {
            cursor_x = eye_x + (location.x - eye_x) * t;
            cursor_y = eye_y + (location.y - eye_y) * t;
        }
        return true;
    }

    // ---- Brushes ----

    private static float toGrid(float meters) {
        return meters / HeightMap.METERS_PER_UNIT_GRID;
    }

    private static <T> @NonNull T chosen(@NonNull PulldownMenu<T> menu) {
        return Objects.requireNonNull(menu.getItem(menu.getChosenItemIndex()).getAttachment());
    }

    private void selectBrush(@NonNull Brush new_brush) {
        cancelStroke();
        // A course clicked out for a river can be raised as a ridge or laid as a path instead, but no other brush uses
        // it.
        if (!new_brush.isCourse())
            course.clear();
        brush = new_brush;
        refreshLabels();
    }

    private void refreshLabels() {
        label_radius.clear();
        label_radius.append(MapEditor.i18n("radius", Math.round(radius)));
        label_intensity.clear();
        label_intensity.append(MapEditor.i18n("intensity", intensity));
        label_hint.clear();
        label_hint.append(brush == Brush.COPY && clipboard != null ? MapEditor.i18n("hint_paste") : brush.getHint());
    }

    private void beginStroke(int sign) {
        if (stroke_sign != 0)
            return;
        if (brush == Brush.SPAWN) {
            clickSpawn(sign);
            return;
        }
        if (brush.isCourse()) {
            if (sign > 0)
                addCoursePoint();
            else
                layCourse();
            return;
        }
        if (brush == Brush.COPY && clipboard != null) {
            if (sign > 0)
                paste(isShiftDown());
            else
                dropClipboard();
            return;
        }
        // The copy brush's area is dragged out with the left button only.
        if (!has_cursor || (brush == Brush.COPY && sign < 0))
            return;
        stroke_sign = sign;
        if (brush.isDragShape()) {
            ramp_x = cursor_x;
            ramp_y = cursor_y;
            return;
        }
        stroke_z = editor.getHeight(toGrid(cursor_x), toGrid(cursor_y));
        if (brush.isResourceBrush()) {
            resource_stroke = new ResourceLayer.Stroke();
            // The first dab lands at once.
            resource_timer = RESOURCE_DAB_INTERVAL;
            return;
        }
        random_seed = random.nextInt();
        if (brush == Brush.STRETCH)
            grab();
        beginTerrainStroke();
    }

    /** Takes hold of the ground under the cursor for the stretch brush. */
    private void grab() {
        ramp_x = stretch_x = cursor_x;
        ramp_y = stretch_y = cursor_y;
    }

    private void remember(@NonNull Object step) {
        keep(history, step);
        redo_history.clear();
        editor.clearRedo();
    }

    private static void keep(@NonNull Deque<Object> steps, @NonNull Object step) {
        steps.push(step);
        while (steps.size() > TerrainEditor.MAX_UNDO_STEPS)
            steps.removeLast();
    }

    private void beginTerrainStroke() {
        pruned = new ResourceLayer.Stroke();
        editor.beginStroke();
    }

    /** Ends the terrain editor's stroke and, if it changed anything, records it to undo. */
    private void finishTerrainStroke() {
        ResourceLayer.Stroke stroke = pruned;
        if (editor.endStroke() && stroke != null) {
            remember(new TerrainStep(stroke));
        } else {
            pruned = null;
        }
    }

    private void finishResourceStroke() {
        ResourceLayer.Stroke stroke = resource_stroke;
        resource_stroke = null;
        if (stroke != null && !stroke.isEmpty()) {
            remember(stroke);
            resources_edited = true;
            resources_modified = true;
        }
    }

    /**
     * Brings the ground texture after the heights: every so often while painting, and in full once an edit is done.
     */
    private void updateGround(float t) {
        if (ground == null)
            return;
        ground_timer += t;
        if (stroke_sign == 0 && remote_busy <= 0f && ground_settle) {
            ground_settle = false;
            ground.settle();
        } else if (ground.hasChanges() && ground_timer >= GROUND_UPDATE_INTERVAL) {
            ground_timer = 0f;
            ground.update();
        }
    }

    /**
     * Brings the playable area after the heights, now and then while painting and at once otherwise, takes away the
     * resources an edit left off it, and brings the overlay, the plants and the sea after it.
     */
    private void updateAccess(float t) {
        access_timer += t;
        // Another player's stroke comes in a few times a second, and is followed now and then like this player's.
        boolean busy = stroke_sign != 0 || remote_busy > 0f;
        if (access_map.isStale() && (!busy || access_timer >= ACCESS_UPDATE_INTERVAL))
            sortAccess();
        // Capturing the island for the overlay takes a moment on a large one, so a stroke only brings it along now
        // and then.
        overlay_timer += t;
        boolean now = !busy || overlay_timer >= OVERLAY_UPDATE_INTERVAL;
        if (now)
            overlay_timer = 0f;
        tint.update(Renderer.getRenderer().getRenderContext(), now);
    }

    private void sortAccess() {
        access_timer = 0f;
        access_map.compute();
        tint.mapChanged();
        // The ground under them changed, so plants and the sea's open water follow the sort too.
        plants.update();
        water.updateOceanPatches();
        // Units could no longer reach them. Undoing the edit brings them back.
        if (layer.prune(pruned != null ? pruned : new ResourceLayer.Stroke())) {
            resources_edited = true;
            resources_modified = true;
            ground_settle = true;
        }
    }

    private void endStroke() {
        if (stroke_sign == 0)
            return;
        ground_settle = true;
        if (brush.isResourceBrush()) {
            finishResourceStroke();
        } else if (brush.isDragShape()) {
            if (has_cursor)
                layDrag();
        } else {
            finishTerrainStroke();
        }
        stroke_sign = 0;
    }

    /** Lays a ramp or an isthmus, or copies an area, from where the drag began to the cursor. */
    private void layDrag() {
        float ax = toGrid(ramp_x);
        float ay = toGrid(ramp_y);
        float bx = toGrid(cursor_x);
        float by = toGrid(cursor_y);
        if (brush == Brush.COPY) {
            copy(ax, ay, bx, by);
            return;
        }
        beginTerrainStroke();
        if (brush == Brush.ISTHMUS)
            editor.applyIsthmus(ax, ay, bx, by, toGrid(radius), intensity / 100f, stroke_sign, random.nextInt());
        else
            editor.applyRamp(ax, ay, editor.getHeight(ax, ay), bx, by, editor.getHeight(bx, by), toGrid(radius),
                    intensity / 100f, stroke_sign);
        finishTerrainStroke();
    }

    // ---- Courses ----

    private void addCoursePoint() {
        if (has_cursor)
            course.add(new float[]{cursor_x, cursor_y, editor.getHeight(toGrid(cursor_x), toGrid(cursor_y))});
    }

    private void removeCoursePoint() {
        if (!course.isEmpty())
            course.removeLast();
    }

    /** Lays the path, digs the river or raises the ridge along the course clicked out, and starts a new one. */
    private void layCourse() {
        if (course.isEmpty()) {
            getGUIRoot().getInfoPrinter().print(MapEditor.i18n("no_course"));
            return;
        }
        List<float[]> points = new ArrayList<>(course.size());
        for (float[] point : course)
            points.add(new float[]{toGrid(point[0]), toGrid(point[1])});
        BrushPath path = new BrushPath(points);
        beginTerrainStroke();
        switch (brush) {
            case PATH -> {
                float[] point_heights = pathHeights(course);
                editor.applyPath(path, point_heights, toGrid(radius), intensity / 100f);
                for (boolean steep : editor.steepLegs(path, point_heights)) {
                    if (steep) {
                        getGUIRoot().getInfoPrinter().print(MapEditor.i18n("path_too_steep"));
                        break;
                    }
                }
            }
            case RIVER -> editor.applyRiver(path, toGrid(radius), intensity / 100f, random.nextInt());
            default -> editor.applyRidge(path, toGrid(radius), intensity / 100f, random.nextInt());
        }
        finishTerrainStroke();
        ground_settle = true;
        course.clear();
    }

    /** A path's height at each of a course's points, from the ground's height there when clicked. */
    private float @NonNull [] pathHeights(@NonNull List<float @NonNull []> points) {
        float[] point_heights = new float[points.size()];
        for (int i = 0; i < point_heights.length; i++)
            point_heights[i] = editor.pathHeight(points.get(i)[2]);
        return point_heights;
    }

    // ---- Copy and paste ----

    /** Copies the heights and resources of the rectangle with corners at two grid positions. */
    private void copy(float ax, float ay, float bx, float by) {
        int last = editor.getSize() - 1;
        int x0 = Math.clamp(Math.round(Math.min(ax, bx)), 0, last);
        int y0 = Math.clamp(Math.round(Math.min(ay, by)), 0, last);
        int x1 = Math.clamp(Math.round(Math.max(ax, bx)), 0, last);
        int y1 = Math.clamp(Math.round(Math.max(ay, by)), 0, last);
        if (x1 - x0 < 1 || y1 - y0 < 1)
            return;
        clipboard = new Clipboard(editor.copyRect(x0, y0, x1, y1), layer.copyRect(x0, y0, x1, y1));
        refreshLabels();
        getGUIRoot().getInfoPrinter().print(MapEditor.i18n("copied", (x1 - x0 + 1) * HeightMap.METERS_PER_UNIT_GRID,
                (y1 - y0 + 1) * HeightMap.METERS_PER_UNIT_GRID));
    }

    /** The first cell the copy lands on when pasted, which puts its middle under the cursor. */
    private int pasteX(@NonNull Clipboard copy) {
        return Math.round(toGrid(cursor_x) - (copy.width() - 1) / 2f);
    }

    private int pasteY(@NonNull Clipboard copy) {
        return Math.round(toGrid(cursor_y) - (copy.height() - 1) / 2f);
    }

    /**
     * Pastes the copy under the cursor: its heights, then, once the playable area is sorted for them, its trees, rock
     * and iron in place of those that were there. It is undone as one step.
     *
     * @param keep_heights whether to keep the heights as copied, rather than meet the ground it lands on
     */
    private void paste(boolean keep_heights) {
        Clipboard copy = clipboard;
        if (copy == null || !has_cursor)
            return;
        int x0 = pasteX(copy);
        int y0 = pasteY(copy);
        beginTerrainStroke();
        ResourceLayer.Stroke terrain_pruned = pruned;
        editor.paste(copy.heights(), x0, y0, intensity / 100f, keep_heights);
        // Resources only go on playable ground, so the heights must reach it, and the sort follow, before they do.
        editor.flush();
        if (access_map.isStale())
            sortAccess();
        ResourceLayer.Stroke placed = new ResourceLayer.Stroke();
        layer.paste(copy.resources(), x0, y0, copy.width(), copy.height(), placed);
        boolean heights_changed = editor.endStroke();
        if (!heights_changed)
            pruned = null;
        if (heights_changed || !placed.isEmpty()) {
            remember(new PasteStep(heights_changed && terrain_pruned != null ? new TerrainStep(terrain_pruned) : null,
                    placed));
            resources_edited = true;
            resources_modified = true;
        }
        ground_settle = true;
    }

    private void dropClipboard() {
        clipboard = null;
        refreshLabels();
    }

    private void turnClipboard(boolean mirror) {
        Clipboard copy = clipboard;
        if (copy != null)
            clipboard = mirror ? copy.mirrored() : copy.rotated();
    }

    /** Drops a stroke without laying a pending ramp, keeping whatever was already painted as one undo step. */
    private void cancelStroke() {
        if (stroke_sign != 0 && brush.isResourceBrush()) {
            finishResourceStroke();
            ground_settle = true;
        } else if (stroke_sign != 0 && !brush.isDragShape()) {
            finishTerrainStroke();
            ground_settle = true;
        }
        stroke_sign = 0;
    }

    private void paint(float t) {
        if (brush.isResourceBrush()) {
            Resource resource = brush.getResource();
            // Dabs rather than every frame: each fills the brush to its density, so more would only cost time.
            resource_timer += t;
            if (resource_timer < RESOURCE_DAB_INTERVAL || resource_stroke == null)
                return;
            resource_timer = 0f;
            // Right click takes away what the brush paints; the eraser takes away everything with either button.
            if (resource != null && stroke_sign > 0)
                layer.paint(resource, cursor_x, cursor_y, radius, intensity / 100f, natural_spread, resource_stroke);
            else
                layer.erase(resource, cursor_x, cursor_y, radius, resource_stroke);
            return;
        }
        float gx = toGrid(cursor_x);
        float gy = toGrid(cursor_y);
        float r = toGrid(radius);
        float strength = intensity / 100f;
        switch (brush) {
            case HEIGHT -> editor.applyHeight(gx, gy, r, strength, stroke_sign, t);
            case FLATTEN -> editor.applyFlatten(gx, gy, r, strength, stroke_sign, t, stroke_z);
            case SMOOTH -> editor.applySmooth(gx, gy, r, strength, stroke_sign, t);
            case RANDOM -> editor.applyRandom(gx, gy, r, strength, stroke_sign, t, random_seed);
            case ROUGHNESS -> editor.applyRoughness(gx, gy, r, strength, stroke_sign, t, random_seed);
            case CLIFFS -> editor.applyCliffs(gx, gy, r, strength, stroke_sign, t, cliff_seed);
            case ERODE -> editor.applyErode(gx, gy, r, strength, stroke_sign, t);
            case WARP -> editor.applyWarp(gx, gy, r, strength, stroke_sign, t, random_seed);
            case TWIST -> editor.applyTwist(gx, gy, r, strength, stroke_sign, t);
            case SWIRL -> editor.applySwirl(gx, gy, r, strength, stroke_sign, t);
            case STRETCH -> {
                editor.applyStretch(toGrid(stretch_x), toGrid(stretch_y), gx, gy, r, strength, stroke_sign);
                stretch_x = cursor_x;
                stretch_y = cursor_y;
            }
            case BEACH -> editor.applyBeach(gx, gy, r, strength, stroke_sign, t);
            default -> {
            }
        }
    }

    private void undo() {
        takeBack(history, redo_history, false, "nothing_to_undo");
    }

    private void redo() {
        takeBack(redo_history, history, true, "nothing_to_redo");
    }

    /**
     * Takes back the newest step of one history and keeps what that changed on the other, so it can be taken back
     * in turn. Resources are taken back the same way either way, as a step whose undo puts back what was there; the
     * terrain editor keeps the heights itself, on stacks of its own.
     *
     * @param redo whether the step is one undo kept, so its heights are laid again rather than taken back
     */
    private void takeBack(@NonNull Deque<Object> from, @NonNull Deque<Object> to, boolean redo,
            @NonNull String nothing_key) {
        cancelStroke();
        ground_settle = true;
        Object step = from.poll();
        pruned = null;
        Object undone = null;
        if (step instanceof ResourceLayer.Stroke stroke) {
            undone = layer.undo(stroke);
            resources_modified = true;
        } else if (step instanceof TerrainStep terrain) {
            undone = takeBackTerrain(terrain, redo);
        } else if (step instanceof SpawnStep spawn) {
            undone = new SpawnStep(spawn.player(), spawns.get(spawn.player()));
            setSpawn(spawn.player(), spawn.cell());
        } else if (step instanceof PasteStep paste) {
            // Undone, what it placed goes first, so what it cleared comes back on the ground it stood on; redone,
            // the heights come first, for the resources to stand on.
            TerrainStep terrain = redo && paste.terrain() != null ? takeBackTerrain(paste.terrain(), true) : null;
            ResourceLayer.Stroke resources = layer.undo(paste.resources());
            resources_modified = true;
            if (!redo && paste.terrain() != null)
                terrain = takeBackTerrain(paste.terrain(), false);
            undone = new PasteStep(terrain, resources);
        } else {
            getGUIRoot().getInfoPrinter().print(MapEditor.i18n(nothing_key));
        }
        if (undone != null)
            keep(to, undone);
    }

    /** @return what was taken back, as a step that puts it back, or null if the heights were not there to take */
    private @Nullable TerrainStep takeBackTerrain(@NonNull TerrainStep terrain, boolean redo) {
        if (!(redo ? editor.redo() : editor.undo()))
            return null;
        // Undone, their cells rejoin the playable area with the heights, so they are not pruned again; redone, they
        // leave it again.
        ResourceLayer.Stroke resources = terrain.pruned();
        if (!resources.isEmpty()) {
            resources = layer.undo(resources);
            resources_modified = true;
        }
        return new TerrainStep(resources);
    }

    // ---- Spawns ----

    /**
     * A click of the spawn tool: the left button puts the chosen player's spawn under the cursor, moving it if the
     * player has one, and the right button takes away the spawn nearest the cursor.
     */
    private void clickSpawn(int sign) {
        if (!has_cursor)
            return;
        int last = editor.getSize() - 1;
        int x = Math.clamp(UnitGrid.toGridCoordinate(cursor_x), 0, last);
        int y = Math.clamp(UnitGrid.toGridCoordinate(cursor_y), 0, last);
        if (sign < 0) {
            int player = spawns.nearest(x, y, Math.max(SPAWN_PICK_CELLS, toGrid(SPAWN_RADIUS)));
            if (player < 0)
                getGUIRoot().getInfoPrinter().print(MapEditor.i18n("no_spawn_here"));
            else
                moveSpawn(player, null);
            return;
        }
        if (!isSpawnGround(x, y)) {
            getGUIRoot().getInfoPrinter().print(MapEditor.i18n("spawn_not_playable"));
            return;
        }
        moveSpawn(spawn_player, new int[]{x, y});
    }

    /**
     * Whether a spawn may lie on a cell: playable ground, or the sea, where the player starts on ships when the game
     * has them and on the nearest shore when it does not.
     */
    private boolean isSpawnGround(int x, int y) {
        return access_map.get(x, y) == AccessMap.Kind.REGION || access_map.isSea(x, y);
    }

    /** Moves a player's spawn, or takes it away when the cell is null, as a step to undo. */
    private void moveSpawn(int player, int @Nullable [] cell) {
        int[] before = spawns.get(player);
        if (Arrays.equals(before, cell))
            return;
        remember(new SpawnStep(player, before));
        setSpawn(player, cell);
    }

    private void setSpawn(int player, int @Nullable [] cell) {
        spawns = spawns.with(player, cell);
        spawns_modified = true;
        if (spawn_sync != null)
            spawn_sync.changed(player);
    }

    /** Every player's spawn, each crossed out while it is on neither playable ground nor the sea. */
    private void drawSpawns(BrushRenderer.@NonNull Batch batch) {
        for (int player = 0; player < Spawns.COUNT; player++) {
            int[] cell = spawns.get(player);
            if (cell != null)
                drawSpawn(batch, UnitGrid.coordinateFromGrid(cell[0]), UnitGrid.coordinateFromGrid(cell[1]), player,
                        .95f, isSpawnGround(cell[0], cell[1]));
        }
    }

    /** A double ring in the player's colour with the player's number in it, centred on a point in meters. */
    private static void drawSpawn(BrushRenderer.@NonNull Batch batch, float x, float y, int player, float a,
            boolean playable) {
        Vector4f[] colours = Settings.getSettings().team_colours;
        Vector4fc c = colours[player % colours.length];
        batch.circle(x, y, SPAWN_RADIUS, c.x(), c.y(), c.z(), a);
        batch.circle(x, y, SPAWN_RADIUS - .5f, c.x(), c.y(), c.z(), a);
        // North up, as the editor's camera starts.
        String number = Integer.toString(player + 1);
        float left = x - (number.length() * (DIGIT_WIDTH + DIGIT_GAP) - DIGIT_GAP) / 2f;
        for (int i = 0; i < number.length(); i++)
            drawDigit(batch, left + i * (DIGIT_WIDTH + DIGIT_GAP), y - DIGIT_HEIGHT / 2f, number.charAt(i) - '0', c,
                    a);
        if (!playable) {
            float d = SPAWN_RADIUS * .7f;
            batch.line(x - d, y - d, x + d, y + d, 1f, .2f, .2f, a);
            batch.line(x - d, y + d, x + d, y - d, 1f, .2f, .2f, a);
        }
    }

    private static void drawDigit(BrushRenderer.@NonNull Batch batch, float left, float bottom, int digit,
            @NonNull Vector4fc c, float a) {
        // A one is only the right hand stroke, which goes in the middle instead.
        if (digit == 1)
            left -= DIGIT_WIDTH / 2f;
        float right = left + DIGIT_WIDTH;
        float top = bottom + DIGIT_HEIGHT;
        float middle = bottom + DIGIT_HEIGHT / 2f;
        float[][] segments = {{left, top, right, top}, {right, top, right, middle}, {right, middle, right, bottom}, {left, bottom, right, bottom}, {left, middle, left, bottom}, {left, top, left, middle}, {left, middle, right, middle}};
        for (int i = 0; i < segments.length; i++) {
            if ((SEGMENTS[digit] & 1 << i) == 0)
                continue;
            // The batch's lines are too sparsely dotted for a figure this small, so the dots are laid here.
            float[] s = segments[i];
            int dots = Math.max(1, (int) Math.ceil(Math.hypot(s[2] - s[0], s[3] - s[1]) / DIGIT_DOT_SPACING));
            for (int k = 0; k <= dots; k++) {
                float t = k / (float) dots;
                batch.dot(s[0] + (s[2] - s[0]) * t, s[1] + (s[3] - s[1]) * t, c.x(), c.y(), c.z(), a);
            }
        }
    }

    // ---- Mouse and keys ----

    /** The per key state, since on some platforms the modifier flags stay set after the key is let go. */
    private static boolean isShiftDown() {
        var input = Renderer.getLocalInput();
        return input.isKeyDown(Key.LSHIFT) || input.isKeyDown(Key.RSHIFT);
    }

    private boolean isOverToolbar(int x, int y) {
        return isOver(toolbar, x, y);
    }

    private static boolean isOver(@NonNull Form form, int x, int y) {
        return x >= form.getX() && x < form.getX() + form.getWidth() && y >= form.getY()
                && y < form.getY() + form.getHeight();
    }

    @Override
    public void mousePressed(@NonNull MouseButton button, int x, int y) {
        if (map_mode || isOverToolbar(x, y))
            return;
        switch (button) {
            case LEFT -> beginStroke(1);
            case RIGHT -> beginStroke(-1);
            case MIDDLE -> {
                cancelStroke();
                endLook();
                look = new LookDelegate();
                getGUIRoot().pushDelegate(look);
            }
        }
    }

    @Override
    public void mouseReleased(@NonNull MouseButton button, int x, int y) {
        if (map_mode) {
            // Clicking the overview flies down to that spot, as in a game.
            if (button == MouseButton.LEFT && getCamera() instanceof MapCamera map_camera)
                picker.pickMapGoto(x, y, map_camera);
            return;
        }
        if (button == MouseButton.MIDDLE)
            endLook();
        if ((button == MouseButton.LEFT && stroke_sign > 0) || (button == MouseButton.RIGHT && stroke_sign < 0))
            endStroke();
    }

    /** Ends the middle button view turn, if one is under way. */
    private void endLook() {
        LookDelegate turning = look;
        look = null;
        if (turning != null)
            turning.pop();
    }

    @Override
    public void mouseMoved(int x, int y) {
        game_camera.mouseMoved(x, y);
    }

    @Override
    public void mouseDragged(@NonNull MouseButton button, int x, int y, int relative_x, int relative_y,
            int absolute_x, int absolute_y) {
        if (button == MouseButton.MIDDLE && look != null) {
            look.getCamera().mouseMoved(x, y);
            return;
        }
        game_camera.mouseMoved(x, y);
    }

    @Override
    public void mouseScrolled(int amount) {
        if (map_mode)
            return;
        // The per key state, since on some platforms the modifier flags stay set after the key is let go.
        var input = Renderer.getLocalInput();
        int steps = Integer.signum(amount);
        if (input.isKeyDown(Key.LCONTROL) || input.isKeyDown(Key.RCONTROL)) {
            radius = Math.clamp(steps > 0 ? radius * RADIUS_STEP : radius / RADIUS_STEP, MIN_RADIUS, MAX_RADIUS);
            refreshLabels();
        } else if (isShiftDown()) {
            intensity = Math.clamp(intensity + steps * INTENSITY_STEP, MIN_INTENSITY, MAX_INTENSITY);
            refreshLabels();
        } else {
            game_camera.mouseScrolled(amount);
        }
    }

    @Override
    public void handleInput(@NonNull InputEvent event) {
        // Space is also the activate key, which would otherwise click the editor like a left button.
        event.consumeAction(GameAction.UI_ACTIVATE);
        if (event.hasAction(GameAction.CAMERA_MAP_MODE)) {
            // Only a fresh press toggles map mode; holding the key must not flip it back and forth.
            if (event.getPhase() == InputPhase.PRESSED && !map_mode) {
                event.consumeAction(GameAction.CAMERA_MAP_MODE);
                enterMapMode();
                event.consume();
                return;
            }
            if (event.getPhase() != InputPhase.PRESSED) {
                event.consumeAction(GameAction.CAMERA_MAP_MODE);
                event.consume();
                return;
            }
            // A press while in map mode is the map camera's to handle: it flies back.
        }
        if (handleChatKey(event))
            return;
        if (event.getPhase() == InputPhase.PRESSED && !map_mode) {
            if (event.isControlDown() && (event.getKeyCode() == Key.Y || (event.getKeyCode() == Key.Z
                    && isShiftDown()))) {
                redo();
                event.consume();
                return;
            }
            if (event.isControlDown() && event.getKeyCode() == Key.Z) {
                undo();
                event.consume();
                return;
            }
            if (handleBrushKey(event)) {
                event.consume();
                return;
            }
            if (event.consumeAction(GameAction.GLOBAL_MENU) || event.consumeAction(GameAction.UI_CANCEL)) {
                openMenu();
                event.consume();
                return;
            }
        }
        super.handleInput(event);
    }

    /**
     * The keys of a course or a copy: Enter, Backspace and Escape for a course, R, M and Escape for a copy.
     *
     * @return whether the key was taken
     */
    private boolean handleBrushKey(@NonNull InputEvent event) {
        Key key = event.getKeyCode();
        if (key == null || event.isControlDown() || event.isAltDown())
            return false;
        if (brush.isCourse()) {
            switch (key) {
                case RETURN -> {
                    // In a session, Enter with no course to lay opens the chat.
                    if (course.isEmpty() && session != null)
                        return false;
                    layCourse();
                }
                case BACK -> removeCoursePoint();
                case ESCAPE -> {
                    if (course.isEmpty())
                        return false;
                    course.clear();
                }
                default -> {
                    return false;
                }
            }
            event.consumeAction(GameAction.UI_CANCEL);
            event.consumeAction(GameAction.GLOBAL_MENU);
            return true;
        }
        if (brush == Brush.COPY && clipboard != null) {
            switch (key) {
                case R -> turnClipboard(false);
                case M -> turnClipboard(true);
                case ESCAPE -> dropClipboard();
                default -> {
                    return false;
                }
            }
            event.consumeAction(GameAction.UI_CANCEL);
            event.consumeAction(GameAction.GLOBAL_MENU);
            return true;
        }
        return false;
    }

    /**
     * The chat key opens the session's chat once it comes up, as in a game, so letting go of it does not send the empty
     * line at once.
     *
     * @return whether the key was taken
     */
    private boolean handleChatKey(@NonNull InputEvent event) {
        if (session == null)
            return false;
        if (event.getPhase() == InputPhase.PRESSED) {
            if (event.consumeAction(GameAction.GLOBAL_CHAT) || event.consumeAction(GameAction.GLOBAL_CHAT_TEAM)) {
                chat_key_down = true;
                event.consume();
                return true;
            }
        } else if (event.getPhase() == InputPhase.RELEASED && chat_key_down) {
            if (event.consumeAction(GameAction.GLOBAL_CHAT) || event.consumeAction(GameAction.GLOBAL_CHAT_TEAM)) {
                chat_key_down = false;
                openChat();
                event.consume();
                return true;
            }
        }
        return false;
    }

    /** Shows the session's chat at the bottom left, ready to type in. */
    private void openChat() {
        EditorSession current = session;
        if (current == null || getGUIRoot().getModalDelegate() != null)
            return;
        EditorChatForm form = chat_form;
        if (form == null) {
            form = new EditorChatForm(getGUIRoot().getInfoPrinter(), current);
            form.addCloseListener(() -> {
                chat_form = null;
                setFocus();
            });
            chat_form = form;
            addChild(form);
            form.setPos(GameCamera.SCROLL_BUFFER, GameCamera.SCROLL_BUFFER);
        }
        form.setFocus();
    }

    private void closeChat() {
        EditorChatForm form = chat_form;
        chat_form = null;
        if (form != null)
            form.remove();
    }

    // ---- Brush outline ----

    @Override
    public void render3D(@NonNull LandscapeRenderer renderer, @NonNull RenderQueues render_queues,
            @NonNull CameraState state, @NonNull MatrixStack model_view, @NonNull MatrixStack projection) {
        tint.render(Renderer.getRenderer().getRenderContext(), renderer, state);
        edges.render(Renderer.getRenderer().getRenderContext(), state);
        // The water reflection is drawn from a camera mirrored below the sea; the brush has no place in it.
        if (state.getCurrentZ() < world.getHeightMap().getSeaLevelMeters())
            return;
        RemoteEditors others = remotes;
        if (others != null) {
            try (BrushRenderer.Batch batch = brush_renderer.begin(renderer, model_view, projection)) {
                others.renderGround(batch);
            }
            others.renderCameras(Renderer.getRenderer().getRenderContext(), state);
        }
        boolean brush_course = brush.isCourse();
        boolean show_brush = !map_mode && getGUIRoot().getModalDelegate() == null
                && (has_cursor || (brush_course && !course.isEmpty()));
        // Spawns float on the sea, where ships start from them.
        boolean spawn_cursor = show_brush && has_cursor && brush == Brush.SPAWN;
        if (!spawns.isEmpty() || spawn_cursor) {
            try (BrushRenderer.Batch batch = brush_renderer.beginAbove(renderer, model_view, projection,
                    world.getHeightMap().getSeaLevelMeters())) {
                drawSpawns(batch);
                // Where a click puts the chosen player's spawn.
                if (spawn_cursor)
                    drawSpawn(batch, cursor_x, cursor_y, spawn_player, .5f, true);
            }
        }
        if (!show_brush)
            return;
        try (BrushRenderer.Batch batch = brush_renderer.begin(renderer, model_view, projection)) {
            float r = stroke_sign < 0 ? 1f : .4f;
            float g = stroke_sign < 0 ? .4f : 1f;
            float b = stroke_sign == 0 ? 1f : .4f;
            if (brush_course)
                drawCourse(batch, r, g, b);
            if (!has_cursor)
                return;
            Clipboard copy = clipboard;
            if (brush == Brush.COPY) {
                if (copy != null) {
                    // Where the copy will land.
                    float m = HeightMap.METERS_PER_UNIT_GRID;
                    batch.rectangle(pasteX(copy) * m, pasteY(copy) * m, (pasteX(copy) + copy.width() - 1) * m,
                            (pasteY(copy) + copy.height() - 1) * m, r, g, b, .9f);
                } else if (stroke_sign != 0) {
                    batch.rectangle(ramp_x, ramp_y, cursor_x, cursor_y, r, g, b, .9f);
                }
                batch.dot(cursor_x, cursor_y, r, g, b, .9f);
                return;
            }
            if (brush == Brush.SPAWN)
                return;
            batch.circle(cursor_x, cursor_y, radius, r, g, b, .9f);
            batch.dot(cursor_x, cursor_y, r, g, b, .9f);
            if ((brush.isDragShape() || brush == Brush.STRETCH) && stroke_sign != 0) {
                batch.circle(ramp_x, ramp_y, radius, r, g, b, .9f);
                batch.line(ramp_x, ramp_y, cursor_x, cursor_y, r, g, b, .9f);
            }
        }
    }

    /**
     * The course clicked so far, and on to the cursor as its next point would take it. A path's legs too steep to
     * walk show red.
     */
    private void drawCourse(BrushRenderer.@NonNull Batch batch, float r, float g, float b) {
        List<float[]> clicked = new ArrayList<>(course);
        if (has_cursor)
            clicked.add(new float[]{cursor_x, cursor_y, editor.getHeight(toGrid(cursor_x), toGrid(cursor_y))});
        List<float[]> points = new ArrayList<>(clicked.size());
        for (float[] point : clicked)
            points.add(new float[]{toGrid(point[0]), toGrid(point[1])});
        for (float[] point : course)
            batch.circle(point[0], point[1], 1.5f, r, g, b, .9f);
        if (points.size() < 2)
            return;
        BrushPath path = new BrushPath(points);
        boolean[] steep = brush == Brush.PATH ? editor.steepLegs(path, pathHeights(clicked)) : new boolean[0];
        float[] knots = path.knots();
        float[] curve = path.curve();
        float m = HeightMap.METERS_PER_UNIT_GRID;
        int leg = 0;
        for (int i = 2; i < curve.length; i += 2) {
            float along = path.along(i / 2);
            while (leg + 2 < knots.length && along > knots[leg + 1])
                leg++;
            boolean red = leg < steep.length && steep[leg];
            batch.line(curve[i - 2] * m, curve[i - 1] * m, curve[i] * m, curve[i + 1] * m, red ? 1f : r,
                    red ? .2f : g, red ? .2f : b, .9f);
        }
    }

    // ---- Menu, saving and leaving ----

    // ---- Map mode ----

    private void enterMapMode() {
        cancelStroke();
        map_mode = true;
        has_cursor = false;
        toolbar.remove();
        setFocus();
        game_camera.disable();
        setCamera(new MapCamera(this, game_camera));
        getCamera().enable();
    }

    @Override
    public void exitMapMode() {
        map_mode = false;
        getCamera().disable();
        // Land exactly where map mode started instead of easing in from the overview.
        game_camera.getState().snapToTarget();
        setCamera(game_camera);
        game_camera.enable();
        addChild(toolbar);
    }

    private void openMenu() {
        cancelStroke();
        // Keep the keyboard on the editor once the menu closes, not on a toolbar button.
        setFocus();
        if (getGUIRoot().getModalDelegate() != null)
            return;
        getGUIRoot().addModalForm(new EditorMenu(this::saveMap, this::exit,
                session == null && EditorSession.canHost() ? this::share : null));
    }

    /** Saves the island as a map, asking for its name. */
    private void saveMap() {
        Path dir = MapEditor.getMapsDir();
        if (dir == null) {
            getGUIRoot().addModalForm(new MessageForm(MapEditor.i18n("no_maps_dir")));
            return;
        }
        getGUIRoot().addModalForm(new SaveMapDialog(getGUIRoot(), dir, map_name != null ? map_name : "", description,
                (name, new_description) -> {
                    boolean keep_heights = edited || editor.isModified();
                    MapFile.Resources current = currentResources();
                    float[][] heights = editor.copyHeights();
                    // The preview shows the map as it is, even what is generated again from the settings.
                    MapPreview preview = MapPreview.render(heights, settings, current, spawns);
                    try {
                        new MapFile(name, settings, keep_heights ? heights : null, resources_edited ? current : null,
                                preview, new_description, spawns).save(dir);
                    } catch (IOException e) {
                        getGUIRoot().addModalForm(new MessageForm(MapEditor.i18n("save_failed", e.getMessage())));
                        return;
                    }
                    map_name = name;
                    description = new_description;
                    edited = keep_heights;
                    editor.markSaved();
                    resources_modified = false;
                    spawns_modified = false;
                    getGUIRoot().getInfoPrinter().print(MapEditor.i18n("saved", name));
                }));
    }

    /** The resources as they are now, by kind. */
    private MapFile.@NonNull Resources currentResources() {
        @SuppressWarnings("unchecked") List<int[]>[] positions = new List[Resource.values().length];
        for (Resource kind : Resource.values())
            positions[kind.ordinal()] = layer.positions(kind);
        return new MapFile.Resources(positions);
    }

    private boolean isModified() {
        return editor.isModified() || resources_modified || spawns_modified;
    }

    private void exit() {
        if (session != null) {
            getGUIRoot().addModalForm(new QuestionForm(MapEditor.i18n("session_exit_confirm"),
                    (_, _, _, _) -> leave()));
        } else if (isModified()) {
            getGUIRoot().addModalForm(new QuestionForm(MapEditor.i18n("exit_confirm"), (_, _, _, _) -> leave()));
        } else {
            leave();
        }
    }

    private void leave() {
        EditorSession current = session;
        endSession();
        if (current != null) {
            current.leave();
            SelectGameMenu.openEditorSessionsNext();
        }
        closeEditor();
        Renderer.startMenu(network, getGUIRoot().getGUI());
    }

    /** Lets go of what the editor made for drawing, as the screen is left. */
    private void closeEditor() {
        if (ground != null)
            ground.close();
        tint.close();
        edges.close();
    }

    // ---- Shared sessions ----

    /** Opens a session on this island, for others to join from the multiplayer menu's shared sessions. */
    void hostSession(@NonNull String name) {
        SessionSync new_sync = startSync();
        EditorSession opened = EditorSession.host(name, settings, new SessionEditor());
        if (opened == null) {
            endSession();
            getGUIRoot().addModalForm(new MessageForm(MapEditor.i18n("shared_not_connected")));
            return;
        }
        session = opened;
        sync = new_sync;
        session_live = true;
        getGUIRoot().getInfoPrinter().print(MapEditor.i18n("session_opened", name));
    }

    /** Takes part in the session this island was handed over from, laying over it what was edited since. */
    void joinSession(@NonNull EditorSession joined) {
        if (attachSession(joined))
            getGUIRoot().getInfoPrinter().print(MapEditor.i18n("session_joined", joined.getName()));
    }

    /** From the editor menu: opens a session on the island as it is, named after it. */
    private void share() {
        hostSession(map_name != null ? map_name : MapEditor.i18n("session_default_name", EditorSession.localNick()));
    }

    /** @return whether the session is still under way */
    private boolean attachSession(@NonNull EditorSession joined) {
        startSync();
        session = joined;
        joined.attach(new SessionEditor());
        if (session == null)
            return false;
        session_live = true;
        return true;
    }

    private @NonNull SessionSync startSync() {
        // Edits only go out once they are in the session's island, which starts as the island is now.
        editor.flush();
        SessionSync new_sync = new SessionSync(new SessionSync.Ground() {
            @Override
            public float @NonNull [] @NonNull [] heights() {
                return editor.heights();
            }

            @Override
            public void rounded(int x0, int y0, int x1, int y1) {
                editor.heightsWritten(x0, y0, x1, y1);
            }

            @Override
            public void applyShared(int x0, int y0, int width, int height, float @NonNull [] values) {
                editor.applyShared(x0, y0, width, height, values);
            }
        }, new SessionSync.Supplies() {
            @Override
            public byte kind(int cell) {
                Resource kind = layer.get(cell % editor.getSize(), cell / editor.getSize());
                return kind != null ? (byte) kind.ordinal() : EditOp.NO_RESOURCE;
            }

            @Override
            public void applyShared(int @NonNull [] cells, byte @NonNull [] kinds, int count) {
                layer.applyShared(cells, kinds, count);
            }
        }, editor.getSize());
        sync = new_sync;
        link.set(new_sync);
        spawn_sync = new SpawnSync();
        RemoteEditors others = new RemoteEditors(this, editor.getSize());
        others.setRosterTop(rosterTop());
        remotes = others;
        // Whatever is saved from a shared island keeps what was made of it.
        edited = true;
        resources_edited = true;
        return new_sync;
    }

    /** Goes on alone: the others and their cameras go, the island stays. */
    private void endSession() {
        session = null;
        session_live = false;
        sync = null;
        spawn_sync = null;
        link.set(null);
        closeChat();
        RemoteEditors others = remotes;
        remotes = null;
        if (others != null) {
            others.clear();
            others.close();
        }
    }

    /** Sends this player's edits and whereabouts now and then, and eases the others' cameras along. */
    private void updateSession(float t) {
        remote_busy = Math.max(0f, remote_busy - t);
        EditorSession current = session;
        RemoteEditors others = remotes;
        if (current == null || others == null)
            return;
        others.tick(t);
        send_timer += t;
        if (send_timer >= send_interval)
            sendEdits();
        presence_timer += t;
        presence_idle += t;
        if (presence_timer >= PRESENCE_INTERVAL) {
            presence_timer = 0f;
            Camera camera = getCamera();
            CameraState state = camera != null ? camera.getState() : game_camera.getState();
            EditorSession.Presence presence = new EditorSession.Presence(state.getCurrentX(), state.getCurrentY(),
                    state.getCurrentZ(), state.getHorizAngle(), state.getCurrentVertAngle(), cursor_x, cursor_y,
                    radius, EditorSession.Presence.pack(brush, has_cursor, stroke_sign, map_mode));
            if (!presence.equals(last_presence) || presence_idle >= PRESENCE_KEEPALIVE) {
                current.sendPresence(presence);
                last_presence = presence;
                presence_idle = 0f;
            }
        }
    }

    /**
     * Sends what this player changed since the last time as edits, the heights and resources and the spawns, if
     * anything, and waits longer after a big one.
     */
    private void sendEdits() {
        send_timer = 0f;
        EditorSession current = session;
        SessionSync current_sync = sync;
        if (current == null || current_sync == null)
            return;
        int sent = 0;
        EditOp op = current_sync.take();
        if (op != null)
            sent += current.sendEdit(SessionMessage.encode(SessionMessage.TERRAIN, op.encode()));
        SpawnSync spawn_edits = spawn_sync;
        byte[] spawn_op = spawn_edits != null ? spawn_edits.take(spawns) : null;
        if (spawn_op != null)
            sent += current.sendEdit(SessionMessage.encode(SessionMessage.SPAWNS, spawn_op));
        send_interval = Math.max(SEND_INTERVAL, sent / SEND_RATE);
    }

    /** What the session tells this editor. */
    private final class SessionEditor implements EditorSession.Editor {
        @Override
        public void memberJoined(int slot, @NonNull String nick) {
            EditorSession current = session;
            boolean self = current != null && slot == current.getSlot();
            if (remotes != null)
                remotes.memberJoined(slot, nick, self);
            // Those already there when this player joined are only listed.
            if (!self && session_live)
                getGUIRoot().getInfoPrinter().print(MapEditor.i18n("session_member_joined", nick));
        }

        @Override
        public void memberLeft(int slot, @NonNull String nick) {
            if (remotes != null)
                remotes.memberLeft(slot);
            getGUIRoot().getInfoPrinter().print(MapEditor.i18n("session_member_left", nick));
        }

        @Override
        public void received(int slot, byte @NonNull [] message) {
            try {
                byte kind = SessionMessage.kind(message);
                switch (kind) {
                    case SessionMessage.TERRAIN ->
                        edited(slot, EditOp.decode(SessionMessage.payload(message), editor.getSize()));
                    case SessionMessage.SPAWNS -> {
                        SpawnSync spawn_edits = spawn_sync;
                        if (spawn_edits != null) {
                            spawns = spawn_edits.apply(spawns, SessionMessage.payload(message), editor.getSize());
                            spawns_modified = true;
                        }
                    }
                    default -> IO.println("Dropping an edit of unknown kind " + kind + " from slot " + slot);
                }
            } catch (IOException | RuntimeException e) {
                IO.println("Dropping an edit from slot " + slot + " that could not be read: " + e);
            }
        }

        private void edited(int slot, @NonNull EditOp op) {
            if (sync == null)
                return;
            sync.apply(op);
            if (remotes != null)
                remotes.edited(slot, op);
            remote_busy = REMOTE_QUIET;
            ground_settle = true;
            if (op.resource_cells().length > 0)
                resources_modified = true;
        }

        @Override
        public void acknowledged(byte kind) {
            if (kind == SessionMessage.TERRAIN && sync != null)
                sync.acknowledged();
            else if (kind == SessionMessage.SPAWNS && spawn_sync != null)
                spawn_sync.acknowledged();
        }

        @Override
        public void presence(int slot, EditorSession.@NonNull Presence presence) {
            if (remotes != null)
                remotes.presence(slot, presence);
        }

        @Override
        public byte @NonNull [] snapshot() throws IOException {
            // What this player changed goes out first, so the island handed over holds nothing that is not on its way.
            // A drag or a course laid since the last frame has not reached the renderer, so look everywhere.
            if (sync != null)
                sync.changedAnywhere();
            sendEdits();
            return new MapFile(map_name != null ? map_name : "", settings, editor.copyHeights(), currentResources(),
                    null, description, spawns).toBytes();
        }

        @Override
        public void chatted() {
            if (chat_form != null)
                chat_form.refresh();
        }

        @Override
        public void ended(@NonNull String reason) {
            endSession();
            getGUIRoot().addModalForm(new MessageForm(MapEditor.i18n("session_ended", reason)));
        }
    }

    /**
     * Turns the view with the game's first person camera while the middle button is held.
     *
     * <p>Another button pressed meanwhile is pressed on this delegate, as it covers the screen, and so are the releases
     * after it, the middle button's among them. Letting go of that other button also turns the drag into plain mouse
     * moves. So this delegate follows both and ends the turn itself, as the game's first person delegate does.
     */
    private final class LookDelegate extends CameraDelegate<FirstPersonCamera> {
        LookDelegate() {
            super(MapEditorDelegate.this.getGUIRoot(), new FirstPersonCamera(MapEditorDelegate.this,
                    world.getHeightMap(), game_camera.getState()));
        }

        @Override
        public void mousePressed(@NonNull MouseButton button, int x, int y) {
        }

        @Override
        public void mouseReleased(@NonNull MouseButton button, int x, int y) {
            if (button == MouseButton.MIDDLE)
                endLook();
        }

        @Override
        public void mouseMoved(int x, int y) {
            getCamera().mouseMoved(x, y);
        }

        @Override
        public void mouseDragged(@NonNull MouseButton button, int x, int y, int relative_x, int relative_y,
                int absolute_x, int absolute_y) {
            getCamera().mouseMoved(x, y);
        }

        @Override
        protected @NonNull CursorType getCursorType() {
            return CursorType.NULL;
        }
    }
}
