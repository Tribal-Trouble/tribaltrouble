package com.oddlabs.tt.editor.ui;

/**
 * Editor state shared between UI forms and the running session.
 * Kept intentionally small and with simple getters/setters.
 */
public final class EditorState {
    public enum EditorMode { Default, Sandbox }

    private EditorMode editorMode = EditorMode.Default;

    // General toggles
    private boolean autoReblend = true;
    private boolean autoUpdatePlacementGrids = true;

    // Overlay toggles (UI-configured availability + master latch)
    private boolean overlayMaster = false;
    private boolean overlayAccess = true;
    private boolean overlayConnected = false; // not implemented yet
    private boolean overlaySlope = true;
    private boolean overlayWater = true;
    private boolean overlayResource = true; // resource placement validity

    public EditorMode getEditorMode() { return editorMode; }
    public void setEditorMode(EditorMode mode) { this.editorMode = mode; }

    public boolean isAutoReblend() { return autoReblend; }
    public void setAutoReblend(boolean v) { this.autoReblend = v; }

    public boolean isAutoUpdatePlacementGrids() { return autoUpdatePlacementGrids; }
    public void setAutoUpdatePlacementGrids(boolean v) { this.autoUpdatePlacementGrids = v; }

    public boolean isOverlayMaster() { return overlayMaster; }
    public void setOverlayMaster(boolean v) { this.overlayMaster = v; }

    public boolean isOverlayAccess() { return overlayAccess; }
    public void setOverlayAccess(boolean v) { this.overlayAccess = v; }

    public boolean isOverlayConnected() { return overlayConnected; }
    public void setOverlayConnected(boolean v) { this.overlayConnected = v; }

    public boolean isOverlaySlope() { return overlaySlope; }
    public void setOverlaySlope(boolean v) { this.overlaySlope = v; }

    public boolean isOverlayWater() { return overlayWater; }
    public void setOverlayWater(boolean v) { this.overlayWater = v; }

    public boolean isOverlayResource() { return overlayResource; }
    public void setOverlayResource(boolean v) { this.overlayResource = v; }
}
