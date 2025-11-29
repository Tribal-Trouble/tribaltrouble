package com.oddlabs.tt.guievent;

import com.oddlabs.tt.gui.MouseButton;
import org.jspecify.annotations.NonNull;

public interface MouseButtonListener extends MouseClickListener {
	void mousePressed(@NonNull MouseButton button, int x, int y);
	void mouseReleased(@NonNull MouseButton button, int x, int y);
	void mouseHeld(@NonNull MouseButton button, int x, int y);
}
