package com.oddlabs.tt.editor.ui;

import com.oddlabs.tt.form.MessageForm;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.Slider;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.guievent.MouseClickListener;
import com.oddlabs.tt.guievent.ValueListener;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.render.DefaultRenderer;
import com.oddlabs.tt.render.LandscapeRenderer;

/**
 * Lightweight toolbar for the Map Editor. Non-modal, docked at the top-left by default.
 * Provides quick access to common actions (Test, Save, Load, Online placeholder).
 *
 * Initial version wires Save/Load/Online; brush controls and overlays are added incrementally.
 */
public final class EditorToolbar extends Form {
    private final GUIRoot guiRoot;
    private final World world;
    private final LandscapeRenderer lr;
    private final DefaultRenderer dr;
    private final int terrainType;
    private BrushBinding brushBinding;
    private boolean suppressProgrammatic;
    private boolean visible = true;

    private final Label title;
    // Brush controls
    private Label radiusLabel;
    private Slider radiusSlider;
    private Label radiusValue;
    private Label intensityLabel;
    private Slider intensitySlider;
    private Label intensityValue;

    // Back-compat: previous code may call a 5-arg constructor; delegate to the new one.
    public EditorToolbar(GUIRoot guiRoot, World world, LandscapeRenderer lr, DefaultRenderer dr, int terrainType) {
        this(guiRoot, world, lr, dr, terrainType, null);
    }

