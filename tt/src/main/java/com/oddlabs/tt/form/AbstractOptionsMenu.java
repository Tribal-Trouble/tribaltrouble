package com.oddlabs.tt.form;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.gui.CancelListener;
import com.oddlabs.tt.gui.CheckBox;
import com.oddlabs.tt.gui.ColumnInfo;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.IconLabel;
import com.oddlabs.tt.gui.IconQuad;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.Languages;
import com.oddlabs.tt.gui.LocalInput;
import com.oddlabs.tt.gui.MultiColumnComboBox;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.gui.Panel;
import com.oddlabs.tt.gui.PanelGroup;
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
import com.oddlabs.tt.util.ServerMessageBundler;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.Locale;
import java.util.ResourceBundle;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.LEFT_MID;
import static com.oddlabs.tt.gui.Placement.RIGHT_MID;
import static com.oddlabs.tt.gui.Placement.RIGHT_TOP;

public abstract class AbstractOptionsMenu extends Form {
	private static final int BUTTON_WIDTH = 100;
	private static final int MAX_VALUE = 20;

	private static final int SLIDER_WIDTH = 270;

	private static final boolean TEMPORARILY_DISABLE_MUSIC_CONTROLS = false;

    private final PulldownMenu<Void> pm_gamespeed = new PulldownMenu<>();

	AbstractOptionsMenu(@NonNull GUIRoot gui_root) {
        ResourceBundle bundle = ResourceBundle.getBundle(OptionsMenu.class.getName());
		Label label_headline = new Label(Utils.getBundleString(bundle, "options_caption"), Skin.getSkin().getHeadlineFont());
		addChild(label_headline);

		PanelGroup panel_group = new PanelGroup(
                createGeneralPanel(gui_root, bundle, this),
                createDisplayPanel(gui_root, bundle, this),
                createAccessibilityPanel(gui_root, bundle, this),
                createSoundPanel(bundle),
                createLanguagePanel(gui_root, bundle)
        );
 		addChild(panel_group);

		// Buttons
		HorizButton button_close = new HorizButton(Utils.getBundleString(bundle, "close"), BUTTON_WIDTH);
		button_close.addMouseClickListener(new CancelListener(this));
		addChild(button_close);

		HorizButton button_about = new HorizButton(Utils.getBundleString(bundle, "about"), BUTTON_WIDTH);
		button_about.addMouseClickListener((_,_,_,_) -> gui_root.addModalForm(new CreditsForm()));
		addChild(button_about);

		// Place objects
		label_headline.place();
		panel_group.place(label_headline, BOTTOM_LEFT);
		button_close.place(Origin.AT_END);
		button_about.place(button_close, LEFT_MID);
		compileCanvas();
	}

    private static Panel createGeneralPanel(@NonNull GUIRoot gui_root, @NonNull ResourceBundle bundle, @NonNull AbstractOptionsMenu options) {
        Panel general = new Panel(Utils.getBundleString(bundle, "general_settings_caption"));

        // Invert camera
        Group group_invert_camera = new Group();
         general.addChild(group_invert_camera);
        CheckBox cb_invert_camera = new CheckBox(Settings.getSettings().invert_camera_pitch, Utils.getBundleString(bundle, "invert_camera"), Utils.getBundleString(bundle, "invert_camera_tip"));
         cb_invert_camera.addCheckBoxListener(marked -> Settings.getSettings().invert_camera_pitch = marked);
        group_invert_camera.addChild(cb_invert_camera);
        cb_invert_camera.place();
        group_invert_camera.compileCanvas();

         // Aggressive units
        Group group_aggressive_units = new Group();
        general.addChild(group_aggressive_units);
        CheckBox cb_aggressive_units = new CheckBox(Settings.getSettings().aggressive_units, Utils.getBundleString(bundle, "aggressive_units"), Utils.getBundleString(bundle, "aggressive_units_tip", "Ctrl-A"));
        cb_aggressive_units.addCheckBoxListener(marked -> Settings.getSettings().aggressive_units = marked);
        group_aggressive_units.addChild(cb_aggressive_units);
        cb_aggressive_units.place();
        group_aggressive_units.compileCanvas();

        // Mapmode delay
        Group group_mapmode = new Group();
        general.addChild(group_mapmode);
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
        general.addChild(group_tooltip);
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
        general.addChild(group_gamespeed);
        Label label_gamespeed = new Label(Utils.getBundleString(bundle, "gamespeed"), Skin.getSkin().getEditFont());
        group_gamespeed.addChild(label_gamespeed);
        
        options.pm_gamespeed.addItem(new PulldownItem<>(ServerMessageBundler.getGamespeedString(Game.GAMESPEED_PAUSE)));
        options.pm_gamespeed.addItem(new PulldownItem<>(ServerMessageBundler.getGamespeedString(Game.GAMESPEED_SLOW)));
        options.pm_gamespeed.addItem(new PulldownItem<>(ServerMessageBundler.getGamespeedString(Game.GAMESPEED_NORMAL)));
        options.pm_gamespeed.addItem(new PulldownItem<>(ServerMessageBundler.getGamespeedString(Game.GAMESPEED_FAST)));
        options.pm_gamespeed.addItem(new PulldownItem<>(ServerMessageBundler.getGamespeedString(Game.GAMESPEED_LUDICROUS)));
        
        PulldownButton pb_gamespeed = new PulldownButton(gui_root, options.pm_gamespeed, 150);
        options.pm_gamespeed.addItemChosenListener((_, item_index) -> options.changeGamespeed(item_index));
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
        general.compileCanvas();

        return general;
    }

