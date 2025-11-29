package com.oddlabs.tt.delegate;


import com.oddlabs.tt.camera.Camera;
import com.oddlabs.tt.form.InGameOptionsMenu;
import com.oddlabs.tt.form.QuestionForm;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.KeyboardEvent;
import com.oddlabs.tt.gui.MenuButton;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.guievent.MouseClickListener;
import com.oddlabs.tt.util.Utils;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;
import org.lwjgl.input.Keyboard;

public final class InGameMainMenu extends Menu {
	private final @NonNull WorldViewer viewer;

	private Group game_infos;

	public InGameMainMenu(@NonNull WorldViewer viewer, Camera camera) {
		super(viewer.getNetwork(), viewer.getGUIRoot(), camera);
		this.viewer = viewer;
		reload();
	}

	@Override
	protected void doAdd() {
		super.doAdd();
		viewer.setPaused(true);
	}

	@Override
	protected void doRemove() {
		super.doRemove();
		viewer.setPaused(false);
	}

	public void addAbortButton(@NonNull String abort_text) {
		MenuButton abort = new MenuButton(abort_text, COLOR_NORMAL, COLOR_ACTIVE);
		addChild(abort);
		abort.addMouseClickListener(new AbortListener());
	}

	@Override
	protected void addButtons() {
		addResumeButton();

		addOptionsButton(() -> new InGameOptionsMenu(getGUIRoot(), viewer));

		game_infos = new Group(false);
		viewer.addGUI(this, game_infos);
		addChild(game_infos);

		addExitButton();
	}

	@Override
	public void displayChangedNotify(int width, int height) {
		super.displayChangedNotify(width, height);
		if (game_infos != null)
			game_infos.setPos((width - game_infos.getWidth())/2, (height - game_infos.getHeight())/2);
	}

	@Override
	protected void keyPressed(@NonNull KeyboardEvent event) {
		switch(event.getKeyCode()) {
			case Keyboard.KEY_ESCAPE:
				pop();
				break;
			default:
				super.keyPressed(event);
				break;
		}
	}

	@Override
	protected void renderGeometry() {
		super.renderGeometry();
		renderBackgroundAlpha();
	}

	private final class AbortListener implements MouseClickListener {
		@Override
		public void mouseClicked(@NonNull MouseButton button, int x, int y, int clicks) {
			setMenuCentered(new QuestionForm(Utils.getBundleString(bundle, "end_game_confirm"), new ActionAbortListener()));
		}
	}

	private final class ActionAbortListener implements MouseClickListener {
		@Override
		public void mouseClicked(@NonNull MouseButton button, int x, int y, int clicks) {
			viewer.abort();
		}
	}
}
