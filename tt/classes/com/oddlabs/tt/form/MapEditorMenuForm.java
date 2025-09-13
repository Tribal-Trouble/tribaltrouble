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
    // Keep a handle to the OK button so we can set default focus reliably
    private OKButton buttonOk;

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
    addChild(group_terrain_type);

    // Align pulldown buttons into a vertical column by offsetting from each label
    int labelColumnWidth = Math.max(
        label_gamespeed.getWidth(),
        Math.max(label_size.getWidth(), label_terrain_type.getWidth()));
    int baseGap = Skin.getSkin().getFormData().getSectionSpacing();

    // Place buttons so their left edge aligns at (max label width + base gap)
    pb_gamespeed.place(label_gamespeed, RIGHT_MID, baseGap + (labelColumnWidth - label_gamespeed.getWidth()));
    group_gamespeed.compileCanvas();

    pb_size.place(label_size, RIGHT_MID, baseGap + (labelColumnWidth - label_size.getWidth()));
    group_size.compileCanvas();

    pb_terrain_type.place(label_terrain_type, RIGHT_MID, baseGap + (labelColumnWidth - label_terrain_type.getWidth()));
    group_terrain_type.compileCanvas();

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

        // Buttons (Enter map code, Load placeholder, Start)
        Group group_buttons = new Group();
        HorizButton button_mapcode = new HorizButton(Utils.getBundleString(terrainBundle, "enter_map_code"), 170);
        button_mapcode.addMouseClickListener(new MapcodeListener());
        group_buttons.addChild(button_mapcode);

        HorizButton button_load = new HorizButton(Utils.getBundleString(Menu.bundle, "load"), 120);
        button_load.addMouseClickListener(new MouseClickListener() {
            @Override
            public void mouseClicked(int button, int x, int y, int clicks) {
                // Placeholder load action
                gui_root.getInfoPrinter().print("Load: coming soon");
            }
        });
        group_buttons.addChild(button_load);

        buttonOk = new OKButton(120);
        buttonOk.addMouseClickListener(
                new MouseClickListener() {
                    @Override
                    public void mouseClicked(int button, int x, int y, int clicks) {
                        if (launching) return;
                        launching = true;
                        buttonOk.setDisabled(true);
                        startEditor();
                    }
                });
        group_buttons.addChild(buttonOk);
        // Bottom layout: [Enter map code] [Load] [Start]
        buttonOk.place();
        button_load.place(buttonOk, LEFT_MID);
        button_mapcode.place(button_load, LEFT_MID);
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
    // Stack pulldown groups vertically, left-aligned, with a very small gap
    int tightGap = 4; // 4px gap requested
    group_gamespeed.place(label_headline, BOTTOM_LEFT);
    group_size.place(group_gamespeed, BOTTOM_LEFT, tightGap);
    group_terrain_type.place(group_size, BOTTOM_LEFT, tightGap);
        group_sliders.place(group_terrain_type, BOTTOM_LEFT);
        group_mode.place(group_sliders, BOTTOM_LEFT);
        group_seed.place(group_mode, BOTTOM_LEFT);
    group_buttons.place(ORIGIN_BOTTOM_RIGHT);

        compileCanvas();
    }

    // Minimal parseMapcode support (subset of TerrainMenu.parseMapcode)
    public void parseMapcode(String text) {
        String code = text.toUpperCase();
    java.math.BigInteger result = RegistrationKey.parseBits(code);
        // Decode in reverse order matching setMapcode packing
        // Reconstruct in same factor sequence used in setMapcode()
        // size(5), terrain(2), supplies(11), vegetation(11), hills(11), seed(40000)
        // Start from highest factor
        java.math.BigInteger f_size = java.math.BigInteger.valueOf(5);
        java.math.BigInteger f_terrain = java.math.BigInteger.valueOf(2);
        java.math.BigInteger f_slider = java.math.BigInteger.valueOf(11);
        java.math.BigInteger f_seed = new java.math.BigInteger("40000");

        // Build MAX = seed*slider*slider*slider*terrain*size
        java.math.BigInteger MAX = f_seed.multiply(f_slider).multiply(f_slider).multiply(f_slider).multiply(f_terrain).multiply(f_size);

    java.math.BigInteger r = result.mod(MAX);

        // size
        java.math.BigInteger div = MAX.divide(f_size);
        int sizeIndex = r.divide(div).intValue();
        r = r.mod(div);

        // terrain
        div = div.divide(f_terrain);
        int terrainIndex = r.divide(div).intValue();
        r = r.mod(div);

        // supplies
        div = div.divide(f_slider);
        int supplies = r.divide(div).intValue();
        r = r.mod(div);

        // vegetation
        div = div.divide(f_slider);
        int vegetation = r.divide(div).intValue();
        r = r.mod(div);

        // hills
        div = div.divide(f_slider);
        int hills = r.divide(div).intValue();
        r = r.mod(div);

        // seed
        div = div.divide(f_seed);
        int newSeed = r.divide(div).intValue();

        // Apply to controls
        pulldown_size.chooseItem(Math.max(0, Math.min(4, sizeIndex)));
        pm_terrain_type.chooseItem(Math.max(0, Math.min(1, terrainIndex)));
        slider_supplies.setValue(Math.max(0, Math.min(10, supplies)));
        slider_vegetation.setValue(Math.max(0, Math.min(10, vegetation)));
        slider_hills.setValue(Math.max(0, Math.min(10, hills)));
        seed = newSeed;
        label_mapcode.clear();
        label_mapcode.append(code);
    }

    private final class MapcodeListener implements com.oddlabs.tt.guievent.MouseClickListener {
        @Override
        public void mouseClicked(int button, int x, int y, int clicks) {
            gui_root.addModalForm(new MapcodeFormProxy(MapEditorMenuForm.this));
        }
    }

    // Small proxy form to reuse MapcodeForm behavior with MapEditorMenuForm
    private static final class MapcodeFormProxy extends com.oddlabs.tt.gui.Form {
        private final MapEditorMenuForm owner;
        private final com.oddlabs.tt.gui.EditLine edit;
        MapcodeFormProxy(MapEditorMenuForm owner) {
            this.owner = owner;
            java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle(com.oddlabs.tt.form.MapcodeForm.class.getName());
            com.oddlabs.tt.gui.Label label = new com.oddlabs.tt.gui.Label(
                    com.oddlabs.tt.util.Utils.getBundleString(bundle, "map_code"),
                    com.oddlabs.tt.gui.Skin.getSkin().getEditFont());
            addChild(label);
        edit = new com.oddlabs.tt.gui.EditLine(200, 12,
            RegistrationKey.CHAR_TO_WORD + RegistrationKey.LOWER_CASE_CHARS,
            com.oddlabs.tt.gui.EditLine.LEFT_ALIGNED);
            addChild(edit);
            com.oddlabs.tt.gui.HorizButton ok = new com.oddlabs.tt.gui.OKButton(100);
            ok.addMouseClickListener(new com.oddlabs.tt.guievent.MouseClickListener(){
                @Override
                public void mouseClicked(int button, int x, int y, int clicks) {
                    done();
                }
            });
            addChild(ok);
            com.oddlabs.tt.gui.HorizButton cancel = new com.oddlabs.tt.gui.CancelButton(100);
            cancel.addMouseClickListener(new com.oddlabs.tt.gui.CancelListener(this));
            addChild(cancel);
            label.place();
            edit.place(label, RIGHT_MID);
            cancel.place(edit, BOTTOM_RIGHT);
            ok.place(cancel, LEFT_MID);
            compileCanvas();
            centerPos();
        }
        private void done() {
            remove();
            owner.parseMapcode(edit.getContents());
            owner.setFocus();
        }
        @Override
        public void setFocus() { edit.setFocus(); }
    }

    @Override
    public final void setFocus() {
        // Make OK the default focus like other menus (ESC will still work globally)
        if (buttonOk != null) {
            buttonOk.setFocus();
            return;
        }
        // Fallback: try to focus the last child if it's an OK button
        try {
            Renderable last = getLastChild();
            if (last instanceof Group) {
                Renderable btn = ((Group) last).getLastChild();
                if (btn instanceof OKButton) ((OKButton) btn).setFocus();
            }
        } catch (Throwable ignore) {}
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
    // Note: no need to multiply max_val further here

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
