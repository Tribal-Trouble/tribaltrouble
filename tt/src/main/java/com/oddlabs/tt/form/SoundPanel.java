package com.oddlabs.tt.form;

import com.oddlabs.tt.audio.AudioManager;
import com.oddlabs.tt.audio.openal.OpenALManager;
import com.oddlabs.tt.global.Settings;
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

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.RIGHT_MID;

public class SoundPanel extends Panel {
    private static final int SLIDER_WIDTH = 270;
    private static final int MAX_VALUE = 20;
    private static final boolean TEMPORARILY_DISABLE_MUSIC_CONTROLS = false;

    public SoundPanel(GUIRoot gui_root) {
        super(AbstractOptionsMenu.i18n("sound_caption"));

        // Sound
        Group group_music = new Group();
        addChild(group_music);
        Label label_music_low = new Label(AbstractOptionsMenu.i18n("low"), Skin.getSkin().getEditFont());
        group_music.addChild(label_music_low);
        Label label_music_high = new Label(AbstractOptionsMenu.i18n("high"), Skin.getSkin().getEditFont());
        group_music.addChild(label_music_high);
        CheckBox cb_music = new CheckBox(Settings.getSettings().play_music, AbstractOptionsMenu.i18n("music"));
        group_music.addChild(cb_music);
        Label label_music = new Label(AbstractOptionsMenu.i18n("music_volume"), Skin.getSkin().getEditFont());
        group_music.addChild(label_music);

        Slider slider_music = new Slider(SLIDER_WIDTH, 0, MAX_VALUE, (int) (Settings.getSettings().music_gain * (MAX_VALUE)));
        slider_music.setDisabled(TEMPORARILY_DISABLE_MUSIC_CONTROLS || !cb_music.isMarked());
        group_music.addChild(slider_music);

        cb_music.addCheckBoxListener(marked -> {
            if (Settings.getSettings().play_music != marked)
                Renderer.getRenderer().toggleMusic();
            slider_music.setDisabled(!marked);
            Settings.getSettings().play_music = marked;
        });
        slider_music.addValueListener(value -> {
            float music_gain = (float) value / (MAX_VALUE);
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
        addChild(group_sound);
        Label label_sound_low = new Label(AbstractOptionsMenu.i18n("low"), Skin.getSkin().getEditFont());
        group_sound.addChild(label_sound_low);
        Label label_sound_high = new Label(AbstractOptionsMenu.i18n("high"), Skin.getSkin().getEditFont());
        group_sound.addChild(label_sound_high);
        CheckBox cb_sound = new CheckBox(Settings.getSettings().play_sfx, AbstractOptionsMenu.i18n("sound_effects"));
        group_sound.addChild(cb_sound);
        Label label_sound = new Label(AbstractOptionsMenu.i18n("sound_effects_volume"), Skin.getSkin().getEditFont());
        group_sound.addChild(label_sound);

        Slider slider_sound = new Slider(SLIDER_WIDTH, 0, MAX_VALUE, (int) (Settings.getSettings().sound_gain * (MAX_VALUE)));
        slider_sound.setDisabled(!cb_sound.isMarked());
        group_sound.addChild(slider_sound);

        cb_sound.addCheckBoxListener(marked -> {
            if (Settings.getSettings().play_sfx != marked)
                Renderer.getRenderer().toggleSound();
            slider_sound.setDisabled(!marked);
            Settings.getSettings().play_sfx = marked;
        });
        slider_sound.addValueListener(value -> Settings.getSettings().sound_gain = (float) value / (MAX_VALUE));

        cb_sound.place();
        label_sound.place(cb_sound, BOTTOM_LEFT);
        label_sound_low.place(label_sound, BOTTOM_LEFT);
        slider_sound.place(label_sound_low, RIGHT_MID);
        label_sound_high.place(slider_sound, RIGHT_MID);
        group_sound.compileCanvas();
        boolean audioCreated = Renderer.getLocalInput().audioIsCreated();
        group_sound.setDisabled(!audioCreated);

        // Audio Output
        Group group_output = new Group();
        addChild(group_output);
        Label label_output = new Label(AbstractOptionsMenu.i18n("audio_output"), Skin.getSkin().getEditFont());
        group_output.addChild(label_output);

        PulldownMenu<Void> pm_output = new PulldownMenu<>();
        pm_output.addItem(new PulldownItem<>(AbstractOptionsMenu.i18n("audio_output_speakers")));
        pm_output.addItem(new PulldownItem<>(AbstractOptionsMenu.i18n("audio_output_headphones")));

        int initialOutput = Settings.getSettings().headphone_mode ? 1 : 0;
        PulldownButton<Void> pb_output = new PulldownButton<>(gui_root, pm_output, initialOutput, 150);
        group_output.addChild(pb_output);

        label_output.place();
        pb_output.place(label_output, RIGHT_MID);

        AudioManager manager = null;
        if (audioCreated) {
            try {
                manager = AudioManager.getManager();
            } catch (Exception e) {
                // Ignore
            }
        }

        boolean hrtfSupported = false;
        if (manager != null) {
            hrtfSupported = manager.isHRTFSupported();
        }

        if (hrtfSupported) {
            final AudioManager mgr = manager;
            pm_output.addItemChosenListener((_, index) -> {
                boolean headphone = (index == 1);
                Settings.getSettings().headphone_mode = headphone;
                if (mgr instanceof OpenALManager alManager) {
                    alManager.setHeadphoneMode(headphone);
                }
            });
        } else {
            pb_output.setDisabled(true);
            pm_output.chooseItem(0);
        }

        group_output.compileCanvas();

        // Placement
        group_music.place();
        group_sound.place(group_music, BOTTOM_LEFT);
        group_output.place(group_sound, BOTTOM_LEFT);
        compileCanvas();
    }
}