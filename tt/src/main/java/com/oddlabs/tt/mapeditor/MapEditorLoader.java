package com.oddlabs.tt.mapeditor;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.audio.AudioManager;
import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.form.LoadCallback;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.landscape.LandscapeResources;
import com.oddlabs.tt.landscape.NotificationListener;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.landscape.WorldParameters;
import com.oddlabs.tt.model.RubberSupply;
import com.oddlabs.tt.model.SupplyManager;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.player.PlayerInfo;
import com.oddlabs.tt.procedural.LandscapeOverride;
import com.oddlabs.tt.render.DefaultRenderer;
import com.oddlabs.tt.render.LandscapeRenderer;
import com.oddlabs.tt.render.MatrixStack;
import com.oddlabs.tt.render.Picker;
import com.oddlabs.tt.render.RenderQueues;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.render.UIRenderer;
import com.oddlabs.tt.resource.IslandGenerator;
import com.oddlabs.tt.resource.WorldInfo;
import com.oddlabs.tt.viewer.Cheat;
import com.oddlabs.tt.viewer.Selection;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/**
 * Builds the editor's island behind the progress screen, the same way the main menu builds its backdrop island:
 * a world with a single idle player and no races loaded, since the editor only shapes terrain.
 */
final class MapEditorLoader implements LoadCallback {
    private final @NonNull NetworkSelector network;
    private final @NonNull MapSettings settings;
    private final @Nullable String map_name;
    private final @NonNull String description;
    private final float @Nullable [] @Nullable [] heights;
    private final MapFile.@Nullable Resources resources;
    private final @NonNull Spawns spawns;
    private final @NonNull SessionStart session;

    /** Whether the editor opens a shared session once built, joins the one the island came from, or neither. */
    sealed interface SessionStart {
        /** Editing alone. */
        record None() implements SessionStart {
        }

        /** Opens a session on the island, for others to join from the multiplayer menu. */
        record Host(@NonNull String name) implements SessionStart {
        }

        /** Joins the session the island was handed over from. */
        record Join(@NonNull EditorSession session) implements SessionStart {
        }
    }

    MapEditorLoader(@NonNull NetworkSelector network, @NonNull MapSettings settings, @Nullable String map_name,
            @NonNull String description, float @Nullable [] @Nullable [] heights,
            MapFile.@Nullable Resources resources, @NonNull Spawns spawns, @NonNull SessionStart session) {
        this.network = network;
        this.settings = settings;
        this.map_name = map_name;
        this.description = description;
        this.heights = heights;
        this.resources = resources;
        this.spawns = spawns;
        this.session = session;
    }

    /**
     * The saved heights and resources for the generator to build the island with, as a game on the map does: the
     * walkable ground, the sea and the regions then follow the saved heights rather than the generated ones. Null
     * for an island generated from its settings alone.
     */
    private LandscapeOverride.@Nullable Source savedIsland() {
        float[][] saved_heights = heights != null && heights.length == MapFile.gridSize(settings) ? heights : null;
        MapFile.Resources saved_resources = resources;
        if (saved_heights == null && saved_resources == null)
            return null;
        LandscapeOverride override = new LandscapeOverride(saved_heights,
                saved_resources == null ? null : new LandscapeOverride.Resources(saved_resources.of(Resource.TREE),
                        saved_resources.of(Resource.PALM), saved_resources.of(Resource.ROCK),
                        saved_resources.of(Resource.IRON)));
        return () -> override;
    }

    /**
     * Puts the resources exactly where the map has them. The generator leaves out those on ground the game would not
     * let them stand on, and those where it clears room for the start, which in the editor is only a stand-in; the
     * editor shows what was saved, so saving again keeps it, and everyone in a shared session starts alike.
     */
    private static void matchResources(@NonNull ResourceLayer layer, int size, MapFile.@NonNull Resources resources) {
        byte[] wanted = new byte[size * size];
        Arrays.fill(wanted, EditOp.NO_RESOURCE);
        for (Resource kind : Resource.values())
            for (int[] position : resources.of(kind))
                wanted[position[1] * size + position[0]] = (byte) kind.ordinal();
        int[] cells = new int[wanted.length];
        byte[] kinds = new byte[wanted.length];
        int count = 0;
        for (int cell = 0; cell < wanted.length; cell++) {
            Resource there = layer.get(cell % size, cell / size);
            if ((there != null ? there.ordinal() : EditOp.NO_RESOURCE) != wanted[cell]) {
                cells[count] = cell;
                kinds[count++] = wanted[cell];
            }
        }
        if (count > 0)
            layer.applyShared(cells, kinds, count);
    }

