package com.oddlabs.tt.gui;

import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.form.MessageForm;
import com.oddlabs.tt.guievent.RowListener;
import com.oddlabs.tt.player.campaign.CampaignState;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.util.Utils;
import com.oddlabs.util.DeterministicSerializer;
import com.oddlabs.util.DeterministicSerializerLoopbackInterface;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.InvalidClassException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class LoadCampaignBox extends GUIObject implements DeterministicSerializerLoopbackInterface<CampaignState[]> {
    private static final Logger logger = Logger.getLogger(LoadCampaignBox.class.getSimpleName());
	public static final Path SAVEGAMES_FILE_NAME = Path.of("savegames");

	private static final int WIDTH_NAME = 210;
	private static final int WIDTH_RACE = 70;
	private static final int WIDTH_DIFFICULTY = 130;
	private static final int WIDTH_DATE = 170;

	private final @NonNull MultiColumnComboBox<CampaignState> list_box;
	private final @NonNull GUIRoot gui_root;
	private static final  ResourceBundle bundle = ResourceBundle.getBundle(LoadCampaignBox.class.getName());

	private @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull ... args) {
		return Utils.getBundleString(bundle, key, args);
	}

	public LoadCampaignBox(@NonNull GUIRoot gui_root, @NonNull RowListener<CampaignState> listener) {
		this.gui_root = gui_root;
		ColumnInfo[] infos = {
			new ColumnInfo(i18n("name"), WIDTH_NAME),
			new ColumnInfo(i18n("race"), WIDTH_RACE),
			new ColumnInfo(i18n("difficulty"), WIDTH_DIFFICULTY),
			new ColumnInfo(i18n("date"), WIDTH_DATE)};
		list_box = new MultiColumnComboBox<>(gui_root, infos, 262);
		list_box.addRowListener(listener);
		addChild(list_box);
		setCanFocus(true);
		setDim(list_box.getWidth(), list_box.getHeight());

		refresh();
	}

	public static <T> void saveSavegames(@NonNull CampaignState @NonNull [] states, @NonNull DeterministicSerializerLoopbackInterface<T> callback) {
		DeterministicSerializer.save(LocalEventQueue.getQueue().getDeterministic(), states, getSaveSavegamesFile(), callback);
	}

	private static @NonNull Path getSaveSavegamesFile() {
		return Renderer.getLocalInput().getGameDir().resolve(SAVEGAMES_FILE_NAME);
	}

	public static <T> void loadSavegames(@NonNull DeterministicSerializerLoopbackInterface<T>  callback) {
		DeterministicSerializer.load(LocalEventQueue.getQueue().getDeterministic(), getLoadSavegamesFile(), callback);
	}

	private static @NonNull Path getLoadSavegamesFile() {
		Path file = getSaveSavegamesFile();
        return !Files.isReadable(file) ? Utils.getInstallDir().resolve(SAVEGAMES_FILE_NAME) : file;
	}

	@Override
	public void setFocus(@NonNull FocusDirection direction) {
		list_box.setFocus(direction);
	}

	public @Nullable CampaignState getSelected() {
		return list_box.getSelected();
	}

	public void refresh() {
		list_box.clear();
		LoadCampaignBox.loadSavegames(this);
	}

	private void fillSlots(@NonNull CampaignState @NonNull [] campaign_states) {
		Box box = Skin.getSkin().getMultiColumnComboBoxData().box();
        for (CampaignState campaign_state : campaign_states) {
            String race = switch (campaign_state.getRace()) {
                case CampaignState.RACE_VIKINGS -> i18n("vikings");
                case CampaignState.RACE_NATIVES -> i18n("natives");
                default -> throw new IllegalArgumentException("invalid race");
            };
            String difficulty = switch (campaign_state.getDifficulty()) {
                case CampaignState.DIFFICULTY_EASY -> i18n("easy");
                case CampaignState.DIFFICULTY_NORMAL -> i18n("normal");
                case CampaignState.DIFFICULTY_HARD -> i18n("hard");
                default -> throw new IllegalArgumentException("invalid difficulty");
            };
            Row<CampaignState,Label> row = new Row<>(
                    new Label[]{
                            new Label(campaign_state.getName(), Skin.getSkin().getMultiColumnComboBoxData().font(), WIDTH_NAME - box.getLeftOffset() - 1),
                            new Label(race, Skin.getSkin().getMultiColumnComboBoxData().font(), WIDTH_RACE),
                            new Label(difficulty, Skin.getSkin().getMultiColumnComboBoxData().font(), WIDTH_DIFFICULTY),
                            new DateLabel(campaign_state.getDate(), Skin.getSkin().getMultiColumnComboBoxData().font(), WIDTH_DATE - box.getRightOffset() + 1)
                    }, campaign_state);
            list_box.addRow(row);
        }
	}

	@Override
	public void loadSucceeded(CampaignState @NonNull [] campaign_states) {
		fillSlots(campaign_states);
		if (list_box.getSize() > 0) {
			list_box.selectFirst();
		}
	}

	@Override
	public void saveSucceeded() {
	}

	@Override
	public void failed(@NonNull Throwable e) {
        logger.log(Level.SEVERE, "Failed to load savegames", e);
		if (e instanceof FileNotFoundException || e instanceof NoSuchFileException) {
		} else if (e instanceof InvalidClassException) {
			String invalid_message = i18n("invalid_message", SAVEGAMES_FILE_NAME);
			gui_root.addModalForm(new MessageForm(invalid_message));
		} else {
			String failed_message = i18n("failed_message", SAVEGAMES_FILE_NAME, e.getMessage());
			gui_root.addModalForm(new MessageForm(failed_message));
		}
	}
}
