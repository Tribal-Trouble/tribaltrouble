package com.oddlabs.tt.form;

import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.delegate.Menu;
import com.oddlabs.tt.editor.MapEditorSession;
import com.oddlabs.tt.editor.ui.EditorState;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.gui.*;
import com.oddlabs.tt.guievent.MouseClickListener;
import com.oddlabs.tt.util.ServerMessageBundler;
import com.oddlabs.tt.util.Utils;
import com.oddlabs.registration.RegistrationKey;

import java.util.ResourceBundle;
import java.util.Random;
import java.math.BigInteger;

/**
 * Map Editor main menu form. Mirrors TerrainMenu-style controls for
 * gamespeed, terrain type, island size, and sliders. Map IO (load/save)
 * and material painting/overlays are intentionally omitted.
 */
public final class MapEditorMenuForm extends Form {
    private final GUIRoot gui_root;
    private final NetworkSelector network;
    private final Menu main_menu;

    private final PulldownMenu pm_gamespeed;
    private final PulldownMenu pulldown_size;
    private final PulldownMenu pm_terrain_type;
    private final Slider slider_hills;
    private final Slider slider_vegetation;
    private final Slider slider_supplies;
    private final Label label_mapcode;
    private int seed;
    private boolean launching = false;
    private com.oddlabs.tt.gui.CheckBox sandboxModeCheck;

    private final ResourceBundle terrainBundle = ResourceBundle.getBundle(TerrainMenu.class.getName());
    // Align with TerrainMenu sizing and archipelago mapping
    private static final int[] SIZES = new int[] {256, 512, 1024, 2048, 2048};
    private static final boolean[] ARCHIPELAGO = new boolean[] {false, false, false, false, true};

