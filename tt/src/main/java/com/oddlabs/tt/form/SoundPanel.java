package com.oddlabs.tt.form;

import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.gui.*;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.RIGHT_MID;

public class SoundPanel extends Panel {
    private static final int SLIDER_WIDTH = 270;
    private static final int MAX_VALUE = 20;
    private static final boolean TEMPORARILY_DISABLE_MUSIC_CONTROLS = false;

    public SoundPanel(ResourceBundle bundle) {
        super(Utils.getBundleString(bundle, "sound_caption"));

        // Sound
        Group group_music = new Group();
        addChild(group_music);
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
        addChild(group_sound);
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
        compileCanvas();
    }
}