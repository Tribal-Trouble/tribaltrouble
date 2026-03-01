package com.oddlabs.tt.net;

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
import com.oddlabs.tt.viewer.InGameInfo;
import com.oddlabs.tt.viewer.WorldViewer;

import java.util.ArrayList;
import java.util.List;

final strictfp class ReplayWorldStarter implements LoadCallback {
    private final UnitInfo[] unit_infos;
    private final PlayerSlot[] player_slots;
    private final short player_slot;
    private final InGameInfo ingame_info;
    private final NetworkSelector network;
    private final WorldGenerator generator;
    private final WorldParameters world_params;
    private final WorldInitAction initial_action;
    private final int session_id;
    private final byte[] event_log_data;
    private final int target_tick;

    ReplayWorldStarter(
            NetworkSelector network,
            int session_id,
            WorldGenerator generator,
            WorldParameters world_params,
            PlayerSlot[] player_slots,
            UnitInfo[] unit_infos,
            short player_slot,
            InGameInfo ingame_info,
            WorldInitAction initial_action,
            byte[] event_log_data,
            int target_tick) {
        this.initial_action = initial_action;
        this.session_id = session_id;
        this.world_params = world_params;
        this.generator = generator;
        this.unit_infos = unit_infos;
        this.player_slots = player_slots;
        this.player_slot = player_slot;
        this.ingame_info = ingame_info;
        this.network = network;
        this.event_log_data = event_log_data;
        this.target_tick = target_tick;
    }

    public final UIRenderer load(GUIRoot gui_root) {
        AnimationManager.freezeTime();
        List player_slot_list = new ArrayList();
        List unit_info_list = new ArrayList();
        List color_list = new ArrayList();
        short corrected_player_slot = -1;
        for (short i = 0; i < player_slots.length; i++) {
            if (player_slots[i].getInfo() != null) {
                if (player_slot == i) corrected_player_slot = (short) player_slot_list.size();
                player_slot_list.add(player_slots[i]);
                unit_info_list.add(unit_infos[i]);
                color_list.add(Player.COLORS[i]);
            }
        }
        assert corrected_player_slot != -1;
        PlayerSlot[] player_slots = (PlayerSlot[]) player_slot_list.toArray(new PlayerSlot[0]);
        UnitInfo[] corrected_unit_infos = (UnitInfo[]) unit_info_list.toArray(new UnitInfo[0]);
        float[][] corrected_colors = (float[][]) color_list.toArray(new float[0][]);
        WorldViewer viewer =
                new WorldViewer(
                        network,
                        gui_root,
                        world_params,
                        ingame_info,
                        generator,
                        player_slots,
                        corrected_unit_infos,
                        corrected_colors,
                        corrected_player_slot,
                        new SessionID(session_id));
        if (initial_action != null) initial_action.run(viewer);

        // Fast-forward the simulation to catch up
        if (event_log_data != null && event_log_data.length > 0 && target_tick > 0) {
            viewer.getPeerHub().fastForward(event_log_data, target_tick);
        }

        System.out.println("ReplayWorldStarter complete (session_id = " + session_id + ")");
        return viewer.getRenderer();
    }
}
