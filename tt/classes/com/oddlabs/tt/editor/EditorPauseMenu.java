package com.oddlabs.tt.editor;

import com.oddlabs.tt.camera.Camera;
import com.oddlabs.tt.delegate.Menu;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.KeyboardEvent;
import com.oddlabs.tt.input.Keyboard;

/**
 * Simple pause menu for the map editor. Matches in-game pause UX:
 * - Pauses world animations while open
 * - ESC or Resume closes the menu
 * - Options opens the standard Options menu
 * - Exit returns to previous menu (pops editor)
 */
public final class EditorPauseMenu extends Menu {
    public EditorPauseMenu(GUIRoot gui_root, Camera camera) {
        super(null, gui_root, camera);
        reload();
    }

    @Override
    protected final void doAdd() {
        super.doAdd();
        MapEditorSession.setPaused(true);
    }

    @Override
    protected final void doRemove() {
        super.doRemove();
        MapEditorSession.setPaused(false);
    }

    @Override
    protected final void addButtons() {
        addResumeButton();

        // Options button (reuses global OptionsMenu)
        addDefaultOptionsButton();

    // End Game button (return to main menu)
    com.oddlabs.tt.gui.MenuButton endGame =
        new com.oddlabs.tt.gui.MenuButton(
            com.oddlabs.tt.util.Utils.getBundleString(
                com.oddlabs.tt.delegate.Menu.bundle, "end_game"),
            com.oddlabs.tt.delegate.Menu.COLOR_NORMAL,
            com.oddlabs.tt.delegate.Menu.COLOR_ACTIVE);
    addChild(endGame);
    endGame.addMouseClickListener(
        new com.oddlabs.tt.guievent.MouseClickListener() {
            public void mouseClicked(int button, int x, int y, int clicks) {
            // Confirm like other pause menus
            setMenuCentered(
                new com.oddlabs.tt.form.QuestionForm(
                    com.oddlabs.tt.util.Utils.getBundleString(
                        com.oddlabs.tt.delegate.Menu.bundle,
                        "end_game_confirm"),
                    new com.oddlabs.tt.guievent.MouseClickListener() {
                        public void mouseClicked(
                            int b, int px, int py, int c) {
                        pop();
                        com.oddlabs.tt.render.Renderer.startMenu(
                            MapEditorSession.getEditorNetwork(),
                            getGUIRoot().getGUI());
                        }
                    }));
            }
        });

        // Exit button (same behavior as other menus)
        addExitButton();
    }

    @Override
    protected final void keyPressed(KeyboardEvent event) {
        switch (event.getKeyCode()) {
            case Keyboard.KEY_ESCAPE:
                pop();
                break;
            default:
                super.keyPressed(event);
                break;
        }
    }
}
