package com.oddlabs.tt.delegate;

import com.oddlabs.tt.animation.TimerAnimation;
import com.oddlabs.tt.animation.Updatable;
import com.oddlabs.tt.camera.Camera;
import com.oddlabs.tt.camera.StaticCamera;
import com.oddlabs.tt.font.Font;
import com.oddlabs.tt.gui.ColumnInfo;
import com.oddlabs.tt.gui.FocusDirection;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.IntegerLabel;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.MultiColumnComboBox;
import com.oddlabs.tt.gui.Row;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.gui.SortedLabel;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.tt.util.Utils;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public final class GameStatsDelegate extends CameraDelegate<StaticCamera> implements Updatable<TimerAnimation> {
    private static final int PLAYER_COLUMN_WIDTH = 100;
    private static final int TEXT_OFFSET = -4;
    private static final ResourceBundle bundle = ResourceBundle.getBundle(GameStatsDelegate.class.getName());

    /**
     * Threshold above which the paginated transposed layout is used
     */
    private static final int PAGINATED_THRESHOLD = 6;
    private static final int PAGINATED_STAT_COLUMN_WIDTH = 130;
    private static final int PAGINATED_PLAYER_NAME_WIDTH = 120;
    private static final int[][] STAT_PAGES = {{0, 1, 2, 3, 9, 10},  // Combat: units lost, killed, buildings lost, wrecked, weapons, magics
            {4, 5, 6, 7, 8, 11},  // Economy: wood, rock, iron, chicken, meters walked, total
    };

    public static @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private final TimerAnimation delay_timer = new TimerAnimation(this, .6f);
    private final @NonNull Group group_buttons;
    private final @NonNull WorldViewer viewer;

    // Paginated layout state (only used when players > PAGINATED_THRESHOLD)
    private String[] paginatedStatNames;
    private int[][] paginatedPlayerStats;
    private Player[] paginatedPlayers;
    private Font paginatedFont;
    private int currentPage;
    private MultiColumnComboBox<Void> paginatedScoreBox;

    public GameStatsDelegate(@NonNull WorldViewer viewer, @NonNull Camera old_camera, @NonNull String label_str) {
        super(viewer.getGUIRoot(), new StaticCamera(old_camera.getState()));
        this.viewer = viewer;
        setDim(getGUIRoot().getWidth(), getGUIRoot().getHeight());
        Label label = new Label(label_str, Skin.getSkin().getHeadlineFont());
        addChild(label);
        label.setPos((getWidth() - label.getWidth()) / 2, (getHeight() - label.getHeight()) * 4 / 5);

        Player[] players = viewer.getWorld().getPlayers();

        MultiColumnComboBox<Void> score_box;
        if (players.length > PAGINATED_THRESHOLD) {
            score_box = buildPaginatedLayout(players);
        } else {
            score_box = buildLegacyLayout(players);
        }

        addChild(score_box);
        score_box.setPos((getWidth() - score_box.getWidth()) / 2, (getHeight() - score_box.getHeight()) / 2);

        group_buttons = new Group();
        viewer.addGameOverGUI(this, score_box.getY(), group_buttons);

        if (players.length > PAGINATED_THRESHOLD) {
            int nav_y = score_box.getY() - Skin.getSkin().getEditFont().getHeight() - 4;
            HorizButton button_prev = new HorizButton(i18n("prev_page"), 100);
            button_prev.addMouseClickListener((_, _, _, _) -> switchPage(-1));
            addChild(button_prev);
            button_prev.setPos(score_box.getX(), nav_y);
            HorizButton button_next = new HorizButton(i18n("next_page"), 100);
            button_next.addMouseClickListener((_, _, _, _) -> switchPage(1));
            addChild(button_next);
            button_next.setPos(score_box.getX() + score_box.getWidth() - button_next.getWidth(), nav_y);
        }

        group_buttons.compileCanvas();
        addChild(group_buttons);
        group_buttons.setPos((getWidth() - group_buttons.getWidth()) / 2,
                (getHeight() - group_buttons.getHeight()) * 1 / 5);

        setFocusCycle(true);
        delay_timer.start();
    }

    /**
     * Original layout: rows=stats, columns=players. Used for ≤6 players.
     */
    private MultiColumnComboBox<Void> buildLegacyLayout(Player[] players) {
        ColumnInfo[] score_infos = new ColumnInfo[players.length + 1];
        score_infos[0] = new ColumnInfo(i18n("type"), 160);
        for (int i = 0; i < players.length; i++) {
            score_infos[i + 1] = new ColumnInfo(players[i].getPlayerInfo().getName(), PLAYER_COLUMN_WIDTH);
        }

        MultiColumnComboBox<Void> score_box = new MultiColumnComboBox<>(viewer.getGUIRoot(), score_infos, 200);

        Label[] units_lost_labels = new Label[players.length + 1];
        units_lost_labels[0] = new SortedLabel(i18n("units_lost"), 0,
                Skin.getSkin().getMultiColumnComboBoxData().font());
        for (int i = 0; i < players.length; i++) {
            units_lost_labels[i + 1] = new IntegerLabel(players[i].getUnitsLost(),
                    Skin.getSkin().getMultiColumnComboBoxData().font(), PLAYER_COLUMN_WIDTH + TEXT_OFFSET);
        }
        score_box.addRow(new Row<>(units_lost_labels, null));

        Label[] units_killed_labels = new Label[players.length + 1];
        units_killed_labels[0] = new SortedLabel(i18n("units_killed"), 1,
                Skin.getSkin().getMultiColumnComboBoxData().font());
        for (int i = 0; i < players.length; i++) {
            units_killed_labels[i + 1] = new IntegerLabel(players[i].getUnitsKilled(),
                    Skin.getSkin().getMultiColumnComboBoxData().font(), PLAYER_COLUMN_WIDTH + TEXT_OFFSET);
        }
        score_box.addRow(new Row<>(units_killed_labels, null));

        Label[] buildings_lost_labels = new Label[players.length + 1];
        buildings_lost_labels[0] = new SortedLabel(i18n("buildings_lost"), 2,
                Skin.getSkin().getMultiColumnComboBoxData().font());
        for (int i = 0; i < players.length; i++) {
            buildings_lost_labels[i + 1] = new IntegerLabel(players[i].getBuildingsLost(),
                    Skin.getSkin().getMultiColumnComboBoxData().font(), PLAYER_COLUMN_WIDTH + TEXT_OFFSET);
        }
        score_box.addRow(new Row<>(buildings_lost_labels, null));

        Label[] buildings_destroyed_labels = new Label[players.length + 1];
        buildings_destroyed_labels[0] = new SortedLabel(i18n("buildings_wrecked"), 3,
                Skin.getSkin().getMultiColumnComboBoxData().font());
        for (int i = 0; i < players.length; i++) {
            buildings_destroyed_labels[i + 1] = new IntegerLabel(players[i].getBuildingsDestroyed(),
                    Skin.getSkin().getMultiColumnComboBoxData().font(), PLAYER_COLUMN_WIDTH + TEXT_OFFSET);
        }
        score_box.addRow(new Row<>(buildings_destroyed_labels, null));

        Label[] tree_harvested_labels = new Label[players.length + 1];
        tree_harvested_labels[0] = new SortedLabel(i18n("tree_resources"), 3,
                Skin.getSkin().getMultiColumnComboBoxData().font());
        for (int i = 0; i < players.length; i++) {
            tree_harvested_labels[i + 1] = new IntegerLabel(players[i].getTreeHarvested(),
                    Skin.getSkin().getMultiColumnComboBoxData().font(), PLAYER_COLUMN_WIDTH + TEXT_OFFSET);
        }
        score_box.addRow(new Row<>(tree_harvested_labels, null));

        Label[] rock_harvested_labels = new Label[players.length + 1];
        rock_harvested_labels[0] = new SortedLabel(i18n("rock_resources"), 4,
                Skin.getSkin().getMultiColumnComboBoxData().font());
        for (int i = 0; i < players.length; i++) {
            rock_harvested_labels[i + 1] = new IntegerLabel(players[i].getRockHarvested(),
                    Skin.getSkin().getMultiColumnComboBoxData().font(), PLAYER_COLUMN_WIDTH + TEXT_OFFSET);
        }
        score_box.addRow(new Row<>(rock_harvested_labels, null));

        Label[] iron_harvested_labels = new Label[players.length + 1];
        iron_harvested_labels[0] = new SortedLabel(i18n("iron_resources"), 5,
                Skin.getSkin().getMultiColumnComboBoxData().font());
        for (int i = 0; i < players.length; i++) {
            iron_harvested_labels[i + 1] = new IntegerLabel(players[i].getIronHarvested(),
                    Skin.getSkin().getMultiColumnComboBoxData().font(), PLAYER_COLUMN_WIDTH + TEXT_OFFSET);
        }
        score_box.addRow(new Row<>(iron_harvested_labels, null));

        Label[] rubber_harvested_labels = new Label[players.length + 1];
        rubber_harvested_labels[0] = new SortedLabel(i18n("chicken_resources"), 6,
                Skin.getSkin().getMultiColumnComboBoxData().font());
        for (int i = 0; i < players.length; i++) {
            rubber_harvested_labels[i + 1] = new IntegerLabel(players[i].getRubberHarvested(),
                    Skin.getSkin().getMultiColumnComboBoxData().font(), PLAYER_COLUMN_WIDTH + TEXT_OFFSET);
        }
        score_box.addRow(new Row<>(rubber_harvested_labels, null));

        Label[] walked_labels = new Label[players.length + 1];
        walked_labels[0] = new SortedLabel(i18n("meters_walked"), 7,
                Skin.getSkin().getMultiColumnComboBoxData().font());
        for (int i = 0; i < players.length; i++) {
            walked_labels[i + 1] = new IntegerLabel(players[i].getUnitsMoved() * 2,
                    Skin.getSkin().getMultiColumnComboBoxData().font(), PLAYER_COLUMN_WIDTH + TEXT_OFFSET);
        }
        score_box.addRow(new Row<>(walked_labels, null));

        Label[] weapons_labels = new Label[players.length + 1];
        weapons_labels[0] = new SortedLabel(i18n("weapons_thrown"), 8,
                Skin.getSkin().getMultiColumnComboBoxData().font());
        for (int i = 0; i < players.length; i++) {
            weapons_labels[i + 1] = new IntegerLabel(players[i].getWeaponsThrown(),
                    Skin.getSkin().getMultiColumnComboBoxData().font(), PLAYER_COLUMN_WIDTH + TEXT_OFFSET);
        }
        score_box.addRow(new Row<>(weapons_labels, null));

        Label[] magics_labels = new Label[players.length + 1];
        magics_labels[0] = new SortedLabel(i18n("magics_used"), 9, Skin.getSkin().getMultiColumnComboBoxData().font());
        for (int i = 0; i < players.length; i++) {
            magics_labels[i + 1] = new IntegerLabel(players[i].getMagics(),
                    Skin.getSkin().getMultiColumnComboBoxData().font(), PLAYER_COLUMN_WIDTH + TEXT_OFFSET);
        }
        score_box.addRow(new Row<>(magics_labels, null));

        List<Label> total_labels = new ArrayList<>(players.length + 1);
        total_labels.add(new SortedLabel(i18n("total"), 10, Skin.getSkin().getMultiColumnComboBoxData().font()));
        for (Player player : players) {
            int unit_killed = player.getUnitsKilled();
            int buildings_wrecked = player.getBuildingsDestroyed();
            int tree = player.getTreeHarvested();
            int rock = player.getRockHarvested();
            int iron = player.getIronHarvested();
            int chicken = player.getRubberHarvested();

            int total_score = unit_killed * 10 + buildings_wrecked * 100 + tree + rock + iron * 2 + chicken * 4;

            total_labels.add(new IntegerLabel(total_score, Skin.getSkin().getMultiColumnComboBoxData().font(),
                    PLAYER_COLUMN_WIDTH + TEXT_OFFSET));
        }
        score_box.addRow(new Row<>(total_labels.toArray(Label[]::new), null));

        return score_box;
    }

    /**
     * Transposed paginated layout: rows=players, columns=stats. Used for >6 players.
     */
    private MultiColumnComboBox<Void> buildPaginatedLayout(Player[] worldPlayers) {
        this.paginatedPlayers = worldPlayers;
        this.paginatedFont = Skin.getSkin().getMultiColumnComboBoxData().font();
        this.currentPage = 0;

        paginatedStatNames = new String[]{i18n("units_lost"), i18n("units_killed"), i18n("buildings_lost"), i18n(
                "buildings_wrecked"), i18n("tree_resources"), i18n("rock_resources"), i18n("iron_resources"), i18n(
                        "chicken_resources"), i18n("meters_walked"), i18n("weapons_thrown"), i18n("magics_used"), i18n(
                                "total")
        };

        paginatedPlayerStats = new int[paginatedPlayers.length][];
        for (int i = 0; i < paginatedPlayers.length; i++) {
            Player player = paginatedPlayers[i];
            int total_score = player.getUnitsKilled() * 10 + player.getBuildingsDestroyed() * 100 + player
                    .getTreeHarvested() + player.getRockHarvested() + player.getIronHarvested() * 2 + player
                            .getRubberHarvested() * 4;
            paginatedPlayerStats[i] = new int[]{player.getUnitsLost(), player.getUnitsKilled(), player
                    .getBuildingsLost(), player.getBuildingsDestroyed(), player.getTreeHarvested(), player
                            .getRockHarvested(), player.getIronHarvested(), player.getRubberHarvested(), player
                                    .getUnitsMoved() * 2, player.getWeaponsThrown(), player.getMagics(), total_score
            };
        }

        paginatedScoreBox = buildPaginatedScoreBox(STAT_PAGES[0]);
        return paginatedScoreBox;
    }

    private MultiColumnComboBox<Void> buildPaginatedScoreBox(int[] statIndices) {
        ColumnInfo[] columns = new ColumnInfo[statIndices.length + 1];
        columns[0] = new ColumnInfo(i18n("player_header"), PAGINATED_PLAYER_NAME_WIDTH);
        for (int j = 0; j < statIndices.length; j++) {
            columns[j + 1] = new ColumnInfo(paginatedStatNames[statIndices[j]], PAGINATED_STAT_COLUMN_WIDTH);
        }

        MultiColumnComboBox<Void> box = new MultiColumnComboBox<>(viewer.getGUIRoot(), columns, 200);
        for (int i = 0; i < paginatedPlayers.length; i++) {
            Label[] row = new Label[statIndices.length + 1];
            row[0] = new SortedLabel(paginatedPlayers[i].getPlayerInfo().getName(), i, paginatedFont);
            for (int j = 0; j < statIndices.length; j++) {
                row[j + 1] = new IntegerLabel(paginatedPlayerStats[i][statIndices[j]], paginatedFont,
                        PAGINATED_STAT_COLUMN_WIDTH + TEXT_OFFSET);
            }
            box.addRow(new Row<>(row, null));
        }
        return box;
    }

    private void switchPage(int direction) {
        currentPage = (currentPage + direction + STAT_PAGES.length) % STAT_PAGES.length;
        int old_x = paginatedScoreBox.getX();
        int old_y = paginatedScoreBox.getY();
        removeChild(paginatedScoreBox);
        paginatedScoreBox = buildPaginatedScoreBox(STAT_PAGES[currentPage]);
        addChild(paginatedScoreBox);
        paginatedScoreBox.setPos(old_x, old_y);
    }

    @Override
    public void update(@NonNull TimerAnimation anim) {
        addChild(group_buttons);
        delay_timer.stop();
    }

    @Override
    protected void renderGeometry(@NonNull GUIRenderer renderer) {
        renderBackgroundAlpha(renderer);
    }

    @Override
    public void handleInput(@NonNull InputEvent event) {
        if (event.getPhase() == InputPhase.PRESSED || event.getPhase() == InputPhase.REPEAT) {
            if (event.consumeAction(GameAction.UI_FOCUS_NEXT)) {
                switchFocus(FocusDirection.FORWARD);
                event.consume();
                return;
            }
            if (event.consumeAction(GameAction.UI_FOCUS_PREV)) {
                switchFocus(FocusDirection.BACKWARD);
                event.consume();
                return;
            }
        }
        super.handleInput(event);
    }

    public void startMenu() {
        viewer.close();
        setDisabled(true);
    }

    public @NonNull WorldViewer getViewer() {
        return viewer;
    }
}
