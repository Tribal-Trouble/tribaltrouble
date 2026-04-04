package com.oddlabs.tt.gui;

import com.oddlabs.tt.model.SupplyCounter;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.tt.util.ToolTip;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;

public class StatusIcon extends GUIObject implements ToolTip {
    private final @NonNull IconQuad icon;
    private final @NonNull TextField label;
    private final @NonNull String tooltip;

    private SupplyCounter counter;
    private int text_count = -1;

    public StatusIcon(int label_width, @NonNull IconQuad icon, @NonNull String tooltip) {
        this.tooltip = tooltip;
        setDim(icon.getWidth() + label_width, icon.getHeight());
        setCanFocus(true); //only to enable tool tips. focus is given to delegate
        this.icon = icon;
        label = new Label("", Skin.getSkin().getEditFont(), label_width, Origin.AT_END);
        addChild(label);
//		label.setPos(icon.getWidth(), (getHeight() - label.getFont().getHeight())/2);
        label.setPos(0, (getHeight() - label.getFont().getHeight()) / 2);
    }

    public final void setCounter(SupplyCounter counter) {
        this.counter = counter;
    }

    public final void doUpdate() {
        int count = counter.getNumSupplies();
        if (count != text_count) {
            text_count = count;
            label.clear();
            label.append(count);
        }
    }

    @Override
    public final void appendToolTip(@NonNull ToolTipBox tool_tip_box) {
        String tooltip_str = Utils.getBundleString(ResourceBundle.getBundle(StatusIcon.class.getName()), "max", tooltip, counter.getMaxSupplies());
        tool_tip_box.append(tooltip_str);
    }

    @Override
    protected void renderGeometry(@NonNull GUIRenderer renderer) {
        renderer.drawIcon(icon, getWidth() - icon.getWidth(), (getHeight() - icon.getHeight()) / 2f);
    }
}
