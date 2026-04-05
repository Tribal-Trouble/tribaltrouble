package com.oddlabs.tt.form;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.gui.CheckBox;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.Panel;
import com.oddlabs.tt.gui.PulldownButton;
import com.oddlabs.tt.gui.PulldownItem;
import com.oddlabs.tt.gui.PulldownMenu;
import com.oddlabs.tt.gui.EditLine;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.gui.Slider;
import com.oddlabs.tt.util.ServerMessageBundler;
import org.jspecify.annotations.NonNull;

import java.util.function.IntConsumer;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.RIGHT_MID;

public class GeneralPanel extends Panel {
    private static final int SLIDER_WIDTH = 270;
    private static final int MAX_VALUE = 20;

    private final PulldownMenu<Void> pm_gamespeed = new PulldownMenu<>();

    public GeneralPanel(@NonNull GUIRoot gui_root, @NonNull IntConsumer onGamespeedChange) {
        super(AbstractOptionsMenu.i18n("general_settings_caption"));

        // Invert camera
        Group group_invert_camera = new Group();
        addChild(group_invert_camera);
        CheckBox cb_invert_camera = new CheckBox(Settings.getSettings().invert_camera_pitch, AbstractOptionsMenu.i18n("invert_camera"), AbstractOptionsMenu.i18n("invert_camera_tip"));
        cb_invert_camera.addCheckBoxListener(marked -> Settings.getSettings().invert_camera_pitch = marked);
        group_invert_camera.addChild(cb_invert_camera);
        cb_invert_camera.place();
        group_invert_camera.compileCanvas();

        // Aggressive units
        Group group_aggressive_units = new Group();
        addChild(group_aggressive_units);
        CheckBox cb_aggressive_units = new CheckBox(Settings.getSettings().aggressive_units, AbstractOptionsMenu.i18n("aggressive_units"), AbstractOptionsMenu.i18n("aggressive_units_tip", "Ctrl-A"));
        cb_aggressive_units.addCheckBoxListener(marked -> Settings.getSettings().aggressive_units = marked);
        group_aggressive_units.addChild(cb_aggressive_units);
        cb_aggressive_units.place();
        group_aggressive_units.compileCanvas();

        // Mapmode delay
        Group group_mapmode = new Group();
        addChild(group_mapmode);
        Label label_mapmode_headline = new Label(AbstractOptionsMenu.i18n("map_mode_delay"), Skin.getSkin().getEditFont());
        group_mapmode.addChild(label_mapmode_headline);
        Label label_mapmode_none = new Label(AbstractOptionsMenu.i18n("delay_none"), Skin.getSkin().getEditFont());
        group_mapmode.addChild(label_mapmode_none);
        Label label_mapmode_high = new Label(AbstractOptionsMenu.i18n("delay_high"), Skin.getSkin().getEditFont());
        group_mapmode.addChild(label_mapmode_high);
        Slider slider_mapmode = new Slider(SLIDER_WIDTH, 0, MAX_VALUE, (int) (Settings.getSettings().mapmode_delay * MAX_VALUE));
        group_mapmode.addChild(slider_mapmode);
        slider_mapmode.addValueListener(value -> Settings.getSettings().mapmode_delay = (float) value / (MAX_VALUE));
        label_mapmode_headline.place();
        label_mapmode_none.place(label_mapmode_headline, BOTTOM_LEFT);
        slider_mapmode.place(label_mapmode_none, RIGHT_MID);
        label_mapmode_high.place(slider_mapmode, RIGHT_MID);
        group_mapmode.compileCanvas();

        // Tooltip delay
        Group group_tooltip = new Group();
        addChild(group_tooltip);
        Label label_tooltip_headline = new Label(AbstractOptionsMenu.i18n("tool_tip_delay"), Skin.getSkin().getEditFont());
        group_tooltip.addChild(label_tooltip_headline);
        Label label_tooltip_none = new Label(AbstractOptionsMenu.i18n("delay_none"), Skin.getSkin().getEditFont());
        group_tooltip.addChild(label_tooltip_none);
        Label label_tooltip_high = new Label(AbstractOptionsMenu.i18n("delay_high"), Skin.getSkin().getEditFont());
        group_tooltip.addChild(label_tooltip_high);
        Slider slider_tooltip = new Slider(SLIDER_WIDTH, 0, MAX_VALUE, (int) (Settings.getSettings().tooltip_delay * MAX_VALUE));
        group_tooltip.addChild(slider_tooltip);
        slider_tooltip.addValueListener(value -> {
            Settings.getSettings().tooltip_delay = (float) value / (MAX_VALUE);
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
        Label label_gamespeed = new Label(AbstractOptionsMenu.i18n("gamespeed"), Skin.getSkin().getEditFont());
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

        // Show compass
        Group group_show_compass = new Group();
        addChild(group_show_compass);
        CheckBox cb_show_compass = new CheckBox(Settings.getSettings().show_compass, AbstractOptionsMenu.i18n("show_compass"), AbstractOptionsMenu.i18n("show_compass_tip"));
        cb_show_compass.addCheckBoxListener(marked -> Settings.getSettings().show_compass = marked);
        group_show_compass.addChild(cb_show_compass);
        cb_show_compass.place();
        group_show_compass.compileCanvas();

        // Multiplayer domain
        Group group_domain = new Group();
        addChild(group_domain);
        Label label_domain = new Label(AbstractOptionsMenu.i18n("multiplayer_domain"), Skin.getSkin().getEditFont());
        group_domain.addChild(label_domain);
        Label label_domain_updated = new Label("", Skin.getSkin().getEditFont(), 100);
        group_domain.addChild(label_domain_updated);
        EditLine editline_domain = new EditLine(200, 250);
        editline_domain.set(Settings.getSettings().getDomainName());
        group_domain.addChild(editline_domain);
        HorizButton btn_update_domain = new HorizButton(AbstractOptionsMenu.i18n("domain_update"), 130);
        btn_update_domain.addMouseClickListener((_, _, _, _) -> {
            String domain = editline_domain.getContents();
            if (!domain.isEmpty()) {
                Settings.getSettings().setDomain(domain);
                label_domain_updated.set(AbstractOptionsMenu.i18n("domain_updated"));
            }
        });
        group_domain.addChild(btn_update_domain);
        HorizButton btn_reset_domain = new HorizButton(AbstractOptionsMenu.i18n("domain_reset"), 130);
        btn_reset_domain.addMouseClickListener((_, _, _, _) -> {
            editline_domain.set(Settings.OFFICIAL_DOMAIN);
            Settings.getSettings().setDomain(Settings.OFFICIAL_DOMAIN);
            label_domain_updated.set("");
        });
        group_domain.addChild(btn_reset_domain);
        label_domain.place();
        editline_domain.place(label_domain, BOTTOM_LEFT);
        label_domain_updated.place(editline_domain, RIGHT_MID);
        btn_update_domain.place(editline_domain, BOTTOM_LEFT);
        btn_reset_domain.place(btn_update_domain, RIGHT_MID);
        group_domain.compileCanvas();

        // Placement
        group_gamespeed.place();
        group_mapmode.place(group_gamespeed, BOTTOM_LEFT);
        group_tooltip.place(group_mapmode, BOTTOM_LEFT);
        group_invert_camera.place(group_tooltip, BOTTOM_LEFT);
        group_aggressive_units.place(group_invert_camera, BOTTOM_LEFT);
        group_show_compass.place(group_aggressive_units, BOTTOM_LEFT);
        group_domain.place(group_show_compass, BOTTOM_LEFT);
        compileCanvas();
    }

    public void chooseGamespeed(int speed) {
        pm_gamespeed.chooseItem(speed);
    }
}