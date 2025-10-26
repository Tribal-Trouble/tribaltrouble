package com.oddlabs.tt.gui;

import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.form.MessageForm;
import com.oddlabs.tt.guievent.RowListener;
import com.oddlabs.tt.player.campaign.CampaignState;
import com.oddlabs.tt.util.Utils;
import com.oddlabs.util.DeterministicSerializer;
import com.oddlabs.util.DeterministicSerializerLoopbackInterface;
import org.jspecify.annotations.NonNull;

import java.io.FileNotFoundException;
import java.io.InvalidClassException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;

public final class LoadCampaignBox extends GUIObject implements DeterministicSerializerLoopbackInterface<CampaignState[]> {
	public final static Path SAVEGAMES_FILE_NAME = Paths.get("savegames");

	private final static int WIDTH_NAME = 210;
	private final static int WIDTH_RACE = 70;
	private final static int WIDTH_DIFFICULTY = 130;
	private final static int WIDTH_DATE = 170;

	private final @NonNull MultiColumnComboBox list_box;
	private final GUIRoot gui_root;
	private final ResourceBundle bundle = ResourceBundle.getBundle(LoadCampaignBox.class.getName());

	public LoadCampaignBox(GUIRoot gui_root, RowListener listener) {
		this.gui_root = gui_root;
		ColumnInfo[] infos = new ColumnInfo[]{
			new ColumnInfo(Utils.getBundleString(bundle, "name"), WIDTH_NAME),
			new ColumnInfo(Utils.getBundleString(bundle, "race"), WIDTH_RACE),
			new ColumnInfo(Utils.getBundleString(bundle, "difficulty"), WIDTH_DIFFICULTY),
			new ColumnInfo(Utils.getBundleString(bundle, "date"), WIDTH_DATE)};
		list_box = new MultiColumnComboBox(gui_root, infos, 262);
		list_box.addRowListener(listener);
		addChild(list_box);
		setCanFocus(true);
		setDim(list_box.getWidth(), list_box.getHeight());

		refresh();
	}

	public static <T> void saveSavegames(CampaignState[] states, DeterministicSerializerLoopbackInterface<T> callback) {
		DeterministicSerializer.save(LocalEventQueue.getQueue().getDeterministic(), states, getSaveSavegamesFile(), callback);
	}

	private static @NonNull Path getSaveSavegamesFile() {
		return LocalInput.getGameDir().resolve(SAVEGAMES_FILE_NAME);
	}

	public static <T> void loadSavegames(DeterministicSerializerLoopbackInterface<T>  callback) {
		DeterministicSerializer.load(LocalEventQueue.getQueue().getDeterministic(), getLoadSavegamesFile(), callback);
	}

	private static @NonNull Path getLoadSavegamesFile() {
		Path file = getSaveSavegamesFile();
		if (!Files.isReadable(file))
			return Utils.getInstallDir().resolve(SAVEGAMES_FILE_NAME);
		else
			return file;
	}

    @Override
	public void setFocus() {
		list_box.setFocus();
	}

    @Override
	protected void renderGeometry() {
	}

	public Object getSelected() {
		return list_box.getSelected();
	}

	public void refresh() {
		list_box.clear();
		LoadCampaignBox.loadSavegames(this);
	}

	private void fillSlots(CampaignState @NonNull [] campaign_states) {
            for (CampaignState campaign_state : campaign_states) {
                String race;
                switch (campaign_state.getRace()) {
                    case CampaignState.RACE_VIKINGS:
                        race = Utils.getBundleString(bundle, "vikings");
                        break;
                    case CampaignState.RACE_NATIVES:
                        race = Utils.getBundleString(bundle, "natives");
                        break;
                    default:
                        throw new RuntimeException();
                }
                String difficulty;
                switch (campaign_state.getDifficulty()) {
                    case CampaignState.DIFFICULTY_EASY:
                        difficulty = Utils.getBundleString(bundle, "easy");
                        break;
                    case CampaignState.DIFFICULTY_NORMAL:
                        difficulty = Utils.getBundleString(bundle, "normal");
                        break;
                    case CampaignState.DIFFICULTY_HARD:
                        difficulty = Utils.getBundleString(bundle, "hard");
                        break;
                    default:
                        throw new RuntimeException();
                }
                Row row = new Row(new GUIObject[]{new Label(campaign_state.getName(), Skin.getSkin().getMultiColumnComboBoxData().getFont(), WIDTH_NAME), new Label(race, Skin.getSkin().getMultiColumnComboBoxData().getFont(), WIDTH_RACE), new Label(difficulty, Skin.getSkin().getMultiColumnComboBoxData().getFont(), WIDTH_DIFFICULTY), new DateLabel(campaign_state.getDate(), Skin.getSkin().getMultiColumnComboBoxData().getFont(), WIDTH_DATE)}, campaign_state);
                list_box.addRow(row);
            }
	}

        @Override
	public void loadSucceeded(CampaignState @NonNull [] campaign_states) {
		fillSlots(campaign_states);
	}

        @Override
	public void saveSucceeded() {
	}

        @Override
	public void failed(Throwable e) {
		if (e instanceof FileNotFoundException) {
		} else if (e instanceof InvalidClassException) {
			String invalid_message = Utils.getBundleString(bundle, "invalid_message", SAVEGAMES_FILE_NAME);
			gui_root.addModalForm(new MessageForm(invalid_message));
		} else {
			String failed_message = Utils.getBundleString(bundle, "failed_message", SAVEGAMES_FILE_NAME, e.getMessage());
			gui_root.addModalForm(new MessageForm(failed_message));
		}
	}
}
