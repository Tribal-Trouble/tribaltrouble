package com.oddlabs.tt.form;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.gui.*;
import com.oddlabs.tt.util.ServerMessageBundler;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;
import java.util.function.IntConsumer;

import static com.oddlabs.tt.gui.Placement.*;

public class GeneralPanel extends Panel {
    private static final int SLIDER_WIDTH = 270;
    private static final int MAX_VALUE = 20;
    
    private final PulldownMenu<Void> pm_gamespeed = new PulldownMenu<>();

    public GeneralPanel(@NonNull GUIRoot gui_root, @NonNull ResourceBundle bundle, @NonNull IntConsumer onGamespeedChange) {
        super(Utils.getBundleString(bundle, "general_settings_caption"));

        // Invert camera
        Group group_invert_camera = new Group();
        addChild(group_invert_camera);
        CheckBox cb_invert_camera = new CheckBox(Settings.getSettings().invert_camera_pitch, Utils.getBundleString(bundle, "invert_camera"), Utils.getBundleString(bundle, "invert_camera_tip"));
        cb_invert_camera.addCheckBoxListener(marked -> Settings.getSettings().invert_camera_pitch = marked);
        group_invert_camera.addChild(cb_invert_camera);
        cb_invert_camera.place();
        group_invert_camera.compileCanvas();

        // Aggressive units
        Group group_aggressive_units = new Group();
        addChild(group_aggressive_units);
        CheckBox cb_aggressive_units = new CheckBox(Settings.getSettings().aggressive_units, Utils.getBundleString(bundle, "aggressive_units"), Utils.getBundleString(bundle, "aggressive_units_tip", "Ctrl-A"));
        cb_aggressive_units.addCheckBoxListener(marked -> Settings.getSettings().aggressive_units = marked);
        group_aggressive_units.addChild(cb_aggressive_units);
        cb_aggressive_units.place();
        group_aggressive_units.compileCanvas();

        // Mapmode delay
        Group group_mapmode = new Group();
        addChild(group_mapmode);
        Label label_mapmode_headline = new Label(Utils.getBundleString(bundle, "map_mode_delay"), Skin.getSkin().getEditFont());
        group_mapmode.addChild(label_mapmode_headline);
        Label label_mapmode_none = new Label(Utils.getBundleString(bundle, "delay_none"), Skin.getSkin().getEditFont());
        group_mapmode.addChild(label_mapmode_none);
        Label label_mapmode_high = new Label(Utils.getBundleString(bundle, "delay_high"), Skin.getSkin().getEditFont());
        group_mapmode.addChild(label_mapmode_high);
        Slider slider_mapmode = new Slider(SLIDER_WIDTH, 0, MAX_VALUE, (int)(Settings.getSettings().mapmode_delay * MAX_VALUE));
        group_mapmode.addChild(slider_mapmode);
        slider_mapmode.addValueListener(value -> Settings.getSettings().mapmode_delay = (float)value/(MAX_VALUE));
        label_mapmode_headline.place();
        label_mapmode_none.place(label_mapmode_headline, BOTTOM_LEFT);
        slider_mapmode.place(label_mapmode_none, RIGHT_MID);
        label_mapmode_high.place(slider_mapmode, RIGHT_MID);
        group_mapmode.compileCanvas();

        // Tooltip delay
        Group group_tooltip = new Group();
        addChild(group_tooltip);
        Label label_tooltip_headline = new Label(Utils.getBundleString(bundle, "tool_tip_delay"), Skin.getSkin().getEditFont());
        group_tooltip.addChild(label_tooltip_headline);
        Label label_tooltip_none = new Label(Utils.getBundleString(bundle, "delay_none"), Skin.getSkin().getEditFont());
        group_tooltip.addChild(label_tooltip_none);
        Label label_tooltip_high = new Label(Utils.getBundleString(bundle, "delay_high"), Skin.getSkin().getEditFont());
        group_tooltip.addChild(label_tooltip_high);
        Slider slider_tooltip = new Slider(SLIDER_WIDTH, 0, MAX_VALUE, (int)(Settings.getSettings().tooltip_delay * MAX_VALUE));
        group_tooltip.addChild(slider_tooltip);
        slider_tooltip.addValueListener(value -> {
            Settings.getSettings().tooltip_delay = (float)value/(MAX_VALUE);
            gui_root.setToolTipTimer();
        });
        label_tooltip_headline.place();
        label_tooltip_none.place(label_tooltip_headline, BOTTOM_LEFT);
        slider_tooltip.place(label_tooltip_none, RIGHT_MID);
        label_tooltip_high.place(slider_tooltip, RIGHT_MID);
        group_tooltip.compileCanvas();

        // Gamespeed
        Group group_gamespeed = new Group();
        addChild(group_gamespeed);
        Label label_gamespeed = new Label(Utils.getBundleString(bundle, "gamespeed"), Skin.getSkin().getEditFont());
        group_gamespeed.addChild(label_gamespeed);
        
        pm_gamespeed.addItem(new PulldownItem<>(ServerMessageBundler.getGamespeedString(Game.GAMESPEED_PAUSE)));
        pm_gamespeed.addItem(new PulldownItem<>(ServerMessageBundler.getGamespeedString(Game.GAMESPEED_SLOW)));
        pm_gamespeed.addItem(new PulldownItem<>(ServerMessageBundler.getGamespeedString(Game.GAMESPEED_NORMAL)));
        pm_gamespeed.addItem(new PulldownItem<>(ServerMessageBundler.getGamespeedString(Game.GAMESPEED_FAST)));
        pm_gamespeed.addItem(new PulldownItem<>(ServerMessageBundler.getGamespeedString(Game.GAMESPEED_LUDICROUS)));
        
        PulldownButton<Void> pb_gamespeed = new PulldownButton<>(gui_root, pm_gamespeed, 150);
        pm_gamespeed.addItemChosenListener((_, item_index) -> onGamespeedChange.accept(item_index));
        group_gamespeed.addChild(pb_gamespeed);
        label_gamespeed.place();
        pb_gamespeed.place(label_gamespeed, RIGHT_MID);
        group_gamespeed.compileCanvas();

        // Placement
        group_gamespeed.place();
        group_mapmode.place(group_gamespeed, BOTTOM_LEFT);
        group_tooltip.place(group_mapmode, BOTTOM_LEFT);
        group_invert_camera.place(group_tooltip, BOTTOM_LEFT);
        group_aggressive_units.place(group_invert_camera, BOTTOM_LEFT);
        compileCanvas();
    }
    
    public void chooseGamespeed(int speed) {
        pm_gamespeed.chooseItem(speed);
    }
}