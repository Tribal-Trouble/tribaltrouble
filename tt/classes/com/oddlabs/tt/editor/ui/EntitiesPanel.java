package com.oddlabs.tt.editor.ui;

import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.PulldownButton;
import com.oddlabs.tt.gui.PulldownItem;
import com.oddlabs.tt.gui.PulldownMenu;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.guievent.MouseMotionListener;
import com.oddlabs.tt.gui.LocalInput;
import org.lwjgl.opengl.GL11;

/**
 * Draggable, semi-transparent panel for Entities tool selectors.
 * Contains Race, Team, and Kind (in that order). Appears when Entities tool is active.
 */
public final class EntitiesPanel extends Form {
    private final GUIRoot guiRoot;
    private EditorOptionsBinding optionsBinding;
    private boolean suppressProgrammatic;

    private Label raceLabel;
    private PulldownButton raceButton;
    private Label teamLabel;
    private PulldownButton teamButton;
    private Label kindLabel;
    private PulldownButton kindButton;

    public EntitiesPanel(GUIRoot guiRoot, EditorOptionsBinding optionsBinding) {
        super("Entities");
        this.guiRoot = guiRoot;
        this.optionsBinding = optionsBinding;

        // Build pulldowns: Race, Team, Kind
        raceLabel = new Label("Race", Skin.getSkin().getEditFont());
        PulldownMenu raceMenu = new PulldownMenu();
        if (optionsBinding != null) {
            for (String n : optionsBinding.getEntitiesRaceNames()) raceMenu.addItem(new PulldownItem(n));
        } else {
            raceMenu.addItem(new PulldownItem("Natives"));
            raceMenu.addItem(new PulldownItem("Vikings"));
        }
        raceButton = new PulldownButton(guiRoot, raceMenu, 0, 120);

        teamLabel = new Label("Team", Skin.getSkin().getEditFont());
        PulldownMenu teamMenu = new PulldownMenu();
        if (optionsBinding != null) {
            for (String n : optionsBinding.getEntitiesTeamNames()) teamMenu.addItem(new PulldownItem(n));
        } else {
            teamMenu.addItem(new PulldownItem("Neutral"));
            for (int i=0;i<8;i++) teamMenu.addItem(new PulldownItem("Team " + i));
        }
        teamButton = new PulldownButton(guiRoot, teamMenu, 1, 120);

        kindLabel = new Label("Kind", Skin.getSkin().getEditFont());
        PulldownMenu kindMenu = new PulldownMenu();
        if (optionsBinding != null) {
            for (String n : optionsBinding.getEntitiesKindNames()) kindMenu.addItem(new PulldownItem(n));
        } else {
            kindMenu.addItem(new PulldownItem("Quarters"));
            kindMenu.addItem(new PulldownItem("Armory"));
            kindMenu.addItem(new PulldownItem("Tower"));
            kindMenu.addItem(new PulldownItem("Ship"));
        }
        kindButton = new PulldownButton(guiRoot, kindMenu, 0, 170);

        addChild(raceLabel);
        addChild(raceButton);
        addChild(teamLabel);
        addChild(teamButton);
        addChild(kindLabel);
        addChild(kindButton);

        int spacing = StrictMath.max(2, Skin.getSkin().getFormData().getObjectSpacing());
        raceLabel.place();
        raceButton.place(raceLabel, RIGHT_MID);
        teamLabel.place(raceButton, RIGHT_MID, spacing);
        teamButton.place(teamLabel, RIGHT_MID);
        kindLabel.place(teamButton, RIGHT_MID, spacing);
        kindButton.place(kindLabel, RIGHT_MID);

        if (optionsBinding != null) {
            // Initialize from binding
            syncOptionsFromBinding();

            raceButton.getMenu().addItemChosenListener((menu, idx) -> {
                if (menu == null) return;
                if (!suppressProgrammatic) optionsBinding.setEntitiesRaceIndex(idx);
            });
            teamButton.getMenu().addItemChosenListener((menu, idx) -> {
                if (menu == null) return;
                if (!suppressProgrammatic) optionsBinding.setEntitiesTeamIndex(idx);
            });
            kindButton.getMenu().addItemChosenListener((menu, idx) -> {
                if (menu == null) return;
                if (!suppressProgrammatic) optionsBinding.setEntitiesKindIndex(idx);
            });
        }

        compileCanvas();
        // Clamp vertical position during drag (free horizontal movement)
        addMouseMotionListener(new MouseMotionListener() {
            public void mouseDragged(int button, int x, int y, int rel_x, int rel_y, int abs_x, int abs_y) {
                int h = EntitiesPanel.this.getHeight();
                int maxY = StrictMath.max(0, LocalInput.getViewHeight() - h);
                int clampedY = clamp(EntitiesPanel.this.getY(), 0, maxY);
                EntitiesPanel.this.setPos(EntitiesPanel.this.getX(), clampedY);
            }
            public void mouseMoved(int x, int y) {}
            public void mouseEntered() {}
            public void mouseExited() {}
        });
    }