    @Override
    public @NonNull UIRenderer load(@NonNull GUIRoot gui_root) {
        AnimationManager.freezeTime();
        IslandGenerator generator = settings.createGenerator(savedIsland());
        PlayerInfo[] players = new PlayerInfo[]{new PlayerInfo(0, 0, "")};
        WorldParameters world_params = new WorldParameters(Game.GAMESPEED_NORMAL, "", 2,
                Player.DEFAULT_MAX_UNIT_COUNT);
        WorldInfo world_info = generator.generate(players.length, world_params.getInitialUnitCount(), 0f);
        float[][] terrain = world_info.heightmap();
        boolean edited = heights != null && heights.length == terrain.length;
        if (edited) {
            // The generator built the island from the saved heights, but scaled to its own units and back, so they
            // go in again exactly as saved: everyone in a shared session must start from the same heights. The
            // height map keeps this array, so they must be in place before the world is built.
            for (int y = 0; y < terrain.length; y++)
                System.arraycopy(heights[y], 0, terrain[y], 0, terrain[y].length);
            TerrainEditor.pinEdges(terrain);
        }

        RenderQueues render_queues = new RenderQueues();
        LandscapeResources landscape_resources = World.loadCommon(render_queues);
        World world = World.newWorld(AudioManager.getManager(), landscape_resources, null, new NotificationListener() {
        }, world_params, world_info, generator.getTerrainType(), players, generator.getFogInfo());
        // The game lets chickens loose by the trees now and then. In the editor they would only stand on cells
        // that resources are painted on, so they stay away.
        SupplyManager chickens = world.getSupplyManager(RubberSupply.class);
        if (chickens != null)
            world.getAnimationManagerGameTime().removeAnimation(chickens);
        AnimationManager manager = new AnimationManager();
        LandscapeRenderer landscape_renderer = new LandscapeRenderer(world, world_info, manager);
        Player local_player = world.getPlayers()[0];
        Selection selection = new Selection(local_player);
        Picker picker = new Picker(manager, local_player, gui_root, render_queues, landscape_renderer, selection);
        // The editor's view toggles (trees, wireframe) are the renderer's cheat switches.
        Cheat view = new Cheat();
        DefaultRenderer renderer = new DefaultRenderer(view, local_player, render_queues, world_info,
                landscape_renderer,
                picker, selection, generator, new MatrixStack(), new MatrixStack(), null);

        GroundTextures ground = GroundTextures.create(world_info, settings,
                Renderer.getRenderer().getRenderContext(), resources == null);
        // The generator baked the ground texture for its own heights and trees, so saved ones need it redone.
        if ((edited || resources != null) && ground != null)
            ground.rebuildAll();
        ResourceSnapper snapper = new ResourceSnapper(world);
        AccessMap access_map = new AccessMap(terrain, settings);
        TintOverlay tint = new TintOverlay(terrain, access_map, settings);
        SessionSync.Link link = new SessionSync.Link();
        ResourceLayer layer = new ResourceLayer(world, access_map, settings, (changed, x0, y0, x1, y1) -> {
            if (ground != null)
                ground.resourcesChanged(changed, x0, y0, x1, y1);
            tint.mapChanged();
            link.resourcesChanged(x0, y0, x1, y1);
        });
        TerrainEditor editor = new TerrainEditor(world.getHeightMap(), terrain, settings, (x0, y0, x1, y1) -> {
            snapper.snap(x0, y0, x1, y1);
            if (ground != null)
                ground.heightsChanged(x0, y0, x1, y1);
            access_map.heightsChanged();
            link.heightsChanged(x0, y0, x1, y1);
        });
        tint.setResources(layer::get);
        if (resources != null)
            matchResources(layer, editor.getSize(), resources);
        MapEditorDelegate delegate = new MapEditorDelegate(network, gui_root, world, manager, picker, view,
                new CameraState(generator.getFogInfo()), editor, ground, access_map, tint, layer,
                new PlantLayer(world, access_map), renderer.getWater(), settings, map_name, description, edited,
                resources != null, spawns, link);
        Renderer.getRenderer().setMusicPath("/music/menu.ogg", 0f);
        gui_root.pushDelegate(delegate);
        switch (session) {
            case SessionStart.Host host -> delegate.hostSession(host.name());
            case SessionStart.Join join -> delegate.joinSession(join.session());
            case SessionStart.None _ -> {
            }
        }
        return renderer;
    }
}
