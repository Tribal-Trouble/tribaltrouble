package com.oddlabs.tt.form;

import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.gui.CheckBox;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.Panel;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.gui.Slider;
import org.jspecify.annotations.NonNull;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.RIGHT_MID;

public class CameraPanel extends Panel {
    private static final int SLIDER_WIDTH = 270;
    private static final int MAX_VALUE = 20;

    public CameraPanel() {
        super(AbstractOptionsMenu.i18n("camera_caption"));

        // Pan speed
        Slider slider_pan = new Slider(SLIDER_WIDTH, MAX_VALUE / 4, MAX_VALUE * 7 / 4,
                Math.round(Settings.getSettings().camera_pan_speed * MAX_VALUE));
        slider_pan.addValueListener(value -> Settings.getSettings().camera_pan_speed = (float) value / MAX_VALUE);
        Group group_pan = speedGroup("camera_pan_speed", slider_pan);

        // Pan acceleration
        Group group_pan_accel = new Group();
        addChild(group_pan_accel);
        CheckBox cb_pan_accel = new CheckBox(Settings.getSettings().camera_pan_acceleration,
                AbstractOptionsMenu.i18n("camera_pan_acceleration"),
                AbstractOptionsMenu.i18n("camera_pan_acceleration_tip"));
        cb_pan_accel.addCheckBoxListener(marked -> Settings.getSettings().camera_pan_acceleration = marked);
        group_pan_accel.addChild(cb_pan_accel);
        cb_pan_accel.place();
        group_pan_accel.compileCanvas();

        // Rotate speed
        Slider slider_rotate = new Slider(SLIDER_WIDTH, MAX_VALUE / 4, MAX_VALUE * 7 / 4,
                Math.round(Settings.getSettings().camera_rotate_speed * MAX_VALUE));
        slider_rotate.addValueListener(value -> Settings.getSettings().camera_rotate_speed = (float) value / MAX_VALUE);
        Group group_rotate = speedGroup("camera_rotate_speed", slider_rotate);

        // Zoom speed
        Slider slider_zoom = new Slider(SLIDER_WIDTH, MAX_VALUE / 4, MAX_VALUE * 7 / 4,
                Math.round(Settings.getSettings().camera_zoom_speed * MAX_VALUE));
        slider_zoom.addValueListener(value -> Settings.getSettings().camera_zoom_speed = (float) value / MAX_VALUE);
        Group group_zoom = speedGroup("camera_zoom_speed", slider_zoom);

        // Cinematic mode speed multiplier
        Slider slider_cinematic = new Slider(SLIDER_WIDTH, 1, MAX_VALUE,
                Math.round(Settings.getSettings().cinematic_camera_speed * MAX_VALUE));
        slider_cinematic.addValueListener(
                value -> Settings.getSettings().cinematic_camera_speed = (float) value / MAX_VALUE);
        Group group_cinematic = speedGroup("cinematic_camera_speed", slider_cinematic);

        // Cinematic limits
        Group group_limits = new Group();
        addChild(group_limits);
        CheckBox cb_unlock_limits = new CheckBox(Settings.getSettings().cinematic_unlock_limits,
                AbstractOptionsMenu.i18n("cinematic_unlock_limits"),
                AbstractOptionsMenu.i18n("cinematic_unlock_limits_tip"));
        cb_unlock_limits.addCheckBoxListener(marked -> Settings.getSettings().cinematic_unlock_limits = marked);
        group_limits.addChild(cb_unlock_limits);
        cb_unlock_limits.place();
        group_limits.compileCanvas();

        // Invert camera
        Group group_invert_camera = new Group();
        addChild(group_invert_camera);
        CheckBox cb_invert_camera = new CheckBox(Settings.getSettings().invert_camera_pitch, AbstractOptionsMenu.i18n(
                "invert_camera_pitch"), AbstractOptionsMenu.i18n("invert_camera_pitch_tip"));
        cb_invert_camera.addCheckBoxListener(marked -> Settings.getSettings().invert_camera_pitch = marked);
        group_invert_camera.addChild(cb_invert_camera);
        CheckBox cb_invert_camera_yaw = new CheckBox(Settings.getSettings().invert_camera_yaw, AbstractOptionsMenu.i18n(
                "invert_camera_yaw"), AbstractOptionsMenu.i18n("invert_camera_yaw_tip"));
        cb_invert_camera_yaw.addCheckBoxListener(marked -> Settings.getSettings().invert_camera_yaw = marked);
        group_invert_camera.addChild(cb_invert_camera_yaw);
        cb_invert_camera.place();
        cb_invert_camera_yaw.place(cb_invert_camera, BOTTOM_LEFT);
        group_invert_camera.compileCanvas();

        // Placement
        group_pan.place();
        group_pan_accel.place(group_pan, BOTTOM_LEFT);
        group_rotate.place(group_pan_accel, BOTTOM_LEFT);
        group_zoom.place(group_rotate, BOTTOM_LEFT);
        group_cinematic.place(group_zoom, BOTTOM_LEFT);
        group_limits.place(group_cinematic, BOTTOM_LEFT);
        group_invert_camera.place(group_limits, BOTTOM_LEFT);
        compileCanvas();
    }

    private @NonNull Group speedGroup(@NonNull String headline_key, @NonNull Slider slider) {
        Group group = new Group();
        addChild(group);
        Label label_headline = new Label(AbstractOptionsMenu.i18n(headline_key), Skin.getSkin().getEditFont());
        group.addChild(label_headline);
        Label label_slow = new Label(AbstractOptionsMenu.i18n("speed_slow"), Skin.getSkin().getEditFont());
        group.addChild(label_slow);
        Label label_fast = new Label(AbstractOptionsMenu.i18n("speed_fast"), Skin.getSkin().getEditFont());
        group.addChild(label_fast);
        group.addChild(slider);
        label_headline.place();
        label_slow.place(label_headline, BOTTOM_LEFT);
        slider.place(label_slow, RIGHT_MID);
        label_fast.place(slider, RIGHT_MID);
        group.compileCanvas();
        return group;
    }
}