    public MapEditorMenuForm(NetworkSelector network, GUIRoot gui_root, Menu main_menu) {
        this.gui_root = gui_root;
        this.network = network;
        this.main_menu = main_menu;

        // Headline
        Label label_headline =
                new Label(Utils.getBundleString(Menu.bundle, "map_editor"),
                        Skin.getSkin().getHeadlineFont());
        addChild(label_headline);

        // Gamespeed
        Group group_gamespeed = new Group();
        Label label_gamespeed =
                new Label(Utils.getBundleString(terrainBundle, "gamespeed"),
                        Skin.getSkin().getEditFont());
        group_gamespeed.addChild(label_gamespeed);
        pm_gamespeed = new PulldownMenu();
        pm_gamespeed.addItem(new PulldownItem(ServerMessageBundler.getGamespeedString(1)));
        pm_gamespeed.addItem(new PulldownItem(ServerMessageBundler.getGamespeedString(2)));
        pm_gamespeed.addItem(new PulldownItem(ServerMessageBundler.getGamespeedString(3)));
        pm_gamespeed.addItem(new PulldownItem(ServerMessageBundler.getGamespeedString(4)));
        PulldownButton pb_gamespeed = new PulldownButton(this.gui_root, pm_gamespeed, 1, 180);
        group_gamespeed.addChild(pb_gamespeed);
        // Place inside group
        label_gamespeed.place();
        pb_gamespeed.place(label_gamespeed, RIGHT_MID);
        group_gamespeed.compileCanvas();
        addChild(group_gamespeed);

        // Island size
        Group group_size = new Group();
        Label label_size =
                new Label(Utils.getBundleString(terrainBundle, "island_size"),
                        Skin.getSkin().getEditFont());
        group_size.addChild(label_size);
        pulldown_size = new PulldownMenu();
        pulldown_size.addItem(new PulldownItem(ServerMessageBundler.getSizeString(0)));
        pulldown_size.addItem(new PulldownItem(ServerMessageBundler.getSizeString(1)));
        pulldown_size.addItem(new PulldownItem(ServerMessageBundler.getSizeString(2)));
        pulldown_size.addItem(new PulldownItem(ServerMessageBundler.getSizeString(3))); // Large
        pulldown_size.addItem(new PulldownItem(ServerMessageBundler.getSizeString(4))); // Archipelago
        PulldownButton pb_size = new PulldownButton(this.gui_root, pulldown_size, 1, 180);
        group_size.addChild(pb_size);
        // Place inside group
        label_size.place();
        pb_size.place(label_size, RIGHT_MID);
        group_size.compileCanvas();
        addChild(group_size);

        // Terrain type
        Group group_terrain_type = new Group();
        Label label_terrain_type =
                new Label(Utils.getBundleString(terrainBundle, "terrain_type"),
                        Skin.getSkin().getEditFont());
        group_terrain_type.addChild(label_terrain_type);
        pm_terrain_type = new PulldownMenu();
        pm_terrain_type.addItem(new PulldownItem(ServerMessageBundler.getTerrainTypeString(0)));
        pm_terrain_type.addItem(new PulldownItem(ServerMessageBundler.getTerrainTypeString(1)));
        PulldownButton pb_terrain_type = new PulldownButton(this.gui_root, pm_terrain_type, 0, 180);
        group_terrain_type.addChild(pb_terrain_type);
        // Place inside group
        label_terrain_type.place();
        pb_terrain_type.place(label_terrain_type, RIGHT_MID);
        group_terrain_type.compileCanvas();
        addChild(group_terrain_type);

        // Sliders
        Group group_sliders = new Group();
        Label label_hills = new Label(Utils.getBundleString(terrainBundle, "hills"), Skin.getSkin().getEditFont());
        Label label_vegetation = new Label(Utils.getBundleString(terrainBundle, "trees"), Skin.getSkin().getEditFont());
        Label label_supplies = new Label(Utils.getBundleString(terrainBundle, "resources"), Skin.getSkin().getEditFont());
        Label label_hills_low = new Label(Utils.getBundleString(terrainBundle, "min"), Skin.getSkin().getEditFont());
        Label label_hills_high = new Label(Utils.getBundleString(terrainBundle, "max"), Skin.getSkin().getEditFont());
        Label label_vegetation_low = new Label(Utils.getBundleString(terrainBundle, "min"), Skin.getSkin().getEditFont());
        Label label_vegetation_high = new Label(Utils.getBundleString(terrainBundle, "max"), Skin.getSkin().getEditFont());
        Label label_supplies_low = new Label(Utils.getBundleString(terrainBundle, "min"), Skin.getSkin().getEditFont());
        Label label_supplies_high = new Label(Utils.getBundleString(terrainBundle, "max"), Skin.getSkin().getEditFont());
        group_sliders.addChild(label_hills);
        group_sliders.addChild(label_vegetation);
        group_sliders.addChild(label_supplies);
        group_sliders.addChild(label_hills_low);
        group_sliders.addChild(label_hills_high);
        group_sliders.addChild(label_vegetation_low);
        group_sliders.addChild(label_vegetation_high);
        group_sliders.addChild(label_supplies_low);
        group_sliders.addChild(label_supplies_high);

        slider_hills = new Slider(250, 0, 10, 5);
        slider_vegetation = new Slider(250, 0, 10, 5);
        slider_supplies = new Slider(250, 0, 10, 5);
        group_sliders.addChild(slider_hills);
        group_sliders.addChild(slider_vegetation);
        group_sliders.addChild(slider_supplies);

        // place sliders
        label_supplies.place();
        label_supplies_low.place(label_supplies, RIGHT_MID);
        slider_supplies.place(label_supplies_low, RIGHT_MID);
        label_supplies_high.place(slider_supplies, RIGHT_MID);

        label_vegetation.place(label_supplies, TOP_LEFT, Skin.getSkin().getFormData().getSectionSpacing());
        slider_vegetation.place(slider_supplies, TOP_MID, Skin.getSkin().getFormData().getSectionSpacing());
        label_vegetation_low.place(slider_vegetation, LEFT_MID);
        label_vegetation_high.place(slider_vegetation, RIGHT_MID);

        label_hills.place(label_vegetation, TOP_LEFT, Skin.getSkin().getFormData().getSectionSpacing());
        slider_hills.place(slider_vegetation, TOP_MID, Skin.getSkin().getFormData().getSectionSpacing());
        label_hills_low.place(slider_hills, LEFT_MID);
        label_hills_high.place(slider_hills, RIGHT_MID);

        group_sliders.compileCanvas();
        addChild(group_sliders);

        // Sandbox mode checkbox
        Group group_mode = new Group();
        sandboxModeCheck = new com.oddlabs.tt.gui.CheckBox(
                false,
                "Sandbox Mode",
                "Enable experimental tools & overlays. Sandbox maps can't be published.");
        group_mode.addChild(sandboxModeCheck);
        sandboxModeCheck.place();
        group_mode.compileCanvas();
        addChild(group_mode);

        // Map code (display only)
        Group group_seed = new Group();
        Label label_seed = new Label(Utils.getBundleString(terrainBundle, "map_code"), Skin.getSkin().getEditFont());
        label_mapcode = new Label("", Skin.getSkin().getHeadlineFont(), 250);
        group_seed.addChild(label_seed);
        group_seed.addChild(label_mapcode);
        label_seed.place();
        label_mapcode.place(label_seed, RIGHT_MID);
        group_seed.compileCanvas();
        addChild(group_seed);

        // Buttons (no Load/Save)
        Group group_buttons = new Group();
        final HorizButton button_ok = new OKButton(100);
        button_ok.addMouseClickListener(
                new MouseClickListener() {
                    public void mouseClicked(int button, int x, int y, int clicks) {
                        if (launching) return;
                        launching = true;
                        button_ok.setDisabled(true);
                        startEditor();
                    }
                });
        HorizButton button_cancel = new CancelButton(100);
        button_cancel.addMouseClickListener(new CancelListener(this));
        group_buttons.addChild(button_ok);
        group_buttons.addChild(button_cancel);
        button_cancel.place();
        button_ok.place(button_cancel, LEFT_MID);
        group_buttons.compileCanvas();
        addChild(group_buttons);

        // Hook up mapcode updates
        slider_hills.addValueListener(new SliderUpdateMapcodeListener());
        slider_vegetation.addValueListener(new SliderUpdateMapcodeListener());
        slider_supplies.addValueListener(new SliderUpdateMapcodeListener());
        pulldown_size.addItemChosenListener(new UpdateMapcodeItemChosenListener());
        pm_terrain_type.addItemChosenListener(new UpdateMapcodeItemChosenListener());

        // Defaults and seed
        Random r = new Random(
                LocalEventQueue.getQueue().getHighPrecisionManager().getTick()
                        * LocalEventQueue.getQueue().getHighPrecisionManager().getTick());
        r.nextInt();
        seed = r.nextInt(40000);
        setMapcode();

        // Layout
        label_headline.place();
        group_gamespeed.place(label_headline, BOTTOM_LEFT);
        group_size.place(group_gamespeed, BOTTOM_LEFT);
        group_terrain_type.place(group_size, BOTTOM_LEFT);
        group_sliders.place(group_terrain_type, BOTTOM_LEFT);
        group_mode.place(group_sliders, BOTTOM_LEFT);
        group_seed.place(group_mode, BOTTOM_LEFT);
        group_buttons.place(ORIGIN_BOTTOM_RIGHT);

        compileCanvas();
    }

