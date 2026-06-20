package com.oddlabs.tt.form;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.tt.model.Gamespeed;
import com.oddlabs.tt.gui.CheckBox;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.Panel;
import com.oddlabs.tt.gui.PulldownButton;
import com.oddlabs.tt.gui.PulldownItem;
import com.oddlabs.tt.gui.PulldownMenu;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.gui.Slider;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.util.ServerMessageBundler;
import org.jspecify.annotations.NonNull;

import java.util.function.IntConsumer;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.RIGHT_MID;

/**
 * UI panel for general settings, such as camera behavior, unit aggression, and delays.
 */
public class GeneralPanel extends Panel {
    private static final int SLIDER_WIDTH = 270;
    private static final int MAX_VALUE = 20;

    private final PulldownMenu<Gamespeed> pm_gamespeed = new PulldownMenu<>();

    public GeneralPanel(@NonNull GUIRoot gui_root, @NonNull IntConsumer onGamespeedChange) {
        super(AbstractOptionsMenu.i18n("general_settings_caption"));

        // Invert camera
        Group group_invert_camera = new Group();
        addChild(group_invert_camera);
        CheckBox cb_invert_camera = new CheckBox(Renderer.getRenderer().getSettings().invert_camera_pitch,
                AbstractOptionsMenu.i18n("invert_camera"), AbstractOptionsMenu.i18n("invert_camera_tip"));
        cb_invert_camera.addCheckBoxListener(marked -> Renderer.getRenderer().getSettings().invert_camera_pitch
                = marked);
        group_invert_camera.addChild(cb_invert_camera);
        cb_invert_camera.place();
        group_invert_camera.compileCanvas();

        // Aggressive units
        Group group_aggressive_units = new Group();
        addChild(group_aggressive_units);
        CheckBox cb_aggressive_units = new CheckBox(Renderer.getRenderer().getSettings().aggressive_units,
                AbstractOptionsMenu.i18n("aggressive_units"), AbstractOptionsMenu.i18n("aggressive_units_tip",
                        "Ctrl-A"));
        cb_aggressive_units.addCheckBoxListener(marked -> Renderer.getRenderer().getSettings().aggressive_units
                = marked);
        group_aggressive_units.addChild(cb_aggressive_units);
        cb_aggressive_units.place();
        group_aggressive_units.compileCanvas();

        // Mapmode delay
        Group group_mapmode = new Group();
        addChild(group_mapmode);
        Label label_mapmode_headline = new Label(AbstractOptionsMenu.i18n("map_mode_delay"), Skin.getSkin()
                .getEditFont());
        group_mapmode.addChild(label_mapmode_headline);
        Label label_mapmode_none = new Label(AbstractOptionsMenu.i18n("delay_none"), Skin.getSkin().getEditFont());
        group_mapmode.addChild(label_mapmode_none);
        Label label_mapmode_high = new Label(AbstractOptionsMenu.i18n("delay_high"), Skin.getSkin().getEditFont());
        group_mapmode.addChild(label_mapmode_high);
        Slider slider_mapmode = new Slider(SLIDER_WIDTH, 0, MAX_VALUE, (int) (Renderer.getRenderer()
                .getSettings().mapmode_delay * MAX_VALUE));
        group_mapmode.addChild(slider_mapmode);
        slider_mapmode.addValueListener(value -> Renderer.getRenderer().getSettings().mapmode_delay = (float) value
                / (MAX_VALUE));
        label_mapmode_headline.place();
        label_mapmode_none.place(label_mapmode_headline, BOTTOM_LEFT);
        slider_mapmode.place(label_mapmode_none, RIGHT_MID);
        label_mapmode_high.place(slider_mapmode, RIGHT_MID);
        group_mapmode.compileCanvas();

        // Tooltip delay
        Group group_tooltip = new Group();
        addChild(group_tooltip);
        Label label_tooltip_headline = new Label(AbstractOptionsMenu.i18n("tool_tip_delay"), Skin.getSkin()
                .getEditFont());
        group_tooltip.addChild(label_tooltip_headline);
        Label label_tooltip_none = new Label(AbstractOptionsMenu.i18n("delay_none"), Skin.getSkin().getEditFont());
        group_tooltip.addChild(label_tooltip_none);
        Label label_tooltip_high = new Label(AbstractOptionsMenu.i18n("delay_high"), Skin.getSkin().getEditFont());
        group_tooltip.addChild(label_tooltip_high);
        Slider slider_tooltip = new Slider(SLIDER_WIDTH, 0, MAX_VALUE, (int) (Renderer.getRenderer()
                .getSettings().tooltip_delay * MAX_VALUE));
        group_tooltip.addChild(slider_tooltip);
        slider_tooltip.addValueListener(value -> {
            Renderer.getRenderer().getSettings().tooltip_delay = (float) value / (MAX_VALUE);
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

        pm_gamespeed.addItem(new PulldownItem<>(ServerMessageBundler.getGamespeedString(Game.GAMESPEED_PAUSE),
                Gamespeed.PAUSE));
        pm_gamespeed.addItem(new PulldownItem<>(ServerMessageBundler.getGamespeedString(Game.GAMESPEED_SLOW),
                Gamespeed.SLOW));
        pm_gamespeed.addItem(new PulldownItem<>(ServerMessageBundler.getGamespeedString(Game.GAMESPEED_NORMAL),
                Gamespeed.NORMAL));
        pm_gamespeed.addItem(new PulldownItem<>(ServerMessageBundler.getGamespeedString(Game.GAMESPEED_FAST),
                Gamespeed.FAST));
        pm_gamespeed.addItem(new PulldownItem<>(ServerMessageBundler.getGamespeedString(Game.GAMESPEED_LUDICROUS),
                Gamespeed.LUDICROUS));

        PulldownButton<Gamespeed> pb_gamespeed = new PulldownButton<>(gui_root, pm_gamespeed, 150);
        pm_gamespeed.addItemChosenListener((_, _) -> onGamespeedChange.accept(pm_gamespeed.getChosenItem()
                .map(PulldownItem::getAttachment).map(Gamespeed::getValue).orElse(Game.GAMESPEED_NORMAL)));
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
