package com.oddlabs.tt.gui;

import com.oddlabs.tt.util.ToolTip;
import com.oddlabs.util.Quad;

public class NonFocusIconButton extends IconButton implements ToolTip {
	private final String tool_tip;

	public NonFocusIconButton(Quad[] icon_quad, String tool_tip) {
		super(icon_quad);
		this.tool_tip = tool_tip;
	}

        @Override
	public void appendToolTip(ToolTipBox tool_tip_box) {
		tool_tip_box.append(tool_tip);
	}

        @Override
	public final void setFocus() {
	}
}
