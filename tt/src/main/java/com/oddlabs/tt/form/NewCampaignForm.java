package com.oddlabs.tt.form;

import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.delegate.Menu;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.gui.ButtonObject;
import com.oddlabs.tt.gui.CancelButton;
import com.oddlabs.tt.gui.EditLine;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.LoadCampaignBox;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.gui.OKButton;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.gui.PulldownButton;
import com.oddlabs.tt.gui.PulldownItem;
import com.oddlabs.tt.gui.PulldownMenu;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.guievent.EnterListener;
import com.oddlabs.tt.guievent.ItemChosenListener;
import com.oddlabs.tt.guievent.MouseClickListener;
import com.oddlabs.tt.player.campaign.Campaign;
import com.oddlabs.tt.player.campaign.CampaignState;
import com.oddlabs.tt.player.campaign.NativeCampaign;
import com.oddlabs.tt.player.campaign.VikingCampaign;
import com.oddlabs.tt.util.Utils;
import com.oddlabs.util.DeterministicSerializerLoopbackInterface;
import org.jspecify.annotations.NonNull;

import java.io.FileNotFoundException;
import java.io.InvalidClassException;
import java.util.ResourceBundle;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.LEFT_MID;

public final class NewCampaignForm extends Form implements DeterministicSerializerLoopbackInterface<CampaignState[]> {
	private static final int BUTTON_WIDTH = 100;
	private static final int EDITLINE_WIDTH = 240;

	private static final int INDEX_VIKINGS = 0;
	private static final int INDEX_NATIVES = 1;

	private final Menu main_menu;
	private final CampaignForm campaign_form;
	private final ResourceBundle bundle = ResourceBundle.getBundle(NewCampaignForm.class.getName());
	private final @NonNull EditLine editline_name;
	private final @NonNull PulldownMenu race_pulldown;
	private final @NonNull PulldownMenu difficulty_pulldown;
	private final GUIRoot gui_root;
	private final NetworkSelector network;
	private CampaignState[] campaign_states;

	public NewCampaignForm(NetworkSelector network, GUIRoot gui_root, Menu main_menu, CampaignForm campaign_form) {
		this.network = network;
		this.gui_root = gui_root;
		this.main_menu = main_menu;
		this.campaign_form = campaign_form;
		// headline
		Label label_headline = new Label(Utils.getBundleString(bundle, "caption"), Skin.getSkin().getHeadlineFont());
		addChild(label_headline);

		// name
		Group group = new Group();
		Label name_label = new Label(Utils.getBundleString(bundle, "name"), Skin.getSkin().getEditFont());
		editline_name = new EditLine(EDITLINE_WIDTH, 200);
		editline_name.addEnterListener(new NameListener());
		group.addChild(name_label);
		group.addChild(editline_name);

		// race
		Label race_label = new Label(Utils.getBundleString(bundle, "race"), Skin.getSkin().getEditFont());
		race_pulldown = new PulldownMenu();
		race_pulldown.addItem(new PulldownItem(Utils.getBundleString(bundle, "vikings")));
		race_pulldown.addItem(new PulldownItem(Utils.getBundleString(bundle, "natives")));
		race_pulldown.addItemChosenListener(new RaceListener());
		PulldownButton race_pb = new PulldownButton(gui_root, race_pulldown, INDEX_VIKINGS, 100);
		group.addChild(race_label);
		group.addChild(race_pb);

		// difficulty
		Label difficulty_label = new Label(Utils.getBundleString(bundle, "difficulty"), Skin.getSkin().getEditFont());
		difficulty_pulldown = new PulldownMenu();
		difficulty_pulldown.addItem(new PulldownItem(Utils.getBundleString(bundle, "easy")));
		difficulty_pulldown.addItem(new PulldownItem(Utils.getBundleString(bundle, "normal")));
		difficulty_pulldown.addItem(new PulldownItem(Utils.getBundleString(bundle, "hard")));
		PulldownButton difficulty_pb = new PulldownButton(gui_root, difficulty_pulldown, 1, 100);
		group.addChild(difficulty_label);
		group.addChild(difficulty_pb);

		// place in group
		editline_name.place();
		name_label.place(editline_name, LEFT_MID);
		race_pb.place(editline_name, BOTTOM_LEFT);
		race_label.place(race_pb, LEFT_MID);
		difficulty_pb.place(race_pb, BOTTOM_LEFT);
		difficulty_label.place(difficulty_pb, LEFT_MID);
		group.compileCanvas();
		addChild(group);

		// buttons
		ButtonObject button_ok = new OKButton(BUTTON_WIDTH);
		button_ok.addMouseClickListener(new NameListener());
		addChild(button_ok);
		ButtonObject button_cancel = new CancelButton(BUTTON_WIDTH);
		button_cancel.addMouseClickListener( (_, _, _, _) -> this.cancel());
		addChild(button_cancel);

		// place
		label_headline.place();
		group.place(label_headline, BOTTOM_LEFT);

		button_cancel.place(Origin.AT_END);
		button_ok.place(button_cancel, LEFT_MID);

		compileCanvas();
		centerPos();
		LoadCampaignBox.loadSavegames(this);
	}

