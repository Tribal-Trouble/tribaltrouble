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
