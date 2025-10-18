package com.oddlabs.tt.gui;

public final class MapIslandData {
	private final IconQuad[] button;
	private final int x;
	private final int y;
	private final IconQuad flag;
	private final IconQuad boat;
	private final int pin_x;
	private final int pin_y;

	public MapIslandData(IconQuad[] button,
			int x,
			int y,
			IconQuad flag,
			IconQuad boat,
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

	public IconQuad[] getButton() {
		return button;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public IconQuad getFlag() {
		return flag;
	}

	public IconQuad getBoat() {
		return boat;
	}

	public int getPinX() {
		return pin_x;
	}

	public int getPinY() {
		return pin_y;
	}
}
