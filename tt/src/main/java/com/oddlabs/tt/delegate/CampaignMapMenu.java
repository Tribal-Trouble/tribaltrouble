package com.oddlabs.tt.delegate;


import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.camera.Camera;
import com.oddlabs.tt.form.QuestionForm;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.KeyboardEvent;
import com.oddlabs.tt.gui.MenuButton;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;

final class CampaignMapMenu extends Menu {

    CampaignMapMenu(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root, @NonNull Camera camera) {
        super(network, gui_root, camera);
        reload();
    }

    private void addAbortButton() {
        String abort_text = Utils.getBundleString(bundle, "end_campaign");
        MenuButton abort = new MenuButton(abort_text, COLOR_NORMAL, COLOR_ACTIVE);
        addChild(abort);
        abort.addMouseClickListener((_, _, _, _) ->
                setMenuCentered(new QuestionForm(Utils.getBundleString(bundle, "end_game_confirm"),
                        (_, _, _, _) -> CampaignMapForm.closeCampaign(getNetwork(), getGUIRoot().getGUI()))));
    }

    @Override
    protected void addButtons() {
        addResumeButton();

        addDefaultOptionsButton();

        addAbortButton();

        addExitButton();
    }

    @Override
    protected boolean keyPressed(@NonNull KeyboardEvent event) {
        switch (event.keyCode()) {
            case ESCAPE -> {
                pop();
                return true;
            }
            default -> {
                return super.keyPressed(event);
            }
        }
    }

    @Override
    protected void renderGeometry(@NonNull GUIRenderer renderer) {
        super.renderGeometry(renderer);
        renderBackgroundAlpha(renderer);
    }
}
