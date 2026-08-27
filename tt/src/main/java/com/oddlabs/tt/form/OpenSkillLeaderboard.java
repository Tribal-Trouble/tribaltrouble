package com.oddlabs.tt.form;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

import com.oddlabs.tt.font.Font;
import com.oddlabs.matchmaking.OpenSkillLeaderboardRankingEntry;
import com.oddlabs.tt.gui.Box;
import com.oddlabs.tt.gui.ColumnInfo;
import com.oddlabs.tt.gui.GUIObject;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.IntegerLabel;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.ModeIconQuads;
import com.oddlabs.tt.gui.MultiColumnComboBox;
import com.oddlabs.tt.gui.NumericLabel;
import com.oddlabs.tt.gui.Row;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.render.GUIRenderer;

@NullMarked
public final class OpenSkillLeaderboard extends GUIObject {
    private static final int RANK_WIDTH = 50;
    private static final int NAME_WIDTH = 350;
    private static final int SCORE_WIDTH = 100;
    private static final int HEIGHT = 350;

    private record Column(String caption, int width, CellFactory cellFactory) {
        ColumnInfo toColumnInfo() {
            return new ColumnInfo(caption, width);
        }

        Label createCell(@Nullable OpenSkillLeaderboardRankingEntry entry) {
            Font font = Skin.getSkin().getMultiColumnComboBoxData().font();
            if (entry == null) {
                return new Label("", font, width);
            }
            return cellFactory.create(entry, font, width);
        }
    }

    @FunctionalInterface
    private interface CellFactory {
        Label create(OpenSkillLeaderboardRankingEntry entry, Font font, int width);
    }

    private final List<Column> columns;
    private final OpenSkillPersonalRankingRow personalRow;
    private final MultiColumnComboBox<OpenSkillLeaderboardRankingEntry> rankingTable;

    public OpenSkillLeaderboard(GUIRoot guiRoot, Function<String, String> i18n) {
        columns = List.of(
                new Column(
                        i18n.apply("rank"),
                        RANK_WIDTH,
                        (entry, f, w) -> entry.rank() > 0 ? new IntegerLabel(entry.rank(), f) : new Label("-", f, w)),
                new Column(
                        i18n.apply("name"),
                        NAME_WIDTH,
                        (entry, f, w) -> new Label(entry.nick(), f, w)),
                new Column(
                        i18n.apply("rating"),
                        SCORE_WIDTH,
                        (entry, f, w) -> new NumericLabel(formatRating(entry), f, w, entry.rating())),
                new Column(
                        i18n.apply("mu"),
                        SCORE_WIDTH,
                        (entry, f, w) -> new NumericLabel(formatScore(entry.mu()), f, w, entry.mu())),
                new Column(
                        i18n.apply("sigma"),
                        SCORE_WIDTH,
                        (entry, f, w) -> new NumericLabel(formatScore(entry.sigma()), f, w, entry.sigma())));
        ColumnInfo[] columnInfos = columns.stream().map(Column::toColumnInfo).toArray(ColumnInfo[]::new);

        int spacing = Skin.getSkin().getFormData().objectSpacing();
        personalRow = new OpenSkillPersonalRankingRow(columnInfos, this::createRow);
        rankingTable = new MultiColumnComboBox<>(guiRoot, columnInfos, HEIGHT - personalRow.getHeight() - spacing);

        rankingTable.setPos(0, 0);
        personalRow.setPos(0, rankingTable.getHeight() + spacing);
        addChild(personalRow);
        addChild(rankingTable);
        setDim(rankingTable.getWidth(), HEIGHT);
        setCanFocus(true);
    }

    public void addRow(OpenSkillLeaderboardRankingEntry entry) {
        rankingTable.addRow(createRow(entry));
    }

    public void clearRows() {
        rankingTable.clear();
    }

    public void setPersonalEntry(@Nullable OpenSkillLeaderboardRankingEntry entry) {
        personalRow.setEntry(entry);
    }

    private Row<OpenSkillLeaderboardRankingEntry, Label> createRow(@Nullable OpenSkillLeaderboardRankingEntry entry) {
        Label[] cells = columns.stream().map(column -> column.createCell(entry)).toArray(Label[]::new);
        return new Row<OpenSkillLeaderboardRankingEntry, Label>(cells, entry);
    }

    private static String formatRating(OpenSkillLeaderboardRankingEntry entry) {
        return entry.rating() + (entry.provisional() ? "?" : "");
    }

    private static String formatScore(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}

@NullMarked
final class OpenSkillPersonalRankingRow extends GUIObject {
    private final Box box;
    private final ColumnInfo[] columnInfos;
    private final Function<@Nullable OpenSkillLeaderboardRankingEntry, Row<OpenSkillLeaderboardRankingEntry, Label>> rowFactory;
    private @Nullable Row<OpenSkillLeaderboardRankingEntry, Label> row;

    OpenSkillPersonalRankingRow(
            ColumnInfo[] columnInfos,
            Function<@Nullable OpenSkillLeaderboardRankingEntry, Row<OpenSkillLeaderboardRankingEntry, Label>> rowFactory
    ) {
        this.columnInfos = columnInfos;
        this.rowFactory = rowFactory;
        box = Skin.getSkin().getMultiColumnComboBoxData().box();
        Row<OpenSkillLeaderboardRankingEntry, Label> initialRow = mountRow(null);
        int width = Arrays.stream(columnInfos).mapToInt(ColumnInfo::width).sum();
        setDim(width, box.getTopOffset() + initialRow.getHeight() + box.getBottomOffset());
    }

    void setEntry(@Nullable OpenSkillLeaderboardRankingEntry entry) {
        if (row != null) {
            row.remove();
        }
        row = mountRow(entry);
    }

    private Row<OpenSkillLeaderboardRankingEntry, Label> mountRow(@Nullable OpenSkillLeaderboardRankingEntry entry) {
        Row<OpenSkillLeaderboardRankingEntry, Label> newRow = rowFactory.apply(entry);
        newRow.setColumnInfos(columnInfos);
        newRow.setPos(box.getLeftOffset(), box.getBottomOffset());
        addChild(newRow);
        return newRow;
    }

    @Override
    protected void renderGeometry(GUIRenderer renderer) {
        box.render(renderer, 0f, 0f, getWidth(), getHeight(), ModeIconQuads.Mode.NORMAL);
    }
}