    public EditorToolbar(GUIRoot guiRoot, World world, LandscapeRenderer lr, DefaultRenderer dr, int terrainType, BrushBinding brushBinding) {
        this.guiRoot = guiRoot;
        this.world = world;
        this.lr = lr;
        this.dr = dr;
        this.terrainType = terrainType;
        this.brushBinding = brushBinding;

        // Title
        title = new Label("Editor", Skin.getSkin().getEditFont());
        addChild(title);
        title.place();

        // Buttons row: [Test] [Save] [Load] [Online]
        HorizButton btnTest = new HorizButton("Test", 80);
        HorizButton btnSave = new HorizButton("Save", 80);
        HorizButton btnLoad = new HorizButton("Load", 80);
        HorizButton btnOnline = new HorizButton("Online", 90);

    addChild(btnTest);
    addChild(btnSave);
    addChild(btnLoad);
    addChild(btnOnline);

        // Layout to the right of the title
        btnTest.place(title, RIGHT_MID, Skin.getSkin().getFormData().getSectionSpacing());
        btnSave.place(btnTest, RIGHT_MID);
        btnLoad.place(btnSave, RIGHT_MID);
        btnOnline.place(btnLoad, RIGHT_MID);

        // Wire actions
        btnSave.addMouseClickListener(new MouseClickListener() {
            @Override public void mouseClicked(int button, int x, int y, int clicks) {
                guiRoot.addModalForm(new com.oddlabs.tt.form.EditorMapDialogs.SaveDialog(guiRoot, world, terrainType));
            }
        });
        btnLoad.addMouseClickListener(new MouseClickListener() {
            @Override public void mouseClicked(int button, int x, int y, int clicks) {
                guiRoot.addModalForm(new com.oddlabs.tt.form.EditorMapDialogs.LoadDialog(guiRoot, world, lr, dr, terrainType));
            }
        });
        btnTest.addMouseClickListener(new MouseClickListener() {
            @Override public void mouseClicked(int button, int x, int y, int clicks) {
                try {
                    // 1) Quick-export current world
                    java.io.File dir = com.oddlabs.tt.mapio.MapIO.mapsDir();
                    java.io.File file = new java.io.File(dir, "editor_map.ttmap");
                    com.oddlabs.tt.mapio.MapIO.saveEditorWorld(world, terrainType, file);
                    guiRoot.getInfoPrinter().print("Exported test map: " + file.getName());

                    // 2) Start single-player test using the exported map
                    int meters = world.getHeightMap().getMetersPerWorld();
                    int gamespeed = world.getGamespeed();
                    com.oddlabs.tt.net.GameNetwork game_network =
                        com.oddlabs.tt.delegate.Menu.startNewGameWithMap(
                                com.oddlabs.tt.editor.MapEditorSession.getEditorNetwork(),
                                guiRoot,
                                null,
                                new com.oddlabs.tt.landscape.WorldParameters(
                                        gamespeed,
                                        "",
                                        com.oddlabs.tt.player.Player.INITIAL_UNIT_COUNT,
                                        com.oddlabs.tt.player.Player.DEFAULT_MAX_UNIT_COUNT),
                                new com.oddlabs.tt.viewer.DefaultInGameInfo(),
                                new com.oddlabs.tt.delegate.Menu.DefaultWorldInitAction(),
                                null,
                                meters,
                                terrainType,
                                .5f,
                                .5f,
                                .5f,
                                1337,
                                false,
                                new String[0],
                                1,
                                file);
                    // Set local player slot as human and start server
                    game_network
                        .getClient()
                        .getServerInterface()
                        .setPlayerSlot(
                                0,
                                com.oddlabs.tt.net.PlayerSlot.HUMAN,
                                0,
                                0,
                                true,
                                com.oddlabs.tt.net.PlayerSlot.AI_NONE);
                    game_network.getClient().getServerInterface().startServer();
                    guiRoot.getInfoPrinter().print("Launching test game...");
                } catch (Throwable t) {
                    guiRoot.getInfoPrinter().print("Test failed: " + t.getMessage());
                }
            }
        });
        btnOnline.addMouseClickListener(new MouseClickListener() {
            @Override public void mouseClicked(int button, int x, int y, int clicks) {
                guiRoot.addModalForm(new MessageForm("Online publishing coming soon"));
            }
        });

        // Brush controls row: Radius [----] 00m   Intensity [----] 0.0
        int sliderLen = 150;
        int spacing = Skin.getSkin().getFormData().getSectionSpacing();
    radiusLabel = new Label("Radius", Skin.getSkin().getEditFont());
        radiusSlider = new Slider(sliderLen, 1, 200, 6);
    radiusValue = new Label("6m", Skin.getSkin().getEditFont());
    intensityLabel = new Label("Intensity", Skin.getSkin().getEditFont());
        intensitySlider = new Slider(sliderLen, 1, 100, 50); // 1..100 -> 0.1..10.0
    intensityValue = new Label("5.0", Skin.getSkin().getEditFont());

        addChild(radiusLabel);
        addChild(radiusSlider);
        addChild(radiusValue);
        addChild(intensityLabel);
        addChild(intensitySlider);
        addChild(intensityValue);

    // Layout second row under buttons
    radiusLabel.place(title, TOP_LEFT, spacing);
        radiusSlider.place(radiusLabel, RIGHT_MID);
        radiusValue.place(radiusSlider, RIGHT_MID, spacing / 2);
        intensityLabel.place(radiusValue, RIGHT_MID, spacing);
        intensitySlider.place(intensityLabel, RIGHT_MID);
        intensityValue.place(intensitySlider, RIGHT_MID, spacing / 2);

        // Wire slider listeners -> binding
        radiusSlider.addValueListener(new ValueListener() {
            @Override public void valueSet(int value) {
                radiusValue.set(value + "m");
                if (brushBinding != null && !suppressProgrammatic) {
                    brushBinding.setRadiusMeters(value);
                }
            }
        });
        intensitySlider.addValueListener(new ValueListener() {
            @Override public void valueSet(int value) {
                float strength = value / 10.0f;
                intensityValue.set(format1(strength));
                if (brushBinding != null && !suppressProgrammatic) {
                    brushBinding.setIntensity(strength);
                }
            }
        });

    compileCanvas();
    // Default docked position; caller should re-position on viewport changes if desired
        setPos(8, 8);
    setHidden(false);

        // Initialize slider positions from binding if available
        syncFromBinding();
    }

    public void toggleVisible() { visible = !visible; setHidden(!visible); }

    public void dockTopLeft(int offsetX, int offsetY) {
        setPos(offsetX, offsetY);
    }

    public void setBrushBinding(BrushBinding binding) {
        this.brushBinding = binding;
        syncFromBinding();
    }

    public void syncFromBinding() {
        if (brushBinding == null) return;
        suppressProgrammatic = true;
        try {
            int radiusMeters = clamp(Math.round(brushBinding.getRadiusMeters()), 1, 200);
            radiusSlider.setValue(radiusMeters);
            radiusValue.set(radiusMeters + "m");

            int intensitySteps = clamp(Math.round(brushBinding.getIntensity() * 10f), 1, 100);
            intensitySlider.setValue(intensitySteps);
            intensityValue.set(format1(intensitySteps / 10f));
        } finally {
            suppressProgrammatic = false;
        }
    }

    private static int clamp(int v, int min, int max) { return v < min ? min : (v > max ? max : v); }
    private static String format1(float v) { return String.format(java.util.Locale.US, "%.1f", v); }
}
