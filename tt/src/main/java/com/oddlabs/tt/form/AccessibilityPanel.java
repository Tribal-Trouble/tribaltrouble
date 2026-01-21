package com.oddlabs.tt.form;

import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.gui.CheckBox;
import com.oddlabs.tt.gui.GUIObject;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.Panel;
import com.oddlabs.tt.gui.PulldownButton;
import com.oddlabs.tt.gui.PulldownItem;
import com.oddlabs.tt.gui.PulldownMenu;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.gui.Slider;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.tt.util.Utils;
import com.oddlabs.util.Color;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.RIGHT_MID;

public class AccessibilityPanel extends Panel {
    private static final int SLIDER_WIDTH = 270;
    private static final int MAX_VALUE = 20;

    public AccessibilityPanel(@NonNull GUIRoot gui_root, @NonNull ResourceBundle bundle) {
        super(Utils.getBundleString(bundle, "accessibility_caption"));

        // High Contrast
        Group group_contrast = new Group();
        addChild(group_contrast);

        CheckBox cb_high_contrast = new CheckBox(Settings.getSettings().high_contrast, Utils.getBundleString(bundle, "high_contrast"), Utils.getBundleString(bundle, "high_contrast_tip"));
        group_contrast.addChild(cb_high_contrast);
        
        Label label_contrast_intensity = new Label(Utils.getBundleString(bundle, "contrast_intensity"), Skin.getSkin().getEditFont());
        group_contrast.addChild(label_contrast_intensity);

        Label label_contrast_low = new Label(Utils.getBundleString(bundle, "low"), Skin.getSkin().getEditFont());
        group_contrast.addChild(label_contrast_low);
        Label label_contrast_high = new Label(Utils.getBundleString(bundle, "high"), Skin.getSkin().getEditFont());
        group_contrast.addChild(label_contrast_high);
        
        // Support up to 2.0 intensity (40 steps)
        Slider slider_contrast = new Slider(SLIDER_WIDTH, 0, 2 * MAX_VALUE, (int)(Settings.getSettings().contrast_intensity * MAX_VALUE));
        slider_contrast.setDisabled(!Settings.getSettings().high_contrast);
        group_contrast.addChild(slider_contrast);

        cb_high_contrast.addCheckBoxListener(marked -> {
            Settings.getSettings().high_contrast = marked;
            slider_contrast.setDisabled(!marked);
        });

        slider_contrast.addValueListener(value -> Settings.getSettings().contrast_intensity = (float)value / MAX_VALUE);

        cb_high_contrast.place();
        label_contrast_intensity.place(cb_high_contrast, BOTTOM_LEFT);
        label_contrast_low.place(label_contrast_intensity, BOTTOM_LEFT);
        slider_contrast.place(label_contrast_low, RIGHT_MID);
        label_contrast_high.place(slider_contrast, RIGHT_MID);
        group_contrast.compileCanvas();

        // CVD
        Group group_cvd = new Group();
        addChild(group_cvd);
        Label label_cvd = new Label(Utils.getBundleString(bundle, "color_vision"), Skin.getSkin().getEditFont());
        group_cvd.addChild(label_cvd);

        PulldownMenu<Void> pm_cvd = new PulldownMenu<>();
        pm_cvd.addItem(new PulldownItem<>(Utils.getBundleString(bundle, "cvd_standard")));
        pm_cvd.addItem(new PulldownItem<>(Utils.getBundleString(bundle, "cvd_protanopia")));
        pm_cvd.addItem(new PulldownItem<>(Utils.getBundleString(bundle, "cvd_deuteranopia")));
        pm_cvd.addItem(new PulldownItem<>(Utils.getBundleString(bundle, "cvd_tritanopia")));
        PulldownButton<Void> pb_cvd = new PulldownButton<>(gui_root, pm_cvd, Settings.getSettings().cvd_mode, SLIDER_WIDTH - label_cvd.getWidth() - Skin.getSkin().getFormData().objectSpacing());
        group_cvd.addChild(pb_cvd);

        Label label_cvd_intensity = new Label(Utils.getBundleString(bundle, "cvd_intensity"), Skin.getSkin().getEditFont());
        group_cvd.addChild(label_cvd_intensity);

        Label label_cvd_low = new Label(Utils.getBundleString(bundle, "low"), Skin.getSkin().getEditFont());
        group_cvd.addChild(label_cvd_low);
        Label label_cvd_high = new Label(Utils.getBundleString(bundle, "high"), Skin.getSkin().getEditFont());
        group_cvd.addChild(label_cvd_high);

        // Support up to 2.0 intensity (40 steps)
        Slider slider_cvd = new Slider(SLIDER_WIDTH, 0, 2 * MAX_VALUE, (int)(Settings.getSettings().cvd_intensity * MAX_VALUE));
        slider_cvd.setDisabled(Settings.getSettings().cvd_mode == 0);
        group_cvd.addChild(slider_cvd);

        pm_cvd.addItemChosenListener((_, index) -> {
            Settings.getSettings().cvd_mode = index;
            slider_cvd.setDisabled(index == 0);
        });
        slider_cvd.addValueListener(value -> Settings.getSettings().cvd_intensity = (float)value / MAX_VALUE);

        label_cvd.place();
        pb_cvd.place(label_cvd, RIGHT_MID);
        label_cvd_intensity.place(label_cvd, BOTTOM_LEFT);
        label_cvd_low.place(label_cvd_intensity, BOTTOM_LEFT);
        slider_cvd.place(label_cvd_low, RIGHT_MID);
        label_cvd_high.place(slider_cvd, RIGHT_MID);
        group_cvd.compileCanvas();

        // Team Colours
        Group group_team_colours = new Group();
        addChild(group_team_colours);

        Label label_team_colours = new Label(Utils.getBundleString(bundle, "team_colours"), Skin.getSkin().getEditFont());
        group_team_colours.addChild(label_team_colours);

        PulldownMenu<Void> pm_team = new PulldownMenu<>();
        for (int i = 0; i < Settings.getSettings().team_colours.length; i++) {
            String player_str = Utils.getBundleString(bundle, "player", Integer.toString(i + 1));
            PulldownItem<Void> item = new PulldownItem<>(player_str);
            item.setLabelColor(Settings.getSettings().team_colours[i]);
            pm_team.addItem(item);
        }
        PulldownButton<Void> pb_team = new PulldownButton<>(gui_root, pm_team, 0, 150);
        group_team_colours.addChild(pb_team);

        // Color Preview Box
        class ColorBox extends GUIObject {
            private final Vector4f color = new Vector4f(Color.WHITE);
            public ColorBox() { setDim(20, 20); }
            @Override protected void renderGeometry(@NonNull GUIRenderer renderer) {
                renderer.drawColoredQuad(0, 0, getWidth(), getHeight(), color);
            }
            public void setColor(Vector4fc c) { this.color.set(c); }
        }
        ColorBox colorBox = new ColorBox();
        group_team_colours.addChild(colorBox);

        // Hue Slider (Color Ramp)
        Slider slider_hue = new Slider(SLIDER_WIDTH, 0, 360, 0);
        group_team_colours.addChild(slider_hue);
        
        // Reset Button
        HorizButton button_reset = new HorizButton(Utils.getBundleString(bundle, "reset"), 100);
        group_team_colours.addChild(button_reset);

        // Update logic
        Runnable updateColor = () -> {
            int teamIndex = pm_team.getChosenItemIndex();
            float hue = slider_hue.getValue();
            int rgb = java.awt.Color.HSBtoRGB(hue / 360f, 1.0f, 1.0f);
            Vector4f newColor = Color.argb4v((0xFF << 24) | (rgb & 0xFFFFFF));
            Settings.getSettings().team_colours[teamIndex] = newColor;
            colorBox.setColor(newColor);
            
            // Update the pulldown item color
            pm_team.getItem(teamIndex).setLabelColor(newColor);
            pb_team.setLabelColor(newColor);
        };
        
        Runnable refreshUI = () -> {
            int index = pm_team.getChosenItemIndex();
            Vector4fc currentColor = Settings.getSettings().team_colours[index];
            float[] hsb = java.awt.Color.RGBtoHSB((int)(currentColor.x() * 255), (int)(currentColor.y() * 255), (int)(currentColor.z() * 255), null);
            slider_hue.setValue((int)(hsb[0] * 360));
            colorBox.setColor(currentColor);
        };

        pm_team.addItemChosenListener((_, _) -> refreshUI.run());
        refreshUI.run();

        slider_hue.addValueListener(_ -> updateColor.run());
        
        button_reset.addMouseClickListener((_, _, _, _) -> {
            int index = pm_team.getChosenItemIndex();
            Settings.getSettings().team_colours[index] = new Vector4f(Settings.DEFAULT_TEAM_COLOURS[index]);
            refreshUI.run();
            pm_team.getItem(index).setLabelColor(Settings.getSettings().team_colours[index]);
            pb_team.setLabelColor(Settings.getSettings().team_colours[index]);
        });

        CheckBox cb_team_stencil = new CheckBox(Settings.getSettings().team_stencil, Utils.getBundleString(bundle, "team_stencil"), Utils.getBundleString(bundle, "team_stencil_tip"));
        cb_team_stencil.addCheckBoxListener(marked -> Settings.getSettings().team_stencil = marked);
        group_team_colours.addChild(cb_team_stencil);

        label_team_colours.place();
        pb_team.place(label_team_colours, RIGHT_MID);
        colorBox.place(pb_team, RIGHT_MID);
        button_reset.place(colorBox, RIGHT_MID);
        slider_hue.place(label_team_colours, BOTTOM_LEFT);
        cb_team_stencil.place(slider_hue, BOTTOM_LEFT);

        group_team_colours.compileCanvas();

        // Placement
        group_contrast.place();
        group_cvd.place(group_contrast, BOTTOM_LEFT);
        group_team_colours.place(group_cvd, BOTTOM_LEFT);
        
        compileCanvas();
    }
}