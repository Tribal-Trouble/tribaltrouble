package com.oddlabs.tt.editor.ui;

/**
 * Binding surface for editor options (tool/mode/resource/overlays) so the toolbar
 * can synchronize state without depending on editor-internal enums.
 *
 * Indices map to the editor's internal enum ordinals; names are used to populate UI.
 */
public interface EditorOptionsBinding {
    // Active tool: 0 = TERRAIN, 1 = RESOURCE, 2 = ENTITIES (buildings/units)
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

    // Entities tool selectors (only used when ActiveTool == ENTITIES)
    // 1) Entities type (Buildings/Units)
    default String[] getEntitiesTypeNames() { return new String[] {"Buildings", "Units"}; }
    default int getEntitiesTypeIndex() { return 0; }
    default void setEntitiesTypeIndex(int idx) {}
    // 2) Entities kind depends on type; names provided dynamically
    default String[] getEntitiesKindNames() {
        return new String[] {"Quarters", "Armory", "Tower", "Ship"};
    }
    default int getEntitiesKindIndex() { return 0; }
    default void setEntitiesKindIndex(int idx) {}
    // 3) Team (Neutral, Team 0..7) and Race (Natives/Vikings)
    default String[] getEntitiesTeamNames() {
        String[] names = new String[1 + 8];
        names[0] = "Neutral";
        for (int i=0;i<8;i++) names[1+i] = "Team " + i;
        return names;
    }
    default int getEntitiesTeamIndex() { return 1; }
    default void setEntitiesTeamIndex(int idx) {}
    default String[] getEntitiesRaceNames() { return new String[] {"Natives", "Vikings"}; }
    default int getEntitiesRaceIndex() { return 0; }
    default void setEntitiesRaceIndex(int idx) {}
}
