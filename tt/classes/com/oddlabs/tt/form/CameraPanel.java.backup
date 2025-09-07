package com.oddlabs.tt.form;

import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.Panel;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.gui.Slider;
import com.oddlabs.tt.guievent.MouseClickListener;
import com.oddlabs.tt.guievent.ValueListener;
import com.oddlabs.tt.input.Mouse;

/** Camera options panel: adjust mouse sensitivity and camera movement parameters. */
public class CameraPanel extends Panel {
    private static final int SLIDER_WIDTH = 270;
    private static final int SLIDER_MIN = 0;
    private static final int SLIDER_MAX = 100;

    // Keep slider refs so we can restore/update values
    private Slider sSensitivity;
    private Slider sAccelTime;
    private Slider sAccelFactor;
    private Slider sStartSpeed;
    private Slider sZoom;
    private Slider sAngle;
    private Slider sEdge;

    public CameraPanel(com.oddlabs.tt.gui.GUIRoot gui_root, String caption) {
        super(caption);

        // Mouse sensitivity -------------------------------------------------
        Group gSensitivity = new Group();
        addChild(gSensitivity);
        Label lblSens = new Label("Mouse sensitivity", Skin.getSkin().getEditFont());
        gSensitivity.addChild(lblSens);
        Label lblSensLow = new Label("low", Skin.getSkin().getEditFont());
        gSensitivity.addChild(lblSensLow);
        Label lblSensHigh = new Label("high", Skin.getSkin().getEditFont());
        gSensitivity.addChild(lblSensHigh);
        sSensitivity =
                new Slider(
                        SLIDER_WIDTH,
                        SLIDER_MIN,
                        SLIDER_MAX,
                        toSlider(Settings.getSettings().mouse_sensitivity, 0.2f, 3.0f));
        gSensitivity.addChild(sSensitivity);
        sSensitivity.addValueListener(
                new ValueListener() {
                    public void valueSet(int value) {
                        float sens = fromSlider(value, 0.2f, 3.0f);
                        Settings.getSettings().mouse_sensitivity = sens;
                        Mouse.updateSensitivity();
                    }
                });
        lblSens.place();
        lblSensLow.place(lblSens, BOTTOM_LEFT);
        sSensitivity.place(lblSensLow, RIGHT_MID);
        lblSensHigh.place(sSensitivity, RIGHT_MID);
        gSensitivity.compileCanvas();

        // Scroll acceleration time max -------------------------------------
        Group gAccelTime = new Group();
        addChild(gAccelTime);
        Label lblAccelTime = new Label("Scroll accel time (s)", Skin.getSkin().getEditFont());
        gAccelTime.addChild(lblAccelTime);
        Label lblAccelTimeLow = new Label("0.1", Skin.getSkin().getEditFont());
        gAccelTime.addChild(lblAccelTimeLow);
        Label lblAccelTimeHigh = new Label("3.0", Skin.getSkin().getEditFont());
        gAccelTime.addChild(lblAccelTimeHigh);
        sAccelTime =
                new Slider(
                        SLIDER_WIDTH,
                        SLIDER_MIN,
                        SLIDER_MAX,
                        toSlider(
                                Settings.getSettings().camera_scroll_accel_seconds_max,
                                0.1f,
                                3.0f));
        gAccelTime.addChild(sAccelTime);
        sAccelTime.addValueListener(
                new ValueListener() {
                    public void valueSet(int value) {
                        Settings.getSettings().camera_scroll_accel_seconds_max =
                                fromSlider(value, 0.1f, 3.0f);
                    }
                });
        lblAccelTime.place();
        lblAccelTimeLow.place(lblAccelTime, BOTTOM_LEFT);
        sAccelTime.place(lblAccelTimeLow, RIGHT_MID);
        lblAccelTimeHigh.place(sAccelTime, RIGHT_MID);
        gAccelTime.compileCanvas();

        // Scroll acceleration factor ---------------------------------------
        Group gAccelFactor = new Group();
        addChild(gAccelFactor);
        Label lblAccelFactor = new Label("Scroll accel factor", Skin.getSkin().getEditFont());
        gAccelFactor.addChild(lblAccelFactor);
        Label lblAccelFactorLow = new Label("0.0", Skin.getSkin().getEditFont());
        gAccelFactor.addChild(lblAccelFactorLow);
        Label lblAccelFactorHigh = new Label("5.0", Skin.getSkin().getEditFont());
        gAccelFactor.addChild(lblAccelFactorHigh);
        sAccelFactor =
                new Slider(
                        SLIDER_WIDTH,
                        SLIDER_MIN,
                        SLIDER_MAX,
                        toSlider(Settings.getSettings().camera_scroll_accel_factor, 0.0f, 5.0f));
        gAccelFactor.addChild(sAccelFactor);
        sAccelFactor.addValueListener(
                new ValueListener() {
                    public void valueSet(int value) {
                        Settings.getSettings().camera_scroll_accel_factor =
                                fromSlider(value, 0.0f, 5.0f);
                    }
                });
        lblAccelFactor.place();
        lblAccelFactorLow.place(lblAccelFactor, BOTTOM_LEFT);
        sAccelFactor.place(lblAccelFactorLow, RIGHT_MID);
        lblAccelFactorHigh.place(sAccelFactor, RIGHT_MID);
        gAccelFactor.compileCanvas();

        // Start max speed ---------------------------------------------------
        Group gStartSpeed = new Group();
        addChild(gStartSpeed);
        Label lblStartSpeed = new Label("Pan start max speed", Skin.getSkin().getEditFont());
        gStartSpeed.addChild(lblStartSpeed);
        Label lblStartSpeedLow = new Label("10", Skin.getSkin().getEditFont());
        gStartSpeed.addChild(lblStartSpeedLow);
        Label lblStartSpeedHigh = new Label("120", Skin.getSkin().getEditFont());
        gStartSpeed.addChild(lblStartSpeedHigh);
        sStartSpeed =
                new Slider(
                        SLIDER_WIDTH,
                        SLIDER_MIN,
                        SLIDER_MAX,
                        toSlider(Settings.getSettings().camera_start_max_speed, 10f, 120f));
        gStartSpeed.addChild(sStartSpeed);
        sStartSpeed.addValueListener(
                new ValueListener() {
                    public void valueSet(int value) {
                        Settings.getSettings().camera_start_max_speed =
                                fromSlider(value, 10f, 120f);
                    }
                });
        lblStartSpeed.place();
        lblStartSpeedLow.place(lblStartSpeed, BOTTOM_LEFT);
        sStartSpeed.place(lblStartSpeedLow, RIGHT_MID);
        lblStartSpeedHigh.place(sStartSpeed, RIGHT_MID);
        gStartSpeed.compileCanvas();

        // Zoom speed --------------------------------------------------------
        Group gZoom = new Group();
        addChild(gZoom);
        Label lblZoom = new Label("Zoom speed", Skin.getSkin().getEditFont());
        gZoom.addChild(lblZoom);
        Label lblZoomLow = new Label("10", Skin.getSkin().getEditFont());
        gZoom.addChild(lblZoomLow);
        Label lblZoomHigh = new Label("150", Skin.getSkin().getEditFont());
        gZoom.addChild(lblZoomHigh);
        sZoom =
                new Slider(
                        SLIDER_WIDTH,
                        SLIDER_MIN,
                        SLIDER_MAX,
                        toSlider(Settings.getSettings().camera_zoom_speed, 10f, 150f));
        gZoom.addChild(sZoom);
        sZoom.addValueListener(
                new ValueListener() {
                    public void valueSet(int value) {
                        Settings.getSettings().camera_zoom_speed = fromSlider(value, 10f, 150f);
                    }
                });
        lblZoom.place();
        lblZoomLow.place(lblZoom, BOTTOM_LEFT);
        sZoom.place(lblZoomLow, RIGHT_MID);
        lblZoomHigh.place(sZoom, RIGHT_MID);
        gZoom.compileCanvas();

        // Angle speed (deg/s) ----------------------------------------------
        Group gAngle = new Group();
        addChild(gAngle);
        Label lblAngle = new Label("Rotate/Pitch speed (deg/s)", Skin.getSkin().getEditFont());
        gAngle.addChild(lblAngle);
        Label lblAngleLow = new Label("30", Skin.getSkin().getEditFont());
        gAngle.addChild(lblAngleLow);
        Label lblAngleHigh = new Label("360", Skin.getSkin().getEditFont());
        gAngle.addChild(lblAngleHigh);
        sAngle =
                new Slider(
                        SLIDER_WIDTH,
                        SLIDER_MIN,
                        SLIDER_MAX,
                        toSlider(Settings.getSettings().camera_angle_delta_deg_per_sec, 30f, 360f));
        gAngle.addChild(sAngle);
        sAngle.addValueListener(
                new ValueListener() {
                    public void valueSet(int value) {
                        Settings.getSettings().camera_angle_delta_deg_per_sec =
                                fromSlider(value, 30f, 360f);
                    }
                });
        lblAngle.place();
        lblAngleLow.place(lblAngle, BOTTOM_LEFT);
        sAngle.place(lblAngleLow, RIGHT_MID);
        lblAngleHigh.place(sAngle, RIGHT_MID);
        gAngle.compileCanvas();

        // Edge scroll buffer ------------------------------------------------
        Group gEdge = new Group();
        addChild(gEdge);
        Label lblEdge = new Label("Edge scroll buffer (px)", Skin.getSkin().getEditFont());
        gEdge.addChild(lblEdge);
        Label lblEdgeLow = new Label("0", Skin.getSkin().getEditFont());
        gEdge.addChild(lblEdgeLow);
        Label lblEdgeHigh = new Label("50", Skin.getSkin().getEditFont());
        gEdge.addChild(lblEdgeHigh);
        sEdge =
                new Slider(
                        SLIDER_WIDTH,
                        0,
                        50,
                        Math.max(
                                0, Math.min(50, Settings.getSettings().camera_edge_scroll_buffer)));
        gEdge.addChild(sEdge);
        sEdge.addValueListener(
                new ValueListener() {
                    public void valueSet(int value) {
                        Settings.getSettings().camera_edge_scroll_buffer = value;
                    }
                });
        lblEdge.place();
        lblEdgeLow.place(lblEdge, BOTTOM_LEFT);
        sEdge.place(lblEdgeLow, RIGHT_MID);
        lblEdgeHigh.place(sEdge, RIGHT_MID);
        gEdge.compileCanvas();

        // Save button -------------------------------------------------------
        Group gButtons = new Group();
        addChild(gButtons);
        HorizButton btnSave = new HorizButton("Save", 120);
        gButtons.addChild(btnSave);
        btnSave.addMouseClickListener(
                new MouseClickListener() {
                    public void mouseClicked(int button, int x, int y, int clicks) {
                        Settings.getSettings().save();
                    }
                });
        btnSave.place();

        HorizButton btnDefaults = new HorizButton("Restore defaults", 160);
        gButtons.addChild(btnDefaults);
        btnDefaults.addMouseClickListener(
                new MouseClickListener() {
                    public void mouseClicked(int button, int x, int y, int clicks) {
                        restoreDefaults();
                    }
                });
        btnDefaults.place(btnSave, RIGHT_MID);
        gButtons.compileCanvas();

        // Layout stacking
        gSensitivity.place();
        gAccelTime.place(gSensitivity, BOTTOM_LEFT);
        gAccelFactor.place(gAccelTime, BOTTOM_LEFT);
        gStartSpeed.place(gAccelFactor, BOTTOM_LEFT);
        gZoom.place(gStartSpeed, BOTTOM_LEFT);
        gAngle.place(gZoom, BOTTOM_LEFT);
        gEdge.place(gAngle, BOTTOM_LEFT);
        gButtons.place(gEdge, BOTTOM_LEFT);

        compileCanvas();
    }