    public void setOptionsBinding(EditorOptionsBinding binding) {
        this.optionsBinding = binding;
        syncOptionsFromBinding();
    }

    public void syncOptionsFromBinding() {
        if (optionsBinding == null) return;
        suppressProgrammatic = true;
        try {
            select(raceButton, clamp(optionsBinding.getEntitiesRaceIndex(), 0, raceButton.getMenu().getSize() - 1));
            select(teamButton, clamp(optionsBinding.getEntitiesTeamIndex(), 0, teamButton.getMenu().getSize() - 1));
            rebuildKindMenu();
            select(kindButton, clamp(optionsBinding.getEntitiesKindIndex(), 0, kindButton.getMenu().getSize() - 1));
        } finally {
            suppressProgrammatic = false;
        }
    }

    private void rebuildKindMenu() {
        if (optionsBinding == null) return;
        PulldownMenu menu = kindButton.getMenu();
        try { menu.clearItems(); } catch (Throwable ignore) {}
        for (String n : optionsBinding.getEntitiesKindNames()) menu.addItem(new PulldownItem(n));
    }

    private void select(PulldownButton btn, int idx) {
        try { btn.getMenu().chooseItem(idx); } catch (Throwable ignore) {}
    }

    private static int clamp(int v, int min, int max) { return v < min ? min : (v > max ? max : v); }

    @Override
    protected void displayChangedNotify(int width, int height) {
        int clampedY = clamp(getY(), 0, StrictMath.max(0, height - getHeight()));
        setPos(getX(), clampedY);
    }

    // Render with opaque borders and 65% translucent center (like the toolbar)
    @Override
    protected void renderGeometry(float clip_left, float clip_right, float clip_bottom, float clip_top) {
        GL11.glEnd();
        com.oddlabs.tt.gui.Box box = Skin.getSkin().getFormData().getForm();
        int w = getWidth();
        int h = getHeight();
        int lw = box.getLeftOffset();
        int rw = box.getRightOffset();
        int bh = box.getBottomOffset();
        int th = box.getTopOffset();
        int innerW = StrictMath.max(0, w - lw - rw);
        int innerH = StrictMath.max(0, h - bh - th);

        int skinType = isDisabled() ? Skin.DISABLED : (isActive() ? Skin.ACTIVE : Skin.NORMAL);

        Skin.getSkin().bindTexture();
        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);

        int rootX = (int) getRootX();
        int rootY = (int) getRootY();

        if (th > 0) {
            GL11.glScissor(rootX, rootY + h - th, w, th);
            GL11.glBegin(GL11.GL_QUADS);
            box.render(0, 0, w, h, skinType);
            GL11.glEnd();
        }
        if (bh > 0) {
            GL11.glScissor(rootX, rootY, w, bh);
            GL11.glBegin(GL11.GL_QUADS);
            box.render(0, 0, w, h, skinType);
            GL11.glEnd();
        }
        if (lw > 0) {
            GL11.glScissor(rootX, rootY, lw, h);
            GL11.glBegin(GL11.GL_QUADS);
            box.render(0, 0, w, h, skinType);
            GL11.glEnd();
        }
        if (rw > 0) {
            GL11.glScissor(rootX + w - rw, rootY, rw, h);
            GL11.glBegin(GL11.GL_QUADS);
            box.render(0, 0, w, h, skinType);
            GL11.glEnd();
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        if (innerW > 0 && innerH > 0) {
            GL11.glColor4f(1f, 1f, 1f, 0.65f);
            GL11.glBegin(GL11.GL_QUADS);
            box.renderHighlight(lw, bh, innerW, innerH, 0, w, 0, h);
            GL11.glEnd();
        }

        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glBegin(GL11.GL_QUADS);
    }
}