    public final void setFocus() {
        // Focus cancel so ESC closes
        Renderable child = getLastChild();
        while (child != null) {
            if (child instanceof Group) {
                Renderable inner = ((Group) child).getLastChild();
                while (inner != null) {
                    if (inner instanceof CancelButton) {
                        ((CancelButton) inner).setFocus();
                        return;
                    }
                    inner = (Renderable) inner.getPrior();
                }
            }
            child = (Renderable) child.getPrior();
        }
    }

    // Simplified map code generation matching exposed controls
    private void setMapcode() {
        BigInteger max_val = BigInteger.ONE;
        BigInteger result = BigInteger.ZERO;
        result = result.add(BigInteger.valueOf(seed).multiply(max_val));
        max_val = max_val.multiply(new BigInteger("40000"));
        result = result.add(BigInteger.valueOf(slider_hills.getValue()).multiply(max_val));
        max_val = max_val.multiply(BigInteger.valueOf(11));
        result = result.add(BigInteger.valueOf(slider_vegetation.getValue()).multiply(max_val));
        max_val = max_val.multiply(BigInteger.valueOf(11));
        result = result.add(BigInteger.valueOf(slider_supplies.getValue()).multiply(max_val));
        max_val = max_val.multiply(BigInteger.valueOf(11));
        result = result.add(BigInteger.valueOf(pm_terrain_type.getChosenItemIndex()).multiply(max_val));
        max_val = max_val.multiply(BigInteger.valueOf(2));
        result = result.add(BigInteger.valueOf(pulldown_size.getChosenItemIndex()).multiply(max_val));
        // size has 5 options (Small/Medium/Large/Enormous/Archipelago)
        max_val = max_val.multiply(BigInteger.valueOf(5));

        String code = RegistrationKey.createString(result);
        label_mapcode.clear();
        label_mapcode.append(code);
    }

    private void startEditor() {
        int sizeIndex = pulldown_size.getChosenItemIndex();
        int meters = SIZES[sizeIndex];
        int terrain_type = pm_terrain_type.getChosenItemIndex();
        float hills = slider_hills.getValue() / 10f;
        float vegetation = slider_vegetation.getValue() / 10f;
        float supplies = slider_supplies.getValue() / 10f;
        int gamespeed = pm_gamespeed.getChosenItemIndex() + 1;
        boolean archipelago = ARCHIPELAGO[sizeIndex];

        com.oddlabs.tt.resource.WorldGenerator generator =
                new com.oddlabs.tt.resource.IslandGenerator(
                        meters, terrain_type, hills, vegetation, supplies, seed * seed, archipelago);

        // Launch minimal editor session; non-ported features will announce via InfoPrinter
        MapEditorSession.start(
                network,
                gui_root.getGUI(),
                meters,
                generator,
                gamespeed,
                sandboxModeCheck != null && sandboxModeCheck.isMarked()
                        ? EditorState.EditorMode.Sandbox
                        : EditorState.EditorMode.Default);
    }

    private final class SliderUpdateMapcodeListener implements com.oddlabs.tt.guievent.ValueListener {
        @Override
        public void valueSet(int value) {
            setMapcode();
        }
    }

    private final class UpdateMapcodeItemChosenListener implements com.oddlabs.tt.guievent.ItemChosenListener {
        @Override
        public void itemChosen(com.oddlabs.tt.gui.PulldownMenu menu, int index) {
            setMapcode();
        }
    }
}
