package com.oddlabs.tt.viewer;

import com.oddlabs.net.NetworkSelector;
import com.oddlabs.router.SessionID;
import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.audio.AudioManager;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.camera.GameCamera;
import com.oddlabs.tt.delegate.GameStatsDelegate;
import com.oddlabs.tt.delegate.InGameMainMenu;
import com.oddlabs.tt.delegate.SelectionDelegate;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.gui.ActionButtonPanel;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.landscape.AudioImplementation;
import com.oddlabs.tt.landscape.LandscapeResources;
import com.oddlabs.tt.landscape.NotificationListener;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.landscape.WorldParameters;
import com.oddlabs.tt.model.Race;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.net.DistributableTable;
import com.oddlabs.tt.net.PeerHub;
import com.oddlabs.tt.net.PlayerSlot;
import com.oddlabs.tt.player.AI;
import com.oddlabs.tt.player.AdvancedAI;
import com.oddlabs.tt.player.NativeChieftainAI;
import com.oddlabs.tt.player.PassiveAI;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.player.PlayerInfo;
import com.oddlabs.tt.player.UnitInfo;
import com.oddlabs.tt.player.VikingChieftainAI;
import com.oddlabs.tt.render.DefaultRenderer;
import com.oddlabs.tt.render.LandscapeRenderer;
import com.oddlabs.tt.render.MatrixStack;
import com.oddlabs.tt.render.Picker;
import com.oddlabs.tt.render.RenderQueues;
import com.oddlabs.tt.resource.FogInfo;
import com.oddlabs.tt.resource.WorldGenerator;
import com.oddlabs.tt.resource.WorldInfo;
import com.oddlabs.tt.util.ServerMessageBundler;
import com.oddlabs.tt.util.Target;
import com.oddlabs.tt.util.Utils;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public final class WorldViewer implements Animated, AutoCloseable {

    private static final String[] GAMESPEED_STRINGS = new String[]{"paused", "slow", "normal", "fast", "ludicrous"};

    private final @NonNull GameCamera camera;
    private final @NonNull ActionButtonPanel panel;
    private final @NonNull SelectionDelegate delegate;
    private final @NonNull DistributableTable distributable_table;
    private final @NonNull PeerHub peerhub;
    private final @NonNull GUIRoot gui_root;
    private final @NonNull NotificationManager notification_manager;
    private final @NonNull InGameInfo ingame_info;
    private final @NonNull NetworkSelector network;
    private final @NonNull Selection selection;
    private final @NonNull World world;
    private final @NonNull Picker picker;
    private final @NonNull DefaultRenderer renderer;
    private final @NonNull LandscapeRenderer landscape_renderer;
    private final @NonNull Player local_player;
    private final @NonNull Cheat cheat;
    private final @NonNull WorldParameters world_params;
    private final @NonNull AnimationManager animation_manager_local;

    public WorldViewer(@NonNull NetworkSelector network, final @NonNull GUIRoot gui_root, @NonNull WorldParameters world_params, @NonNull InGameInfo ingame_info, @NonNull WorldGenerator generator, PlayerSlot @NonNull [] player_slots, UnitInfo[] unit_infos, short player_slot, SessionID session_id) {
        this.world_params = world_params;
        this.ingame_info = ingame_info;
        this.network = network;
        this.notification_manager = new NotificationManager(gui_root);
        this.cheat = new Cheat(!ingame_info.isMultiplayer());
        this.animation_manager_local = new AnimationManager();
        final FogInfo worldFog = generator.getFogInfo();
        final CameraState camera_state = new CameraState(worldFog);
        MatrixStack modelViewStack = new MatrixStack();
        MatrixStack projectionStack = new MatrixStack();
        RenderQueues render_queues = new RenderQueues();
        LandscapeResources landscape_resources = World.loadCommon(render_queues);
        RacesResources races_resources = World.loadInGame(render_queues);
        AudioImplementation audio_impl = (AudioParameters<?> params) -> AudioManager.getManager().newAudio(camera_state, params);
        this.distributable_table = new DistributableTable();
        NotificationListener listener = new NotificationListener() {
            @Override
            public void gamespeedChanged(int speed) {
                gui_root.getInfoPrinter().print(Utils.getBundleString(PeerHub.bundle, "changed_to_" + GAMESPEED_STRINGS[speed]));
                Globals.gamespeed = speed;
            }

            @Override
            public void playerGamespeedChanged() {
                String result = Arrays.stream(world.getPlayers())
                        .filter(p -> World.isValidGamespeed(p.getPreferredGamespeed()))
                        .map(p -> p.getPlayerInfo().getName() + ": " + ServerMessageBundler.getGamespeedString(p.getPreferredGamespeed()))
                        .collect(Collectors.joining(", "));
                if (!result.isEmpty() && isMultiplayer())
                    gui_root.getInfoPrinter().print(result);
            }

            @Override
            public void newAttackNotification(@NonNull Selectable<?> target) {
                Player owner = target.getOwner();
                if (owner == getLocalPlayer())
                    notification_manager.newAttackNotification(animation_manager_local, target, getLocalPlayer());
            }

            @Override
            public void newSelectableNotification(@NonNull Selectable<?> target) {
                Player owner = target.getOwner();
                if (owner == getLocalPlayer())
                    notification_manager.newSelectableNotification(target, animation_manager_local, getLocalPlayer());
            }

            @Override
            public void registerTarget(@NonNull Target target) {
                distributable_table.register(target);
            }

            @Override
            public void unregisterTarget(@NonNull Target target) {
                distributable_table.unregister(target);
                if (target instanceof Selectable<?> selectable)
                    getSelection().removeFromArmies(selectable);
            }
        };
        PlayerInfo[] player_infos = Arrays.stream(player_slots).map(PlayerSlot::getInfo).toArray(PlayerInfo[]::new);
        WorldInfo world_info = generator.generate(player_infos.length, world_params.getInitialUnitCount(), ingame_info.getRandomStartPosition());
        this.world = World.newWorld(audio_impl, landscape_resources, races_resources, listener, world_params, world_info, generator.getTerrainType(), player_infos, worldFog);
        this.local_player = world.getPlayers()[player_slot];
        this.selection = new Selection(local_player);
        landscape_renderer = new LandscapeRenderer(world, world_info, animation_manager_local);
        this.picker = new Picker(animation_manager_local, local_player, gui_root, render_queues, landscape_renderer, selection);
        this.renderer = new DefaultRenderer(cheat, local_player, render_queues, world_info, landscape_renderer, picker, selection, generator, modelViewStack, projectionStack);
        this.gui_root = gui_root;
        this.peerhub = new PeerHub(animation_manager_local, ingame_info.isMultiplayer(), ingame_info.isRated(), local_player, player_slots, network, gui_root, notification_manager, distributable_table, session_id, new ViewerStallHandler(this));
        this.camera = new GameCamera(this, camera_state);
        this.panel = new ActionButtonPanel(this, camera);
        this.delegate = new SelectionDelegate(this, camera);
        camera.reset(getLocalPlayer().getStartX(), getLocalPlayer().getStartY());
        initPlayers(world_info.starting_locations, player_slots, world.getPlayers(), unit_infos, world_params.getInitialGameSpeed());
        LocalEventQueue.getQueue().getManager().registerAnimation(this);
    }

    public @NonNull AnimationManager getAnimationManagerLocal() {
        return animation_manager_local;
    }

    @Override
    public void animate(float t) {
        animation_manager_local.runAnimations(t);
    }

    @Override
    public void close() {
        LocalEventQueue.getQueue().getManager().removeAnimation(this);
        peerhub.close();
        ingame_info.close(this);
    }

    public @NonNull WorldParameters getParameters() {
        return world_params;
    }

    public @NonNull Cheat getCheat() {
        return cheat;
    }

    public void setPaused(boolean p) {
        peerhub.setPaused(p);
    }

    public @NonNull Player getLocalPlayer() {
        return local_player;
    }

    private void initPlayer(@NonNull ResourceBundle bundle, float[] starting_location, @NonNull PlayerSlot slot, @NonNull Player player, @NonNull UnitInfo unit_info, int initial_gamespeed) {
        if (slot.getType() == PlayerSlot.AI) {
            AI ai = null;
            switch (slot.getAIDifficulty()) {
                case PlayerSlot.AI_NORMAL -> ai = new AdvancedAI(player, unit_info, AdvancedAI.DIFFICULTY_NORMAL);
                case PlayerSlot.AI_HARD -> ai = new AdvancedAI(player, unit_info, AdvancedAI.DIFFICULTY_HARD);
                case PlayerSlot.AI_EASY -> ai = new AdvancedAI(player, unit_info, AdvancedAI.DIFFICULTY_EASY);
                case PlayerSlot.AI_BATTLE_TUTORIAL -> ai = new PassiveAI(player, unit_info, true);
                case PlayerSlot.AI_TOWER_TUTORIAL -> {
                }
                case PlayerSlot.AI_CHIEFTAIN_TUTORIAL -> {
                    new Unit(player, 100, 100, null, player.getRace().getUnitTemplate(Race.UNIT_PEON));
                    new Unit(player, 200, 100, null, player.getRace().getUnitTemplate(Race.UNIT_PEON));
                    new Unit(player, 40, 200, null, player.getRace().getUnitTemplate(Race.UNIT_PEON));
                }
                case PlayerSlot.AI_PASSIVE_CAMPAIGN -> ai = new PassiveAI(player, unit_info, true);
                case PlayerSlot.AI_NEUTRAL_CAMPAIGN -> ai = new PassiveAI(player, unit_info, false);
                default -> throw new IllegalArgumentException("unexpected difficulty: " + slot.getAIDifficulty());
            }
            player.setAI(ai);
        } else {
            player.setPreferredGamespeed(initial_gamespeed);
            int i = 0;
            for (int j = 0; j < unit_info.numPeons(); j++, i++) {
                new Unit(player, starting_location[2 * i], starting_location[2 * i + 1], null, player.getRace().getUnitTemplate(Race.UNIT_PEON));
            }
            for (int j = 0; j < unit_info.numRockWarriors(); j++, i++) {
                new Unit(player, starting_location[2 * i], starting_location[2 * i + 1], null, player.getRace().getUnitTemplate(Race.UNIT_WARRIOR_ROCK));
            }
            for (int j = 0; j < unit_info.numIronWarriors(); j++, i++) {
                new Unit(player, starting_location[2 * i], starting_location[2 * i + 1], null, player.getRace().getUnitTemplate(Race.UNIT_WARRIOR_IRON));
            }
            for (int j = 0; j < unit_info.numRubberWarriors(); j++, i++) {
                new Unit(player, starting_location[2 * i], starting_location[2 * i + 1], null, player.getRace().getUnitTemplate(Race.UNIT_WARRIOR_RUBBER));
            }
            if (unit_info.hasChieftain()) {
                Unit chieftain;
                if (player.getRace().getChieftainAI() instanceof VikingChieftainAI)
                    chieftain = new Unit(player, starting_location[2 * i], starting_location[2 * i + 1], null, player.getRace().getUnitTemplate(Race.UNIT_CHIEFTAIN), Utils.getBundleString(bundle, "chieftain_name"), false);
                else if (player.getRace().getChieftainAI() instanceof NativeChieftainAI)
                        chieftain = new Unit(player, starting_location[2 * i], starting_location[2 * i + 1], null, player.getRace().getUnitTemplate(Race.UNIT_CHIEFTAIN), Utils.getBundleString(bundle, "native_chieftain_name"), false);
                else
                    throw new RuntimeException("Unknown chieftain AI");
                chieftain.increaseMagicEnergy(0, 1000);
                chieftain.increaseMagicEnergy(1, 1000);
                player.setActiveChieftain(chieftain);
                i++;
            }
        }
    }

    private void initPlayers(float[][] starting_locations, PlayerSlot @NonNull [] slots, Player[] players, UnitInfo[] unit_infos, int initial_gamespeed) {
        ResourceBundle bundle = ResourceBundle.getBundle(Player.class.getName());
        for (int i = 0; i < slots.length; i++) {
            initPlayer(bundle, starting_locations[i], slots[i], players[i], unit_infos[i], initial_gamespeed);
        }
    }

    private @NonNull LandscapeRenderer getLandscapeRenderer() {
        return landscape_renderer;
    }

    public @NonNull Picker getPicker() {
        return picker;
    }

    public @NonNull DefaultRenderer getRenderer() {
        return renderer;
    }

    public @NonNull World getWorld() {
        return world;
    }

    public @NonNull NetworkSelector getNetwork() {
        return network;
    }

    public @NonNull Selection getSelection() {
        return selection;
    }

    public @NonNull NotificationManager getNotificationManager() {
        return notification_manager;
    }

    public @NonNull DistributableTable getDistributableTable() {
        return distributable_table;
    }

    public @NonNull GUIRoot getGUIRoot() {
        return gui_root;
    }

    public boolean isMultiplayer() {
        return ingame_info.isMultiplayer();
    }

    public void abort() {
        ingame_info.abort(this);
    }

    public void addGameOverGUI(GameStatsDelegate delegate, int header_y, Group buttons) {
        ingame_info.addGameOverGUI(this, delegate, header_y, buttons);
    }

    public void addGUI(InGameMainMenu menu, Group game_infos) {
        ingame_info.addGUI(this, menu, game_infos);
    }

    public @NonNull GameCamera getCamera() {
        return camera;
    }

    public @NonNull PeerHub getPeerHub() {
        return peerhub;
    }

    public @NonNull ActionButtonPanel getPanel() {
        return panel;
    }

    public @NonNull SelectionDelegate getDelegate() {
        return delegate;
    }
}
