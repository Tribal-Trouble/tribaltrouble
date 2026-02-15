package com.oddlabs.tt.gui;

import com.codedisaster.steamworks.SteamAPI;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.form.MessageForm;
import com.oddlabs.tt.guievent.RowListener;
import com.oddlabs.tt.player.campaign.CampaignState;
import com.oddlabs.tt.steam.SteamManager;
import com.oddlabs.tt.util.Utils;
import com.oddlabs.util.DeterministicSerializer;
import com.oddlabs.util.DeterministicSerializerLoopbackInterface;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InvalidClassException;
import java.util.ResourceBundle;

public final strictfp class LoadCampaignBox extends GUIObject
        implements DeterministicSerializerLoopbackInterface {
    public static final String SAVEGAMES_FILE_NAME = "savegames";

    private static final int WIDTH_NAME = 210;
    private static final int WIDTH_RACE = 70;
    private static final int WIDTH_DIFFICULTY = 130;
    private static final int WIDTH_DATE = 170;

    private final MultiColumnComboBox list_box;
    private final GUIRoot gui_root;
    private final ResourceBundle bundle = ResourceBundle.getBundle(LoadCampaignBox.class.getName());

    public LoadCampaignBox(GUIRoot gui_root, RowListener listener) {
        this.gui_root = gui_root;
        ColumnInfo[] infos =
                new ColumnInfo[] {
                    new ColumnInfo(Utils.getBundleString(bundle, "name"), WIDTH_NAME),
                    new ColumnInfo(Utils.getBundleString(bundle, "race"), WIDTH_RACE),
                    new ColumnInfo(Utils.getBundleString(bundle, "difficulty"), WIDTH_DIFFICULTY),
                    new ColumnInfo(Utils.getBundleString(bundle, "date"), WIDTH_DATE)
                };
        list_box = new MultiColumnComboBox(gui_root, infos, 262);
        list_box.addRowListener(listener);
        addChild(list_box);
        setCanFocus(true);
        setDim(list_box.getWidth(), list_box.getHeight());

        refresh();
    }

    public static final void saveSavegames(
            CampaignState[] states, DeterministicSerializerLoopbackInterface callback) {
        DeterministicSerializer.save(
                LocalEventQueue.getQueue().getDeterministic(),
                states,
                getSaveSavegamesFile(),
                callback);
    }

    private static File getSaveSavegamesFile() {
        return new File(LocalInput.getGameDir(), getSavegamesFileName());
    }

    public static final void loadSavegames(DeterministicSerializerLoopbackInterface callback) {
        DeterministicSerializer.load(
                LocalEventQueue.getQueue().getDeterministic(), getLoadSavegamesFile(), callback);
    }

    private static File getLoadSavegamesFile() {
        File file = getSaveSavegamesFile();
        if (!file.canRead()) return new File(LocalInput.getGameDir(), getSavegamesFileName());
        else return file;
    }

    public final void setFocus() {
        list_box.setFocus();
    }

    protected final void renderGeometry() {}

    public final Object getSelected() {
        return list_box.getSelected();
    }

    public final void refresh() {
        list_box.clear();
        LoadCampaignBox.loadSavegames(this);
    }

    private final void fillSlots(CampaignState[] campaign_states) {
        for (int i = 0; i < campaign_states.length; i++) {
            String race;
            switch (campaign_states[i].getRace()) {
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
            switch (campaign_states[i].getDifficulty()) {
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
            Row row =
                    new Row(
                            new GUIObject[] {
                                new Label(
                                        campaign_states[i].getName(),
                                        Skin.getSkin().getMultiColumnComboBoxData().getFont(),
                                        WIDTH_NAME),
                                new Label(
                                        race,
                                        Skin.getSkin().getMultiColumnComboBoxData().getFont(),
                                        WIDTH_RACE),
                                new Label(
                                        difficulty,
                                        Skin.getSkin().getMultiColumnComboBoxData().getFont(),
                                        WIDTH_DIFFICULTY),
                                new DateLabel(
                                        campaign_states[i].getDate(),
                                        Skin.getSkin().getMultiColumnComboBoxData().getFont(),
                                        WIDTH_DATE)
                            },
                            campaign_states[i]);
            list_box.addRow(row);
        }
    }

    public final void loadSucceeded(Object object) {
        CampaignState[] campaign_states = (CampaignState[]) object;
        fillSlots(campaign_states);
    }

    public final void saveSucceeded() {}

    public final void failed(Exception e) {
        if (e instanceof FileNotFoundException) {
        } else if (e instanceof InvalidClassException) {
            String invalid_message =
                    Utils.getBundleString(
                            bundle, "invalid_message", new Object[] {getSavegamesFileName()});
            gui_root.addModalForm(new MessageForm(invalid_message));
        } else {
            String failed_message =
                    Utils.getBundleString(
                            bundle,
                            "failed_message",
                            new Object[] {getSavegamesFileName(), e.getMessage()});
            gui_root.addModalForm(new MessageForm(failed_message));
        }
    }

    public static final String getSavegamesFileName() {
        if (SteamAPI.isSteamRunning()) {
            long account_id = SteamManager.getInstance().getAccountID();
            return account_id + "." + SAVEGAMES_FILE_NAME;
        }
        return SAVEGAMES_FILE_NAME;
    }
}