    private static Panel createDisplayPanel(@NonNull GUIRoot gui_root, @NonNull ResourceBundle bundle, @NonNull AbstractOptionsMenu options) {
        Panel display = new Panel(Utils.getBundleString(bundle, "graphics_caption"));

        // Fullscreen
        Group group_fullscreen = new Group();
        display.addChild(group_fullscreen);
        CheckBox cb_fullscreen = new CheckBox(Renderer.getLocalInput().inFullscreen(), Utils.getBundleString(bundle, "fullscreen"), Utils.getBundleString(bundle, "fullscreen_tip"));
        cb_fullscreen.addCheckBoxListener(_ -> {
            DisplayChangeForm display_change_form = new DisplayChangeForm(
                    switch_now -> Renderer.getLocalInput().toggleFullscreen());
            gui_root.addModalForm(display_change_form);
        });
        group_fullscreen.addChild(cb_fullscreen);
        cb_fullscreen.place();
        group_fullscreen.compileCanvas();

        // Hardware cursor
        Group group_hardware_cursor = new Group();
        display.addChild(group_hardware_cursor);
        CheckBox cb_hardware_cursor = new CheckBox(Settings.getSettings().use_native_cursor, Utils.getBundleString(bundle, "hardware_cursor"), Utils.getBundleString(bundle, "hardware_cursor_tip", "Ctrl-H"));
        cb_hardware_cursor.addCheckBoxListener(marked -> Settings.getSettings().use_native_cursor = marked);
        group_hardware_cursor.addChild(cb_hardware_cursor);
        cb_hardware_cursor.place();
        group_hardware_cursor.compileCanvas();
        group_hardware_cursor.setDisabled((Renderer.getLocalInput().getNativeCursorCaps() & LocalInput.CURSOR_ONE_BIT_TRANSPARENCY) == 0);

        // Detail
        Group group_detail = new Group();
        display.addChild(group_detail);

        Label label_detail = new Label(Utils.getBundleString(bundle, "graphical_detail"), Skin.getSkin().getEditFont());
        group_detail.addChild(label_detail);

        int initial_detail_value = Settings.getSettings().graphic_detail;
        PulldownMenu<Void> pm_detail = new PulldownMenu<>();
        pm_detail.addItem(new PulldownItem<>(Utils.getBundleString(bundle, "low")));
        pm_detail.addItem(new PulldownItem<>(Utils.getBundleString(bundle, "medium")));
        pm_detail.addItem(new PulldownItem<>(Utils.getBundleString(bundle, "high")));
        PulldownButton pb_detail = new PulldownButton(gui_root, pm_detail, initial_detail_value, 150);

        group_detail.addChild(pb_detail);
        options.addCloseListener(() -> {
            int slider_value = pm_detail.getChosenItemIndex();
            if (initial_detail_value != slider_value) {
                Settings.getSettings().graphic_detail = slider_value;
                gui_root.addModalForm(new MessageForm(Utils.getBundleString(bundle, "change_next_run")));
            }
        });
        label_detail.place();
        pb_detail.place(label_detail, BOTTOM_LEFT);
        group_detail.compileCanvas();

        // Display mode
        Group mode_group = new Group();
        display.addChild(mode_group);

        Label mode_label = new Label(Utils.getBundleString(bundle, "display_mode"), Skin.getSkin().getEditFont());
        mode_group.addChild(mode_label);

        ColumnInfo[] mode_infos = new ColumnInfo[]{new ColumnInfo("", 150)};
        MultiColumnComboBox<SerializableDisplayMode> mode_list_box = new MultiColumnComboBox<>(gui_root, mode_infos, 200, false);

        SerializableDisplayMode[] modes = Renderer.getRenderer().getWindow().getAvailableDisplayModes();
        SerializableDisplayMode current_mode = Renderer.getRenderer().getLocalInput().getCurrentMode();
        Row<SerializableDisplayMode, Label> current_row = null;
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].getBitsPerPixel() == current_mode.getBitsPerPixel()) {
                String mode_string = Utils.getBundleString(bundle, "mode", Integer.toString(modes[i].getWidth()), Integer.toString(modes[i].getHeight()), Integer.toString(modes[i].getFrequency()));
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
            public void rowChosen(SerializableDisplayMode mode) {
                gui_root.addModalForm(new DisplayChangeForm(switch_now -> {
                    Renderer.getLocalInput().switchMode(mode, switch_now);
                    gui_root.displayChanged();
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
        group_fullscreen.place(group_detail, BOTTOM_LEFT);
        group_hardware_cursor.place(group_fullscreen, BOTTOM_LEFT);
        display.compileCanvas();

        return display;
    }

    private static Panel createAccessibilityPanel(@NonNull GUIRoot gui_root, @NonNull ResourceBundle bundle, @NonNull AbstractOptionsMenu options) {
        Panel accessibility = new Panel(Utils.getBundleString(bundle, "accessibility_caption"));

        // High Contrast
        Group group_contrast = new Group();
        accessibility.addChild(group_contrast);
        CheckBox cb_high_contrast = new CheckBox(Settings.getSettings().high_contrast, Utils.getBundleString(bundle, "high_contrast"), Utils.getBundleString(bundle, "high_contrast_tip"));
        group_contrast.addChild(cb_high_contrast);
        
        CheckBox cb_team_stencil = new CheckBox(Settings.getSettings().team_stencil, Utils.getBundleString(bundle, "team_stencil"), Utils.getBundleString(bundle, "team_stencil_tip"));
        group_contrast.addChild(cb_team_stencil);
        
        Label label_contrast_intensity = new Label(Utils.getBundleString(bundle, "contrast_intensity"), Skin.getSkin().getEditFont());
        group_contrast.addChild(label_contrast_intensity);
        
        // Support up to 2.0 intensity (40 steps)
        Slider slider_contrast = new Slider(SLIDER_WIDTH, 0, 2 * MAX_VALUE, (int)(Settings.getSettings().contrast_intensity * MAX_VALUE));
        slider_contrast.setDisabled(!Settings.getSettings().high_contrast);
        group_contrast.addChild(slider_contrast);

        cb_high_contrast.addCheckBoxListener(marked -> {
            Settings.getSettings().high_contrast = marked;
            slider_contrast.setDisabled(!marked);
        });
        cb_team_stencil.addCheckBoxListener(marked -> Settings.getSettings().team_stencil = marked);
        
        slider_contrast.addValueListener(value -> Settings.getSettings().contrast_intensity = (float)value / MAX_VALUE);

        // Layout:
        // [Checkbox High Contrast]
        // [Checkbox Team Overlay]
        // [Label Intensity] [Slider]
        cb_high_contrast.place();
        cb_team_stencil.place(cb_high_contrast, BOTTOM_LEFT);
        label_contrast_intensity.place(cb_team_stencil, BOTTOM_LEFT);
        slider_contrast.place(label_contrast_intensity, RIGHT_MID);
        group_contrast.compileCanvas();

        // CVD
        Group group_cvd = new Group();
        accessibility.addChild(group_cvd);
        Label label_cvd = new Label(Utils.getBundleString(bundle, "color_vision"), Skin.getSkin().getEditFont());
        group_cvd.addChild(label_cvd);

        PulldownMenu<Void> pm_cvd = new PulldownMenu<>();
        pm_cvd.addItem(new PulldownItem<>(Utils.getBundleString(bundle, "cvd_standard")));
        pm_cvd.addItem(new PulldownItem<>(Utils.getBundleString(bundle, "cvd_protanopia")));
        pm_cvd.addItem(new PulldownItem<>(Utils.getBundleString(bundle, "cvd_deuteranopia")));
        pm_cvd.addItem(new PulldownItem<>(Utils.getBundleString(bundle, "cvd_tritanopia")));
        PulldownButton pb_cvd = new PulldownButton(gui_root, pm_cvd, Settings.getSettings().cvd_mode, 200);
        group_cvd.addChild(pb_cvd);

        Label label_cvd_intensity = new Label(Utils.getBundleString(bundle, "cvd_intensity"), Skin.getSkin().getEditFont());
        group_cvd.addChild(label_cvd_intensity);

        // Support up to 2.0 intensity (40 steps)
        Slider slider_cvd = new Slider(SLIDER_WIDTH, 0, 2 * MAX_VALUE, (int)(Settings.getSettings().cvd_intensity * MAX_VALUE));
        slider_cvd.setDisabled(Settings.getSettings().cvd_mode == 0);
        group_cvd.addChild(slider_cvd);

        pm_cvd.addItemChosenListener((_, index) -> {
            Settings.getSettings().cvd_mode = index;
            slider_cvd.setDisabled(index == 0);
        });
        slider_cvd.addValueListener(value -> Settings.getSettings().cvd_intensity = (float)value / MAX_VALUE);

        // Layout:
        // [Label Mode] [Pulldown]
        // [Label Intensity] [Slider]
        label_cvd.place();
        pb_cvd.place(label_cvd, RIGHT_MID);
        label_cvd_intensity.place(label_cvd, BOTTOM_LEFT);
        slider_cvd.place(label_cvd_intensity, RIGHT_MID);
        group_cvd.compileCanvas();

        // Placement
        group_contrast.place();
        group_cvd.place(group_contrast, BOTTOM_LEFT);
        
        accessibility.compileCanvas();
        return accessibility;
    }

    private static Panel createSoundPanel(@NonNull ResourceBundle bundle) {
        Panel sound = new Panel(Utils.getBundleString(bundle, "sound_caption"));

        // Sound
        Group group_music = new Group();
        sound.addChild(group_music);
        Label label_music_low = new Label(Utils.getBundleString(bundle, "low"), Skin.getSkin().getEditFont());
        group_music.addChild(label_music_low);
        Label label_music_high = new Label(Utils.getBundleString(bundle, "high"), Skin.getSkin().getEditFont());
        group_music.addChild(label_music_high);
        CheckBox cb_music = new CheckBox(Settings.getSettings().play_music, Utils.getBundleString(bundle, "music"));
        group_music.addChild(cb_music);
        Label label_music = new Label(Utils.getBundleString(bundle, "music_volume"), Skin.getSkin().getEditFont());
        group_music.addChild(label_music);
        
        Slider slider_music = new Slider(SLIDER_WIDTH, 0, MAX_VALUE, (int)(Settings.getSettings().music_gain*(MAX_VALUE)));
        slider_music.setDisabled(TEMPORARILY_DISABLE_MUSIC_CONTROLS || !cb_music.isMarked());
        group_music.addChild(slider_music);
        
        cb_music.addCheckBoxListener(marked -> {
            if (Settings.getSettings().play_music != marked)
                Renderer.getRenderer().toggleMusic();
            slider_music.setDisabled(!marked);
            Settings.getSettings().play_music = marked;
        });
        slider_music.addValueListener(value -> {
            float music_gain = (float)value/(MAX_VALUE);
            Settings.getSettings().music_gain = music_gain;
            Renderer.getRenderer().getMusicPlayer().setGain(music_gain);
        });
        
        cb_music.place();
        label_music.place(cb_music, BOTTOM_LEFT);
        label_music_low.place(label_music, BOTTOM_LEFT);
        slider_music.place(label_music_low, RIGHT_MID);
        label_music_high.place(slider_music, RIGHT_MID);
        group_music.compileCanvas();
        group_music.setDisabled(TEMPORARILY_DISABLE_MUSIC_CONTROLS || !Renderer.getLocalInput().audioIsCreated());

        Group group_sound = new Group();
        sound.addChild(group_sound);
        Label label_sound_low = new Label(Utils.getBundleString(bundle, "low"), Skin.getSkin().getEditFont());
        group_sound.addChild(label_sound_low);
        Label label_sound_high = new Label(Utils.getBundleString(bundle, "high"), Skin.getSkin().getEditFont());
        group_sound.addChild(label_sound_high);
        CheckBox cb_sound = new CheckBox(Settings.getSettings().play_sfx, Utils.getBundleString(bundle, "sound_effects"));
        group_sound.addChild(cb_sound);
        Label label_sound = new Label(Utils.getBundleString(bundle, "sound_effects_volume"), Skin.getSkin().getEditFont());
        group_sound.addChild(label_sound);
        
        Slider slider_sound = new Slider(SLIDER_WIDTH, 0, MAX_VALUE, (int)(Settings.getSettings().sound_gain*(MAX_VALUE)));
        slider_sound.setDisabled(!cb_sound.isMarked());
        group_sound.addChild(slider_sound);
        
        cb_sound.addCheckBoxListener(marked -> {
            if (Settings.getSettings().play_sfx != marked)
                Renderer.getRenderer().toggleSound();
            slider_sound.setDisabled(!marked);
            Settings.getSettings().play_sfx = marked;
        });
        slider_sound.addValueListener(value -> Settings.getSettings().sound_gain = (float)value/(MAX_VALUE));
        
        cb_sound.place();
        label_sound.place(cb_sound, BOTTOM_LEFT);
        label_sound_low.place(label_sound, BOTTOM_LEFT);
        slider_sound.place(label_sound_low, RIGHT_MID);
        label_sound_high.place(slider_sound, RIGHT_MID);
        group_sound.compileCanvas();
        group_sound.setDisabled(!Renderer.getLocalInput().audioIsCreated());

        // Placement
        group_music.place();
        group_sound.place(group_music, BOTTOM_LEFT);
        sound.compileCanvas();

        return sound;
    }

    private static Panel createLanguagePanel(@NonNull GUIRoot gui_root, @NonNull ResourceBundle bundle) {
        Panel language = new Panel(Utils.getBundleString(bundle, "language_caption"));

        // language
        Group language_group = new Group();
        language.addChild(language_group);
        Label language_label = new Label(Utils.getBundleString(bundle, "language_label"), Skin.getSkin().getEditFont());
        language_group.addChild(language_label);

        ColumnInfo[] language_infos = new ColumnInfo[]{new ColumnInfo("", 300)};
        var language_list_box = new MultiColumnComboBox<@NonNull Locale>(gui_root, language_infos, 200, false);

        // Check language logic
        boolean languageFound = false;
        if (!Settings.getSettings().language.equals("default")) {
            for (String[] lang : Languages.getLanguages()) {
                if (Settings.getSettings().language.equals(lang[0])) {
                    languageFound = true;
                    break;
                }
            }
            if (!languageFound) {
                Settings.getSettings().language = "default";
            }
        }

        Row<Locale,IconLabel> selectedLanguage = null;
        IconLabel label = new IconLabel(Skin.getSkin().getFlagDefault(), new Label(Utils.getBundleString(bundle, "system_default"), Skin.getSkin().getMultiColumnComboBoxData().font()));
        Row<Locale,IconLabel> row = new Row<>(new IconLabel[]{label}, Renderer.getRenderer().getDefaultLocale());
        language_list_box.addRow(row);
        if (Settings.getSettings().language.equals("default"))
            selectedLanguage = row;
        String[][] languages = Languages.getLanguages();
        IconQuad[] flags = Languages.getFlags();
        for (int i = 0; i < languages.length; i++) {
            label = new IconLabel(flags[i], new Label(languages[i][1], Skin.getSkin().getMultiColumnComboBoxData().font()));
            row = new Row<>(new IconLabel[]{label}, Locale.of(languages[i][0]));
            language_list_box.addRow(row);
            if (languages[i][0].equals(Settings.getSettings().language))
                selectedLanguage = row;
        }

        language_list_box.selectRow(selectedLanguage);
        language_list_box.addRowListener(new RowListener<>() {
            @Override
            public void rowChosen(Locale locale) {
                Settings.getSettings().language = locale.getVariant().equals("default") ? "default" : locale.getLanguage();
                gui_root.addModalForm(new MessageForm(Utils.getBundleString(bundle, "language_change_next_run")));
            }
        });

        language_group.addChild(language_list_box);
        language_label.place();
        language_list_box.place(language_label, BOTTOM_LEFT);
        language_group.compileCanvas();

        // Placement
        language_group.place();
        language.compileCanvas();

        return language;
    }

	@Override
	protected void displayChangedNotify(int width, int height) {
		super.displayChangedNotify(width, height);
		centerPos();
	}

	protected final void chooseGamespeed(int speed) {
		pm_gamespeed.chooseItem(speed);
	}

	protected void changeGamespeed(int index) {
		Globals.gamespeed = index;
	}
}
