package com.oddlabs.tt.mapeditor;

import com.oddlabs.matchmaking.MatchmakingServerInterface;
import com.oddlabs.matchmaking.Profile;
import com.oddlabs.matchmaking.SharedMap;
import com.oddlabs.tt.font.Font;
import com.oddlabs.tt.form.MessageForm;
import com.oddlabs.tt.form.QuestionForm;
import com.oddlabs.tt.gui.ColumnInfo;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.IntegerLabel;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.MultiColumnComboBox;
import com.oddlabs.tt.gui.Panel;
import com.oddlabs.tt.gui.Row;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.gui.TextBox;
import com.oddlabs.tt.guievent.RowListener;
import com.oddlabs.tt.net.Network;
import com.oddlabs.tt.util.ServerMessageBundler;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.RIGHT_MID;
import static com.oddlabs.tt.gui.Placement.RIGHT_TOP;

/**
 * The multiplayer menu's tab of maps players have shared: browse them with their previews, download one to play
 * or edit, upload one of your own, or take one of yours off the server. It also opens the shared map editor sessions.
 */
public final class MapBrowserPanel extends Panel {
    // With the preview beside it, as wide as the games tab's list.
    private static final int NAME_WIDTH = 180;
    private static final int AUTHOR_WIDTH = 120;
    private static final int PREVIEW_SIZE = 200;
    private static final int BUTTON_WIDTH = 110;
    private static final int BUTTON_WIDTH_LONG = 150;
    private static final int BUTTON_WIDTH_EXTRA_LONG = 170;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(
            ZoneId.systemDefault());

    private final @NonNull GUIRoot gui_root;
    private final @NonNull MultiColumnComboBox<SharedMap> list;
    private final @NonNull SharedMapView view;
    private final @NonNull Label label_author;
    private final @NonNull Label label_uploaded;
    private final @NonNull Label label_downloads;
    private final @NonNull TextBox box_description;
    private final @NonNull HorizButton button_download;
    private final @NonNull HorizButton button_delete;
    // The map to select once the list comes back from the server.
    private @Nullable String reselect;
    private @Nullable SharedMap shown;

    /**
     * @param list_height   the height of the list, to line up with the other tabs' lists
     * @param open_sessions opens the window of shared map editor sessions
     */
    public MapBrowserPanel(@NonNull GUIRoot gui_root, int list_height, @NonNull Runnable open_sessions) {
        super(MapEditor.i18n("shared_caption"));
        this.gui_root = gui_root;
        Label label_headline = new Label(MapEditor.i18n("shared_headline"), Skin.getSkin().getHeadlineFont());

        ColumnInfo[] columns = new ColumnInfo[]{new ColumnInfo(MapEditor.i18n("column_name"),
                NAME_WIDTH), new ColumnInfo(MapEditor.i18n("column_author"), AUTHOR_WIDTH), new ColumnInfo(
                        MapEditor.i18n("column_size"), 100), new ColumnInfo(MapEditor.i18n("column_downloads"), 90)};
        list = new MultiColumnComboBox<>(gui_root, columns, list_height);
        list.addRowListener(new RowListener<>() {
            @Override
            public void rowChosen(@NonNull SharedMap map) {
                reselect = null;
                show(map);
            }

            @Override
            public void rowDoubleClicked(@NonNull SharedMap map) {
                download(map);
            }
        });

        view = new SharedMapView(PREVIEW_SIZE);
        Font font = Skin.getSkin().getEditFont();
        label_author = new Label("", font, PREVIEW_SIZE);
        label_uploaded = new Label("", font, PREVIEW_SIZE);
        label_downloads = new Label("", font, PREVIEW_SIZE);
        // The description fills the rest of the height beside the list.
        int spacing = Skin.getSkin().getFormData().objectSpacing();
        box_description = new TextBox(PREVIEW_SIZE, Math.max(2 * font.getHeight(),
                list.getHeight() - PREVIEW_SIZE - label_author.getHeight() - label_uploaded.getHeight() - label_downloads.getHeight() - spacing),
                font,
                MapFile.MAX_DESCRIPTION_LENGTH);

        HorizButton button_update = new HorizButton(MapEditor.i18n("shared_update"), BUTTON_WIDTH_EXTRA_LONG);
        button_update.addMouseClickListener((_, _, _, _) -> refresh());
        HorizButton button_upload = new HorizButton(MapEditor.i18n("shared_upload"), BUTTON_WIDTH_LONG);
        button_upload.addMouseClickListener((_, _, _, _) -> MapEditor.chooseMapToUpload(gui_root, map -> {
            reselect = map.getHash();
            refresh();
            gui_root.addModalForm(new MessageForm(MapEditor.i18n("shared_uploaded", map.getName())));
        }));
        button_download = new HorizButton(MapEditor.i18n("shared_download"), BUTTON_WIDTH);
        button_download.addMouseClickListener((_, _, _, _) -> {
            SharedMap selected = list.getSelected();
            if (selected != null)
                download(selected);
        });
        button_delete = new HorizButton(MapEditor.i18n("delete_button"), BUTTON_WIDTH);
        button_delete.addMouseClickListener((_, _, _, _) -> {
            SharedMap selected = list.getSelected();
            if (selected != null && isMine(selected))
                gui_root.addModalForm(new QuestionForm(MapEditor.i18n("shared_delete_confirm", selected.getName()),
                        (_, _, _, _) -> delete(selected)));
        });
        HorizButton button_sessions = new HorizButton(MapEditor.i18n("sessions_button"), BUTTON_WIDTH_LONG);
        button_sessions.addMouseClickListener((_, _, _, _) -> open_sessions.run());

        addChild(label_headline);
        addChild(list);
        addChild(view);
        addChild(label_author);
        addChild(label_uploaded);
        addChild(label_downloads);
        addChild(box_description);
        addChild(button_update);
        addChild(button_upload);
        addChild(button_download);
        addChild(button_delete);
        addChild(button_sessions);
        label_headline.place();
        list.place(label_headline, BOTTOM_LEFT);
        view.place(list, RIGHT_TOP);
        label_author.place(view, BOTTOM_LEFT);
        label_uploaded.place(label_author, BOTTOM_LEFT);
        label_downloads.place(label_uploaded, BOTTOM_LEFT);
        box_description.place(label_downloads, BOTTOM_LEFT, spacing);
        button_update.place(list, BOTTOM_LEFT);
        button_upload.place(button_update, RIGHT_MID);
        button_download.place(button_upload, RIGHT_MID);
        button_delete.place(button_update, BOTTOM_LEFT);
        button_sessions.place(button_delete, RIGHT_MID);
        compileCanvas();
        show(null);

        addFocusListener(activated -> {
            if (activated)
                refresh();
        });
    }

