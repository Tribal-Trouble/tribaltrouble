package com.oddlabs.tt.gui;

import org.jspecify.annotations.NonNull;

public interface CampaignIcons {
    @NonNull
    GUIIcon @NonNull [] getHiddenRoutes();

    @NonNull
    IconQuad @NonNull [] getFaces();

    @NonNull
    IconQuad getMap();

    int getNumIslands();

    //	public int getOffsetX();
//	public int getOffsetY();
//	public int getInternalWidth();
//	public int getInternalHeight();
    @NonNull
    MapIslandData getMapIslandData(int i);
}
