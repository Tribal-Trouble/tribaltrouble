package com.oddlabs.tt.form;

import com.oddlabs.tt.delegate.MainMenu;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.LabelBox;
import com.oddlabs.tt.gui.OKButton;
import com.oddlabs.tt.gui.OKListener;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.util.Utils;

import java.util.ResourceBundle;

public final class WelcomeForm extends Form {


	public WelcomeForm(GUIRoot gui_root, MainMenu main_menu) {
		ResourceBundle bundle = ResourceBundle.getBundle(WelcomeForm.class.getName());
		Label label_headline = new Label(Utils.getBundleString(bundle, "welcome_caption"), Skin.getSkin().getHeadlineFont());
		addChild(label_headline);

		LabelBox box = new LabelBox(Utils.getBundleString(bundle, "welcome_message"), Skin.getSkin().getEditFont(), 400);
		addChild(box);

		HorizButton ok_button = new OKButton(100);
		addChild(ok_button);
		ok_button.addMouseClickListener(new OKListener(this));

		// Place objects
		label_headline.place();
		box.place(label_headline, BOTTOM_LEFT);
		ok_button.place(ORIGIN_BOTTOM_RIGHT);

		// headline
		compileCanvas();
		centerPos();
	}

}
