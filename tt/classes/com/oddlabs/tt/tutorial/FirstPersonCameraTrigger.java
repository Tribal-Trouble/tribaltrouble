package com.oddlabs.tt.tutorial;

import com.oddlabs.tt.delegate.Delegate;
import com.oddlabs.tt.delegate.FirstPersonDelegate;

public final class FirstPersonCameraTrigger extends TutorialTrigger {
	public FirstPersonCameraTrigger() {
		super(.1f, 2f, "fpc");
	}

        @Override
	protected void run(Tutorial tutorial) {
		Delegate delegate = tutorial.getViewer().getGUIRoot().getDelegate();
		if (delegate instanceof FirstPersonDelegate)
			tutorial.next(new MapModeTrigger());
	}
}
