package com.oddlabs.tt.form;

import com.oddlabs.tt.model.CVDMode;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.gui.CheckBox;
import com.oddlabs.tt.gui.GUIObject;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.Panel;
import com.oddlabs.tt.gui.PulldownButton;
import com.oddlabs.tt.gui.PulldownItem;
import com.oddlabs.tt.gui.PulldownMenu;
import com.oddlabs.tt.gui.ScrollablePulldownMenu;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.gui.Slider;
import com.oddlabs.tt.gui.TitledBorderGroup;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.RIGHT_MID;

/**
 * A UI panel allowing players to configure various accessibility options, such as
 * high contrast modes, color vision deficiency (CVD) filters, and custom team colors.
 */
public final class AccessibilityPanel extends Panel {
    private static final int MAX_VALUE = 20;
    private static final int GROUP_WIDTH = 450;

    public AccessibilityPanel(@NonNull GUIRoot gui_root) {
        super(AbstractOptionsMenu.i18n("accessibility_caption"));

        final int SLIDER_PADDING = Skin.getSkin().getGroupData().group().getLeftOffset() + Skin.getSkin().getGroupData()
                .group().getRightOffset();
        final int FULL_SLIDER_WIDTH = GROUP_WIDTH - SLIDER_PADDING;

        // Contrast
        TitledBorderGroup group_contrast = new TitledBorderGroup(AbstractOptionsMenu.i18n("contrast_header"));
        group_contrast.setFixedWidth(GROUP_WIDTH);
        addChild(group_contrast);

        CheckBox cb_high_contrast = new CheckBox(Renderer.getRenderer().getSettings().high_contrast, AbstractOptionsMenu
                .i18n("high_contrast"), AbstractOptionsMenu.i18n("high_contrast_tip"));
        group_contrast.addChild(cb_high_contrast);

        CheckBox cb_invert_colors = new CheckBox(Renderer.getRenderer().getSettings().invert_colours,
                AbstractOptionsMenu.i18n("invert_colours"), AbstractOptionsMenu.i18n("invert_colours_tip"));
        cb_invert_colors.setDisabled(!Renderer.getRenderer().getSettings().high_contrast);
        group_contrast.addChild(cb_invert_colors);

        Label label_contrast_intensity = new Label(AbstractOptionsMenu.i18n("contrast_intensity"), Skin.getSkin()
                .getEditFont());
        group_contrast.addChild(label_contrast_intensity);

        // Support up to 2.0 intensity (40 steps)
        int label_area_width = 140;
        label_contrast_intensity.setDim(label_area_width, label_contrast_intensity.getHeight());
        int contrast_slider_width = GROUP_WIDTH - label_area_width - SLIDER_PADDING - Skin.getSkin().getFormData()
                .objectSpacing();

        Slider slider_contrast = new Slider(contrast_slider_width, 0, 2 * MAX_VALUE, (int) (Renderer.getRenderer()
                .getSettings().contrast_intensity * MAX_VALUE));
        slider_contrast.setDisabled(!Renderer.getRenderer().getSettings().high_contrast);
        label_contrast_intensity.setDisabled(!Renderer.getRenderer().getSettings().high_contrast);
        group_contrast.addChild(slider_contrast);

        Label label_contrast_brightness = new Label(AbstractOptionsMenu.i18n("contrast_brightness"), Skin.getSkin()
                .getEditFont());
        label_contrast_brightness.setDim(label_area_width, label_contrast_brightness.getHeight());
        label_contrast_brightness.setDisabled(!Renderer.getRenderer().getSettings().high_contrast);
        group_contrast.addChild(label_contrast_brightness);

        Slider slider_brightness = new Slider(contrast_slider_width, -MAX_VALUE, MAX_VALUE, (int) (Renderer
                .getRenderer().getSettings().contrast_brightness * 3 * MAX_VALUE));
        slider_brightness.setDisabled(!Renderer.getRenderer().getSettings().high_contrast);
        group_contrast.addChild(slider_brightness);

        Label label_contrast_clarity = new Label(AbstractOptionsMenu.i18n("contrast_clarity"), Skin.getSkin()
                .getEditFont());
        label_contrast_clarity.setDim(label_area_width, label_contrast_clarity.getHeight());
        label_contrast_clarity.setDisabled(!Renderer.getRenderer().getSettings().high_contrast);
        group_contrast.addChild(label_contrast_clarity);

        Slider slider_clarity = new Slider(contrast_slider_width, 0, MAX_VALUE, (int) (Renderer.getRenderer()
                .getSettings().contrast_clarity * MAX_VALUE));
        slider_clarity.setDisabled(!Renderer.getRenderer().getSettings().high_contrast);
        group_contrast.addChild(slider_clarity);

        cb_high_contrast.addCheckBoxListener(marked -> {
            Renderer.getRenderer().getSettings().high_contrast = marked;
            cb_invert_colors.setDisabled(!marked);
            slider_contrast.setDisabled(!marked);
            label_contrast_intensity.setDisabled(!marked);
            slider_brightness.setDisabled(!marked);
            label_contrast_brightness.setDisabled(!marked);
            slider_clarity.setDisabled(!marked);
            label_contrast_clarity.setDisabled(!marked);
        });

        cb_invert_colors.addCheckBoxListener(marked -> Renderer.getRenderer().getSettings().invert_colours = marked);

        slider_contrast.addValueListener(value -> Renderer.getRenderer().getSettings().contrast_intensity
                = (float) value / MAX_VALUE);

        slider_brightness.addValueListener(value -> Renderer.getRenderer().getSettings().contrast_brightness
                = (float) value / (3 * MAX_VALUE));

        slider_clarity.addValueListener(value -> Renderer.getRenderer().getSettings().contrast_clarity = (float) value
                / MAX_VALUE);

        cb_high_contrast.place();
        cb_invert_colors.place(cb_high_contrast, RIGHT_MID);
        cb_invert_colors.setPos(GROUP_WIDTH - cb_invert_colors.getWidth() - Skin.getSkin().getGroupData().group()
                .getRightOffset(), cb_invert_colors.getY());

        label_contrast_intensity.place(cb_high_contrast, BOTTOM_LEFT);
        slider_contrast.place(label_contrast_intensity, RIGHT_MID);

        label_contrast_brightness.place(label_contrast_intensity, BOTTOM_LEFT);
        slider_brightness.place(label_contrast_brightness, RIGHT_MID);

        label_contrast_clarity.place(label_contrast_brightness, BOTTOM_LEFT);
        slider_clarity.place(label_contrast_clarity, RIGHT_MID);

        group_contrast.compileCanvas();

        // CVD
        TitledBorderGroup group_cvd = new TitledBorderGroup(AbstractOptionsMenu.i18n("cvd_header"));
        group_cvd.setFixedWidth(GROUP_WIDTH);
        addChild(group_cvd);

        PulldownMenu<CVDMode> pm_cvd = new PulldownMenu<>();
        pm_cvd.addItem(new PulldownItem<>(AbstractOptionsMenu.i18n("cvd_standard"), CVDMode.NONE));
        pm_cvd.addItem(new PulldownItem<>(AbstractOptionsMenu.i18n("cvd_protanopia"), CVDMode.PROTANOPIA));
        pm_cvd.addItem(new PulldownItem<>(AbstractOptionsMenu.i18n("cvd_deuteranopia"), CVDMode.DEUTERANOPIA));
        pm_cvd.addItem(new PulldownItem<>(AbstractOptionsMenu.i18n("cvd_tritanopia"), CVDMode.TRITANOPIA));

        Label label_cvd_mode = new Label(AbstractOptionsMenu.i18n("colour_vision"), Skin.getSkin().getEditFont());
        label_cvd_mode.setDim(label_area_width, label_cvd_mode.getHeight());
        group_cvd.addChild(label_cvd_mode);

        PulldownButton<CVDMode> pb_cvd = new PulldownButton<>(gui_root, pm_cvd, Renderer.getRenderer()
                .getSettings().cvd_mode, contrast_slider_width);
        group_cvd.addChild(pb_cvd);

        Label label_cvd_intensity = new Label(AbstractOptionsMenu.i18n("cvd_intensity"), Skin.getSkin().getEditFont());
        label_cvd_intensity.setDim(label_area_width, label_cvd_intensity.getHeight());
        label_cvd_intensity.setDisabled(Renderer.getRenderer().getSettings().cvd_mode == 0);
        group_cvd.addChild(label_cvd_intensity);

        // Support up to 2.0 intensity (40 steps)
        Slider slider_cvd = new Slider(contrast_slider_width, 0, 2 * MAX_VALUE, (int) (Renderer.getRenderer()
                .getSettings().cvd_intensity * MAX_VALUE));
        slider_cvd.setDisabled(Renderer.getRenderer().getSettings().cvd_mode == 0);
        group_cvd.addChild(slider_cvd);

        pm_cvd.addItemChosenListener((_, _) -> {
            CVDMode mode = pm_cvd.getChosenItem().map(PulldownItem::getAttachment).orElse(CVDMode.NONE);
            Renderer.getRenderer().getSettings().cvd_mode = mode.getValue();
            slider_cvd.setDisabled(mode == CVDMode.NONE);
            label_cvd_intensity.setDisabled(mode == CVDMode.NONE);
        });
        slider_cvd.addValueListener(value -> Renderer.getRenderer().getSettings().cvd_intensity = (float) value
                / MAX_VALUE);

        label_cvd_mode.place();
        pb_cvd.place(label_cvd_mode, RIGHT_MID);
        label_cvd_intensity.place(label_cvd_mode, BOTTOM_LEFT);
        slider_cvd.place(label_cvd_intensity, RIGHT_MID);
        group_cvd.compileCanvas();

        // Team Colours
        TitledBorderGroup group_team_colours = new TitledBorderGroup(AbstractOptionsMenu.i18n("team_header"));
        group_team_colours.setFixedWidth(GROUP_WIDTH);
        addChild(group_team_colours);

        PulldownMenu<Integer> pm_team = new ScrollablePulldownMenu<>(6);
        for (int i = 0; i < Renderer.getRenderer().getSettings().team_colours.length; i++) {
            String player_str = AbstractOptionsMenu.i18n("player", Integer.toString(i + 1));
            PulldownItem<Integer> item = new PulldownItem<>(player_str, i);
            item.setLabelColor(Renderer.getRenderer().getSettings().team_colours[i]);
            pm_team.addItem(item);
        }
        PulldownButton<Integer> pb_team = new PulldownButton<>(gui_root, pm_team, 0, 150);
        group_team_colours.addChild(pb_team);

        // Colour Preview Box
        class ColourBox extends GUIObject {
            private Color.@NonNull Linear colour = Color.Linear.WHITE;

            ColourBox() {
                setDim(20, 20);
            }

            @Override
            protected void renderGeometry(@NonNull GUIRenderer renderer) {
                renderer.drawColoredQuad(0, 0, getWidth(), getHeight(), colour);
            }

            void setColour(@NonNull Color c) {
                this.colour = c instanceof Color.Linear linear ? linear : new Color.Linear(c);
            }
        }
        ColourBox colourBox = new ColourBox();
        group_team_colours.addChild(colourBox);

        // Hue Slider (Colour Ramp)
        Slider slider_hue = new Slider(FULL_SLIDER_WIDTH, 0, 360, 0);
        group_team_colours.addChild(slider_hue);

        // Reset Button
        HorizButton button_reset = new HorizButton(AbstractOptionsMenu.i18n("reset"), 100);
        group_team_colours.addChild(button_reset);

        // Update logic
        Runnable updateColour = () -> {
            int teamIndex = pm_team.getChosenItem().map(PulldownItem::getAttachment).orElse(0);
            float hue = slider_hue.getValue();
            int rgb = java.awt.Color.HSBtoRGB(hue / 360f, 1f, 1f);
            var newColour = new Color.Standard(rgb);
            Renderer.getRenderer().getSettings().team_colours[teamIndex] = newColour;
            colourBox.setColour(newColour);

            // Update the pulldown item colour
            pm_team.getChosenItem().ifPresent(pi -> pi.setLabelColor(newColour));
            pb_team.setLabelColor(newColour);
        };

        Runnable refreshUI = () -> {
            int index = pm_team.getChosenItem().map(PulldownItem::getAttachment).orElse(0);
            var currentColour = Renderer.getRenderer().getSettings().team_colours[index];
            float[] hsb = java.awt.Color.RGBtoHSB(Math.round(currentColour.r() * 255), Math.round(currentColour.g()
                    * 255),
                    Math.round(currentColour.b() * 255), null);
            slider_hue.setValue((int) (hsb[0] * 360f));
            colourBox.setColour(currentColour);
        };

        pm_team.addItemChosenListener((_, _) -> refreshUI.run());
        refreshUI.run();

        slider_hue.addValueListener(_ -> updateColour.run());

        button_reset.addMouseClickListener((_, _, _, _) -> {
            int index = pm_team.getChosenItem().map(PulldownItem::getAttachment).orElse(0);
            Renderer.getRenderer().getSettings().team_colours[index] = new Color.Standard(
                    Settings.DEFAULT_TEAM_COLOURS[index]);
            refreshUI.run();
            pm_team.getChosenItem().ifPresent(pi -> pi.setLabelColor(Renderer.getRenderer()
                    .getSettings().team_colours[index]));
            pb_team.setLabelColor(Renderer.getRenderer().getSettings().team_colours[index]);
        });

        CheckBox cb_team_stencil = new CheckBox(Renderer.getRenderer().getSettings().team_stencil, AbstractOptionsMenu
                .i18n("team_stencil"), AbstractOptionsMenu.i18n("team_stencil_tip"));
        cb_team_stencil.addCheckBoxListener(marked -> Renderer.getRenderer().getSettings().team_stencil = marked);
        group_team_colours.addChild(cb_team_stencil);

        pb_team.place();
        colourBox.place(pb_team, RIGHT_MID);
        button_reset.place(colourBox, RIGHT_MID);
        slider_hue.place(pb_team, BOTTOM_LEFT);
        cb_team_stencil.place(slider_hue, BOTTOM_LEFT);

        group_team_colours.compileCanvas();

        // Placement
        GUIObject top_spacer = new GUIObject() {
        };
        top_spacer.setDim(0, 10);
        addChild(top_spacer);

        int group_spacing = 25;
        top_spacer.place();
        group_contrast.place(top_spacer, BOTTOM_LEFT, 0);
        group_cvd.place(group_contrast, BOTTOM_LEFT, group_spacing);
        group_team_colours.place(group_cvd, BOTTOM_LEFT, group_spacing);

        compileCanvas();
    }
}
