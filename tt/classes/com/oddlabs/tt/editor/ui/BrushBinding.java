package com.oddlabs.tt.editor.ui;

/**
 * Minimal bridge for two-way binding of brush parameters between the editor runtime
 * and toolbar UI without exposing internal editor classes.
 */
public interface BrushBinding {
    float getRadiusMeters();
    void setRadiusMeters(float meters);

    float getIntensity();
    void setIntensity(float strength);
}
