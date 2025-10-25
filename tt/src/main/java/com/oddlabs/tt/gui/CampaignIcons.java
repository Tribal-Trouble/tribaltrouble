package com.oddlabs.tt.gui;

public interface CampaignIcons {
//	public CampaignIcons getIcons();

	GUIIcon[] getHiddenRoutes();
	IconQuad[] getFaces();
	IconQuad getMap();
	int getNumIslands();
//	public int getOffsetX();
//	public int getOffsetY();
//	public int getInternalWidth();
//	public int getInternalHeight();
MapIslandData getMapIslandData(int i);
}
