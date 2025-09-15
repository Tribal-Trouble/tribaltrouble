package com.oddlabs.tt.viewer;

import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.delegate.GameStatsDelegate;
import com.oddlabs.tt.delegate.InGameMainMenu;
import com.oddlabs.tt.editor.MapEditorSession;
import com.oddlabs.tt.editor.ui.EditorState;
import com.oddlabs.tt.gui.GUIObject;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.MenuButton;
import com.oddlabs.tt.guievent.MouseClickListener;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.mapio.MapIO;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.resource.IslandGenerator;
import com.oddlabs.tt.resource.WorldGenerator;

/**
 * InGameInfo used when a match was launched from the Editor's Test Map form.
 * Adds a "Return to Editor" action in pause and results screens and safely
 * tears down the match before reopening the editor with the same map.
 */
public final class EditorTestInGameInfo extends DefaultInGameInfo {
    private final java.io.File editorMapFile;

    public EditorTestInGameInfo(java.io.File editorMapFile) {
        this.editorMapFile = editorMapFile;
    }

    @Override
    public void addGUI(final WorldViewer viewer, InGameMainMenu menu, Group game_infos) {
        // Default buttons and player info
        super.addGUI(viewer, menu, game_infos);

        // Add conditional "Return to Editor" button
        if (editorMapFile != null && editorMapFile.exists() && !viewer.isMultiplayer()) {
            // Match MenuButton look using the same RGB values used by core menus.
            float[] COLOR_NORMAL = new float[] {1f, 1f, 1f};
            float[] COLOR_ACTIVE = new float[] {1f, .8f, .63f};
            MenuButton backToEditor = new MenuButton("Return to Editor", COLOR_NORMAL, COLOR_ACTIVE);
            menu.addChild(backToEditor);
            backToEditor.addMouseClickListener(
                    new MouseClickListener() {
                        public void mouseClicked(int button, int x, int y, int clicks) {
                            returnToEditor(viewer);
                        }
                    });
        }
    }

    @Override
    public void addGameOverGUI(
            final WorldViewer viewer,
            final GameStatsDelegate delegate,
            int header_y,
            Group buttons) {
        // Recreate DefaultInGameInfo buttons to control layout and add Return to Editor
        String map_code_str =
                com.oddlabs.tt.util.Utils.getBundleString(
                        GameStatsDelegate.bundle,
                        "map_code",
                        new Object[] {viewer.getParameters().getMapcode()});
        com.oddlabs.tt.gui.Label map_code =
                new com.oddlabs.tt.gui.Label(map_code_str, com.oddlabs.tt.gui.Skin.getSkin().getEditFont());
        delegate.addChild(map_code);
        map_code.setPos(
                (delegate.getWidth() - map_code.getWidth()) / 2,
                header_y - map_code.getHeight());

        // Observer and Main Menu as in DefaultInGameInfo
        HorizButton button_observer =
                new HorizButton(
                        com.oddlabs.tt.util.Utils.getBundleString(
                                GameStatsDelegate.bundle, "observer_mode"),
                        150);
        button_observer.addMouseClickListener(
                new MouseClickListener() {
                    public void mouseClicked(int button, int x, int y, int clicks) {
                        delegate.getViewer().getDelegate().setObserverMode();
                        delegate.pop();
                    }
                });

        HorizButton button_end =
                new HorizButton(
                        com.oddlabs.tt.util.Utils.getBundleString(
                                GameStatsDelegate.bundle, "main_menu"),
                        150);
        button_end.addMouseClickListener(
                new MouseClickListener() {
                    public void mouseClicked(int button, int x, int y, int clicks) {
                        delegate.startMenu();
                    }
                });

        // New: Return to Editor
        HorizButton button_editor = new HorizButton("Return to Editor", 170);
        button_editor.addMouseClickListener(
                new MouseClickListener() {
                    public void mouseClicked(int button, int x, int y, int clicks) {
                        returnToEditor(viewer);
                    }
                });

        // Add and place: [Return to Editor] [Observer] [Main Menu]
        buttons.addChild(button_end);
        buttons.addChild(button_observer);
        buttons.addChild(button_editor);

        button_end.place();
        button_observer.place(button_end, GUIObject.LEFT_MID);
        button_editor.place(button_observer, GUIObject.LEFT_MID);
    }

    private void returnToEditor(WorldViewer viewer) {
        // Shared handler used by pause and results buttons.
        try {
            if (editorMapFile == null || !editorMapFile.exists()) {
                viewer.getGUIRoot().getInfoPrinter().print("Editor map missing; returning to main menu.");
                // Fallback: standard main menu navigation
                Renderer.startMenu(viewer.getNetwork(), viewer.getGUIRoot().getGUI());
                return;
            }

            // Teardown the running match softly (no main menu navigation).
            try { com.oddlabs.tt.event.LocalEventQueue.getQueue().getManager().removeAnimation(viewer); } catch (Throwable ignore) {}
            try { if (viewer.getPeerHub() != null) viewer.getPeerHub().close(); } catch (Throwable ignore) {}

            // Build a generator that loads the same .ttmap used for this test.
            int terrainType = 0;
            int metersPerWorld = 256;
            try {
                MapIO.MapSummary sum = MapIO.peek(editorMapFile);
                if (sum != null) {
                    terrainType = sum.terrainType;
                    metersPerWorld = (sum.metersPerWorld > 0) ? sum.metersPerWorld : (sum.size > 0 ? sum.size : metersPerWorld);
                }
            } catch (Throwable ignore) {}

            WorldGenerator base =
                    new IslandGenerator(
                            metersPerWorld,
                            terrainType,
                            Globals.LANDSCAPE_HILLS,
                            Globals.LANDSCAPE_VEGETATION,
                            Globals.LANDSCAPE_RESOURCES,
                            Globals.LANDSCAPE_SEED,
                            false);
            WorldGenerator gen = new com.oddlabs.tt.mapio.LoadedMapGenerator(base, editorMapFile);

            NetworkSelector network = viewer.getNetwork();
            // Use the initial gamespeed configured for the match as a sane default for the editor.
            int gamespeed = viewer.getParameters().getInitialGameSpeed();

            viewer.getGUIRoot().getInfoPrinter().print("Returning to editor...");
            MapEditorSession.start(
                    network,
                    viewer.getGUIRoot().getGUI(),
                    metersPerWorld,
                    gen,
                    gamespeed,
                    EditorState.EditorMode.Default);
        } catch (Throwable t) {
            try {
                viewer.getGUIRoot().getInfoPrinter().print("Return to editor failed: " + t.getMessage());
            } catch (Throwable ignore) {}
            // Fallback to main menu in case of any failure
            Renderer.startMenu(viewer.getNetwork(), viewer.getGUIRoot().getGUI());
        }
    }
}