    private void restoreDefaults() {
        // Create a temporary Settings to read constructor defaults
        Settings def = new Settings();
        Settings cur = Settings.getSettings();

        cur.mouse_sensitivity = def.mouse_sensitivity;
        cur.camera_scroll_accel_seconds_max = def.camera_scroll_accel_seconds_max;
        cur.camera_scroll_accel_factor = def.camera_scroll_accel_factor;
        cur.camera_start_max_speed = def.camera_start_max_speed;
        cur.camera_zoom_speed = def.camera_zoom_speed;
        cur.camera_angle_delta_deg_per_sec = def.camera_angle_delta_deg_per_sec;
        cur.camera_edge_scroll_buffer = def.camera_edge_scroll_buffer;

        // Update UI controls
        sSensitivity.setValue(toSlider(cur.mouse_sensitivity, 0.2f, 3.0f));
        sAccelTime.setValue(toSlider(cur.camera_scroll_accel_seconds_max, 0.1f, 3.0f));
        sAccelFactor.setValue(toSlider(cur.camera_scroll_accel_factor, 0.0f, 5.0f));
        sStartSpeed.setValue(toSlider(cur.camera_start_max_speed, 10f, 120f));
        sZoom.setValue(toSlider(cur.camera_zoom_speed, 10f, 150f));
        sAngle.setValue(toSlider(cur.camera_angle_delta_deg_per_sec, 30f, 360f));
        sEdge.setValue(Math.max(0, Math.min(50, cur.camera_edge_scroll_buffer)));

        Mouse.updateSensitivity();
        cur.save();
    }

