package com.oddlabs.tt.guievent;

import com.oddlabs.tt.gui.PulldownMenu;
import org.jspecify.annotations.NonNull;

@FunctionalInterface
public interface ItemChosenListener<T> extends EventListener {
	void itemChosen(@NonNull PulldownMenu<T> menu, int item_index);
}
