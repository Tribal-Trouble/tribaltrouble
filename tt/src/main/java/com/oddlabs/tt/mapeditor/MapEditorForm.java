package com.oddlabs.tt.mapeditor;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.form.MessageForm;
import com.oddlabs.tt.form.ProgressForm;
import com.oddlabs.tt.gui.CancelButton;
import com.oddlabs.tt.gui.FocusDirection;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUIObject;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.gui.PulldownButton;
import com.oddlabs.tt.gui.PulldownItem;
import com.oddlabs.tt.gui.PulldownMenu;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.gui.Slider;
import com.oddlabs.tt.util.ServerMessageBundler;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.BOTTOM_RIGHT;
import static com.oddlabs.tt.gui.Placement.LEFT_MID;
import static com.oddlabs.tt.gui.Placement.RIGHT_MID;
import static com.oddlabs.tt.gui.Placement.TOP_LEFT;
import static com.oddlabs.tt.gui.Placement.TOP_MID;

/**
 * The map editor's start window: pick the island to start from, by options or map code, or open a saved map. Opened
 * from the multiplayer menu, it hosts a shared session on the island instead of editing it alone.
 */
public final class MapEditorForm extends Form {
    private static final int SLIDER_LENGTH = 250;
    private static final int BUTTON_WIDTH = 100;
    private static final int PULLDOWN_WIDTH = 150;

    private final @NonNull NetworkSelector network;
    private final @NonNull GUIRoot gui_root;

    private final @NonNull PulldownMenu<Void> pulldown_size;
    private final @NonNull PulldownMenu<Void> pulldown_terrain;
    private final @NonNull Slider slider_hills;
    private final @NonNull Slider slider_trees;
    private final @NonNull Slider slider_supplies;
    private final @NonNull Label label_mapcode;
    // The map code in a smaller font, for a code too long for the line in the headline font.
    private final @NonNull Label label_mapcode_small;
    private final @NonNull Label label_map;
    private final @NonNull HorizButton button_start;
    // Where to go back to when hosting from the multiplayer menu, and null when editing alone.
    private final @Nullable Runnable back;

    private @NonNull MapSettings settings;
    // The saved map the settings came from, and its edited heights, resources and spawns if it has them.
    private @Nullable String map_name;
    private @NonNull String description = "";
    private float @Nullable [] @Nullable [] heights;
    private MapFile.@Nullable Resources resources;
    private @NonNull Spawns spawns = Spawns.NONE;
    private boolean applying;

    public MapEditorForm(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root) {
        this(network, gui_root, null);
    }

    /**
     * The start window for hosting a shared session, from the multiplayer menu.
     *
     * @param back reopens the multiplayer menu if the window is cancelled
     */
    public static @NonNull MapEditorForm forHosting(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root,
            @NonNull Runnable back) {
        return new MapEditorForm(network, gui_root, back);
    }