    private static void refresh() {
        Network.getMatchmakingClient().requestList(MatchmakingServerInterface.TYPE_MAP_LIST);
    }

    /** Empties the list, as a new one is coming from the server. */
    public void clear() {
        if (shown != null && reselect == null)
            reselect = shown.getHash();
        list.clear();
        show(null);
    }

    /** Adds maps the server lists. */
    public void add(Object @NonNull [] maps) {
        Font font = Skin.getSkin().getMultiColumnComboBoxData().font();
        for (Object entry : maps) {
            if (!(entry instanceof SharedMap map))
                continue;
            Row<SharedMap, Label> row = new Row<>(List.of(
                    new Label(map.getName(), font, NAME_WIDTH),
                    new Label(map.getAuthor(), font, AUTHOR_WIDTH),
                    new Label(ServerMessageBundler.getSizeString(map.getSize()), font),
                    new IntegerLabel(map.getDownloads(), font)),
                    map);
            list.addRow(row);
            // The first map, until the one selected before comes along.
            boolean before = map.getHash().equals(reselect);
            if (before || shown == null) {
                if (before)
                    reselect = null;
                list.selectRow(row);
                show(map);
            }
        }
    }

    private void show(@Nullable SharedMap map) {
        shown = map;
        if (map == null) {
            view.clear();
            label_author.set("");
            label_uploaded.set("");
            label_downloads.set("");
            LoadMapDialog.showDescription(box_description, null);
        } else {
            view.show(map.getHash(), map.getName(), map.getSize(), map.getTerrainType());
            label_author.set(MapEditor.i18n("shared_author", map.getAuthor()));
            label_uploaded.set(MapEditor.i18n("shared_uploaded_on", DATE_FORMAT.format(Instant.ofEpochMilli(
                    map.getUploaded()))));
            label_downloads.set(MapEditor.i18n("shared_downloads", map.getDownloads()));
            LoadMapDialog.showDescription(box_description, map.getDescription());
        }
        button_download.setDisabled(map == null);
        button_delete.setDisabled(map == null || !isMine(map));
    }

    private boolean isShown(@NonNull SharedMap map) {
        return shown != null && shown.getHash().equals(map.getHash());
    }

    private static boolean isMine(@NonNull SharedMap map) {
        Profile profile = Network.getMatchmakingClient().getProfile();
        return profile != null && profile.getNick().equalsIgnoreCase(map.getAuthor());
    }

    /** Downloads a map and copies it in among the player's maps, to play or edit. */
    private void download(@NonNull SharedMap map) {
        SharedMaps.get().download(map.getHash(), new SharedMaps.DownloadListener() {
            @Override
            public void progress(float fraction) {
                if (isShown(map))
                    view.setStatus(MapEditor.i18n("shared_downloading", Math.round(fraction * 100)), false);
            }

            @Override
            public void downloaded(@NonNull Path cached) {
                if (isShown(map))
                    view.showInfo();
                Path kept;
                try {
                    kept = SharedMaps.keep(cached, map.getName());
                } catch (IOException e) {
                    gui_root.addModalForm(new MessageForm(MapEditor.i18n("save_failed", e.getMessage())));
                    return;
                }
                Path maps = MapEditor.getMapsDir();
                String where = maps != null ? maps.relativize(kept).toString() : kept.toString();
                gui_root.addModalForm(new MessageForm(MapEditor.i18n("shared_downloaded", map.getName(), where)));
            }

            @Override
            public void failed(@NonNull String reason) {
                if (isShown(map))
                    view.showInfo();
                gui_root.addModalForm(new MessageForm(MapEditor.i18n("shared_download_failed_named",
                        map.getName(), reason)));
            }
        });
    }

    private void delete(@NonNull SharedMap map) {
        MatchmakingServerInterface server = Network.getMatchmakingClient().isConnected() ? Network.getMatchmakingClient().getInterface() : null;
        if (server == null)
            return;
        server.deleteMap(map.getHash());
        SharedMaps.get().forgetPreview(map.getHash());
        shown = null;
        refresh();
    }
}
