package com.oddlabs.tt.mapeditor;

import com.oddlabs.matchmaking.SharedMap;
import com.oddlabs.tt.form.MapcodeForm;
import com.oddlabs.tt.form.MessageForm;
import com.oddlabs.tt.form.TerrainMenu;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.resource.IslandGenerator;
import com.oddlabs.tt.resource.WorldGenerator;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Shared strings and locations for the map editor.
 */
public final class MapEditor {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(MapEditor.class.getName());
    // The terrain options share their captions with the skirmish menu so they stay translated alike.
    private static final ResourceBundle terrain_bundle = ResourceBundle.getBundle(TerrainMenu.class.getName());
    private static final ResourceBundle mapcode_bundle = ResourceBundle.getBundle(MapcodeForm.class.getName());

    private static final String MAPS_DIR = "maps";

    private MapEditor() {
    }

    public static @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    static @NonNull String terrainI18n(@NonNull String key, @NonNull Object @NonNull... args) {
        return Utils.getBundleString(terrain_bundle, key, args);
    }

    static @NonNull String mapcodeI18n(@NonNull String key) {
        return Utils.getBundleString(mapcode_bundle, key);
    }

    /**
     * Opens the map browser to choose a saved map to play, and hands it over once chosen.
     */
    public static void chooseMapToPlay(@NonNull GUIRoot gui_root, @NonNull Consumer<@NonNull CustomMap> play) {
        chooseMap(gui_root, i18n("play_caption"), i18n("play_button"), (entry, map) -> chooseSpawns(gui_root, map,
                choice -> play.accept(new CustomMap(map.name(), map.settings(), entry.path(), choice))));
    }

    /**
     * Opens the map browser to choose a saved map to host a multiplayer game on. The map is uploaded to the server
     * first, unless the server has it, so the players joining can download it; it is handed over once it is there.
     */
    public static void chooseMapToHost(@NonNull GUIRoot gui_root, @NonNull Consumer<@NonNull CustomMap> host) {
        chooseMap(gui_root, i18n("host_caption"), i18n("host_button"), (entry, map) -> chooseSpawns(gui_root, map,
                choice -> UploadMapForm.upload(gui_root, entry.path(), map.name(), shared -> host.accept(
                        new CustomMap(shared.getName(), map.settings(), entry.path(), shared.getHash(), choice)))));
    }

    /**
     * Asks where the players start, when the map has spawns: at them in order, at them shuffled, or anywhere. A map
     * without spawns goes on at once.
     */
    private static void chooseSpawns(@NonNull GUIRoot gui_root, @NonNull MapFile map,
            @NonNull Consumer<@NonNull SpawnChoice> chosen) {
        if (map.spawns().isEmpty()) {
            chosen.accept(SpawnChoice.RANDOM);
            return;
        }
        List<EditorMenu.Entry> entries = new ArrayList<>();
        for (SpawnChoice choice : SpawnChoice.values())
            entries.add(new EditorMenu.Entry(choice.getLabel(), () -> chosen.accept(choice)));
        entries.add(new EditorMenu.Entry(i18n("spawns_cancel"), () -> {
        }));
        gui_root.addModalForm(new EditorMenu(i18n("spawns_caption", map.spawns().count()), entries));
    }

    /** Opens the map browser to choose a saved map to upload, and hands over the shared map once it is there. */
    public static void chooseMapToUpload(@NonNull GUIRoot gui_root, @NonNull Consumer<@NonNull SharedMap> done) {
        chooseMap(gui_root, i18n("upload_caption"), i18n("upload_button"), (entry, map) -> UploadMapForm.upload(
                gui_root, entry.path(), map.name(), done));
    }

    private static void chooseMap(@NonNull GUIRoot gui_root, @NonNull String caption, @NonNull String action,
            @NonNull BiConsumer<MapFile.@NonNull Entry, @NonNull MapFile> chosen) {
        Path dir = getMapsDir();
        if (dir != null) {
            try {
                Files.createDirectories(dir);
            } catch (IOException _) {
                dir = null;
            }
        }
        Path start = dir != null ? dir : Path.of(System.getProperty("user.home"));
        gui_root.addModalForm(new LoadMapDialog(gui_root, start, caption, action, entry -> {
            MapFile map;
            try {
                // Read it all now, so a broken file is reported here rather than while the game loads.
                map = MapFile.load(entry.path());
            } catch (IOException e) {
                gui_root.addModalForm(new MessageForm(i18n("load_failed", e.getMessage())));
                return;
            }
            chosen.accept(entry, map);
        }));
    }

    /**
     * Where the players of a game start, as the host chose when picking the custom map the generator builds.
     *
     * @return the choice, or null when the game is not on a custom map
     */
    public static @Nullable String describeStarts(@Nullable WorldGenerator generator) {
        if (!(generator instanceof IslandGenerator island))
            return null;
        return switch (island.getOverride()) {
            case SharedMapSource shared -> shared.getSpawnChoice().getLabel();
            case SavedMapSource saved -> saved.getSpawnChoice().getLabel();
            case null, default -> null;
        };
    }

    /** Where saved maps live, or null when the game has no writable directory. */
    static @Nullable Path getMapsDir() {
        Path game_dir = Renderer.getLocalInput().getGameDir();
        return game_dir == null ? null : game_dir.resolve(MAPS_DIR);
    }
}
