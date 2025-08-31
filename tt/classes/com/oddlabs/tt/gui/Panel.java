package com.oddlabs.tt.gui;

public strictfp class Panel extends Group {
    private final PanelTab tab;

    public Panel(String caption) {
        tab = new PanelTab(caption);
    }

    public final PanelTab getTab() {
        return tab;
    }

    public final void compileCanvas() {
        Box box = Skin.getSkin().getPanelData().getBox();
        super.compileCanvas(
                box.getLeftOffset(),
                box.getBottomOffset(),
                box.getRightOffset(),
                box.getTopOffset());
    }

    /**
     * Called by PanelGroup when this panel becomes the active/visible panel.
     * Default implementation does nothing; panels can override to refresh UI state.
     */
    public void onActivated() {}
}