	@Override
	protected void doCancel() {
		main_menu.setMenu(campaign_form);
	}

	@Override
	public void setFocus() {
		editline_name.setFocus();
	}

	private boolean nameIsUnique(String name) {
		if (campaign_states != null) {
                    for (CampaignState campaign_state : campaign_states) {
                        if (campaign_state.getName().equals(name)) {
                            return false;
                        }
                    }
		}
		return true;
	}

	private void save() {
		String name = editline_name.getContents().trim();
		if (name.isEmpty()) {
			gui_root.addModalForm(new MessageForm(Utils.getBundleString(bundle, "invalid")));
			return;
		}
		if (!nameIsUnique(name)) {
			gui_root.addModalForm(new MessageForm(Utils.getBundleString(bundle, "exists")));
			return;
		}

		CampaignState[] new_states;
		if (campaign_states != null) {
			new_states = new CampaignState[campaign_states.length + 1];
                    System.arraycopy(campaign_states, 0, new_states, 0, campaign_states.length);
		} else {
			new_states = new CampaignState[1];
		}
		Campaign campaign;
            switch (race_pulldown.getChosenItemIndex()) {
                case 0:
                    campaign = new VikingCampaign(network, gui_root);
                    campaign.getState().setRace(CampaignState.RACE_VIKINGS);
                    break;
                case 1:
                    campaign = new NativeCampaign(network, gui_root);
                    campaign.getState().setRace(CampaignState.RACE_NATIVES);
                    break;
                default:
                    throw new RuntimeException();
            }
		campaign.getState().setName(name);
		campaign.getState().setDate(System.currentTimeMillis());

		int difficulty;
            switch (difficulty_pulldown.getChosenItemIndex()) {
                case 0:
                    difficulty = CampaignState.DIFFICULTY_EASY;
                    break;
                case 1:
                    difficulty = CampaignState.DIFFICULTY_NORMAL;
                    break;
                case 2:
                    difficulty = CampaignState.DIFFICULTY_HARD;
                    break;
                default:
                    throw new RuntimeException();
            }
		campaign.getState().setDifficulty(difficulty);
		new_states[new_states.length - 1] = campaign.getState();
		LoadCampaignBox.saveSavegames(new_states, this);
		remove();
	}

	@Override
	public void saveSucceeded() {
	}

	@Override
	public void loadSucceeded(CampaignState[] campaign_states) {
		this.campaign_states = campaign_states;
	}

	@Override
	public void failed(Throwable e) {
		if (e instanceof FileNotFoundException) {
		} else if (e instanceof InvalidClassException) {
		} else {
			String failed_message = Utils.getBundleString(bundle, "failed_message", LoadCampaignBox.SAVEGAMES_FILE_NAME, e.getMessage());
			gui_root.addModalForm(new MessageForm(failed_message));
		}
	}

	private final class NameListener implements MouseClickListener, EnterListener {
		@Override
		public void mouseClicked(@NonNull MouseButton button, int x, int y, int clicks) {
			save();
		}

		@Override
		public void enterPressed(CharSequence text) {
			save();
		}
	}

	private final class RaceListener implements ItemChosenListener {
		@Override
		public void itemChosen(@NonNull PulldownMenu menu, int item_index) {
			if (item_index == INDEX_NATIVES && (!Settings.getSettings().has_native_campaign)) {
				menu.chooseItem(INDEX_VIKINGS);
				gui_root.addModalForm(new MessageForm(Utils.getBundleString(bundle, "native_unavailable")));
			}
		}
	}
}
