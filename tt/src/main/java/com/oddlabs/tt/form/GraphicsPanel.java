package com.oddlabs.tt.form;

import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.gui.CheckBox;
import com.oddlabs.tt.gui.ColumnInfo;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.MultiColumnComboBox;
import com.oddlabs.tt.gui.Panel;
import com.oddlabs.tt.gui.PulldownButton;
import com.oddlabs.tt.gui.PulldownItem;
import com.oddlabs.tt.gui.PulldownMenu;
import com.oddlabs.tt.gui.Row;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.gui.Slider;
import com.oddlabs.tt.gui.SortedLabel;
import com.oddlabs.tt.guievent.RowListener;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.render.SerializableDisplayMode;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.RIGHT_MID;
import static com.oddlabs.tt.gui.Placement.RIGHT_TOP;

public class GraphicsPanel extends Panel {
    private final @NonNull Label label_pct;

    public GraphicsPanel(@NonNull GUIRoot gui_root, @NonNull Form options) {
        super(AbstractOptionsMenu.i18n("graphics_caption"));
        var labelFont = Skin.getSkin().getEditFont();

        // Fullscreen
        Group group_fullscreen = new Group();
        addChild(group_fullscreen);
        CheckBox cb_fullscreen = new CheckBox(Settings.getSettings().fullscreen, AbstractOptionsMenu.i18n("fullscreen"), AbstractOptionsMenu.i18n("fullscreen_tip"));
        cb_fullscreen.addCheckBoxListener(marked -> {
            DisplayChangeForm display_change_form = new DisplayChangeForm(
                    switch_now -> {
                        if (switch_now) {
                            Renderer.getRenderer().toggleFullscreen();
                        } else {
                            Settings.getSettings().fullscreen = marked;
                        }
                    });
            gui_root.addModalForm(display_change_form);
        });
        group_fullscreen.addChild(cb_fullscreen);
        cb_fullscreen.place();
        group_fullscreen.compileCanvas();

        // UI Scale
        Group group_ui_scale = new Group();
        addChild(group_ui_scale);
        Label label_ui_scale = new Label(AbstractOptionsMenu.i18n("ui_scale"), labelFont);
        group_ui_scale.addChild(label_ui_scale);

        // Initial percentage label
        label_pct = new Label("9.9.9%", labelFont);
        updateScaleLabel();
        group_ui_scale.addChild(label_pct);

        int initialValue = Math.clamp((long)(Settings.getSettings().ui_scale * 1000), 0, 1000);

        Slider slider_ui_scale = new Slider(150, 0, 1000, initialValue);
        group_ui_scale.addChild(slider_ui_scale);
        
        slider_ui_scale.addValueListener(value -> {
            Settings.getSettings().ui_scale = value / 1000f;
            updateScaleLabel();
        });
        
        slider_ui_scale.addReleaseListener(() -> 
            gui_root.displayChanged(Renderer.getRenderer().getWindow().getWidth(), Renderer.getRenderer().getWindow().getHeight())
        );

        label_ui_scale.place();
        label_pct.place(label_ui_scale, RIGHT_MID);
        slider_ui_scale.place(label_ui_scale, BOTTOM_LEFT);
        group_ui_scale.compileCanvas();

        // Detail
        Group group_detail = new Group();
        addChild(group_detail);

        Label label_detail = new Label(AbstractOptionsMenu.i18n("graphical_detail"), labelFont);
        group_detail.addChild(label_detail);

        int initial_detail_value = Settings.getSettings().graphic_detail;
        PulldownMenu<Void> pm_detail = new PulldownMenu<>();
        pm_detail.addItem(new PulldownItem<>(AbstractOptionsMenu.i18n("low")));
        pm_detail.addItem(new PulldownItem<>(AbstractOptionsMenu.i18n("medium")));
        pm_detail.addItem(new PulldownItem<>(AbstractOptionsMenu.i18n("high")));
        PulldownButton<Void> pb_detail = new PulldownButton<>(gui_root, pm_detail, initial_detail_value, 150);

        group_detail.addChild(pb_detail);
        options.addCloseListener(() -> {
            int slider_value = pm_detail.getChosenItemIndex();
            if (initial_detail_value != slider_value) {
                Settings.getSettings().graphic_detail = slider_value;
                gui_root.addModalForm(new MessageForm(AbstractOptionsMenu.i18n("change_next_run")));
            }
        });
        label_detail.place();
        pb_detail.place(label_detail, BOTTOM_LEFT);
        group_detail.compileCanvas();

        // Display mode
        Group mode_group = new Group();
        addChild(mode_group);

        Label mode_label = new Label(AbstractOptionsMenu.i18n("display_mode"), labelFont);
        mode_group.addChild(mode_label);

        ColumnInfo[] mode_infos = new ColumnInfo[]{new ColumnInfo("", 150)};

        MultiColumnComboBox<SerializableDisplayMode> mode_list_box = new MultiColumnComboBox<>(gui_root, mode_infos, 200, false);
        SerializableDisplayMode[] modes = Renderer.getRenderer().getWindow().getAvailableDisplayModes();
        SerializableDisplayMode current_mode = Renderer.getRenderer().getCurrentDisplayMode();
        Row<SerializableDisplayMode, Label> current_row = null;
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].getBitsPerPixel() == current_mode.getBitsPerPixel()) {
                String mode_string = AbstractOptionsMenu.i18n("mode", Integer.toString(modes[i].getWidth()), Integer.toString(modes[i].getHeight()), Integer.toString(modes[i].getFrequency()));
                Label label = new SortedLabel(mode_string, i, Skin.getSkin().getMultiColumnComboBoxData().font());
                Row<SerializableDisplayMode, Label> row = new Row<>(new Label[]{label}, modes[i]);
                mode_list_box.addRow(row);
                if (modes[i].equals(current_mode))
                    current_row = row;
            }
        }
        if (current_row != null)
            mode_list_box.selectRow(current_row);
        mode_list_box.addRowListener(new RowListener<>() {
            @Override
            public void rowDoubleClicked(@NonNull SerializableDisplayMode mode) {
                gui_root.addModalForm(new DisplayChangeForm(switch_now -> {
                    Renderer.getRenderer().switchMode(mode, switch_now);
                    if (switch_now) {
                        gui_root.displayChanged(mode.getWidth(), mode.getHeight());
                    }
                }));
            }
        });

        mode_group.addChild(mode_list_box);
        mode_label.place();
        mode_list_box.place(mode_label, BOTTOM_LEFT);
        mode_group.compileCanvas();

        // Placement
        mode_group.place();
        group_detail.place(mode_group, RIGHT_TOP);
        group_ui_scale.place(group_detail, BOTTOM_LEFT);
        group_fullscreen.place(group_ui_scale, BOTTOM_LEFT);
        compileCanvas();
    }
    
    public void updateScaleLabel() {
        int w = Renderer.getRenderer().getWindow().getWidth();
        int h = Renderer.getRenderer().getWindow().getHeight();

        float scale = GUIRoot.calculateEffectiveScale(w, h);
        label_pct.setText(String.format("%d%%", (int)(scale * 100)));
    }
}