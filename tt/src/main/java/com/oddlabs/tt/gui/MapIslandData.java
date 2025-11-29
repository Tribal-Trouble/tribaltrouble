package com.oddlabs.tt.gui;

import org.jspecify.annotations.NonNull;

public final class MapIslandData {
	private final @NonNull ModeIconQuads button;
	private final int x;
	private final int y;
	private final @NonNull IconQuad flag;
	private final @NonNull IconQuad boat;
	private final int pin_x;
	private final int pin_y;

	public MapIslandData(@NonNull ModeIconQuads button,
                         int x,
                         int y,
                         @NonNull IconQuad flag,
                         @NonNull IconQuad boat,
                         int pin_x,
                         int pin_y) {
		 this.button = button;
		 this.x = x;
		 this.y = y;
		 this.flag = flag;
		 this.boat = boat;
		 this.pin_x = pin_x;
		 this.pin_y = pin_y;
	}

	public @NonNull ModeIconQuads getButton() {
		return button;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public @NonNull IconQuad getFlag() {
		return flag;
	}

	public @NonNull IconQuad getBoat() {
		return boat;
	}

	public int getPinX() {
		return pin_x;
	}

	public int getPinY() {
		return pin_y;
	}
}
