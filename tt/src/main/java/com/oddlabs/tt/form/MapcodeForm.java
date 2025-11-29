package com.oddlabs.tt.form;

import com.oddlabs.registration.RegistrationKey;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.gui.CancelButton;
import com.oddlabs.tt.gui.EditLine;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.gui.OKButton;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.guievent.EnterListener;
import com.oddlabs.tt.guievent.MouseClickListener;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;

import java.math.BigInteger;
import java.util.Random;
import java.util.ResourceBundle;

import static com.oddlabs.tt.gui.Placement.BOTTOM_RIGHT;
import static com.oddlabs.tt.gui.Placement.LEFT_MID;
import static com.oddlabs.tt.gui.Placement.RIGHT_MID;

public final class MapcodeForm extends Form {
	private static final int BUTTON_WIDTH = 100;

	private final TerrainMenu menu;

	private final @NonNull EditLine editline_seed;

	public MapcodeForm(TerrainMenu menu) {
		this.menu = menu;
		ResourceBundle bundle = ResourceBundle.getBundle(MapcodeForm.class.getName());
		Label label_seed = new Label(Utils.getBundleString(bundle, "map_code"), Skin.getSkin().getEditFont());
		editline_seed = new EditLine(200, 12, RegistrationKey.CHAR_TO_WORD + RegistrationKey.LOWER_CASE_CHARS, Origin.AT_START);
		editline_seed.addEnterListener(new CodeEnterListener());

		HorizButton button_ok = new OKButton(BUTTON_WIDTH);
		button_ok.addMouseClickListener(new OKListener());
		HorizButton button_cancel = new CancelButton(BUTTON_WIDTH);
		button_cancel.addMouseClickListener( (_, _, _, _) -> this.cancel());
		HorizButton button_rand = new HorizButton(Utils.getBundleString(bundle, "randomize"), BUTTON_WIDTH);
		button_rand.addMouseClickListener(new RandButtonListener());

		addChild(label_seed);
		addChild(editline_seed);
		addChild(button_ok);
		addChild(button_cancel);
		addChild(button_rand);
		label_seed.place();
		editline_seed.place(label_seed, RIGHT_MID);
		button_cancel.place(editline_seed, BOTTOM_RIGHT);
		button_ok.place(button_cancel, LEFT_MID);
		button_rand.place(button_ok, LEFT_MID);
		compileCanvas();
		centerPos();
	}

	@Override
	public void setFocus() {
		editline_seed.setFocus();
	}

	private void done() {
		remove();
		menu.parseMapcode(editline_seed.getContents());
		menu.setFocus();
	}

	private final class OKListener implements MouseClickListener {
		@Override
		public void mouseClicked(@NonNull MouseButton button, int x, int y, int clicks) {
			done();
		}
	}

	private final class RandButtonListener implements MouseClickListener {
		@Override
		public void mouseClicked(@NonNull MouseButton button, int x, int y, int clicks) {
			Random random = new Random(LocalEventQueue.getQueue().getHighPrecisionManager().getTick()*LocalEventQueue.getQueue().getHighPrecisionManager().getTick());
			random.nextInt();
			BigInteger rand_int = new BigInteger(60, random);
			String rand_string = RegistrationKey.createString(rand_int);
			editline_seed.clear();
			editline_seed.append(rand_string);
		}
	}

	public final class CodeEnterListener implements EnterListener {
		@Override
		public void enterPressed(CharSequence text) {
			done();
		}
	}

}
