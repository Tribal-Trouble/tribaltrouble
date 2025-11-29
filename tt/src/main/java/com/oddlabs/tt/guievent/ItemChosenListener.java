package com.oddlabs.tt.guievent;

import com.oddlabs.tt.gui.PulldownMenu;

@FunctionalInterface
public interface ItemChosenListener<T> extends EventListener {
	void itemChosen(PulldownMenu<T> menu, int item_index);
}
