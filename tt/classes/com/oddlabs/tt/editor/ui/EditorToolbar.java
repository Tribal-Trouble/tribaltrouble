package com.oddlabs.tt.editor.ui;

import com.oddlabs.tt.form.MessageForm;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.LocalInput;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.CheckBox;
import com.oddlabs.tt.gui.Slider;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.gui.PulldownButton;
import com.oddlabs.tt.gui.PulldownMenu;
import com.oddlabs.tt.gui.PulldownItem;
import com.oddlabs.tt.guievent.MouseClickListener;
import com.oddlabs.tt.guievent.MouseMotionListener;
import com.oddlabs.tt.guievent.ValueListener;
import com.oddlabs.tt.guievent.CheckBoxListener;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.render.DefaultRenderer;
import com.oddlabs.tt.render.LandscapeRenderer;
// No direct GL usage here; rely on Form rendering

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
    private EditorOptionsBinding optionsBinding;
    private boolean suppressProgrammatic;
    private boolean visible = true;

    // Brush controls
    private Label radiusLabel;
    private Slider radiusSlider;
    private Label radiusValue;
    private Label intensityLabel;
    private Slider intensitySlider;
    private Label intensityValue;

    // Selectors
    private Label toolLabel;
    private PulldownButton toolButton;
    private Label modeLabel;
    private PulldownButton modeButton;
    private Label resourceLabel;
    private PulldownButton resourceButton;
    // Entities tool selectors
    private Label entitiesTypeLabel;
    private PulldownButton entitiesTypeButton; // Building/Unit
    private Label entitiesKindLabel;
    private PulldownButton entitiesKindButton; // specific building/unit
    private Label entitiesTeamLabel;
    private PulldownButton entitiesTeamButton; // Team index 0..7 + Neutral
    private Label entitiesRaceLabel;
    private PulldownButton entitiesRaceButton; // Natives/Vikings
    private CheckBox overlayMaster;
    private Label overlayLabel;
    private PulldownButton overlayButton;

    // Drag handling (we clamp horizontally to keep it a strip; Form adds top bar drag)

    // Back-compat: previous code may call a 5-arg constructor; delegate to the new one.
    public EditorToolbar(GUIRoot guiRoot, World world, LandscapeRenderer lr, DefaultRenderer dr, int terrainType) {
        this(guiRoot, world, lr, dr, terrainType, null, null);
    }

    public EditorToolbar(GUIRoot guiRoot, World world, LandscapeRenderer lr, DefaultRenderer dr, int terrainType, BrushBinding brushBinding) {
        this(guiRoot, world, lr, dr, terrainType, brushBinding, null);
    }

    public EditorToolbar(GUIRoot guiRoot, World world, LandscapeRenderer lr, DefaultRenderer dr, int terrainType, BrushBinding brushBinding, EditorOptionsBinding optionsBinding) {
        super("Editor"); // enable top bar with caption + close (draggable)
        this.guiRoot = guiRoot;
        this.world = world;
        this.lr = lr;
        this.dr = dr;
        this.terrainType = terrainType;
        this.brushBinding = brushBinding;
        this.optionsBinding = optionsBinding;
    // Toolbar width will be set dynamically via getWidth()

        // Buttons row: [Test] [Save] [Load] [Online] [Trigger]
        HorizButton btnTest = new HorizButton("Test (Ctrl+P)", 120);
        HorizButton btnSave = new HorizButton("Save", 80);
        HorizButton btnLoad = new HorizButton("Load", 80);
        HorizButton btnOnline = new HorizButton("Online", 90);
        HorizButton btnTrigger = new HorizButton("Trigger", 90);

        addChild(btnTest);
        addChild(btnSave);
        addChild(btnLoad);
        addChild(btnOnline);
        addChild(btnTrigger);

        // Layout in a single horizontal row (start at content origin, chain RIGHT_MID)
        int spacing = StrictMath.max(2, Skin.getSkin().getFormData().getObjectSpacing());
        btnTest.place();
        btnSave.place(btnTest, RIGHT_MID);
        btnLoad.place(btnSave, RIGHT_MID);
        btnOnline.place(btnLoad, RIGHT_MID);
        // Place Trigger after overlays pulldown, in line with other buttons
        // ...existing code...

        // Wire actions
        btnSave.addMouseClickListener(new MouseClickListener() {
            @Override public void mouseClicked(int button, int x, int y, int clicks) {
                EditorToolbar.this.guiRoot.addModalForm(new com.oddlabs.tt.form.EditorMapDialogs.SaveDialog(EditorToolbar.this.guiRoot, EditorToolbar.this.world, EditorToolbar.this.terrainType));
            }
        });
        btnLoad.addMouseClickListener(new MouseClickListener() {
            @Override public void mouseClicked(int button, int x, int y, int clicks) {
                EditorToolbar.this.guiRoot.addModalForm(new com.oddlabs.tt.form.EditorMapDialogs.LoadDialog(EditorToolbar.this.guiRoot, EditorToolbar.this.world, EditorToolbar.this.lr, EditorToolbar.this.dr, EditorToolbar.this.terrainType));
            }
        });
        btnTest.addMouseClickListener(new MouseClickListener() {
            @Override public void mouseClicked(int button, int x, int y, int clicks) {
                // Test flow: open the same Load dialog, then open TestMapForm for players, then start game
                try {
                    EditorToolbar.this.guiRoot.addModalForm(
                            new com.oddlabs.tt.form.EditorMapDialogs.LoadDialog(
                                    EditorToolbar.this.guiRoot,
                                    EditorToolbar.this.world,
                                    EditorToolbar.this.lr,
                                    EditorToolbar.this.dr,
                                    EditorToolbar.this.terrainType,
                                    (java.io.File chosen) -> {
                                        try {
                                            EditorToolbar.this.guiRoot.addModalForm(
                                                    new com.oddlabs.tt.form.TestMapForm(
                                                            EditorToolbar.this.guiRoot,
                                                            com.oddlabs.tt.editor.MapEditorSession.getEditorNetwork(),
                                                            EditorToolbar.this.world,
                                                            EditorToolbar.this.terrainType,
                                                            chosen));
                                        } catch (Throwable t) {
                                            EditorToolbar.this.guiRoot.getInfoPrinter().print("Open Test Map failed: " + t.getMessage());
                                        }
                                    }));
                } catch (Throwable t) {
                    EditorToolbar.this.guiRoot.getInfoPrinter().print("Open Test Map failed: " + t.getMessage());
                }
            }
        });
        btnOnline.addMouseClickListener(new MouseClickListener() {
            @Override public void mouseClicked(int button, int x, int y, int clicks) {
                EditorToolbar.this.guiRoot.addModalForm(new MessageForm("Online publishing coming soon"));
            }
        });
        btnTrigger.addMouseClickListener(new MouseClickListener() {
            @Override public void mouseClicked(int button, int x, int y, int clicks) {
                EditorToolbar.this.guiRoot.addModalForm(new com.oddlabs.tt.form.TriggerEditorDemoForm(EditorToolbar.this.guiRoot, null));
            }
        });

    // Brush controls placed inline after buttons: Radius [----] 00m   Intensity [----] 0.0
    int sliderLen = 120;
        radiusLabel = new Label("Radius", Skin.getSkin().getEditFont());
        radiusSlider = new Slider(sliderLen, 1, 200, 6);
        // Widen numeric labels to prevent clipping (allow up to "200m" and "10.0")
        radiusValue = new Label("200m", Skin.getSkin().getEditFont(), Skin.getSkin().getEditFont().getWidth("200m") + 12);
        intensityLabel = new Label("Intensity", Skin.getSkin().getEditFont());
        intensitySlider = new Slider(sliderLen, 1, 100, 50); // 1..100 -> 0.1..10.0
        intensityValue = new Label("10.0", Skin.getSkin().getEditFont(), Skin.getSkin().getEditFont().getWidth("10.0") + 12);

        addChild(radiusLabel);
        addChild(radiusSlider);
        addChild(radiusValue);
        addChild(intensityLabel);
        addChild(intensitySlider);
        addChild(intensityValue);

    // Inline layout following the action buttons
    radiusLabel.place(btnOnline, RIGHT_MID, spacing);
    radiusSlider.place(radiusLabel, RIGHT_MID);
    radiusValue.place(radiusSlider, RIGHT_MID, spacing / 2);
    intensityLabel.place(radiusValue, RIGHT_MID, spacing);
    intensitySlider.place(intensityLabel, RIGHT_MID);
    intensityValue.place(intensitySlider, RIGHT_MID, spacing / 2);

        // Third row: selectors (Tool, Mode, Resource, Overlays)
        toolLabel = new Label("Tool", Skin.getSkin().getEditFont());
        PulldownMenu toolMenu = new PulldownMenu();
    toolMenu.addItem(new PulldownItem("Terrain"));
    toolMenu.addItem(new PulldownItem("Resource"));
    toolMenu.addItem(new PulldownItem("Entities"));
    toolButton = new PulldownButton(guiRoot, toolMenu, 110);

        modeLabel = new Label("Mode", Skin.getSkin().getEditFont());
        PulldownMenu modeMenu = new PulldownMenu();
        if (optionsBinding != null) {
            for (String n : optionsBinding.getBrushModeNames()) modeMenu.addItem(new PulldownItem(n));
        } else {
            modeMenu.addItem(new PulldownItem("Raise/Lower"));
            modeMenu.addItem(new PulldownItem("Flatten"));
            modeMenu.addItem(new PulldownItem("Soften"));
            modeMenu.addItem(new PulldownItem("Smooth"));
            modeMenu.addItem(new PulldownItem("River"));
            modeMenu.addItem(new PulldownItem("Ramp"));
            modeMenu.addItem(new PulldownItem("Rough"));
            modeMenu.addItem(new PulldownItem("Random"));
        }
    modeButton = new PulldownButton(guiRoot, modeMenu, 140);

        resourceLabel = new Label("Resource", Skin.getSkin().getEditFont());
        PulldownMenu resMenu = new PulldownMenu();
        if (optionsBinding != null) {
            for (String n : optionsBinding.getResourceTypeNames()) resMenu.addItem(new PulldownItem(n));
        } else {
            resMenu.addItem(new PulldownItem("Rock"));
            resMenu.addItem(new PulldownItem("Iron"));
            resMenu.addItem(new PulldownItem("Tree Jungle"));
            resMenu.addItem(new PulldownItem("Tree Palm"));
            resMenu.addItem(new PulldownItem("Tree Oak"));
            resMenu.addItem(new PulldownItem("Tree Pine"));
        }
    resourceButton = new PulldownButton(guiRoot, resMenu, 140);

        overlayMaster = new CheckBox(false, "Overlays");
        overlayLabel = new Label("Layer", Skin.getSkin().getEditFont());
        PulldownMenu ovMenu = new PulldownMenu();
        if (optionsBinding != null) {
            for (String n : optionsBinding.getOverlayLayerNames()) ovMenu.addItem(new PulldownItem(n));
        } else {
            ovMenu.addItem(new PulldownItem("Water"));
            ovMenu.addItem(new PulldownItem("Dock"));
            ovMenu.addItem(new PulldownItem("Access"));
            ovMenu.addItem(new PulldownItem("Build"));
            ovMenu.addItem(new PulldownItem("Resource"));
            ovMenu.addItem(new PulldownItem("Slope"));
        }
    overlayButton = new PulldownButton(guiRoot, ovMenu, 140);

        addChild(toolLabel);
        addChild(toolButton);
        addChild(modeLabel);
        addChild(modeButton);
        addChild(resourceLabel);
        addChild(resourceButton);
        // Entities UI (hidden unless tool=Entities)
        entitiesTypeLabel = new Label("Type", Skin.getSkin().getEditFont());
        PulldownMenu entitiesTypeMenu = new PulldownMenu();
        entitiesTypeMenu.addItem(new PulldownItem("Buildings"));
        entitiesTypeMenu.addItem(new PulldownItem("Units"));
        entitiesTypeButton = new PulldownButton(guiRoot, entitiesTypeMenu, 0, 120);

        entitiesKindLabel = new Label("Kind", Skin.getSkin().getEditFont());
        PulldownMenu entitiesKindMenu = new PulldownMenu();
        // Default contents; concrete items come from binding/tool logic, but provide sensible defaults
        entitiesKindMenu.addItem(new PulldownItem("Quarters"));
        entitiesKindMenu.addItem(new PulldownItem("Armory"));
        entitiesKindMenu.addItem(new PulldownItem("Tower"));
        entitiesKindMenu.addItem(new PulldownItem("Ship"));
        entitiesKindMenu.addItem(new PulldownItem("Peon"));
        entitiesKindMenu.addItem(new PulldownItem("Warrior Rock"));
        entitiesKindMenu.addItem(new PulldownItem("Warrior Iron"));
        entitiesKindMenu.addItem(new PulldownItem("Warrior Chicken"));
        entitiesKindMenu.addItem(new PulldownItem("Chieftain"));
        entitiesKindButton = new PulldownButton(guiRoot, entitiesKindMenu, 0, 170);

        entitiesTeamLabel = new Label("Team", Skin.getSkin().getEditFont());
        PulldownMenu teamMenu = new PulldownMenu();
        teamMenu.addItem(new PulldownItem("Neutral"));
        for (int i=0;i<8;i++) teamMenu.addItem(new PulldownItem("Team " + i));
        entitiesTeamButton = new PulldownButton(guiRoot, teamMenu, 1, 120);

        entitiesRaceLabel = new Label("Race", Skin.getSkin().getEditFont());
        PulldownMenu raceMenu = new PulldownMenu();
        raceMenu.addItem(new PulldownItem("Natives"));
        raceMenu.addItem(new PulldownItem("Vikings"));
        entitiesRaceButton = new PulldownButton(guiRoot, raceMenu, 0, 120);

        addChild(entitiesTypeLabel);
        addChild(entitiesTypeButton);
        addChild(entitiesKindLabel);
        addChild(entitiesKindButton);
        addChild(entitiesTeamLabel);
        addChild(entitiesTeamButton);
        addChild(entitiesRaceLabel);
        addChild(entitiesRaceButton);
        addChild(overlayMaster);
        addChild(overlayLabel);
        addChild(overlayButton);


    // Tool selector follows intensity controls
        toolLabel.place(intensityValue, RIGHT_MID, spacing);
        toolButton.place(toolLabel, RIGHT_MID);
        // Mode and Resource occupy the same position; we'll toggle visibility later
        modeLabel.place(toolButton, RIGHT_MID, spacing);
        modeButton.place(modeLabel, RIGHT_MID);
    resourceLabel.place(toolButton, RIGHT_MID, spacing);
    resourceButton.place(resourceLabel, RIGHT_MID);
    // Entities controls occupy the same slot sequence as Resource, chained after tool selector
    entitiesTypeLabel.place(toolButton, RIGHT_MID, spacing);
    entitiesTypeButton.place(entitiesTypeLabel, RIGHT_MID);
    entitiesKindLabel.place(entitiesTypeButton, RIGHT_MID, spacing);
    entitiesKindButton.place(entitiesKindLabel, RIGHT_MID);
    entitiesTeamLabel.place(entitiesKindButton, RIGHT_MID, spacing);
    entitiesTeamButton.place(entitiesTeamLabel, RIGHT_MID);
    entitiesRaceLabel.place(entitiesTeamButton, RIGHT_MID, spacing);
    entitiesRaceButton.place(entitiesRaceLabel, RIGHT_MID);
        // Overlay controls follow whichever is visible (anchor to resourceButton for stable layout)
    overlayMaster.place(resourceButton, RIGHT_MID, spacing);
    overlayLabel.place(overlayMaster, RIGHT_MID, spacing);
    overlayButton.place(overlayLabel, RIGHT_MID);
    btnTrigger.place(overlayButton, RIGHT_MID);

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

        // Wire selectors -> binding
        if (optionsBinding != null) {
            // Initialize defaults
            selectPulldownIndex(toolButton, clamp(optionsBinding.getActiveToolIndex(), 0, 2));
            selectPulldownIndex(modeButton, clamp(optionsBinding.getBrushModeIndex(), 0, modeButton.getMenu().getSize() - 1));
            selectPulldownIndex(resourceButton, clamp(optionsBinding.getResourceTypeIndex(), 0, resourceButton.getMenu().getSize() - 1));
            // Entities: populate and initialize
            // Type
            int eTypeIdx = clamp(optionsBinding.getEntitiesTypeIndex(), 0, entitiesTypeButton.getMenu().getSize() - 1);
            selectPulldownIndex(entitiesTypeButton, eTypeIdx);
            // Kind: rebuild menu items based on type
            rebuildEntitiesKindMenu();
            int eKindMax = entitiesKindButton.getMenu().getSize() - 1;
            int eKindIdx = clamp(optionsBinding.getEntitiesKindIndex(), 0, eKindMax);
            selectPulldownIndex(entitiesKindButton, eKindIdx);
            // Team
            int eTeamIdx = clamp(optionsBinding.getEntitiesTeamIndex(), 0, entitiesTeamButton.getMenu().getSize() - 1);
            selectPulldownIndex(entitiesTeamButton, eTeamIdx);
            // Race
            int eRaceIdx = clamp(optionsBinding.getEntitiesRaceIndex(), 0, entitiesRaceButton.getMenu().getSize() - 1);
            selectPulldownIndex(entitiesRaceButton, eRaceIdx);
            overlayMaster.setMarked(optionsBinding.isOverlayMaster());
            selectPulldownIndex(overlayButton, clamp(optionsBinding.getOverlayLayerIndex(), 0, overlayButton.getMenu().getSize() - 1));

            resourceButton.getMenu().addItemChosenListener((menu, idx) -> {
                if (menu == null) return; // mark param used to satisfy strict checks
                if (!suppressProgrammatic) optionsBinding.setResourceTypeIndex(idx);
            });
            modeButton.getMenu().addItemChosenListener((menu, idx) -> {
                if (menu == null) return;
                if (!suppressProgrammatic) optionsBinding.setBrushModeIndex(idx);
            });
            toolButton.getMenu().addItemChosenListener((menu, idx) -> {
                if (menu == null) return;
                if (!suppressProgrammatic) optionsBinding.setActiveToolIndex(idx);
                // Reflect tool change immediately in the UI (toggle Mode vs Resource)
                updateActiveToolUI();
            });
            entitiesTypeButton.getMenu().addItemChosenListener((menu, idx) -> {
                if (menu == null) return;
                if (!suppressProgrammatic) optionsBinding.setEntitiesTypeIndex(idx);
                rebuildEntitiesKindMenu();
                // After type change, reset kind to 0 visually
                selectPulldownIndex(entitiesKindButton, 0);
            });
            entitiesKindButton.getMenu().addItemChosenListener((menu, idx) -> {
                if (menu == null) return;
                if (!suppressProgrammatic) optionsBinding.setEntitiesKindIndex(idx);
            });
            entitiesTeamButton.getMenu().addItemChosenListener((menu, idx) -> {
                if (menu == null) return;
                if (!suppressProgrammatic) optionsBinding.setEntitiesTeamIndex(idx);
            });
            entitiesRaceButton.getMenu().addItemChosenListener((menu, idx) -> {
                if (menu == null) return;
                if (!suppressProgrammatic) optionsBinding.setEntitiesRaceIndex(idx);
            });
            overlayButton.getMenu().addItemChosenListener((menu, idx) -> {
                if (menu == null) return;
                if (!suppressProgrammatic) optionsBinding.setOverlayLayerIndex(idx);
            });
            overlayMaster.addCheckBoxListener(new CheckBoxListener() {
                @Override public void checked(boolean marked) {
                    if (!suppressProgrammatic) optionsBinding.setOverlayMaster(marked);
                }
            });
        }

    compileCanvas();
    // Stretch toolbar to full screen width while keeping computed height, and clamp inside borders
    com.oddlabs.tt.gui.Box box = Skin.getSkin().getFormData().getForm();
    int lw = box.getLeftOffset();
    int rw = box.getRightOffset();
    int th = box.getTopOffset();
    int bh = box.getBottomOffset();
    int screenW = LocalInput.getViewWidth();
    int screenH = LocalInput.getViewHeight();
    setDim(screenW, getHeight());
    int clampedX = clamp(getX(), lw, StrictMath.max(lw, screenW - getWidth() - rw));
    int clampedY = clamp(getY(), th, StrictMath.max(th, screenH - getHeight() - bh));
    setPos(clampedX, clampedY);
        setHidden(false);

        // During drags, clamp vertical position within viewport; allow free horizontal movement.
        addMouseMotionListener(new MouseMotionListener() {
            public void mouseDragged(int button, int x, int y, int rel_x, int rel_y, int abs_x, int abs_y) {
                int h = EditorToolbar.this.getHeight();
                int maxY = StrictMath.max(0, LocalInput.getViewHeight() - h);
                int clampedY = clamp(EditorToolbar.this.getY(), 0, maxY);
                EditorToolbar.this.setPos(EditorToolbar.this.getX(), clampedY);
            }
            public void mouseMoved(int x, int y) {}
            public void mouseEntered() {}
            public void mouseExited() {}
        });

        // Initialize positions from bindings if available
        syncFromBinding();
        syncOptionsFromBinding();
    }

    public void toggleVisible() { visible = !visible; setHidden(!visible); }

    public void dockTopLeft(int offsetX, int offsetY) {
    // Natural-width panel anchored near top-left, including border
    com.oddlabs.tt.gui.Box box = Skin.getSkin().getFormData().getForm();
    int lw = box.getLeftOffset();
    int th = box.getTopOffset();
    setPos(lw + offsetX, th + offsetY);
    }

    public void dockBottomLeft(int offsetX, int offsetY) {
    // Natural-width panel anchored near bottom-left, including border
    com.oddlabs.tt.gui.Box box = Skin.getSkin().getFormData().getForm();
    int lw = box.getLeftOffset();
    int bh = box.getBottomOffset();
    int y = StrictMath.max(bh, LocalInput.getViewHeight() - getHeight() - bh - offsetY);
    setPos(lw + offsetX, y);
    }

    public void setBrushBinding(BrushBinding binding) {
        this.brushBinding = binding;
        syncFromBinding();
    }

    public void setOptionsBinding(EditorOptionsBinding binding) {
        this.optionsBinding = binding;
        syncOptionsFromBinding();
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

    public void syncOptionsFromBinding() {
        if (optionsBinding == null) return;
        suppressProgrammatic = true;
        try {
            selectPulldownIndex(toolButton, clamp(optionsBinding.getActiveToolIndex(), 0, 2));
            selectPulldownIndex(modeButton, clamp(optionsBinding.getBrushModeIndex(), 0, modeButton.getMenu().getSize() - 1));
            selectPulldownIndex(resourceButton, clamp(optionsBinding.getResourceTypeIndex(), 0, resourceButton.getMenu().getSize() - 1));
            // Entities
            rebuildEntitiesKindMenu();
            selectPulldownIndex(entitiesTypeButton, clamp(optionsBinding.getEntitiesTypeIndex(), 0, entitiesTypeButton.getMenu().getSize() - 1));
            selectPulldownIndex(entitiesKindButton, clamp(optionsBinding.getEntitiesKindIndex(), 0, entitiesKindButton.getMenu().getSize() - 1));
            selectPulldownIndex(entitiesTeamButton, clamp(optionsBinding.getEntitiesTeamIndex(), 0, entitiesTeamButton.getMenu().getSize() - 1));
            selectPulldownIndex(entitiesRaceButton, clamp(optionsBinding.getEntitiesRaceIndex(), 0, entitiesRaceButton.getMenu().getSize() - 1));
            overlayMaster.setMarked(optionsBinding.isOverlayMaster());
            selectPulldownIndex(overlayButton, clamp(optionsBinding.getOverlayLayerIndex(), 0, overlayButton.getMenu().getSize() - 1));
            updateActiveToolUI();
        } finally {
            suppressProgrammatic = false;
        }
    }

    // Close any open pulldown menus owned by this toolbar to avoid stealing keyboard focus
    public void closeOpenMenus() {
        try { if (toolButton != null) toolButton.getMenu().remove(); } catch (Throwable ignore) {}
        try { if (modeButton != null) modeButton.getMenu().remove(); } catch (Throwable ignore) {}
        try { if (resourceButton != null) resourceButton.getMenu().remove(); } catch (Throwable ignore) {}
        try { if (entitiesTypeButton != null) entitiesTypeButton.getMenu().remove(); } catch (Throwable ignore) {}
        try { if (entitiesKindButton != null) entitiesKindButton.getMenu().remove(); } catch (Throwable ignore) {}
        try { if (entitiesTeamButton != null) entitiesTeamButton.getMenu().remove(); } catch (Throwable ignore) {}
        try { if (entitiesRaceButton != null) entitiesRaceButton.getMenu().remove(); } catch (Throwable ignore) {}
        try { if (overlayButton != null) overlayButton.getMenu().remove(); } catch (Throwable ignore) {}
    }

    // Update the Entities Kind menu to reflect the current Type (Buildings vs Units)
    private void rebuildEntitiesKindMenu() {
        if (optionsBinding == null) return;
        PulldownMenu menu = entitiesKindButton.getMenu();
        try { menu.clearItems(); } catch (Throwable ignore) {}
        String[] names = optionsBinding.getEntitiesKindNames();
        for (String n : names) menu.addItem(new PulldownItem(n));
    }

    // Toggle Mode vs Resource controls in the same slot based on active tool
    private void updateActiveToolUI() {
        if (optionsBinding == null) return;
        int toolIdx = optionsBinding.getActiveToolIndex();
        boolean terrain = (toolIdx == 0);
        boolean resource = (toolIdx == 1);
    boolean entities = (toolIdx == 2);
        // Show Mode for terrain tool, Resource for resource tool, and disable the inactive ones
        modeLabel.setHidden(!terrain);
        modeButton.setHidden(!terrain);
        modeLabel.setDisabled(!terrain);
        modeButton.setDisabled(!terrain);
        resourceLabel.setHidden(!resource);
        resourceButton.setHidden(!resource);
        resourceLabel.setDisabled(!resource);
        resourceButton.setDisabled(!resource);
    // Entities selectors: keep Type visible in toolbar; move Kind/Team/Race to separate panel
    entitiesTypeLabel.setHidden(!entities);
    entitiesTypeButton.setHidden(!entities);
    entitiesTypeLabel.setDisabled(!entities);
    entitiesTypeButton.setDisabled(!entities);
    // Hide the rest (now in Entities panel)
    entitiesKindLabel.setHidden(true);
    entitiesKindButton.setHidden(true);
    entitiesTeamLabel.setHidden(true);
    entitiesTeamButton.setHidden(true);
    entitiesRaceLabel.setHidden(true);
    entitiesRaceButton.setHidden(true);
        // Close only the menu belonging to the now-hidden control to prevent stale overlays.
        // Important: do NOT change focus here; when tool/mode is updated programmatically (Q/W,
        // scroll, etc.), we must not steal focus from the editor canvas.
        try {
            if (terrain) { resourceButton.getMenu().remove(); }
            else if (resource) { modeButton.getMenu().remove(); }
            else { modeButton.getMenu().remove(); resourceButton.getMenu().remove(); }
        } catch (Throwable ignore) {}
    }


    private void selectPulldownIndex(PulldownButton btn, int idx) {
        try {
            btn.getMenu().chooseItem(idx);
        } catch (Throwable ignore) {}
    }

    private static int clamp(int v, int min, int max) { return v < min ? min : (v > max ? max : v); }
    private static String format1(float v) { return String.format(java.util.Locale.US, "%.1f", v); }

    @Override
    protected void displayChangedNotify(int width, int height) {
        // Stretch to full screen width and clamp position so borders remain visible
        setDim(width, getHeight());
        com.oddlabs.tt.gui.Box box = Skin.getSkin().getFormData().getForm();
        int lw = box.getLeftOffset();
        int rw = box.getRightOffset();
        int th = box.getTopOffset();
        int bh = box.getBottomOffset();
        int clampedX = clamp(getX(), lw, StrictMath.max(lw, width - getWidth() - rw));
        int clampedY = clamp(getY(), th, StrictMath.max(th, height - getHeight() - bh));
        setPos(clampedX, clampedY);
    }

    // Note: width is controlled via setDim after compile and on resize.

    // No mouse overrides: Form handles top bar drag; we clamp via listener

    // Rendering uses Form's default border/background; no custom GL code here.
}
