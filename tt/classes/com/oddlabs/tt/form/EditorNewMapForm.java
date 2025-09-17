package com.oddlabs.tt.form;

import com.oddlabs.tt.editor.MapEditorSession;
import com.oddlabs.tt.editor.ui.EditorState;
import com.oddlabs.tt.gui.*;
import com.oddlabs.tt.guievent.ValueListener;
import com.oddlabs.tt.util.WordsEncoding;

import java.util.Random;
import java.math.BigInteger;
import com.oddlabs.tt.util.ServerMessageBundler;

/**
 * Minimal in-editor "New Map" dialog. Mirrors a subset of MapEditorMenuForm:
 * - Size, Terrain Type, Gamespeed pulldowns
 * - Hills / Vegetation / Supplies sliders
 * - Seed (implicit via mapcode regeneration)
 * - Sandbox toggle
 *
 * Excludes Load/Test buttons; action buttons are [Create] [Cancel].
 */
public class EditorNewMapForm extends Form {
    private final GUIRoot gui_root;

    private final PulldownMenu pm_gamespeed;
    private final PulldownMenu pulldown_size;
    private final PulldownMenu pm_terrain_type;
    private final Slider slider_hills;
    private final Slider slider_vegetation;
    private final Slider slider_supplies;
    private final Label label_mapcode;
    private final int seed;
    private final CheckBox sandboxModeCheck;

    // Match MapEditorMenuForm / ServerMessageBundler SIZE indices: small, medium, large, enormous, archipelago
    private static final int[] SIZES = new int[] {256, 512, 1024, 2048, 2048}; // indices 0..4
    private static final boolean[] ARCHIPELAGO = new boolean[] {false, false, false, false, true};

    // (No direct resource bundle lookups here; ServerMessageBundler supplies localized strings.)

