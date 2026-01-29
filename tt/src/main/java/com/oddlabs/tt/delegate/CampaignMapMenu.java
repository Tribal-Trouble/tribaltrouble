package com.oddlabs.tt.delegate;

import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.form.OptionsMenu;
import com.oddlabs.tt.form.QuestionForm;
import com.oddlabs.tt.form.QuitForm;
import com.oddlabs.tt.gui.FocusDirection;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUIImage;
import com.oddlabs.tt.gui.GUIObject;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.MenuButton;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import com.oddlabs.tt.render.GUIRenderer;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;

final class CampaignMapMenu extends Form {
    private static final Vector4fc DARK_GLASS = new Vector4f(0f, 0f, 0f, 0.7f);
    private final @NonNull GUIRoot gui_root;
    private final @NonNull NetworkSelector network;
    private GUIImage overlay;
    private GUIImage logo;
    private MenuButton resumeButton;

    CampaignMapMenu(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root) {
        this.network = network;
        this.gui_root = gui_root;
        
        initBackground();
        
        addResumeButton();
        addOptionsButton();
        addAbortButton();
        addExitButton();
        
        layoutButtons();
        setFocus();
    }

    private void initBackground() {
        int width = gui_root.getWidth();
        int height = gui_root.getHeight();
        
        overlay = new GUIImage(width, height, 0f, 0f, 800f/1024f, 600f/1024f, "/textures/gui/mainmenu");
        overlay.setPos(0, 0);
        addChild(overlay);

        String logo_file = Menu.i18n("logo_file");
        float heightScale = height / 600f;
        int logoHeight = (int) (206f * heightScale);
        int logoWidth = (int) (347f * heightScale);
        
        logo = new GUIImage(logoWidth, logoHeight, 0f, 0f, 347f / 512f, 206f / 256f, logo_file);
        logo.setPos(0, height - logoHeight);
        addChild(logo);
    }

    private void addResumeButton() {
        resumeButton = new MenuButton(Menu.i18n("resume"), Menu.COLOR_NORMAL, Menu.COLOR_ACTIVE);
        addChild(resumeButton);
        resumeButton.addMouseClickListener((_, _, _, _) -> cancel());
    }

    @Override
    public void setFocus(@NonNull FocusDirection direction) {
        if (direction == FocusDirection.BACKWARD) {
            super.setFocus(direction);
        } else {
            setFocus();
        }
    }

    @Override
    public void setFocus() {
        if (resumeButton != null) {
            resumeButton.setFocus();
        } else {
            super.setFocus();
        }
    }

    private void addOptionsButton() {
        MenuButton options = new MenuButton(Menu.i18n("options"), Menu.COLOR_NORMAL, Menu.COLOR_ACTIVE);
        addChild(options);
        options.addMouseClickListener((_, _, _, _) -> gui_root.addModalForm(new OptionsMenu(gui_root)));
    }

    private void addAbortButton() {
        String abort_text = Menu.i18n("end_campaign");
        MenuButton abort = new MenuButton(abort_text, Menu.COLOR_NORMAL, Menu.COLOR_ACTIVE);
        addChild(abort);
        abort.addMouseClickListener((_, _, _, _) ->
                gui_root.addModalForm(new QuestionForm(Menu.i18n("end_game_confirm"),
                        (_, _, _, _) -> CampaignMapForm.closeCampaign(network, gui_root.getGUI()))));
    }

    private void addExitButton() {
        MenuButton exit = new MenuButton(Menu.i18n("quit"), Menu.COLOR_NORMAL, Menu.COLOR_ACTIVE);
        addChild(exit);
        exit.addMouseClickListener((_, _, _, _) -> gui_root.addModalForm(new QuitForm(gui_root)));
    }

    private void layoutButtons() {
        int width = gui_root.getWidth();
        int height = gui_root.getHeight();
        setDim(width, height);
        
        if (overlay != null) {
            overlay.setDim(width, height);
        }
        
        if (logo != null) {
            float heightScale = height / 600f;
            int logoHeight = (int) (206f * heightScale);
            int logoWidth = (int) (347f * heightScale);
            logo.setDim(logoWidth, logoHeight);
            logo.setPos(0, height - logoHeight);
        }
        
        int x = 15;
        int y = getHeight() - (int) (190f * getHeight() / 600f);

        GUIObject child = getLastChild();
        while (child != null) {
            if (child instanceof MenuButton) {
                child.setPos(x, y - child.getHeight());
                y -= (int) (child.getHeight() * .875);
            }
            child = child.getPrior();
        }
    }

    @Override
    public void displayChangedNotify(int width, int height) {
        setDim(width, height);
        layoutButtons();
    }

    @Override
    public void handleInput(@NonNull InputEvent event) {
        if (event.getPhase() == InputPhase.PRESSED) {
            if (event.consumeAction(GameAction.UI_CANCEL)) {
                cancel();
                event.consume();
                return;
            }
        }
        super.handleInput(event);
    }

    @Override
    protected void renderGeometry(@NonNull GUIRenderer renderer) {
        // Draw dark background
        renderer.drawColoredQuad(0, 0, getWidth(), getHeight(), DARK_GLASS);
    }
}
