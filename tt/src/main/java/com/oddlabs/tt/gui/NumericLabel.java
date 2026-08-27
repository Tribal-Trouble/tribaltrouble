package com.oddlabs.tt.gui;

import org.jspecify.annotations.NullMarked;

import com.oddlabs.tt.font.Font;

@NullMarked
public final class NumericLabel extends Label {
    private double value;

    public NumericLabel(CharSequence text, Font font, double value) {
        this(text, font, font.getWidth(text), value);
    }

    public NumericLabel(CharSequence text, Font font, int width, double value) {
        this(text, font, width, Origin.AT_END, value);
    }

    public NumericLabel(CharSequence text, Font font, int width, Origin align, double value) {
        super(text, font, width, align);
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    public void setNumeric(CharSequence text, double value) {
        setText(text);
        this.value = value;
    }

    @Override
    public int compareTo(Label o) {
        return o instanceof NumericLabel x ? Double.compare(value, x.value) : -1;
    }
}
