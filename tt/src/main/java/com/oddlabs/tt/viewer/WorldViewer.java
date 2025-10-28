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
import com.oddlabs.tt.landscape.TreeSupply;
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
import com.oddlabs.tt.render.Picker;
import com.oddlabs.tt.render.RenderQueues;
import com.oddlabs.tt.resource.WorldGenerator;
import com.oddlabs.tt.resource.WorldInfo;
import com.oddlabs.tt.util.ServerMessageBundler;
import com.oddlabs.tt.util.StateChecksum;
import com.oddlabs.tt.util.Target;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;
import org.lwjgl.util.vector.Matrix4f;

import java.util.ResourceBundle;

public final class WorldViewer implements Animated {

    private final static String[] GAMESPEED_STRINGS = new String[]{"paused", "slow", "normal", "fast", "ludicrous"};

    private final @NonNull GameCamera camera;
    private final @NonNull ActionButtonPanel panel;
    private final @NonNull SelectionDelegate delegate;
    private final @NonNull DistributableTable distributable_table;
    private final @NonNull PeerHub peerhub;
    private final GUIRoot gui_root;
    private final @NonNull NotificationManager notification_manager;
    private final InGameInfo ingame_info;
    private final NetworkSelector network;
    private final @NonNull Selection selection;
    private final @NonNull World world;
    private final @NonNull Picker picker;
    private final @NonNull DefaultRenderer renderer;
    private final @NonNull LandscapeRenderer landscape_renderer;
    private final Player local_player;
    private final @NonNull Cheat cheat;
    private final WorldParameters world_params;
    private final @NonNull AnimationManager animation_manager_local;

    public WorldViewer(@NonNull NetworkSelector network, final @NonNull GUIRoot gui_root, @NonNull WorldParameters world_params, @NonNull InGameInfo ingame_info, @NonNull WorldGenerator generator, PlayerSlot @NonNull [] player_slots, UnitInfo[] unit_infos, float[][] colors, short player_slot, SessionID session_id) {
        this.world_params = world_params;
        this.ingame_info = ingame_info;
        this.network = network;
        this.notification_manager = new NotificationManager(gui_root);
        this.cheat = new Cheat(!ingame_info.isMultiplayer());
        this.animation_manager_local = new AnimationManager();
        final CameraState camera_state = new CameraState();
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
                String result = "";
                Player[] players = world.getPlayers();
                int count = 0;
                for (Player player : players) {
                    int preferred_gamespeed = player.getPreferredGamespeed();
                    if (World.isValidGamespeed(preferred_gamespeed)) {
                        if (count > 0)
                            result += ", ";
                        count++;
                        result += player.getPlayerInfo().getName() + ": " + ServerMessageBundler.getGamespeedString(preferred_gamespeed);
                    }
                }
                if (count > 0 && isMultiplayer())
                    gui_root.getInfoPrinter().print(result);
            }

            @Override
            public void newAttackNotification(@NonNull Selectable target) {
                Player owner = target.getOwner();
                if (owner == getLocalPlayer())
                    notification_manager.newAttackNotification(animation_manager_local, target, getLocalPlayer());
            }

            @Override
            public void newSelectableNotification(@NonNull Selectable target) {
                Player owner = target.getOwner();
                if (owner == getLocalPlayer())
                    notification_manager.newSelectableNotification(target, animation_manager_local, getLocalPlayer());
            }

            @Override
            public void registerTarget(Target target) {
                distributable_table.register(target);
            }

            @Override
            public void unregisterTarget(Target target) {
                distributable_table.unregister(target);
                if (target instanceof Selectable)
                    getSelection().removeFromArmies((Selectable) target);
            }

            @Override
            public void updateTreeLowDetail(@NonNull Matrix4f matrix, @NonNull TreeSupply tree) {
                getRenderer().getTreeRenderer().getLowDetail().updateLowDetail(matrix, tree);
            }

