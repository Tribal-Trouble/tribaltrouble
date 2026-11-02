package com.oddlabs.tt.mapeditor;

import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.util.ServerMessageBundler;
import com.oddlabs.util.Color;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;

/**
 * A map shared on the server, as the multiplayer menus show it: its name over its preview, and a line under it
 * telling its size and terrain, or how its download goes.
 */
public final class SharedMapView extends Group {
    private static final int MAX_PREVIEW_SIZE = 256;
    private static final Vector4fc ERROR_COLOR = Color.argb4v(0xFF_FF_70_60);

    private final @NonNull MapPreviewView preview;
    private final @NonNull Label label_name;
    private final @NonNull Label label_status;
    private @Nullable String hash;
    private @NonNull String info = "";
    private SharedMaps.@Nullable DownloadListener download;

    public SharedMapView(int preview_size) {
        super(false);
        preview = new MapPreviewView(preview_size);
        label_name = new Label("", Skin.getSkin().getEditFont(), preview_size);
        label_status = new Label("", Skin.getSkin().getEditFont(), preview_size);
        addChild(label_name);
        addChild(preview);
        addChild(label_status);
        label_name.place();
        preview.place(label_name, BOTTOM_LEFT);
        label_status.place(preview, BOTTOM_LEFT);
        compileCanvas();
    }

    /** The largest preview a view fits in a height with, or 0 when even the text does not fit. */
    public static int previewSizeFor(int height) {
        int text = 2 * (Skin.getSkin().getEditFont().getHeight() + Skin.getSkin().getFormData().objectSpacing());
        return Math.clamp(height - text, 0, MAX_PREVIEW_SIZE);
    }

    /** Shows a map, with its size and terrain under it. */
    public void show(@NonNull String map_hash, @NonNull String name, int size, int terrain) {
        stopFetching();
        hash = map_hash;
        info = MapEditor.i18n("map_info", ServerMessageBundler.getSizeString(size),
                ServerMessageBundler.getTerrainTypeString(terrain));
        label_name.set(name);
        setStatus(info, false);
        preview.show(null, MapEditor.i18n("shared_loading_preview"));
        SharedMaps.get().requestPreview(map_hash, found -> {
            if (map_hash.equals(hash))
                preview.show(found, MapEditor.i18n("no_preview"));
        });
    }

    /** Shows nothing. */
    public void clear() {
        stopFetching();
        hash = null;
        info = "";
        label_name.set("");
        setStatus("", false);
        preview.show(null, "");
    }

    /**
     * Downloads the map shown unless it is here already, telling how it goes in place of its size and terrain.
     * The download goes on while the view is off the screen, as when its tab is not the one shown.
     *
     * @param ready run once the map is here to play
     */
    public void fetch(@NonNull Runnable ready) {
        String map_hash = hash;
        if (map_hash == null)
            return;
        stopFetching();
        SharedMaps.DownloadListener listener = new SharedMaps.DownloadListener() {
            @Override
            public void progress(float fraction) {
                setStatus(MapEditor.i18n("shared_downloading", Math.round(fraction * 100)), false);
            }

            @Override
            public void downloaded(@NonNull Path path) {
                download = null;
                setStatus(info, false);
                ready.run();
            }

            @Override
            public void failed(@NonNull String reason) {
                download = null;
                setStatus(reason, true);
            }
        };
        download = listener;
        SharedMaps.get().download(map_hash, listener);
    }

    private void stopFetching() {
        if (download != null) {
            SharedMaps.get().forget(download);
            download = null;
        }
    }

    /** Shows the map's size and terrain under it again. */
    void showInfo() {
        setStatus(info, false);
    }

    void setStatus(@NonNull String status, boolean error) {
        label_status.set(status);
        label_status.setColor(error ? ERROR_COLOR : Label.DEFAULT_COLOR);
    }
}
