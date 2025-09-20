package com.oddlabs.tt.form;

import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.Panel;
import com.oddlabs.tt.gui.ScrollableSliderContainer;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.gui.Slider;
import com.oddlabs.tt.guievent.MouseClickListener;
import com.oddlabs.tt.guievent.ValueListener;
import com.oddlabs.tt.gui.LocalInput;
import com.oddlabs.tt.input.Mouse;

/** Camera options panel: adjust mouse sensitivity and camera movement parameters. */
public class CameraPanel extends Panel {
    // Base width used elsewhere (e.g., AbstractOptionsMenu) for panel sizing
    private static final int PANEL_BASE_TRACK_WIDTH = 300; // panel base width stays 300px
    // Opposite compensation: slider is wider than base; also +20px from previous (now 310)
    private static final int SLIDER_WIDTH = PANEL_BASE_TRACK_WIDTH - 46;
    private static final int SLIDER_LEFT_INDENT = 50; // fixed indent to align all slider tracks
    private static final int SLIDER_MIN = 0;
    private static final int SLIDER_MAX = 100;
    private static final int GROUP_SPACING = 10;

    // Keep slider refs so we can restore/update values
    private Slider sAccelTime;
    private Slider sAccelFactor;
    private Slider sStartSpeed;
    private Slider sZoom;
    private Slider sAngle;
    private Slider sMaxZ;
    private Slider sEdge;

    // Helper class to return both group and slider
    private static class SliderGroupPair {
        final Group group;
        final Slider slider;
        
        SliderGroupPair(Group group, Slider slider) {
            this.group = group;
            this.slider = slider;
        }
    }