    private MapEditorForm(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root, @Nullable Runnable back) {
        this.network = network;
        this.gui_root = gui_root;
        this.back = back;
        long tick = LocalEventQueue.getQueue().getHighPrecisionManager().getTick();
        this.settings = MapSettings.random(new Random(tick * tick));

        Label label_headline = new Label(MapEditor.i18n("headline"), Skin.getSkin().getHeadlineFont());
        addChild(label_headline);

        // Island size and terrain type
        pulldown_size = new PulldownMenu<>();
        for (int i = 0; i < MapSettings.NUM_SIZES; i++)
            pulldown_size.addItem(new PulldownItem<>(ServerMessageBundler.getSizeString(i)));
        pulldown_terrain = new PulldownMenu<>();
        pulldown_terrain.addItem(new PulldownItem<>(ServerMessageBundler.getTerrainTypeString(
                Game.TERRAIN_TYPE_NATIVE)));
        pulldown_terrain.addItem(new PulldownItem<>(ServerMessageBundler.getTerrainTypeString(
                Game.TERRAIN_TYPE_VIKING)));
        Group group_size = labelled("island_size", new PulldownButton<>(gui_root, pulldown_size, settings.size(),
                PULLDOWN_WIDTH));
        Group group_terrain = labelled("terrain_type", new PulldownButton<>(gui_root, pulldown_terrain,
                settings.terrain(), PULLDOWN_WIDTH));
        addChild(group_size);
        addChild(group_terrain);
        pulldown_size.addItemChosenListener((_, index) -> settingsChanged(settings.withSize(index)));
        pulldown_terrain.addItemChosenListener((_, index) -> settingsChanged(settings.withTerrain(index)));

        // Sliders, laid out like the skirmish menu's advanced options
        Group group_sliders = new Group();
        slider_hills = new Slider(SLIDER_LENGTH, 0, MapSettings.SLIDER_MAX, settings.hills());
        slider_trees = new Slider(SLIDER_LENGTH, 0, MapSettings.SLIDER_MAX, settings.trees());
        slider_supplies = new Slider(SLIDER_LENGTH, 0, MapSettings.SLIDER_MAX, settings.supplies());
        SliderRow row_supplies = new SliderRow(group_sliders, "resources", slider_supplies);
        SliderRow row_trees = new SliderRow(group_sliders, "trees", slider_trees);
        SliderRow row_hills = new SliderRow(group_sliders, "hills", slider_hills);
        row_supplies.place(null);
        row_trees.place(row_supplies);
        row_hills.place(row_trees);
        group_sliders.compileCanvas();
        addChild(group_sliders);
        slider_hills.addValueListener(value -> settingsChanged(settings.withHills((int) value)));
        slider_trees.addValueListener(value -> settingsChanged(settings.withTrees((int) value)));
        slider_supplies.addValueListener(value -> settingsChanged(settings.withSupplies((int) value)));

        // The window is as wide as the sliders and the rows above them, and what goes below keeps to that.
        int content_width = Math.max(group_sliders.getWidth(), Math.max(group_size.getWidth(),
                group_terrain.getWidth()));
        int spacing = Skin.getSkin().getFormData().objectSpacing();

        // Map code and the map being edited. The code gets a line of its own.
        Group group_code = new Group();
        Label label_code = new Label(MapEditor.terrainI18n("map_code"), Skin.getSkin().getEditFont());
        label_mapcode = new Label("", Skin.getSkin().getHeadlineFont(), content_width);
        label_mapcode_small = new Label("", Skin.getSkin().getEditFont(), content_width);
        group_code.addChild(label_code);
        group_code.addChild(label_mapcode);
        group_code.addChild(label_mapcode_small);
        label_code.place();
        label_mapcode.place(label_code, BOTTOM_LEFT);
        label_mapcode_small.place(label_code, BOTTOM_LEFT);
        group_code.compileCanvas();
        addChild(group_code);
        label_map = new Label("", Skin.getSkin().getEditFont(), content_width);
        addChild(label_map);

        // Buttons: map code, save and load, as wide together as the sliders, with start and cancel under them.
        Group group_buttons = new Group();
        HorizButton button_mapcode = new HorizButton(MapEditor.terrainI18n("enter_map_code"), Math.max(170,
                content_width - 2 * BUTTON_WIDTH - 2 * spacing));
        button_mapcode.addMouseClickListener((_, _, _, _) -> gui_root.addModalForm(new MapcodeDialog(
                settings.toMapcode(), this::applyMapcode)));
        HorizButton button_save = new HorizButton(MapEditor.i18n("save"), BUTTON_WIDTH);
        button_save.addMouseClickListener((_, _, _, _) -> save());
        HorizButton button_load = new HorizButton(MapEditor.i18n("load"), BUTTON_WIDTH);
        button_load.addMouseClickListener((_, _, _, _) -> load());
        button_start = new HorizButton(MapEditor.i18n(back != null ? "session_host_button" : "start"), BUTTON_WIDTH);
        button_start.addMouseClickListener((_, _, _, _) -> start());
        HorizButton button_cancel = new CancelButton(BUTTON_WIDTH);
        button_cancel.addMouseClickListener((_, _, _, _) -> cancel());
        group_buttons.addChild(button_mapcode);
        group_buttons.addChild(button_save);
        group_buttons.addChild(button_load);
        group_buttons.addChild(button_start);
        group_buttons.addChild(button_cancel);
        button_mapcode.place();
        button_save.place(button_mapcode, RIGHT_MID);
        button_load.place(button_save, RIGHT_MID);
        button_cancel.place(button_load, BOTTOM_RIGHT);
        button_start.place(button_cancel, LEFT_MID);
        group_buttons.compileCanvas();
        addChild(group_buttons);

        int section = Skin.getSkin().getFormData().sectionSpacing();
        label_headline.place();
        group_size.place(label_headline, BOTTOM_LEFT, section);
        group_terrain.place(group_size, BOTTOM_RIGHT);
        group_sliders.place(group_terrain, BOTTOM_RIGHT, section);
        group_code.place(group_sliders, BOTTOM_LEFT, section);
        label_map.place(group_code, BOTTOM_LEFT);
        group_buttons.place(Origin.AT_END);
        compileCanvas();

        refresh();
    }

    /** A caption followed by a control, as one row. */
    private static @NonNull Group labelled(@NonNull String caption_key, @NonNull GUIObject control) {
        Group group = new Group();
        Label label = new Label(MapEditor.terrainI18n(caption_key), Skin.getSkin().getEditFont());
        group.addChild(label);
        group.addChild(control);
        label.place();
        control.place(label, RIGHT_MID);
        group.compileCanvas();
        return group;
    }

    /** A slider row: caption, "Min", the slider and "Max". */
    private static final class SliderRow {
        private final @NonNull Label caption;
        private final @NonNull Label low;
        private final @NonNull Label high;
        private final @NonNull Slider slider;

