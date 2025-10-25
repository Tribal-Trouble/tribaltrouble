package com.oddlabs.tt.guievent;

public interface RowListener extends EventListener {
	void rowDoubleClicked(Object row_context);
	void rowChosen(Object row_context);
}