    public CameraPanel(com.oddlabs.tt.gui.GUIRoot gui_root, String caption) {
        super(caption);

        // Create scrollable container for sliders
        int dynamicHeight = calculateDynamicScrollableHeight();
    // Container width: decoupled from SLIDER_WIDTH so shortening the track increases the
    // gap to the scrollbar without narrowing the entire panel. Keep panel aligned to
    // the base width used by other menus for visual consistency.
    // Ensure the container accommodates the wider slider plus margin for the scrollbar
    int containerWidth = SLIDER_LEFT_INDENT + Math.max(PANEL_BASE_TRACK_WIDTH, SLIDER_WIDTH) + 20;
    ScrollableSliderContainer scrollContainer =
        new ScrollableSliderContainer(containerWidth, dynamicHeight, GROUP_SPACING);
        SliderGroupPair mapModeGroup = createSliderGroupWithSlider(
            "Map mode delay", "none", "high",
            Settings.getSettings().mapmode_delay, 0.0f, 1.0f,
            new ValueListener() {
                public void valueSet(int value) {
                    Settings.getSettings().mapmode_delay = fromSlider(value, 0.0f, 1.0f);
                }
            }
        );
        scrollContainer.addGroup(mapModeGroup.group);

        // Scroll acceleration time max -------------------------------------
        SliderGroupPair accelTimeGroup = createSliderGroupWithSlider(
            "Scroll accel time (s)", "0.1", "3.0",
            Settings.getSettings().camera_scroll_accel_seconds_max, 0.1f, 3.0f,
            new ValueListener() {
                public void valueSet(int value) {
                    Settings.getSettings().camera_scroll_accel_seconds_max =
                            fromSlider(value, 0.1f, 3.0f);
                }
            }
        );
        sAccelTime = accelTimeGroup.slider;
        scrollContainer.addGroup(accelTimeGroup.group);

        // Scroll acceleration factor ---------------------------------------
        SliderGroupPair accelFactorGroup = createSliderGroupWithSlider(
            "Scroll accel factor", "0.0", "5.0",
            Settings.getSettings().camera_scroll_accel_factor, 0.0f, 5.0f,
            new ValueListener() {
                public void valueSet(int value) {
                    Settings.getSettings().camera_scroll_accel_factor =
                            fromSlider(value, 0.0f, 5.0f);
                }
            }
        );
        sAccelFactor = accelFactorGroup.slider;
        scrollContainer.addGroup(accelFactorGroup.group);

        // Start max speed ---------------------------------------------------
        SliderGroupPair startSpeedGroup = createSliderGroupWithSlider(
            "Pan start max speed", "10", "120",
            Settings.getSettings().camera_start_max_speed, 10f, 120f,
            new ValueListener() {
                public void valueSet(int value) {
                    Settings.getSettings().camera_start_max_speed =
                            fromSlider(value, 10f, 120f);
                }
            }
        );
        sStartSpeed = startSpeedGroup.slider;
        scrollContainer.addGroup(startSpeedGroup.group);

        // Zoom speed --------------------------------------------------------
        SliderGroupPair zoomGroup = createSliderGroupWithSlider(
            "Zoom speed", "10", "150",
            Settings.getSettings().camera_zoom_speed, 10f, 150f,
            new ValueListener() {
                public void valueSet(int value) {
                    Settings.getSettings().camera_zoom_speed =
                            fromSlider(value, 10f, 150f);
                }
            }
        );
        sZoom = zoomGroup.slider;
        scrollContainer.addGroup(zoomGroup.group);

        // Rotate/Pitch speed -----------------------------------------------
        SliderGroupPair angleGroup = createSliderGroupWithSlider(
            "Rotate/Pitch speed (deg/s)", "30", "360",
            Settings.getSettings().camera_angle_delta_deg_per_sec, 30f, 360f,
            new ValueListener() {
                public void valueSet(int value) {
                    Settings.getSettings().camera_angle_delta_deg_per_sec =
                            fromSlider(value, 30f, 360f);
                }
            }
        );
        sAngle = angleGroup.slider;
        scrollContainer.addGroup(angleGroup.group);

        // Max camera height ------------------------------------------------
        SliderGroupPair maxZGroup = createSliderGroupWithSlider(
            "Max camera height", "50", "400",
            Settings.getSettings().camera_max_z, 50f, 400f,
            new ValueListener() {
                public void valueSet(int value) {
                    Settings.getSettings().camera_max_z = fromSlider(value, 50f, 400f);
                }
            }
        );
        sMaxZ = maxZGroup.slider;
        scrollContainer.addGroup(maxZGroup.group);

        // Edge scroll buffer ------------------------------------------------
        Group gEdge = new Group();
        Label lblEdge = new Label("Edge scroll buffer (px)", Skin.getSkin().getEditFont());
        gEdge.addChild(lblEdge);
        Label lblEdgeLow = new Label("0", Skin.getSkin().getEditFont());
        gEdge.addChild(lblEdgeLow);
        Label lblEdgeHigh = new Label("50", Skin.getSkin().getEditFont());
        gEdge.addChild(lblEdgeHigh);
        sEdge = new Slider(
                SLIDER_WIDTH,
                0,
                50,
                Math.max(0, Math.min(50, Settings.getSettings().camera_edge_scroll_buffer)));
        gEdge.addChild(sEdge);
        sEdge.addValueListener(
                new ValueListener() {
                    public void valueSet(int value) {
                        Settings.getSettings().camera_edge_scroll_buffer = value;
                    }
                });
        lblEdge.place();
        // Place slider below title with fixed left indent for alignment
        sEdge.place(lblEdge, BOTTOM_LEFT);
        sEdge.correctPos(SLIDER_LEFT_INDENT, -4);
        // Place low/high labels relative to the slider so their widths don't affect alignment
        lblEdgeLow.place(sEdge, LEFT_MID);
        lblEdgeHigh.place(sEdge, RIGHT_MID);
        gEdge.compileCanvas();
        scrollContainer.addGroup(gEdge);

        // Buttons -----------------------------------------------------------
        Group gButtons = new Group();
        HorizButton btnDefaults = new HorizButton("Restore defaults", 160);
        gButtons.addChild(btnDefaults);
        btnDefaults.addMouseClickListener(
                new MouseClickListener() {
                    public void mouseClicked(int button, int x, int y, int clicks) {
                        restoreDefaults();
                    }
                });
        btnDefaults.place();
        gButtons.compileCanvas();

        // Layout
        addChild(scrollContainer);
        addChild(gButtons);
        
        scrollContainer.place();
        gButtons.place(scrollContainer, BOTTOM_LEFT);
        
        compileCanvas();
    }
    