    public EditorNewMapForm(GUIRoot gui_root) {
        super("New Map");
        this.gui_root = gui_root;

    // NOTE: Previously created a Panel with a null caption which caused an NPE because
    // PanelTab -> Label does not allow a null CharSequence (TextField constructor calls toString()).
    // Removed the unused Panel container to avoid passing null captions.

    // Gamespeed (slow, normal, fast, ludicrous) using ServerMessageBundler; exclude pause
    pm_gamespeed = new PulldownMenu();
    pm_gamespeed.addItem(new PulldownItem(ServerMessageBundler.getGamespeedString(1))); // slow
    pm_gamespeed.addItem(new PulldownItem(ServerMessageBundler.getGamespeedString(2))); // normal
    pm_gamespeed.addItem(new PulldownItem(ServerMessageBundler.getGamespeedString(3))); // fast
    pm_gamespeed.addItem(new PulldownItem(ServerMessageBundler.getGamespeedString(4))); // ludicrous
    pm_gamespeed.chooseItem(1); // default Normal
    PulldownButton gamespeedButton = new PulldownButton(gui_root, pm_gamespeed, 0, 180);

    // Size (small, medium, large, enormous, archipelago)
    pulldown_size = new PulldownMenu();
    pulldown_size.addItem(new PulldownItem(ServerMessageBundler.getSizeString(0))); // small
    pulldown_size.addItem(new PulldownItem(ServerMessageBundler.getSizeString(1))); // medium
    pulldown_size.addItem(new PulldownItem(ServerMessageBundler.getSizeString(2))); // large
    pulldown_size.addItem(new PulldownItem(ServerMessageBundler.getSizeString(3))); // enormous
    pulldown_size.addItem(new PulldownItem(ServerMessageBundler.getSizeString(4))); // archipelago
    pulldown_size.chooseItem(1); // default Medium (512)
    PulldownButton sizeButton = new PulldownButton(gui_root, pulldown_size, 2, 180);

        // Terrain type (native/tropical, viking/northern)
        pm_terrain_type = new PulldownMenu();
        pm_terrain_type.addItem(new PulldownItem(ServerMessageBundler.getTerrainTypeString(0))); // native
        pm_terrain_type.addItem(new PulldownItem(ServerMessageBundler.getTerrainTypeString(1))); // viking
        // Default to first terrain type (index 0). Use PulldownButton ctor with item index to ensure label updates.
        PulldownButton terrainButton = new PulldownButton(gui_root, pm_terrain_type, 0, 180);

        // Sliders (0..10 scale like main menu form)
        slider_hills = new Slider(140, 0, 10, 5);
        slider_vegetation = new Slider(140, 0, 10, 5);
        slider_supplies = new Slider(140, 0, 10, 5);

        label_mapcode = new Label("", Skin.getSkin().getEditFont());
        seed = new Random().nextInt(Integer.MAX_VALUE);
        setMapcode();

        sandboxModeCheck = new CheckBox(false, "Sandbox");

        // Layout redesign:
        //  - Left column: pulldowns (Size, Terrain, Gamespeed)
        //  - Middle column: sliders (Hills, Vegetation, Supplies) then Sandbox + Mapcode
        //  - Bottom full-width: buttons (Create, Cancel) horizontally
        int spacing = StrictMath.max(6, Skin.getSkin().getFormData().getObjectSpacing());

        // Place pulldowns column
        sizeButton.place();
        terrainButton.place(sizeButton, BOTTOM_LEFT, spacing);
        gamespeedButton.place(terrainButton, BOTTOM_LEFT, spacing);

        // Sliders column to the right of pulldowns
        slider_hills.place(sizeButton, RIGHT_TOP, spacing * 4); // align top with size button
        slider_vegetation.place(slider_hills, BOTTOM_LEFT, spacing);
        slider_supplies.place(slider_vegetation, BOTTOM_LEFT, spacing);

        // Sandbox + mapcode below sliders column
        sandboxModeCheck.place(slider_supplies, BOTTOM_LEFT, spacing * 2);
        label_mapcode.place(sandboxModeCheck, BOTTOM_LEFT, spacing / 2);

        // Buttons row at bottom centered relative to form content width
        HorizButton btnCreate = new HorizButton("Create", 110);
        HorizButton btnCancel = new HorizButton("Cancel", 110);
        // Temporarily place create under mapcode to get reference position
        btnCreate.place(label_mapcode, BOTTOM_LEFT, spacing * 3);
        btnCancel.place(btnCreate, RIGHT_MID, spacing);
        addChild(btnCreate);
        addChild(btnCancel);

        // Register all interactive children
        addChild(sizeButton); addChild(terrainButton); addChild(gamespeedButton);
        addChild(slider_hills); addChild(slider_vegetation); addChild(slider_supplies);
        addChild(sandboxModeCheck); addChild(label_mapcode);

        // Listeners to update mapcode
    ValueListener v = new ValueListener() { @Override public void valueSet(int value) { setMapcode(); } };
        slider_hills.addValueListener(v);
        slider_vegetation.addValueListener(v);
        slider_supplies.addValueListener(v);
    pm_gamespeed.addItemChosenListener(new com.oddlabs.tt.guievent.ItemChosenListener() { @Override public void itemChosen(PulldownMenu m,int i){ setMapcode(); }});
    pulldown_size.addItemChosenListener(new com.oddlabs.tt.guievent.ItemChosenListener() { @Override public void itemChosen(PulldownMenu m,int i){ setMapcode(); }});
    pm_terrain_type.addItemChosenListener(new com.oddlabs.tt.guievent.ItemChosenListener() { @Override public void itemChosen(PulldownMenu m,int i){ setMapcode(); }});

    btnCancel.addMouseClickListener(new com.oddlabs.tt.guievent.MouseClickListener(){ @Override public void mouseClicked(int b,int x,int y,int c){ remove(); }});
    btnCreate.addMouseClickListener(new com.oddlabs.tt.guievent.MouseClickListener(){ @Override public void mouseClicked(int b,int x,int y,int c){ startEditor(); }});

        compileCanvas();
    }

    // Removed addTerrainItem helper (no longer needed with ServerMessageBundler)

