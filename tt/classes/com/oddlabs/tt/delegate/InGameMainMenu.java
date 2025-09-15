package com.oddlabs.tt.delegate;

import com.oddlabs.tt.camera.Camera;
import com.oddlabs.tt.form.InGameOptionsMenu;
import com.oddlabs.tt.form.QuestionForm;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.KeyboardEvent;
import com.oddlabs.tt.gui.MenuButton;
import com.oddlabs.tt.guievent.MouseClickListener;
import com.oddlabs.tt.input.Keyboard;
import com.oddlabs.tt.landscape.WorldParameters;
import com.oddlabs.tt.util.Utils;
import com.oddlabs.tt.viewer.WorldViewer;

public final strictfp class InGameMainMenu extends Menu {
    private final WorldParameters world_params;

    private final WorldViewer viewer;

    private Group game_infos;

    public InGameMainMenu(WorldViewer viewer, Camera camera, WorldParameters world_params) {
        super(viewer.getNetwork(), viewer.getGUIRoot(), camera);
        this.viewer = viewer;
        this.world_params = world_params;
        reload();
    }

    protected final void doAdd() {
        super.doAdd();
        viewer.setPaused(true);
    }

    protected final void doRemove() {
        super.doRemove();
        viewer.setPaused(false);
    }

    public final void addAbortButton(String abort_text) {
        MenuButton abort = new MenuButton(abort_text, COLOR_NORMAL, COLOR_ACTIVE);
        addChild(abort);
        abort.addMouseClickListener(new AbortListener());
    }

    protected final void addButtons() {
        addResumeButton();

        addOptionsButton(
                new FormFactory() {
                    public final Form create() {
                        return new InGameOptionsMenu(getGUIRoot(), viewer);
                    }
                });

        game_infos = new Group(true);
        viewer.addGUI(this, game_infos);
        addChild(game_infos);

        addExitButton();
    }

    public final void displayChangedNotify(int width, int height) {
        super.displayChangedNotify(width, height);
        if (game_infos != null)
            game_infos.setPos(
                    (width - game_infos.getWidth()) / 2, (height - game_infos.getHeight()) / 2);
    }

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

    protected final void renderGeometry() {
        super.renderGeometry();
        renderBackgroundAlpha();
    }

    private final strictfp class AbortListener implements MouseClickListener {
        public final void mouseClicked(int button, int x, int y, int clicks) {
            setMenuCentered(
                    new QuestionForm(
                            Utils.getBundleString(bundle, "end_game_confirm"),
                            new ActionAbortListener()));
        }
    }

    private final strictfp class ActionAbortListener implements MouseClickListener {
        public final void mouseClicked(int button, int x, int y, int clicks) {
            viewer.abort();
        }
    }
}
