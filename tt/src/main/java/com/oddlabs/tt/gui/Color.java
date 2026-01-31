package com.oddlabs.tt.gui;

public record Color(float r, float g, float b, float a) {
	public Color(int r, int g, int b, int a) {
		this(r / 255f, g / 255f, b / 255f, a / 255f);
	}


}