    @Override
    public void onActivated() {
        Settings s = Settings.getSettings();
        sSensitivity.setValue(toSlider(s.mouse_sensitivity, 0.2f, 3.0f));
        sAccelTime.setValue(toSlider(s.camera_scroll_accel_seconds_max, 0.1f, 3.0f));
        sAccelFactor.setValue(toSlider(s.camera_scroll_accel_factor, 0.0f, 5.0f));
        sStartSpeed.setValue(toSlider(s.camera_start_max_speed, 10f, 120f));
        sZoom.setValue(toSlider(s.camera_zoom_speed, 10f, 150f));
        sAngle.setValue(toSlider(s.camera_angle_delta_deg_per_sec, 30f, 360f));
        sEdge.setValue(Math.max(0, Math.min(50, s.camera_edge_scroll_buffer)));
    }

    private static int toSlider(float value, float min, float max) {
        if (value < min) value = min;
        if (value > max) value = max;
        float t = (value - min) / (max - min);
        return (int) StrictMath.round(SLIDER_MIN + t * (SLIDER_MAX - SLIDER_MIN));
    }

    private static float fromSlider(int sliderValue, float min, float max) {
        float t = (float) (sliderValue - SLIDER_MIN) / (float) (SLIDER_MAX - SLIDER_MIN);
        return min + t * (max - min);
    }
}