    private void setMapcode() {
        int sizeIndex = pulldown_size.getChosenItemIndex();
    int terrain_index = pm_terrain_type.getChosenItemIndex();
    if (terrain_index < 0) terrain_index = 0;
    if (terrain_index > 1) terrain_index = 1; // only native/viking currently
        int gamespeedIndex = pm_gamespeed.getChosenItemIndex();
        int hills = slider_hills.getValue();
        int vegetation = slider_vegetation.getValue();
        int supplies = slider_supplies.getValue();
        // Compose a simple BigInteger similar to MapEditorMenuForm encoding
    // Only 2 terrain types exposed (0/1); store 1 bit instead of masking with 3
    BigInteger bi = BigInteger.valueOf(terrain_index & 1);
        bi = bi.shiftLeft(3).or(BigInteger.valueOf(hills & 7));
        bi = bi.shiftLeft(3).or(BigInteger.valueOf(vegetation & 7));
        bi = bi.shiftLeft(3).or(BigInteger.valueOf(supplies & 7));
        bi = bi.shiftLeft(3).or(BigInteger.valueOf(gamespeedIndex & 7));
        bi = bi.shiftLeft(3).or(BigInteger.valueOf(sizeIndex & 7));
        bi = bi.shiftLeft(17).or(BigInteger.valueOf(seed & 0x1FFFF));
    label_mapcode.set(WordsEncoding.encode(bi));
    }

    private void startEditor() {
        if (gui_root == null) { remove(); return; }
        int sizeIndex = pulldown_size.getChosenItemIndex();
        int meters = SIZES[sizeIndex];
    int terrain_type = pm_terrain_type.getChosenItemIndex();
    if (terrain_type < 0) terrain_type = 0;
    if (terrain_type > 1) terrain_type = 1;
        float hills = slider_hills.getValue() / 10f;
        float vegetation = slider_vegetation.getValue() / 10f;
        float supplies = slider_supplies.getValue() / 10f;
    // Convert selection (0..3) to actual Game speed constant values (slow=1..ludicrous=4)
    int gamespeed = pm_gamespeed.getChosenItemIndex() + 1; // slow=1 .. ludicrous=4
        boolean archipelago = ARCHIPELAGO[sizeIndex];

        // Debug instrumentation to diagnose persistent index OOB
        System.out.println("[EditorNewMapForm] startEditor() sizeIndex=" + sizeIndex +
                " meters=" + meters +
                " terrain_type=" + terrain_type +
                " gamespeed=" + gamespeed +
                " hills=" + hills +
                " vegetation=" + vegetation +
                " supplies=" + supplies +
                " archipelago=" + archipelago +
                " sandbox=" + (sandboxModeCheck != null && sandboxModeCheck.isMarked()));

        com.oddlabs.tt.resource.WorldGenerator generator =
            new com.oddlabs.tt.resource.IslandGenerator(
                meters, terrain_type, hills, vegetation, supplies, seed * seed, archipelago);
        try {
            // Attempt to reflect terrain type back out if interface allows
            if (generator instanceof com.oddlabs.tt.resource.IslandGenerator) {
                System.out.println("[EditorNewMapForm] IslandGenerator terrain_type=" + ((com.oddlabs.tt.resource.IslandGenerator)generator).getTerrainType());
            }
        } catch (Throwable ignore) {}
        try {
            MapEditorSession.start(
                MapEditorSession.getEditorNetwork(),
                gui_root.getGUI(),
                meters,
                generator,
                gamespeed,
                sandboxModeCheck != null && sandboxModeCheck.isMarked() ? EditorState.EditorMode.Sandbox : EditorState.EditorMode.Default);
        } catch (Throwable t) {
            // Print full stack trace to console for diagnostic purposes
            System.err.println("[EditorNewMapForm] Create map failed: " + t);
            t.printStackTrace();
            try { gui_root.getInfoPrinter().print("Create map failed: " + t.getMessage()); } catch (Throwable ignore) {}
        }
        remove();
    }

    @Override
    public void setFocus() {
        super.setFocus();
    }
}
