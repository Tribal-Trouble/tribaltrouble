package com.oddlabs.tt.form;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.gui.FocusDirection;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.OKButton;
import com.oddlabs.tt.gui.OKListener;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.util.ServerMessageBundler;
import com.oddlabs.tt.util.Utils;
import com.oddlabs.util.Compatibility;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.RIGHT_TOP;

public final class GameInfoForm extends Form {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(GameInfoForm.class.getName());

    private @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private final @NonNull HorizButton ok_button;

    public GameInfoForm(@NonNull Game game) {
        this(game, Compatibility.SIM_VERSION);
    }

    public GameInfoForm(@NonNull Game game, int sim_version) {
        Label label_headline = new Label(i18n("game_info"), Skin.getSkin().getHeadlineFont());
        addChild(label_headline);

        Group types = new Group();
        Group values = new Group();

        Label label_name = new Label(i18n("name"), Skin.getSkin().getEditFont());
        Label label_name_value = new Label(game.getName(), Skin.getSkin().getEditFont());
        types.addChild(label_name);
        values.addChild(label_name_value);

        Label label_size = new Label(i18n("size"), Skin.getSkin().getEditFont());
        Label label_size_value = new Label(ServerMessageBundler.getSizeString(game.getSize()),
                Skin.getSkin().getEditFont());
        types.addChild(label_size);
        values.addChild(label_size_value);

        Label label_terrain_type = new Label(i18n("terrain_type"), Skin.getSkin().getEditFont());
        Label label_terrain_type_value = new Label(ServerMessageBundler.getTerrainTypeString(game.getTerrainType()),
                Skin.getSkin().getEditFont());
        types.addChild(label_terrain_type);
        values.addChild(label_terrain_type_value);

        Label label_hills = new Label(i18n("hills"), Skin.getSkin().getEditFont());
        Label label_hills_value = new Label(ServerMessageBundler.getHillsString(game.getHills()),
                Skin.getSkin().getEditFont());
        types.addChild(label_hills);
        values.addChild(label_hills_value);

        Label label_trees = new Label(i18n("trees"), Skin.getSkin().getEditFont());
        Label label_trees_value = new Label(ServerMessageBundler.getTreesString(game.getTrees()),
                Skin.getSkin().getEditFont());
        types.addChild(label_trees);
        values.addChild(label_trees_value);

        Label label_supplies = new Label(i18n("supplies"), Skin.getSkin().getEditFont());
        Label label_supplies_value = new Label(ServerMessageBundler.getSuppliesString(game.getSupplies()),
                Skin.getSkin().getEditFont());
        types.addChild(label_supplies);
        values.addChild(label_supplies_value);

        Label label_rated = new Label(i18n("rated"), Skin.getSkin().getEditFont());
        Label label_rated_value = new Label(ServerMessageBundler.getRatedString(game.isRated()),
                Skin.getSkin().getEditFont());
        types.addChild(label_rated);
        values.addChild(label_rated_value);

        Label label_gamespeed = new Label(i18n("gamespeed"), Skin.getSkin().getEditFont());
        Label label_gamespeed_value = new Label(ServerMessageBundler.getGamespeedString(game.getGamespeed()),
                Skin.getSkin().getEditFont());
        types.addChild(label_gamespeed);
        values.addChild(label_gamespeed_value);

        Label label_mapcode = new Label(i18n("mapcode"), Skin.getSkin().getEditFont());
        Label label_mapcode_value = new Label(game.getMapcode(), Skin.getSkin().getEditFont());
        types.addChild(label_mapcode);
        values.addChild(label_mapcode_value);

        Label label_max_units = new Label(i18n("max_units"), Skin.getSkin().getEditFont());
        Label label_max_units_value = new Label(Integer.toString(game.getMaxUnitCount()),
                Skin.getSkin().getEditFont());
        types.addChild(label_max_units);
        values.addChild(label_max_units_value);

        Label label_starting_units = new Label(i18n("starting_units"), Skin.getSkin().getEditFont());
        Label label_starting_units_value = new Label(Integer.toString(game.getInitialUnitCount()),
                Skin.getSkin().getEditFont());
        types.addChild(label_starting_units);
        values.addChild(label_starting_units_value);

        Label label_max_buildings = new Label(i18n("max_buildings"), Skin.getSkin().getEditFont());
        Label label_max_buildings_value = new Label(Integer.toString(game.getMaxBuildingCount()),
                Skin.getSkin().getEditFont());
        types.addChild(label_max_buildings);
        values.addChild(label_max_buildings_value);

        Label label_ships = new Label(i18n("ships"), Skin.getSkin().getEditFont());
        Label label_ships_value = new Label(i18n(game.isShips() ? "yes" : "no"), Skin.getSkin().getEditFont());
        if (Globals.SHIPS_ENABLED) {
            types.addChild(label_ships);
            values.addChild(label_ships_value);
        }

        Label label_version = new Label(i18n("version"), Skin.getSkin().getEditFont());
        String version = sim_version == Compatibility.SIM_VERSION ? Integer.toString(sim_version) : i18n(
                "version_other", Integer.toString(sim_version), Integer.toString(Compatibility.SIM_VERSION));
        Label label_version_value = new Label(version, Skin.getSkin().getEditFont());
        types.addChild(label_version);
        values.addChild(label_version_value);

        label_name.place();
        label_rated.place(label_name, BOTTOM_LEFT);
        label_gamespeed.place(label_rated, BOTTOM_LEFT);
        label_terrain_type.place(label_gamespeed, BOTTOM_LEFT);
        label_size.place(label_terrain_type, BOTTOM_LEFT);
        label_hills.place(label_size, BOTTOM_LEFT);
        label_trees.place(label_hills, BOTTOM_LEFT);
        label_supplies.place(label_trees, BOTTOM_LEFT);
        label_mapcode.place(label_supplies, BOTTOM_LEFT);
        label_max_units.place(label_mapcode, BOTTOM_LEFT);
        label_starting_units.place(label_max_units, BOTTOM_LEFT);
        label_max_buildings.place(label_starting_units, BOTTOM_LEFT);
        Label last_type = label_max_buildings;
        if (Globals.SHIPS_ENABLED) {
            label_ships.place(label_max_buildings, BOTTOM_LEFT);
            last_type = label_ships;
        }
        label_version.place(last_type, BOTTOM_LEFT);
        types.compileCanvas();
        addChild(types);

        label_name_value.place();
        label_rated_value.place(label_name_value, BOTTOM_LEFT);
        label_gamespeed_value.place(label_rated_value, BOTTOM_LEFT);
        label_terrain_type_value.place(label_gamespeed_value, BOTTOM_LEFT);
        label_size_value.place(label_terrain_type_value, BOTTOM_LEFT);
        label_hills_value.place(label_size_value, BOTTOM_LEFT);
        label_trees_value.place(label_hills_value, BOTTOM_LEFT);
        label_supplies_value.place(label_trees_value, BOTTOM_LEFT);
        label_mapcode_value.place(label_supplies_value, BOTTOM_LEFT);
        label_max_units_value.place(label_mapcode_value, BOTTOM_LEFT);
        label_starting_units_value.place(label_max_units_value, BOTTOM_LEFT);
        label_max_buildings_value.place(label_starting_units_value, BOTTOM_LEFT);
        Label last_value = label_max_buildings_value;
        if (Globals.SHIPS_ENABLED) {
            label_ships_value.place(label_max_buildings_value, BOTTOM_LEFT);
            last_value = label_ships_value;
        }
        label_version_value.place(last_value, BOTTOM_LEFT);
        values.compileCanvas();
        addChild(values);

        ok_button = new OKButton(100);
        addChild(ok_button);
        ok_button.addMouseClickListener(new OKListener(this));

        label_headline.place();
        types.place(label_headline, BOTTOM_LEFT);
        values.place(types, RIGHT_TOP);

        ok_button.place(Origin.AT_END);
        compileCanvas();
        centerPos();
    }

    @Override
    public void setFocus(@NonNull FocusDirection direction) {
        if (direction == FocusDirection.BACKWARD) {
            super.setFocus(direction);
        } else {
            ok_button.setFocus(direction);
        }
    }
}
