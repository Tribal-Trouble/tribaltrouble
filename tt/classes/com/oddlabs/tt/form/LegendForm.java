package com.oddlabs.tt.form;

import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.OKListener;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.gui.TextBox;

public final strictfp class LegendForm extends Form {
    private final HorizButton closeButton;

    public LegendForm() {
        super("Keybind Legend");

        Group content = new Group();
        addChild(content);

        Label title = new Label("Legend", Skin.getSkin().getEditFont());
        content.addChild(title);

        TextBox body = new TextBox(420, 220, Skin.getSkin().getEditFont(), 10000);
        content.addChild(body);
        body.append("Red = conflict within the same section (must fix)\n");
        body.append("Gold = overlap across sections (allowed)\n");
        body.append("Blue = changed from default [Custom]\n");
        body.append("Amber = unbound [Unbound]\n");

        closeButton = new HorizButton("Close", 100);
        addChild(closeButton);
        closeButton.addMouseClickListener(new OKListener(this));

        // Layout: place the content group before compiling the form canvas
        title.place();
        body.place(title, BOTTOM_LEFT);
        content.compileCanvas();
        content.place();

        closeButton.place(ORIGIN_BOTTOM_RIGHT);
        compileCanvas();
        centerPos();
    }
}
