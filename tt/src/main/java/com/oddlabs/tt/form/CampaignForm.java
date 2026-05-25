package com.oddlabs.tt.form;

import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.delegate.Menu;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.gui.CancelButton;
import com.oddlabs.tt.gui.FocusDirection;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.LoadCampaignBox;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.guievent.RowListener;
import com.oddlabs.tt.player.campaign.Campaign;
import com.oddlabs.tt.player.campaign.CampaignState;
import com.oddlabs.tt.player.campaign.NativeCampaign;
import com.oddlabs.tt.player.campaign.VikingCampaign;
import com.oddlabs.tt.util.Utils;
import com.oddlabs.util.DeterministicSerializerLoopbackInterface;
import org.jspecify.annotations.NonNull;

import java.io.FileNotFoundException;
import java.io.InvalidClassException;
import java.nio.file.NoSuchFileException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.LEFT_MID;

public final class CampaignForm extends Form implements DeterministicSerializerLoopbackInterface<@NonNull CampaignState[]> {
    private static final Logger logger = Logger.getLogger(CampaignForm.class.getSimpleName());

    private final @NonNull HorizButton button_vikings;
    private final @NonNull HorizButton button_load;
    private final @NonNull HorizButton button_delete;
    private final @NonNull LoadCampaignBox load_campaign_box;
    private static final ResourceBundle bundle = ResourceBundle.getBundle(CampaignForm.class.getName());

    private @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private final @NonNull GUIRoot gui_root;
    private final @NonNull NetworkSelector network;

    public CampaignForm(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root, @NonNull Menu main_menu) {
        this.gui_root = gui_root;
        this.network = network;
        Label headline = new Label(i18n("campaign"), Skin.getSkin().getHeadlineFont());
        addChild(headline);

        button_delete = new HorizButton(i18n("delete"), 120);
        button_delete.addMouseClickListener(this::mouseClickedDelete);
        button_delete.setDisabled(true);

        button_vikings = new HorizButton(i18n("new"), 120);
        button_vikings.addMouseClickListener((_, _, _, _) -> main_menu.setMenu(new NewCampaignForm(network, gui_root,
                main_menu, CampaignForm.this)));

        button_load = new HorizButton(i18n("load"), 120);
        button_load.setDisabled(true);

        HorizButton button_cancel = new CancelButton(120);
        button_cancel.addMouseClickListener((_, _, _, _) -> this.cancel());

        // Combo box
        RowListener<CampaignState> listListener = new RowListener<>() {
            @Override
            public void rowDoubleClicked(@NonNull CampaignState object) {
                load(object);
            }

            @Override
            public void rowChosen(@NonNull CampaignState object) {
                button_delete.setDisabled(false);
                button_load.setDisabled(false);
            }
        };
        load_campaign_box = new LoadCampaignBox(gui_root, listListener);

        // Add listener after load_campaign_box is initialized
        button_load.addMouseClickListener((_, _, _, _) -> {
            CampaignState selected = load_campaign_box.getSelected();
            if (selected != null)
                load(selected);
        });

        // Add objects
        addChild(button_delete);
        addChild(button_vikings);
        addChild(load_campaign_box);
        addChild(button_load);
        addChild(button_cancel);

        // Place objects
        headline.place();
        load_campaign_box.place(button_vikings, BOTTOM_LEFT);
        button_cancel.place(Origin.AT_END);
        button_delete.place(button_cancel, LEFT_MID);
        button_load.place(button_delete, LEFT_MID);
        button_vikings.place(button_load, LEFT_MID);

        // headline
        compileCanvas();
        centerPos();
    }

    @Override
    public void setFocus(@NonNull FocusDirection direction) {
        if (direction == FocusDirection.BACKWARD) {
            super.setFocus(direction);
        } else {
            button_vikings.setFocus(direction);
        }
    }

    public void load(@NonNull CampaignState campaign_state) {
        Campaign campaign = campaign_state.getRace() == CampaignState.RACE_VIKINGS ? new VikingCampaign(network,
                gui_root, campaign_state) : new NativeCampaign(network, gui_root, campaign_state);
        setDisabled(true);
        if (campaign_state.getIslandState(0) == CampaignState.ISLAND_COMPLETED) {
            campaign.pushDelegate(network, gui_root.getGUI());
        } else
            campaign.startIsland(network, gui_root, 0);
    }

    @Override
    public void saveSucceeded() {
        load_campaign_box.refresh();
    }

    @Override
    public void loadSucceeded(CampaignState @NonNull [] campaign_states) {
        CampaignState selected = load_campaign_box.getSelected();
        if (selected != null) {
            CampaignState[] new_states = new CampaignState[campaign_states.length - 1];
            int offset = 0;
            for (int i = 0; i < campaign_states.length; i++) {
                if (campaign_states[i].getName().equals(selected.getName())) {
                    offset = 1;
                } else {
                    new_states[i - offset] = campaign_states[i];
                }
            }
            LoadCampaignBox.saveSavegames(new_states, this);
        }
    }

    @Override
    public void failed(@NonNull Throwable e) {
        if (e instanceof FileNotFoundException || e instanceof NoSuchFileException) {
        } else if (e instanceof InvalidClassException) {
        } else {
            logger.log(Level.SEVERE, "Load failed", e);
            String failed_message = i18n("failed_message", Globals.getSavegamesFileName(), e.getMessage());
            gui_root.addModalForm(new MessageForm(failed_message));
        }
    }

    private void mouseClickedDelete(@NonNull MouseButton button, int x, int y, int clicks) {
        CampaignState state = load_campaign_box.getSelected();
        if (state != null) {
            String confirm_str = i18n("confirm_delete", state.getName());
            gui_root.addModalForm(new QuestionForm(confirm_str, (_, _, _, _) -> LoadCampaignBox.loadSavegames(
                    CampaignForm.this)));
        }
    }
}
