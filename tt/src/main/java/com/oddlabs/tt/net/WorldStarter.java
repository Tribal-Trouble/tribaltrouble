package com.oddlabs.tt.net;

import com.oddlabs.matchmaking.GamePlayer;
import com.oddlabs.matchmaking.GameSession;
import com.oddlabs.matchmaking.Participant;
import com.oddlabs.matchmaking.PlayerTypes;
import com.oddlabs.net.NetworkSelector;
import com.oddlabs.router.SessionID;
import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.form.LoadCallback;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.landscape.WorldParameters;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.player.UnitInfo;
import com.oddlabs.tt.render.UIRenderer;
import com.oddlabs.tt.resource.WorldGenerator;
import com.oddlabs.tt.steam.SteamManager;
import com.oddlabs.tt.viewer.InGameInfo;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

final class WorldStarter implements LoadCallback {
    private final UnitInfo[] unit_infos;
    private final PlayerSlot[] player_slots;
    private final short player_slot;
    private final InGameInfo ingame_info;
    private final NetworkSelector network;
    private final WorldGenerator generator;
    private final WorldParameters world_params;
    private final @Nullable WorldInitAction initial_action;
    private final int session_id;

    WorldStarter(NetworkSelector network, int session_id, WorldGenerator generator, WorldParameters world_params,
            PlayerSlot[] player_slots, UnitInfo[] unit_infos, short player_slot, InGameInfo ingame_info,
            @Nullable WorldInitAction initial_action) {
        this.initial_action = initial_action;
        this.session_id = session_id;
        this.world_params = world_params;
        this.generator = generator;
        this.unit_infos = unit_infos;
        this.player_slots = player_slots;
        this.player_slot = player_slot;
        this.ingame_info = ingame_info;
        this.network = network;
    }

    @Override
    public @NonNull UIRenderer load(@NonNull GUIRoot gui_root) {
        AnimationManager.freezeTime();
        List<PlayerSlot> player_slot_list = new ArrayList<>();
        List<UnitInfo> unit_info_list = new ArrayList<>();
        short corrected_player_slot = -1;
        for (short i = 0; i < player_slots.length; i++) {
            if (player_slots[i].getInfo() != null) {
                if (player_slot == i)
                    corrected_player_slot = (short) player_slot_list.size();
                player_slot_list.add(player_slots[i]);
                unit_info_list.add(unit_infos[i]);
            }
        }
        assert corrected_player_slot != -1;
        PlayerSlot[] player_slots = player_slot_list.toArray(new PlayerSlot[0]);
        UnitInfo[] corrected_unit_infos = unit_info_list.toArray(new UnitInfo[0]);
        WorldViewer viewer = new WorldViewer(network, gui_root, world_params, ingame_info, generator, player_slots,
                corrected_unit_infos, corrected_player_slot, new SessionID(session_id));
        if (initial_action != null)
            initial_action.run(viewer);
        Participant[] participants = getParticipants(viewer, player_slots);
        GamePlayer[] gamePlayers = getGamePlayers(viewer, player_slots);
        if (Network.getMatchmakingClient().isConnected()) {
            GameSession game_session = new GameSession(session_id, participants, ingame_info.isRated(), gamePlayers);
            Network.getMatchmakingClient().getInterface().gameStartedNotify(game_session);
            sendWorldParams(player_slots, corrected_unit_infos);
            SteamManager.setMultiplayerInGameRichPresence();
        }
        SteamManager.setInActiveWorld(true);
        IO.println("PeerHub created (session_id = " + session_id + ") Player list:");
        return viewer.getRenderer();
    }

    private void sendWorldParams(PlayerSlot[] player_slots, UnitInfo[] unit_infos) {
        try {
            var baos = new java.io.ByteArrayOutputStream();
            var oos = new java.io.ObjectOutputStream(baos);
            oos.writeObject(generator);
            oos.writeObject(world_params);
            oos.writeObject(player_slots);
            oos.writeObject(unit_infos);
            oos.writeFloat(ingame_info.getRandomStartPosition());
            oos.writeInt(session_id);
            oos.close();
            IO.println("sendWorldParams: serialized " + baos.size() + " bytes, sending to server");
            Network.getMatchmakingClient().getInterface().updateWorldParams(baos.toByteArray());
        } catch (Exception e) {
            IO.println("Exception serializing world params: " + e);
            e.printStackTrace();
        }
    }

    private static @NonNull Participant @NonNull [] getParticipants(@NonNull WorldViewer viewer,
            @NonNull PlayerSlot @NonNull [] player_slots) {
        List<Participant> participant_list = new ArrayList<>();
        Player[] players = viewer.getWorld().getPlayers();
        for (short i = 0; i < players.length; i++) {
            if (player_slots[i].getType() != PlayerSlot.HUMAN)
                continue;
            Player player = players[i];
            int host_id = player_slots[i].getAddress() != null ? player_slots[i].getAddress().getHostID() : -1;
            participant_list.add(new Participant(host_id, player.getPlayerInfo().getName(),
                    player.getPlayerInfo().getTeam(), player.getPlayerInfo().getRace()));
        }
        return participant_list.toArray(new Participant[0]);
    }

    private static @NonNull GamePlayer @NonNull [] getGamePlayers(@NonNull WorldViewer viewer,
            @NonNull PlayerSlot @NonNull [] player_slots) {
        Player[] players = viewer.getWorld().getPlayers();
        GamePlayer[] gamePlayers = new GamePlayer[players.length];
        for (short i = 0; i < players.length; i++) {
            Player player = players[i];
            PlayerTypes playerType = mapPlayerType(player_slots[i]);
            String name = player_slots[i].getType() == PlayerSlot.HUMAN ? player.getPlayerInfo().getName() : null;
            gamePlayers[i] = new GamePlayer(name, player.getPlayerInfo().getTeam(), player.getPlayerInfo().getRace(),
                    playerType);
        }
        return gamePlayers;
    }

    private static @NonNull PlayerTypes mapPlayerType(@NonNull PlayerSlot slot) {
        if (slot.getType() == PlayerSlot.HUMAN) return PlayerTypes.Human;
        return switch (slot.getAIDifficulty()) {
            case PlayerSlot.AI_EASY -> PlayerTypes.AIEasy;
            case PlayerSlot.AI_NORMAL -> PlayerTypes.AINormal;
            case PlayerSlot.AI_HARD -> PlayerTypes.AIHard;
            default -> PlayerTypes.None;
        };
    }
}
