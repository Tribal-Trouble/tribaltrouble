package com.oddlabs.tt.form;

import com.oddlabs.matchmaking.MatchmakingServerInterface;
import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.delegate.Menu;
import com.oddlabs.tt.gui.GUIObject;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.guievent.MouseClickListener;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.landscape.WorldParameters;
import com.oddlabs.tt.net.GameNetwork;
import com.oddlabs.tt.net.PlayerSlot;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.viewer.DefaultInGameInfo;

import java.io.File;
import java.util.ResourceBundle;

/**
 * Minimal modal to configure players for testing the current editor map.
 * Reuses the Single Player players section and starts via the same pipeline.
 */
public final class TestMapForm extends Form {
    private final GUIRoot guiRoot;
    private final World world;
    private final int terrainType;
    private final NetworkSelector network;
    private final PlayersSection players;

    public TestMapForm(GUIRoot guiRoot, NetworkSelector network, World world, int terrainType) {
        super("Test Map");
        this.guiRoot = guiRoot;
        this.world = world;
        this.terrainType = terrainType;
        this.network = network;

    Label header = new Label("Configure players, teams, and races", Skin.getSkin().getHeadlineFont());
        addChild(header);
        header.place();

        players = new PlayersSection(guiRoot);
        addChild(players);
    players.place(header, GUIObject.BOTTOM_LEFT, Skin.getSkin().getFormData().getSectionSpacing());

        HorizButton startBtn = new HorizButton("Start Test", 140);
        HorizButton cancelBtn = new HorizButton("Cancel", 110);
        addChild(startBtn);
        addChild(cancelBtn);
    cancelBtn.place(players, GUIObject.BOTTOM_RIGHT, Skin.getSkin().getFormData().getSectionSpacing());
    startBtn.place(cancelBtn, GUIObject.LEFT_MID);

        startBtn.addMouseClickListener(new MouseClickListener() {
            public void mouseClicked(int button, int x, int y, int clicks) {
                startTest();
            }
        });
        cancelBtn.addMouseClickListener(new MouseClickListener() {
            public void mouseClicked(int button, int x, int y, int clicks) {
                TestMapForm.this.remove();
            }
        });

        compileCanvas();
        centerPos();
    }

    private void startTest() {
        try {
            // Basic validation: require at least two teams among active slots
            int count = players.getPlayerCount();
            int team0 = players.getTeamIndex(0);
            boolean hasEnemy = false;
            for (int i = 1; i < count; i++) {
                if (players.isAI(i) && players.getTeamIndex(i) != team0) {
                    hasEnemy = true;
                    break;
                }
            }
            if (!hasEnemy) {
                guiRoot.getInfoPrinter().print("Need at least two teams to start a test.");
                return;
            }
            // Export current world to a temp .ttmap
            File dir = com.oddlabs.tt.mapio.MapIO.mapsDir();
            File file = new File(dir, "editor_map.ttmap");
            com.oddlabs.tt.mapio.MapIO.saveEditorWorld(world, terrainType, file);
            guiRoot.getInfoPrinter().print("Exported test map: " + file.getName());

            int meters = world.getHeightMap().getMetersPerWorld();
            int gamespeed = world.getGamespeed();

            GameNetwork game_network =
                    Menu.startNewGameWithMap(
                            network,
                            guiRoot,
                            null,
                            new WorldParameters(
                                    gamespeed,
                                    "",
                                    Player.INITIAL_UNIT_COUNT,
                                    Player.DEFAULT_MAX_UNIT_COUNT),
                            new DefaultInGameInfo(),
                            new Menu.DefaultWorldInitAction(),
                            null,
                            meters,
                            terrainType,
                            .5f,
                            .5f,
                            .5f,
                            1337,
                            false,
                            generateAINames(),
                            players.getPlayerCount(),
                            file);

            // Apply player slots per PlayersSection state
            // Slot 0: local human
            game_network
                    .getClient()
                    .getServerInterface()
                    .setPlayerSlot(
                            0,
                            PlayerSlot.HUMAN,
                            players.getRaceIndex(0),
                            players.getTeamIndex(0),
                            true,
                            PlayerSlot.AI_NONE);

            for (int i = 1; i < players.getPlayerCount(); i++) {
                if (players.isAI(i)) {
                    game_network
                            .getClient()
                            .getServerInterface()
                            .setPlayerSlot(
                                    i,
                                    PlayerSlot.AI,
                                    players.getRaceIndex(i),
                                    players.getTeamIndex(i),
                                    true,
                                    players.getAIDifficultyIndex(i));
                } else if (players.isClosed(i)) {
                    // leave closed
                } else {
                    // Open human (rare in single-player test), treat as closed for now
                }
            }
            game_network.getClient().getServerInterface().startServer();
            guiRoot.getInfoPrinter().print("Launching test game...");
            remove();
        } catch (Throwable t) {
            guiRoot.getInfoPrinter().print("Test failed: " + t.getMessage());
        }
    }

    /** Creates an array of translated AI names based on the max player limit. */
    private String[] generateAINames() {
        ResourceBundle bundle = ResourceBundle.getBundle(TerrainMenu.class.getName());
        String ai_string = com.oddlabs.tt.util.Utils.getBundleString(bundle, "ai");
        String[] ai_names = new String[MatchmakingServerInterface.MAX_PLAYERS];
        for (int i = 0; i < MatchmakingServerInterface.MAX_PLAYERS; i++) {
            ai_names[i] = ai_string + i;
        }
        return ai_names;
    }
}