    /**
     * Calculates dynamic height for the scrollable container based on viewport size
     * with reasonable bounds for camera settings visibility.
     */
    private int calculateDynamicScrollableHeight() {
        int viewHeight = LocalInput.getViewHeight();
        
        // Use 35% of screen height as base, with min 220px (original) and max 350px
        int baseHeight = (int)(viewHeight * 0.20f);
        int minHeight = 100; // Original hardcoded value as minimum
        int maxHeight = 284;
        
        return Math.max(minHeight, Math.min(maxHeight, baseHeight));
    }
    
    /**
     * Helper method to create a consistent slider group and return both group and slider
     */
    private SliderGroupPair createSliderGroupWithSlider(String title, String lowLabel, String highLabel,
                                                       float currentValue, float minValue, float maxValue,
                                                       ValueListener listener) {
        Group group = new Group();
        Label lblTitle = new Label(title, Skin.getSkin().getEditFont());
        group.addChild(lblTitle);
        Label lblLow = new Label(lowLabel, Skin.getSkin().getEditFont());
        group.addChild(lblLow);
        Label lblHigh = new Label(highLabel, Skin.getSkin().getEditFont());
        group.addChild(lblHigh);
        
        Slider slider = new Slider(
                SLIDER_WIDTH,
                SLIDER_MIN,
                SLIDER_MAX,
                toSlider(currentValue, minValue, maxValue));
        group.addChild(slider);
        slider.addValueListener(listener);
        
        lblTitle.place();
        // Place slider below title with fixed left indent so all slider tracks align
        slider.place(lblTitle, BOTTOM_LEFT);
    // Nudge down slightly to avoid title text overlapping the track
    slider.correctPos(SLIDER_LEFT_INDENT, -4);
        // Low label sits to the left of the slider; its width won't change slider position
        lblLow.place(slider, LEFT_MID);
        // High label sits to the right of the slider
        lblHigh.place(slider, RIGHT_MID);
        group.compileCanvas();
        
        return new SliderGroupPair(group, slider);
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
        cur.camera_max_z = def.camera_max_z;
        cur.camera_edge_scroll_buffer = def.camera_edge_scroll_buffer;

        // Update UI controls
        sAccelTime.setValue(toSlider(cur.camera_scroll_accel_seconds_max, 0.1f, 3.0f));
        sAccelFactor.setValue(toSlider(cur.camera_scroll_accel_factor, 0.0f, 5.0f));
        sStartSpeed.setValue(toSlider(cur.camera_start_max_speed, 10f, 120f));
        sZoom.setValue(toSlider(cur.camera_zoom_speed, 10f, 150f));
        sAngle.setValue(toSlider(cur.camera_angle_delta_deg_per_sec, 30f, 360f));
        sMaxZ.setValue(toSlider(cur.camera_max_z, 50f, 400f));
        sEdge.setValue(Math.max(0, Math.min(50, cur.camera_edge_scroll_buffer)));

        Mouse.updateSensitivity();
        cur.save();
    }

    @Override
    public void onActivated() {
        Settings s = Settings.getSettings();
        sAccelTime.setValue(toSlider(s.camera_scroll_accel_seconds_max, 0.1f, 3.0f));
        sAccelFactor.setValue(toSlider(s.camera_scroll_accel_factor, 0.0f, 5.0f));
        sStartSpeed.setValue(toSlider(s.camera_start_max_speed, 10f, 120f));
        sZoom.setValue(toSlider(s.camera_zoom_speed, 10f, 150f));
        sAngle.setValue(toSlider(s.camera_angle_delta_deg_per_sec, 30f, 360f));
        sMaxZ.setValue(toSlider(s.camera_max_z, 50f, 400f));
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