            @Override
            public void patchesEdited(int patch_x0, int patch_y0, int patch_x1, int patch_y1) {
                getLandscapeRenderer().patchesEdited(patch_x0, patch_y0, patch_x1, patch_y1);
            }
        };
        PlayerInfo[] player_infos = new PlayerInfo[player_slots.length];
        for (int i = 0; i < player_slots.length; i++) {
            player_infos[i] = player_slots[i].getInfo();
        }
        WorldInfo world_info = generator.generate(player_slots.length, world_params.getInitialUnitCount(), ingame_info.getRandomStartPosition());
        this.world = World.newWorld(audio_impl, landscape_resources, races_resources, LandscapeResources.loadTreeLowDetails(), listener, world_params, world_info, generator.getTerrainType(), player_infos, colors);
        this.local_player = world.getPlayers()[player_slot];
        this.selection = new Selection(local_player);
        landscape_renderer = new LandscapeRenderer(world, world_info, gui_root, animation_manager_local);
        this.picker = new Picker(animation_manager_local, local_player, render_queues, landscape_renderer, selection);
        this.renderer = new DefaultRenderer(cheat, local_player, render_queues, generator.getTerrainType(), world_info, landscape_renderer, picker, selection, generator);
        this.gui_root = gui_root;
        this.peerhub = new PeerHub(animation_manager_local, ingame_info.isMultiplayer(), ingame_info.isRated(), local_player, player_slots, network, gui_root, notification_manager, distributable_table, session_id, new ViewerStallHandler(this));
        this.camera = new GameCamera(this, camera_state);
        this.panel = new ActionButtonPanel(this, camera);
        this.delegate = new SelectionDelegate(this, camera);
        camera.reset(getLocalPlayer().getStartX(), getLocalPlayer().getStartY());
        initPlayers(world_info.starting_locations, player_slots, world.getPlayers(), unit_infos, world_params.getInitialGameSpeed());
        LocalEventQueue.getQueue().getManager().registerAnimation(this);
    }

    public AnimationManager getAnimationManagerLocal() {
        return animation_manager_local;
    }

    @Override
    public void animate(float t) {
        animation_manager_local.runAnimations(t);
    }

    @Override
    public void updateChecksum(StateChecksum sum) {
    }

    public WorldParameters getParameters() {
        return world_params;
    }

    public Cheat getCheat() {
        return cheat;
    }

    public void setPaused(boolean p) {
        peerhub.setPaused(p);
    }

    public Player getLocalPlayer() {
        return local_player;
    }

    private void initPlayer(@NonNull ResourceBundle bundle, float[] starting_location, @NonNull PlayerSlot slot, @NonNull Player player, @NonNull UnitInfo unit_info, int initial_gamespeed) {
        if (slot.getType() == PlayerSlot.AI) {
            AI ai = null;
            switch (slot.getAIDifficulty()) {
                case PlayerSlot.AI_NORMAL:
                    ai = new AdvancedAI(player, unit_info, AdvancedAI.DIFFICULTY_NORMAL);
                    break;
                case PlayerSlot.AI_HARD:
                    ai = new AdvancedAI(player, unit_info, AdvancedAI.DIFFICULTY_HARD);
                    break;
                case PlayerSlot.AI_EASY:
                    ai = new AdvancedAI(player, unit_info, AdvancedAI.DIFFICULTY_EASY);
                    break;
                case PlayerSlot.AI_BATTLE_TUTORIAL:
                    ai = new PassiveAI(player, unit_info, true);
                    break;
                case PlayerSlot.AI_TOWER_TUTORIAL:
                    break;
                case PlayerSlot.AI_CHIEFTAIN_TUTORIAL:
                    new Unit(player, 100, 100, null, player.getRace().getUnitTemplate(Race.UNIT_PEON));
                    new Unit(player, 200, 100, null, player.getRace().getUnitTemplate(Race.UNIT_PEON));
                    new Unit(player, 40, 200, null, player.getRace().getUnitTemplate(Race.UNIT_PEON));
                    break;
                case PlayerSlot.AI_PASSIVE_CAMPAIGN:
                    ai = new PassiveAI(player, unit_info, true);
                    break;
                case PlayerSlot.AI_NEUTRAL_CAMPAIGN:
                    ai = new PassiveAI(player, unit_info, false);
                    break;
                default:
                    throw new RuntimeException();
            }
            player.setAI(ai);
        } else {
            player.setPreferredGamespeed(initial_gamespeed);
            int i = 0;
            for (int j = 0; j < unit_info.getNumPeons(); j++, i++) {
                new Unit(player, starting_location[2 * i], starting_location[2 * i + 1], null, player.getRace().getUnitTemplate(Race.UNIT_PEON));
            }
            for (int j = 0; j < unit_info.getNumRockWarriors(); j++, i++) {
                new Unit(player, starting_location[2 * i], starting_location[2 * i + 1], null, player.getRace().getUnitTemplate(Race.UNIT_WARRIOR_ROCK));
            }
            for (int j = 0; j < unit_info.getNumIronWarriors(); j++, i++) {
                new Unit(player, starting_location[2 * i], starting_location[2 * i + 1], null, player.getRace().getUnitTemplate(Race.UNIT_WARRIOR_IRON));
            }
            for (int j = 0; j < unit_info.getNumRubberWarriors(); j++, i++) {
                new Unit(player, starting_location[2 * i], starting_location[2 * i + 1], null, player.getRace().getUnitTemplate(Race.UNIT_WARRIOR_RUBBER));
            }
            if (unit_info.hasChieftain()) {
                Unit chieftain;
                if (player.getRace().getChieftainAI() instanceof VikingChieftainAI)
                    chieftain = new Unit(player, starting_location[2 * i], starting_location[2 * i + 1], null, player.getRace().getUnitTemplate(Race.UNIT_CHIEFTAIN), Utils.getBundleString(bundle, "chieftain_name"), false);
                else
                    if (player.getRace().getChieftainAI() instanceof NativeChieftainAI)
                        chieftain = new Unit(player, starting_location[2 * i], starting_location[2 * i + 1], null, player.getRace().getUnitTemplate(Race.UNIT_CHIEFTAIN), Utils.getBundleString(bundle, "native_chieftain_name"), false);
                    else
                        throw new RuntimeException();
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

    private LandscapeRenderer getLandscapeRenderer() {
        return landscape_renderer;
    }

    public Picker getPicker() {
        return picker;
    }

    public DefaultRenderer getRenderer() {
        return renderer;
    }

    public World getWorld() {
        return world;
    }

    public NetworkSelector getNetwork() {
        return network;
    }

    public Selection getSelection() {
        return selection;
    }

    public NotificationManager getNotificationManager() {
        return notification_manager;
    }

    public DistributableTable getDistributableTable() {
        return distributable_table;
    }

    public GUIRoot getGUIRoot() {
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

    public void close() {
        LocalEventQueue.getQueue().getManager().removeAnimation(this);
        if (peerhub != null)
            peerhub.close();
        ingame_info.close(this);
    }

    public GameCamera getCamera() {
        return camera;
    }

    public PeerHub getPeerHub() {
        return peerhub;
    }

    public ActionButtonPanel getPanel() {
        return panel;
    }

    public SelectionDelegate getDelegate() {
        return delegate;
    }
}
