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
import com.oddlabs.tt.util.WordsEncoding;

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

    // Use same cardinalities as TerrainMenu for compatibility with global map code system
    private static final String SEED_CARDINALITY = "40000";
    private static final int SLIDER_CARDINALITY = 11;
    private static final int TERRAIN_TYPE_CARDINALITY = 4;
    private static final int SIZE_CARDINALITY = 7;

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

    // Sandbox mode checkbox (moved to top row)
    sandboxModeCheck = new com.oddlabs.tt.gui.CheckBox(
        false,
        "Sandbox",
        "Enable experimental tools & overlays. Sandbox maps can't be published.");

    // Map code display label (words-based, like TerrainMenu)
    // 300px wide per request
    label_mapcode = new Label("", Skin.getSkin().getHeadlineFont(), 300);

    // Buttons row (right-aligned)
        Group group_buttons = new Group();
    // Use same label as TerrainMenu for consistency
    HorizButton button_mapcode = new HorizButton(Utils.getBundleString(terrainBundle, "enter_map_code"), 170);
        button_mapcode.addMouseClickListener(new MapcodeListener());

        HorizButton button_test = new HorizButton("Test", 120);
        button_test.addMouseClickListener(new MouseClickListener() {
            @Override
            public void mouseClicked(int button, int x, int y, int clicks) {
                try {
                    // Use the editor Load dialog that lists .ttmap files and returns a File via callback
                    gui_root.addModalForm(
                        new EditorMapDialogs.LoadDialog(
                            gui_root,
                            null, // world not required for selection-only usage
                            null,
                            null,
                            pm_terrain_type.getChosenItemIndex(),
                            (java.io.File chosen) -> {
                                try {
                                    gui_root.addModalForm(
                                        new TestMapForm(
                                            gui_root,
                                            network,
                                            null, // world is unused when a chosen map file is provided
                                            pm_terrain_type.getChosenItemIndex(),
                                            chosen,
                                            pm_gamespeed.getChosenItemIndex() + 1 // pass gamespeed override
                                        )
                                    );
                                } catch (Throwable t) {
                                    gui_root.getInfoPrinter().print("Open Test failed: " + t.getMessage());
                                }
                            }
                        )
                    );
                } catch (Throwable t) {
                    gui_root.getInfoPrinter().print("Open Test failed: " + t.getMessage());
                }
            }
        });

        HorizButton button_load = new HorizButton(Utils.getBundleString(Menu.bundle, "load"), 120);
        button_load.addMouseClickListener(new MouseClickListener() {
            @Override
            public void mouseClicked(int button, int x, int y, int clicks) {
                gui_root.addModalForm(new EditorMapLoadFromMenu(MapEditorMenuForm.this));
            }
        });

        buttonOk = new OKButton(120);
        buttonOk.addMouseClickListener(new MouseClickListener() {
            @Override
            public void mouseClicked(int button, int x, int y, int clicks) {
                if (launching) return;
                launching = true;
                buttonOk.setDisabled(true);
                startEditor();
            }
        });

    // Two subgroups (right-aligned):
    // Top row: [Enter map code...] [Test] [sandbox]
    // Bottom row: [Load] [OK] [Cancel]
    Group rowTop = new Group();
    rowTop.addChild(sandboxModeCheck);
    rowTop.addChild(button_mapcode);
    rowTop.addChild(button_test);
    // Place buttons horizontally
    sandboxModeCheck.place();
    button_mapcode.place(sandboxModeCheck, RIGHT_MID);
    button_test.place(button_mapcode, RIGHT_MID);
    rowTop.compileCanvas();

    Group rowBottom = new Group();
    rowBottom.addChild(button_load);
    rowBottom.addChild(buttonOk);
    com.oddlabs.tt.gui.HorizButton button_cancel = new com.oddlabs.tt.gui.CancelButton(120);
    button_cancel.addMouseClickListener(new CancelListener(this));
    rowBottom.addChild(button_cancel);
    button_load.place();
    buttonOk.place(button_load, RIGHT_MID);
    button_cancel.place(buttonOk, RIGHT_MID);
    rowBottom.compileCanvas();

    group_buttons.addChild(rowBottom);
    group_buttons.addChild(rowTop);
    // Place bottom row first, then top row directly above it, right-aligned with 4px gap
    rowBottom.place();
    rowTop.place(rowBottom, TOP_RIGHT, 4);
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

    // Map code line (like TerrainMenu): label on left, words-based code on right
    Label label_seed = new Label(Utils.getBundleString(terrainBundle, "map_code"), Skin.getSkin().getEditFont());
    Group group_seed = new Group();
    group_seed.addChild(label_seed);
    group_seed.addChild(label_mapcode);
    label_seed.place();
    label_mapcode.place(label_seed, RIGHT_MID);
    group_seed.compileCanvas();
    addChild(group_seed);
    // Place map code group just under the sliders
    group_seed.place(group_sliders, BOTTOM_LEFT);
    // Place the buttons at the bottom-right
    group_buttons.place(ORIGIN_BOTTOM_RIGHT);

        compileCanvas();
    }

    // Parse both legacy (bits) and words-based codes like TerrainMenu, then clamp to editor options
    public void parseMapcode(String text) {
        String code = text.toUpperCase();
        try {
            BigInteger result;
            if (code.indexOf(' ') == -1) {
                // Legacy bits encoding
                result = RegistrationKey.parseBits(code);
            } else {
                // Words-based encoding
                result = WordsEncoding.decode(text);
            }
            parseBigIntegerEditor(result);
            // Re-encode with words so display matches global system
            setMapcode();
        } catch (Throwable ignore) {
            // If parsing fails, keep existing values
        }
    }

    // Decode size, terrain, supplies, vegetation, hills, seed using TerrainMenu cardinalities
    private void parseBigIntegerEditor(BigInteger result) {
        BigInteger max = BigInteger.ONE;
        max = max.multiply(new BigInteger(SEED_CARDINALITY));
        max = max.multiply(new BigInteger(new byte[] {(byte) SLIDER_CARDINALITY}));
        max = max.multiply(new BigInteger(new byte[] {(byte) SLIDER_CARDINALITY}));
        max = max.multiply(new BigInteger(new byte[] {(byte) SLIDER_CARDINALITY}));
        max = max.multiply(new BigInteger(new byte[] {(byte) TERRAIN_TYPE_CARDINALITY}));
        max = max.multiply(new BigInteger(new byte[] {(byte) SIZE_CARDINALITY}));

        result = result.mod(max);

        // size
        max = max.divide(new BigInteger(new byte[] {(byte) SIZE_CARDINALITY}));
        int sizeIndex = result.divide(max).intValue();
        pulldown_size.chooseItem(Math.max(0, Math.min(4, sizeIndex))); // clamp to 0..4
        result = result.mod(max);

        // terrain
        max = max.divide(new BigInteger(new byte[] {(byte) TERRAIN_TYPE_CARDINALITY}));
        int terrainIndex = result.divide(max).intValue();
        pm_terrain_type.chooseItem(Math.max(0, Math.min(1, terrainIndex))); // clamp to 0..1
        result = result.mod(max);

        // supplies
        max = max.divide(new BigInteger(new byte[] {(byte) SLIDER_CARDINALITY}));
        int supplies = result.divide(max).intValue();
        slider_supplies.setValue(Math.max(0, Math.min(10, supplies)));
        result = result.mod(max);

        // vegetation
        max = max.divide(new BigInteger(new byte[] {(byte) SLIDER_CARDINALITY}));
        int vegetation = result.divide(max).intValue();
        slider_vegetation.setValue(Math.max(0, Math.min(10, vegetation)));
        result = result.mod(max);

        // hills
        max = max.divide(new BigInteger(new byte[] {(byte) SLIDER_CARDINALITY}));
        int hills = result.divide(max).intValue();
        slider_hills.setValue(Math.max(0, Math.min(10, hills)));
        result = result.mod(max);

        // seed
        max = max.divide(new BigInteger(SEED_CARDINALITY));
        seed = result.divide(max).intValue();
    }

    private final class MapcodeListener implements com.oddlabs.tt.guievent.MouseClickListener {
        @Override
        public void mouseClicked(int button, int x, int y, int clicks) {
            gui_root.addModalForm(new MapcodeFormProxy(MapEditorMenuForm.this));
        }
    }

    // Modal form to choose a saved map from main menu and start editor seeded with it
    private static final class EditorMapLoadFromMenu extends com.oddlabs.tt.gui.Form {
        private final MapEditorMenuForm owner;
        private final com.oddlabs.tt.gui.MultiColumnComboBox listBox;
        private java.util.List<com.oddlabs.tt.mapio.MapIO.MapSummary> summaries = java.util.Collections.emptyList();
        private int chosenIndex = -1;
    private final com.oddlabs.tt.gui.Label metaLabel;
    private com.oddlabs.tt.gui.GUIImage previewImage;
        private String metaLabelContent = "Select a .ttmap on the left.";
        // Zoom/pan state (mirrors EditorMapDialogs.LoadDialog)
        private float previewZoom = 1f; // 1 = full texture
        private float previewOffsetX = 0f;
        private float previewOffsetY = 0f;
        private boolean previewDragging = false;
        private static final float PREVIEW_MAX_ZOOM = 8f;

        private void resetPreviewTransform() { previewZoom = 1f; previewOffsetX = 0f; previewOffsetY = 0f; }
        private void clampPreviewOffsets() {
            float vis = 1f / previewZoom;
            if (previewOffsetX < 0) previewOffsetX = 0;
            if (previewOffsetY < 0) previewOffsetY = 0;
            if (previewOffsetX > 1f - vis) previewOffsetX = 1f - vis;
            if (previewOffsetY > 1f - vis) previewOffsetY = 1f - vis;
        }
        private void rebuildPreviewImage(com.oddlabs.tt.render.Texture tex) {
            float vis = 1f / previewZoom;
            float u1 = previewOffsetX;
            float v1 = previewOffsetY;
            float u2 = u1 + vis;
            float v2 = v1 + vis;
            int w = previewImage.getWidth();
            int h = previewImage.getHeight();
            removeChild(previewImage);
            previewImage = new com.oddlabs.tt.gui.GUIImage(w, h, u1, v1, u2, v2, tex);
            attachPreviewInteraction();
            addChild(previewImage);
            previewImage.place(listBox, RIGHT_TOP, com.oddlabs.tt.gui.Skin.getSkin().getFormData().getSectionSpacing());
            metaLabel.place(previewImage, com.oddlabs.tt.gui.GUIObject.BOTTOM_LEFT);
            compileCanvas();
        }
        private void attachPreviewInteraction() {
            previewImage.addMouseWheelListener(amount -> {
                if (!previewImage.isHovered()) return;
                if (amount != 0) {
                    try { owner.gui_root.getInfoPrinter().print("Menu preview zoom wheel=" + amount); } catch (Throwable ignore) {}
                }
                int w = previewImage.getWidth();
                int h = previewImage.getHeight();
                int absX = com.oddlabs.tt.gui.LocalInput.getMouseX();
                int absY = com.oddlabs.tt.gui.LocalInput.getMouseY();
                int localX = previewImage.translateXToLocal(absX);
                int localY = previewImage.translateYToLocal(absY);
                if (localX < 0) localX = 0; if (localX >= w) localX = w - 1;
                if (localY < 0) localY = 0; if (localY >= h) localY = h - 1;
                float fx = (float)localX / (float)w;
                float fy = (float)localY / (float)h;
                float old = previewZoom;
                if (amount < 0) previewZoom *= 1.15f; else previewZoom /= 1.15f;
                if (previewZoom < 1f) previewZoom = 1f; if (previewZoom > PREVIEW_MAX_ZOOM) previewZoom = PREVIEW_MAX_ZOOM;
                if (Math.abs(previewZoom - old) < 1e-4f) return;
                float visOld = 1f / old;
                float anchorU = previewOffsetX + fx * visOld;
                float anchorV = previewOffsetY + fy * visOld;
                float visNew = 1f / previewZoom;
                previewOffsetX = anchorU - fx * visNew;
                previewOffsetY = anchorV - fy * visNew;
                clampPreviewOffsets();
                try {
                    java.lang.reflect.Field f = com.oddlabs.tt.gui.GUIImage.class.getDeclaredField("texture");
                    f.setAccessible(true);
                    com.oddlabs.tt.render.Texture tex = (com.oddlabs.tt.render.Texture) f.get(previewImage);
                    rebuildPreviewImage(tex);
                } catch (Throwable ignore) {}
                metaLabelContent = metaLabelContent + "\nZoom: " + String.format(java.util.Locale.US, "%.2f", previewZoom);
                metaLabel.set(metaLabelContent);
            });
            previewImage.addMouseButtonListener(new com.oddlabs.tt.guievent.MouseButtonListener() {
                @Override public void mousePressed(int button, int x, int y) { if (button==0) previewDragging = true; }
                @Override public void mouseReleased(int button, int x, int y) { if (button==0) previewDragging = false; }
                @Override public void mouseHeld(int button, int x, int y) { }
                @Override public void mouseClicked(int button, int x, int y, int clicks) { }
            });
            previewImage.addMouseMotionListener(new com.oddlabs.tt.guievent.MouseMotionListener() {
                @Override public void mouseDragged(int button, int x, int y, int rel_x, int rel_y, int abs_x, int abs_y) {
                    if (!previewDragging || previewZoom <= 1f) return;
                    float vis = 1f / previewZoom;
                    float scale = vis / previewImage.getWidth();
                    previewOffsetX -= rel_x * scale;
                    previewOffsetY -= rel_y * scale;
                    clampPreviewOffsets();
                    try {
                        java.lang.reflect.Field f = com.oddlabs.tt.gui.GUIImage.class.getDeclaredField("texture");
                        f.setAccessible(true);
                        com.oddlabs.tt.render.Texture tex = (com.oddlabs.tt.render.Texture) f.get(previewImage);
                        rebuildPreviewImage(tex);
                    } catch (Throwable ignore) {}
                }
                @Override public void mouseMoved(int x, int y) { }
                @Override public void mouseEntered() { }
                @Override public void mouseExited() { }
            });
        }

        EditorMapLoadFromMenu(MapEditorMenuForm owner) {
            this.owner = owner;
            com.oddlabs.tt.gui.Label title = new com.oddlabs.tt.gui.Label("Load Map", com.oddlabs.tt.gui.Skin.getSkin().getHeadlineFont());
            addChild(title);
            com.oddlabs.tt.gui.ColumnInfo[] infos = new com.oddlabs.tt.gui.ColumnInfo[] {
                new com.oddlabs.tt.gui.ColumnInfo("Name", 220),
                new com.oddlabs.tt.gui.ColumnInfo("Size", 90),
                new com.oddlabs.tt.gui.ColumnInfo("Modified", 200)
            };
            listBox = new com.oddlabs.tt.gui.MultiColumnComboBox(owner.gui_root, infos, 300);
            addChild(listBox);
            // Preview image (match in-editor load dialog sizing and support zoom)
            previewImage = new com.oddlabs.tt.gui.GUIImage(512,512,0f,0f,1f,1f, com.oddlabs.tt.mapio.MapPreview.getBlankTexture());
            addChild(previewImage);
            attachPreviewInteraction();
            metaLabel = new com.oddlabs.tt.gui.Label(metaLabelContent, com.oddlabs.tt.gui.Skin.getSkin().getEditFont(), 340);
            addChild(metaLabel);

            com.oddlabs.tt.gui.HorizButton open = new com.oddlabs.tt.gui.OKButton(100);
            open.addMouseClickListener(new com.oddlabs.tt.guievent.MouseClickListener() {
                @Override public void mouseClicked(int button, int x, int y, int clicks) { onOpen(); }
            });
            addChild(open);
            com.oddlabs.tt.gui.HorizButton cancel = new com.oddlabs.tt.gui.CancelButton(100);
            cancel.addMouseClickListener(new com.oddlabs.tt.gui.CancelListener(this));
            addChild(cancel);

            // Layout
            title.place();
            listBox.place(title, BOTTOM_LEFT);
            previewImage.place(listBox, RIGHT_TOP, com.oddlabs.tt.gui.Skin.getSkin().getFormData().getSectionSpacing());
            metaLabel.place(previewImage, BOTTOM_LEFT);
            cancel.place(metaLabel, BOTTOM_RIGHT);
            open.place(cancel, LEFT_MID);

            refreshList();
            listBox.addRowListener(new com.oddlabs.tt.guievent.RowListener() {
                @Override public void rowDoubleClicked(Object row_context) {
                    if (row_context instanceof com.oddlabs.tt.mapio.MapIO.MapSummary) {
                        onRowDoubleClicked((com.oddlabs.tt.mapio.MapIO.MapSummary) row_context);
                    }
                }
                @Override public void rowChosen(Object row_context) {
                    if (row_context instanceof com.oddlabs.tt.mapio.MapIO.MapSummary) {
                        onRowChosen((com.oddlabs.tt.mapio.MapIO.MapSummary) row_context);
                    }
                }
            });

            compileCanvas();
            centerPos();
        }

        private void refreshList() {
            listBox.clear();
            summaries = com.oddlabs.tt.mapio.MapIO.listMaps();
            for (com.oddlabs.tt.mapio.MapIO.MapSummary s : summaries) {
                String fname = s.file.getName();
                long sizeKB = s.file.length() / 1024;
                String sizeStr = sizeKB + " KB";
                com.oddlabs.tt.gui.Row row = new com.oddlabs.tt.gui.Row(
                        new com.oddlabs.tt.gui.GUIObject[] {
                                new com.oddlabs.tt.gui.Label(fname, com.oddlabs.tt.gui.Skin.getSkin().getMultiColumnComboBoxData().getFont(), 220),
                                new com.oddlabs.tt.gui.Label(sizeStr, com.oddlabs.tt.gui.Skin.getSkin().getMultiColumnComboBoxData().getFont(), 90),
                                new com.oddlabs.tt.gui.DateLabel(s.lastModified, com.oddlabs.tt.gui.Skin.getSkin().getMultiColumnComboBoxData().getFont(), 200)
                        },
                        s);
                listBox.addRow(row);
            }
        }

        private void selectIndex(int idx) {
            this.chosenIndex = idx;
            try {
                com.oddlabs.tt.mapio.MapIO.MapSummary sum = summaries.get(idx);
                StringBuilder sb = new StringBuilder();
                sb.append(sum.file.getName()).append("\n\n");
                if (sum.name != null && !sum.name.isEmpty()) sb.append("Name: ").append(sum.name).append("\n");
                if (sum.author != null && !sum.author.isEmpty()) sb.append("Author: ").append(sum.author).append("\n");
                if (sum.description != null && !sum.description.isEmpty()) sb.append("Desc: ").append(sum.description).append("\n");
                sb.append("Size: ").append(sum.size).append(" gu, Terrain: ").append(sum.terrainType);
                metaLabel.set(sb.toString());
                try {
                    com.oddlabs.tt.render.Texture tex = com.oddlabs.tt.mapio.MapPreview.getPreviewTexture(sum.file);
                    resetPreviewTransform();
                    rebuildPreviewImage(tex);
                } catch (Throwable ignored) {}
            } catch (Exception t) {
                metaLabel.set("Preview failed: " + t.getMessage());
            }
        }

        private void onOpen() {
            if (chosenIndex < 0 || chosenIndex >= summaries.size()) {
                owner.gui_root.getInfoPrinter().print("Choose a map first.");
                return;
            }
            com.oddlabs.tt.mapio.MapIO.MapSummary s = summaries.get(chosenIndex);
            // Use summary meters/terrain to build base generator, overlay with LoadedMapGenerator
            int meters = s.metersPerWorld > 0 ? s.metersPerWorld : MapEditorMenuForm.SIZES[owner.pulldown_size.getChosenItemIndex()];
            int terr = s.terrainType >= 0 ? s.terrainType : owner.pm_terrain_type.getChosenItemIndex();
            int gamespeed = owner.pm_gamespeed.getChosenItemIndex() + 1;
            boolean sandbox = owner.sandboxModeCheck != null && owner.sandboxModeCheck.isMarked();
            com.oddlabs.tt.resource.WorldGenerator base =
                    new com.oddlabs.tt.resource.IslandGenerator(meters, terr, .5f, .5f, .5f, 1337, false);
            com.oddlabs.tt.resource.WorldGenerator gen =
                    new com.oddlabs.tt.mapio.LoadedMapGenerator(base, s.file);
            // Start the editor session via ProgressForm fade
            com.oddlabs.tt.editor.MapEditorSession.start(
                    owner.network,
                    owner.gui_root.getGUI(),
                    meters,
                    gen,
                    gamespeed,
                    sandbox ? com.oddlabs.tt.editor.ui.EditorState.EditorMode.Sandbox : com.oddlabs.tt.editor.ui.EditorState.EditorMode.Default);
            remove();
        }

        private void onRowDoubleClicked(com.oddlabs.tt.mapio.MapIO.MapSummary s) {
            int idx = summaries.indexOf(s);
            if (idx >= 0) { selectIndex(idx); onOpen(); }
        }

        private void onRowChosen(com.oddlabs.tt.mapio.MapIO.MapSummary s) {
            int idx = summaries.indexOf(s);
            if (idx >= 0) selectIndex(idx);
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
        // Match TerrainMenu's MapcodeForm: allow free text (including spaces) for words-based codes
        edit = new com.oddlabs.tt.gui.EditLine(200, 100, com.oddlabs.tt.gui.EditLine.LEFT_ALIGNED);
            addChild(edit);
            // Pressing Enter should submit, mirroring TerrainMenu behavior
            edit.addEnterListener(new com.oddlabs.tt.guievent.EnterListener() {
                @Override
                public void enterPressed(CharSequence text) { done(); }
            });
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

    // Map code generation compatible with TerrainMenu (words-based encoding)
    private void setMapcode() {
        BigInteger max_val = BigInteger.ONE;
        BigInteger result = BigInteger.ZERO;
        // seed
        result = result.add(BigInteger.valueOf(seed).multiply(max_val));
        max_val = max_val.multiply(new BigInteger(SEED_CARDINALITY));
        // hills
        result = result.add(BigInteger.valueOf(slider_hills.getValue()).multiply(max_val));
        max_val = max_val.multiply(new BigInteger(new byte[] {(byte) SLIDER_CARDINALITY}));
        // vegetation
        result = result.add(BigInteger.valueOf(slider_vegetation.getValue()).multiply(max_val));
        max_val = max_val.multiply(new BigInteger(new byte[] {(byte) SLIDER_CARDINALITY}));
        // supplies
        result = result.add(BigInteger.valueOf(slider_supplies.getValue()).multiply(max_val));
        max_val = max_val.multiply(new BigInteger(new byte[] {(byte) SLIDER_CARDINALITY}));
        // terrain (use 0..1 in a 0..3 space for compatibility)
        result = result.add(BigInteger.valueOf(pm_terrain_type.getChosenItemIndex()).multiply(max_val));
        max_val = max_val.multiply(new BigInteger(new byte[] {(byte) TERRAIN_TYPE_CARDINALITY}));
        // size (use 0..4 in a 0..6 space for compatibility)
        result = result.add(BigInteger.valueOf(pulldown_size.getChosenItemIndex()).multiply(max_val));
        max_val = max_val.multiply(new BigInteger(new byte[] {(byte) SIZE_CARDINALITY}));

        String code = WordsEncoding.encode(result);
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
