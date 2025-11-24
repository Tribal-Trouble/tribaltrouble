package com.oddlabs.tt.viewer;

import com.oddlabs.tt.delegate.GameStatsDelegate;
import com.oddlabs.tt.delegate.InGameMainMenu;
import com.oddlabs.tt.delegate.Menu;
import com.oddlabs.tt.form.TerrainMenu;
import com.oddlabs.tt.gui.GUIObject;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.player.PlayerInfo;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;

import static com.oddlabs.tt.gui.GUIObject.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.GUIObject.Placement.LEFT_MID;
import static com.oddlabs.tt.gui.GUIObject.Placement.RIGHT_TOP;

public class DefaultInGameInfo implements InGameInfo {
	private final ResourceBundle terrain_menu_bundle = ResourceBundle.getBundle(TerrainMenu.class.getName());
	private boolean replay_island_flag;

	private void addAbortButton(@NonNull InGameMainMenu menu) {
		String abort_text = Utils.getBundleString(Menu.bundle, "end_game");
		menu.addAbortButton(abort_text);
	}

	@Override
	public float getRandomStartPosition() {
		return 0f;
	}

	@Override
	public boolean isRated() {
		return false;
	}

	@Override
	public void addGameOverGUI(@NonNull WorldViewer viewer, @NonNull GameStatsDelegate delegate, int header_y, @NonNull Group group) {
		addGameOverGUI(viewer, delegate, header_y, group, true);
	}

	protected final void addGameOverGUI(final @NonNull WorldViewer viewer, final @NonNull GameStatsDelegate delegate, int header_y, @NonNull Group group, boolean replay) {
		String map_code_str = Utils.getBundleString(GameStatsDelegate.bundle, "map_code", viewer.getParameters().getMapcode());
		Label map_code = new Label(map_code_str, Skin.getSkin().getEditFont());
		delegate.addChild(map_code);
		map_code.setPos((delegate.getWidth() - map_code.getWidth())/2, header_y - map_code.getHeight());

		HorizButton button_replay = new HorizButton(Utils.getBundleString(GameStatsDelegate.bundle, "replay_island"), 150);
		button_replay.addMouseClickListener((int _, int _, int _, int _) -> {
                    replay_island_flag = true;
                    delegate.startMenu();
                });
		HorizButton button_observer = new HorizButton(Utils.getBundleString(GameStatsDelegate.bundle, "observer_mode"), 150);
		button_observer.addMouseClickListener((int _, int _, int _, int _) -> {
                    delegate.getViewer().getDelegate().setObserverMode();
                    delegate.pop();
                });

		HorizButton button_end = new HorizButton(Utils.getBundleString(GameStatsDelegate.bundle, "main_menu"), 150);
		button_end.addMouseClickListener((int _, int _, int _, int _) -> delegate.startMenu());

		if (replay)
			group.addChild(button_replay);
		group.addChild(button_observer);
		group.addChild(button_end);

		button_end.place();
		button_observer.place(button_end, LEFT_MID);
		if (replay)
			button_replay.place(button_observer, LEFT_MID);
	}

	private void addGameInfos(@NonNull WorldViewer viewer, Menu menu, @NonNull Group game_infos) {
		Player[] players = viewer.getWorld().getPlayers();
		Group names = new Group();
		GUIObject last_name = null;
		Group races = new Group();
		GUIObject last_race = null;
		Group teams = new Group();
		GUIObject last_team = null;
        for (Player player : players) {
            PlayerInfo player_info = player.getPlayerInfo();
            float[] color = player.getColor();
            if (!viewer.getPeerHub().isAlive(player)) {
                color = new float[]{color[0], color[1], color[2], .25f};
            }
            Label name = new Label(player_info.getName(), Skin.getSkin().getHeadlineFont());
            name.setColor(color);
            String race_str = RacesResources.getRaceName(player_info.getRace());
            Label race = new Label(race_str, Skin.getSkin().getHeadlineFont());
            race.setColor(color);
            String team_str = Utils.getBundleString(terrain_menu_bundle, "team", Integer.toString(player_info.getTeam() + 1));
            Label team = new Label(team_str, Skin.getSkin().getHeadlineFont());
            team.setColor(color);
            names.addChild(name);
            if (last_name != null)
                name.place(last_name, BOTTOM_LEFT);
            else
                name.place();
            last_name = name;
            races.addChild(race);
            if (last_race != null)
                race.place(last_race, BOTTOM_LEFT);
            else
                race.place();
            last_race = race;
            teams.addChild(team);
            if (last_team != null)
                team.place(last_team, BOTTOM_LEFT);
            else
                team.place();
            last_team = team;
        }
		names.compileCanvas();
		races.compileCanvas();
		teams.compileCanvas();
		game_infos.addChild(names);
		game_infos.addChild(races);
		game_infos.addChild(teams);
		names.place();
		races.place(names, RIGHT_TOP);
		teams.place(races, RIGHT_TOP);
		game_infos.compileCanvas();
	}

	@Override
	public void addGUI(@NonNull WorldViewer viewer, @NonNull InGameMainMenu menu, @NonNull Group game_infos) {
		addAbortButton(menu);
		addGameInfos(viewer, menu, game_infos);
	}

	@Override
	public final void close(@NonNull WorldViewer viewer) {
		if (replay_island_flag) {
			TerrainMenu menu = new TerrainMenu(viewer.getNetwork(), viewer.getGUIRoot(), null, false, null);
			menu.parseMapcode(viewer.getParameters().getMapcode());
			menu.startGame();
		} else
			Renderer.startMenu(viewer.getNetwork(), viewer.getGUIRoot().getGUI());
	}

	@Override
	public final void abort(@NonNull WorldViewer viewer) {
		viewer.getGUIRoot().pushDelegate(new GameStatsDelegate(viewer, viewer.getGUIRoot().getDelegate().getCamera(), Utils.getBundleString(Menu.bundle, "game_aborted")));
	}

	@Override
	public boolean isMultiplayer() {
		return false;
	}
}
