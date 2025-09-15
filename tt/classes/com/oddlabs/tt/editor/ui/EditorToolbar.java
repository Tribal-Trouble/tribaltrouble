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
import org.lwjgl.opengl.GL11;

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

        // Buttons row: [Test] [Save] [Load] [Online]
    HorizButton btnTest = new HorizButton("Test (Ctrl+P)", 120);
        HorizButton btnSave = new HorizButton("Save", 80);
        HorizButton btnLoad = new HorizButton("Load", 80);
        HorizButton btnOnline = new HorizButton("Online", 90);

        addChild(btnTest);
        addChild(btnSave);
        addChild(btnLoad);
        addChild(btnOnline);

    // Layout in a single horizontal row (start at content origin, chain RIGHT_MID)
    int spacing = StrictMath.max(2, Skin.getSkin().getFormData().getObjectSpacing());
    btnTest.place();
        btnSave.place(btnTest, RIGHT_MID);
        btnLoad.place(btnSave, RIGHT_MID);
        btnOnline.place(btnLoad, RIGHT_MID);

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
                // Open the Test Map modal form (reuses Single Player players section)
                try {
                    EditorToolbar.this.guiRoot.addModalForm(
                            new com.oddlabs.tt.form.TestMapForm(
                                    EditorToolbar.this.guiRoot,
                                    com.oddlabs.tt.editor.MapEditorSession.getEditorNetwork(),
                                    EditorToolbar.this.world,
                                    EditorToolbar.this.terrainType));
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
        // Overlay controls follow whichever is visible (anchor to resourceButton for stable layout)
    overlayMaster.place(resourceButton, RIGHT_MID, spacing);
        overlayLabel.place(overlayMaster, RIGHT_MID, spacing);
        overlayButton.place(overlayLabel, RIGHT_MID);

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
            selectPulldownIndex(toolButton, clamp(optionsBinding.getActiveToolIndex(), 0, 1));
            selectPulldownIndex(modeButton, clamp(optionsBinding.getBrushModeIndex(), 0, modeButton.getMenu().getSize() - 1));
            selectPulldownIndex(resourceButton, clamp(optionsBinding.getResourceTypeIndex(), 0, resourceButton.getMenu().getSize() - 1));
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
        // Natural-size panel; caller sets initial position.
        setPos(getX(), Math.max(0, getY() == 0 ? 8 : getY()));
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
        // Natural-width panel anchored near top-left
        setPos(offsetX, offsetY);
    }

    public void dockBottomLeft(int offsetX, int offsetY) {
        // Natural-width panel anchored near bottom-left
        int y = StrictMath.max(0, LocalInput.getViewHeight() - getHeight() - offsetY);
        setPos(offsetX, y);
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
            selectPulldownIndex(toolButton, clamp(optionsBinding.getActiveToolIndex(), 0, 1));
            selectPulldownIndex(modeButton, clamp(optionsBinding.getBrushModeIndex(), 0, modeButton.getMenu().getSize() - 1));
            selectPulldownIndex(resourceButton, clamp(optionsBinding.getResourceTypeIndex(), 0, resourceButton.getMenu().getSize() - 1));
            overlayMaster.setMarked(optionsBinding.isOverlayMaster());
            selectPulldownIndex(overlayButton, clamp(optionsBinding.getOverlayLayerIndex(), 0, overlayButton.getMenu().getSize() - 1));
            updateActiveToolUI();
        } finally {
            suppressProgrammatic = false;
        }
    }

    // Toggle Mode vs Resource controls in the same slot based on active tool
    private void updateActiveToolUI() {
        if (optionsBinding == null) return;
        int toolIdx = optionsBinding.getActiveToolIndex();
        boolean terrain = (toolIdx == 0); // 0 = TERRAIN, 1 = RESOURCE
        // Show Mode for terrain tool, Resource for resource tool, and disable the inactive ones
        modeLabel.setHidden(!terrain);
        modeButton.setHidden(!terrain);
        modeLabel.setDisabled(!terrain);
        modeButton.setDisabled(!terrain);
        resourceLabel.setHidden(terrain);
        resourceButton.setHidden(terrain);
        resourceLabel.setDisabled(terrain);
        resourceButton.setDisabled(terrain);
        // Close only the menu belonging to the now-hidden control to prevent stale overlays,
        // leaving the visible control free to open immediately on click.
        try {
            if (terrain) {
                resourceButton.getMenu().remove();
                // Nudge focus to the now-visible Mode control so it accepts clicks immediately
                modeButton.setFocus();
            } else {
                modeButton.getMenu().remove();
                // Nudge focus to the now-visible Resource control
                resourceButton.setFocus();
            }
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
        // Keep natural width; clamp vertical position within the viewport
        int clampedY = clamp(getY(), 0, StrictMath.max(0, height - getHeight()));
        setPos(getX(), clampedY);
    }

    // No mouse overrides: Form handles top bar drag; we clamp via listener

    // Render with opaque border and semi-transparent center fill.
    @Override
    protected void renderGeometry(float clip_left, float clip_right, float clip_bottom, float clip_top) {
        // Close the current batch so we can use scissor states safely
        GL11.glEnd();

        // Resolve skin box and dimensions
        com.oddlabs.tt.gui.Box box = Skin.getSkin().getFormData().getForm();
        int w = getWidth();
        int h = getHeight();
        int lw = box.getLeftOffset();
        int rw = box.getRightOffset();
        int bh = box.getBottomOffset();
        int th = box.getTopOffset();
        int innerW = StrictMath.max(0, w - lw - rw);
        int innerH = StrictMath.max(0, h - bh - th);

        // Select skin state
        int skinType = isDisabled() ? Skin.DISABLED : (isActive() ? Skin.ACTIVE : Skin.NORMAL);

        // Bind texture and draw borders opaque using scissor strips
        Skin.getSkin().bindTexture();
        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);

        int rootX = (int) getRootX();
        int rootY = (int) getRootY();

        // Top border strip
        if (th > 0) {
            GL11.glScissor(rootX, rootY + h - th, w, th);
            GL11.glBegin(GL11.GL_QUADS);
            box.render(0, 0, w, h, skinType);
            GL11.glEnd();
        }
        // Bottom border strip
        if (bh > 0) {
            GL11.glScissor(rootX, rootY, w, bh);
            GL11.glBegin(GL11.GL_QUADS);
            box.render(0, 0, w, h, skinType);
            GL11.glEnd();
        }
        // Left border strip
        if (lw > 0) {
            GL11.glScissor(rootX, rootY, lw, h);
            GL11.glBegin(GL11.GL_QUADS);
            box.render(0, 0, w, h, skinType);
            GL11.glEnd();
        }
        // Right border strip
        if (rw > 0) {
            GL11.glScissor(rootX + w - rw, rootY, rw, h);
            GL11.glBegin(GL11.GL_QUADS);
            box.render(0, 0, w, h, skinType);
            GL11.glEnd();
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // Draw center with 65% opacity
        if (innerW > 0 && innerH > 0) {
            GL11.glColor4f(1f, 1f, 1f, 0.65f);
            GL11.glBegin(GL11.GL_QUADS);
            box.renderHighlight(lw, bh, innerW, innerH, 0, w, 0, h);
            GL11.glEnd();
        }

        // Restore color and leave a GL_QUADS batch open for parent pipeline
        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glBegin(GL11.GL_QUADS);
    }
}
