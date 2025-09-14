package com.oddlabs.tt.editor.ui;

/**
 * Binding surface for editor options (tool/mode/resource/overlays) so the toolbar
 * can synchronize state without depending on editor-internal enums.
 *
 * Indices map to the editor's internal enum ordinals; names are used to populate UI.
 */
public interface EditorOptionsBinding {
    // Active tool: 0 = TERRAIN, 1 = RESOURCE
    int getActiveToolIndex();
    void setActiveToolIndex(int idx);

    // Brush mode selection
    String[] getBrushModeNames();
    int getBrushModeIndex();
    void setBrushModeIndex(int idx);

    // Resource type selection
    String[] getResourceTypeNames();
    int getResourceTypeIndex();
    void setResourceTypeIndex(int idx);

    // Overlay layer selection and master toggle
    String[] getOverlayLayerNames();
    int getOverlayLayerIndex();
    void setOverlayLayerIndex(int idx);
    boolean isOverlayMaster();
    void setOverlayMaster(boolean v);
}