        SliderRow(@NonNull Group group, @NonNull String caption_key, @NonNull Slider slider) {
            this.caption = new Label(MapEditor.terrainI18n(caption_key), Skin.getSkin().getEditFont());
            this.low = new Label(MapEditor.terrainI18n("min"), Skin.getSkin().getEditFont());
            this.high = new Label(MapEditor.terrainI18n("max"), Skin.getSkin().getEditFont());
            this.slider = slider;
            group.addChild(caption);
            group.addChild(low);
            group.addChild(slider);
            group.addChild(high);
        }

        void place(@Nullable SliderRow below) {
            int section = Skin.getSkin().getFormData().sectionSpacing();
            if (below == null) {
                caption.place();
                low.place(caption, RIGHT_MID);
                slider.place(low, RIGHT_MID);
            } else {
                caption.place(below.caption, TOP_LEFT, section);
                slider.place(below.slider, TOP_MID, section);
                low.place(slider, LEFT_MID);
            }
            high.place(slider, RIGHT_MID);
        }
    }

    @Override
    public void setFocus(@NonNull FocusDirection direction) {
        if (direction == FocusDirection.BACKWARD) {
            super.setFocus(direction);
        } else {
            button_start.setFocus(direction);
        }
    }

    private void settingsChanged(@NonNull MapSettings new_settings) {
        if (applying || new_settings.equals(settings))
            return;
        settings = new_settings;
        if (heights != null || resources != null || !spawns.isEmpty()) {
            // Edits only fit the island they were made on, so this is a new map now.
            heights = null;
            resources = null;
            spawns = Spawns.NONE;
            map_name = null;
            description = "";
        }
        refresh();
    }

    private boolean applyMapcode(@NonNull String code) {
        MapSettings parsed = settings.parseMapcode(code);
        if (parsed == null)
            return false;
        heights = null;
        resources = null;
        spawns = Spawns.NONE;
        map_name = null;
        description = "";
        showSettings(parsed);
        return true;
    }

    /** Moves the controls to the given settings without treating it as an edit. */
    private void showSettings(@NonNull MapSettings new_settings) {
        settings = new_settings;
        applying = true;
        try {
            pulldown_size.chooseItem(settings.size());
            pulldown_terrain.chooseItem(settings.terrain());
            slider_hills.setValue(settings.hills());
            slider_trees.setValue(settings.trees());
            slider_supplies.setValue(settings.supplies());
        } finally {
            applying = false;
        }
        refresh();
    }

    private void refresh() {
        String code = settings.toMapcode();
        // A label keeps two pixels clear at its end.
        boolean fits = Skin.getSkin().getHeadlineFont().getWidth(code) + 2 <= label_mapcode.getWidth();
        label_mapcode.clear();
        label_mapcode_small.clear();
        (fits ? label_mapcode : label_mapcode_small).append(code);
        label_map.clear();
        label_map.append(map_name == null ? MapEditor.i18n("new_map") : MapEditor.i18n("current_map", map_name));
    }

    private @Nullable Path mapsDir() {
        Path dir = MapEditor.getMapsDir();
        if (dir == null)
            gui_root.addModalForm(new MessageForm(MapEditor.i18n("no_maps_dir")));
        return dir;
    }

    private void save() {
        Path dir = mapsDir();
        if (dir == null)
            return;
        gui_root.addModalForm(new SaveMapDialog(gui_root, dir, map_name != null ? map_name : "", description,
                (name, new_description) -> {
                    try {
                        MapPreview preview = heights != null ? MapPreview.render(heights, settings, resources,
                                spawns) : null;
                        new MapFile(name, settings, heights, resources, preview, new_description, spawns).save(dir);
                        map_name = name;
                        description = new_description;
                        refresh();
                        gui_root.getInfoPrinter().print(MapEditor.i18n("saved", name));
                    } catch (IOException e) {
                        gui_root.addModalForm(new MessageForm(MapEditor.i18n("save_failed", e.getMessage())));
                    }
                }));
    }

    private void load() {
        Path dir = mapsDir();
        if (dir == null)
            return;
        gui_root.addModalForm(new LoadMapDialog(gui_root, dir, MapEditor.i18n("load_caption"),
                MapEditor.i18n("load_button"), entry -> {
                    MapFile map;
                    try {
                        map = MapFile.load(entry.path());
                    } catch (IOException e) {
                        gui_root.addModalForm(new MessageForm(MapEditor.i18n("load_failed", e.getMessage())));
                        return;
                    }
                    heights = map.heights();
                    resources = map.resources();
                    spawns = map.spawns();
                    map_name = map.name();
                    description = map.description();
                    showSettings(map.settings());
                }));
    }

    @Override
    protected void doCancel() {
        if (back != null)
            back.run();
    }

    private void start() {
        button_start.setDisabled(true);
        MapEditorLoader.SessionStart session = back == null ? new MapEditorLoader.SessionStart.None() : new MapEditorLoader.SessionStart.Host(
                map_name != null ? map_name : MapEditor.i18n(
                        "session_default_name", EditorSession.localNick()));
        ProgressForm.setProgressForm(network, gui_root.getGUI(), new MapEditorLoader(network, settings, map_name,
                description, heights, resources, spawns, session));
    }
}
