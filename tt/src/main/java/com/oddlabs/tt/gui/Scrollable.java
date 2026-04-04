package com.oddlabs.tt.gui;

public interface Scrollable {
    void setOffsetY(int new_offset);

    int getOffsetY();

    int getStepHeight();

    void jumpPage(boolean up);

    float getScrollBarRatio();

    float getScrollBarOffset();

    void setScrollBarOffset(float offset);
}
