package com.oddlabs.tt.guievent;

import com.oddlabs.tt.gui.PulldownMenu;

@FunctionalInterface
public interface ItemChosenListener extends EventListener {
	public void itemChosen(PulldownMenu menu, int item_index);
}
