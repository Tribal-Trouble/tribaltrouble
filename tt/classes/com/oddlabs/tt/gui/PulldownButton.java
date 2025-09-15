package com.oddlabs.tt.gui;

import com.oddlabs.tt.guievent.ItemChosenListener;
import com.oddlabs.util.Quad;

public final strictfp class PulldownButton extends GUIObject {
    private final PulldownMenu menu;
    private final Label label;
    private final GUIRoot gui_root;
    private boolean menu_active;
    private boolean hiddenFlag;

    public PulldownButton(GUIRoot gui_root, PulldownMenu menu, int width) {
        this.menu = menu;
        this.gui_root = gui_root;
        setCanFocus(true);
        menu.addItemChosenListener(new ItemListener());
        label = new Label("", Skin.getSkin().getEditFont(), 0, Label.ALIGN_LEFT);
        addChild(label);
        setDim(width, Skin.getSkin().getPulldownData().getPulldownButton().getHeight());
    }

    @Override
    public void setHidden(boolean hidden) {
        super.setHidden(hidden);
        hiddenFlag = hidden;
        if (hidden) {
            // If this control is hidden, forcibly close and reset menu state
            // Explicitly defocus the menu so its internal 'active' flag is cleared
            menu.focusNotifyAll(false);
            menu.remove();
            menu_active = false;
        }
        // Hidden controls should not be focusable or participate in picking
        setCanFocus(!hidden);
    }

    @Override
    public void setDisabled(boolean disabled) {
        super.setDisabled(disabled);
        if (disabled) {
            // If disabled, always close the menu and reset state
            // Explicitly defocus first to ensure active state is cleared
            menu.focusNotifyAll(false);
            menu.remove();
            menu_active = false;
        }
        // Disabled controls should not be focusable to avoid intercepting events
        setCanFocus(!disabled && !hiddenFlag);
    }

    public PulldownButton(GUIRoot gui_root, PulldownMenu menu, int item_index, int width) {
        this(gui_root, menu, width);
        menu.chooseItem(item_index);
    }

    public final void setDim(int width, int height) {
        super.setDim(width, height);
        PulldownData data = Skin.getSkin().getPulldownData();
        label.setDim(
                getWidth()
                        - data.getTextOffsetLeft()
                        - data.getArrowOffsetRight()
                        - data.getArrow()[0].getWidth(),
                label.getHeight());
        label.setPos(data.getTextOffsetLeft(), (getHeight() - label.getHeight()) / 2);
        if (menu.getWidth() < width) menu.setDim(width, menu.getHeight());
    }

    protected final void renderGeometry() {
        PulldownData data = Skin.getSkin().getPulldownData();
        Quad[] arrow = data.getArrow();

        if (isDisabled()) {
            data.getPulldownButton().render(0, 0, getWidth(), Skin.DISABLED);
            arrow[Skin.DISABLED].render(
                    getWidth() - data.getArrowOffsetRight() - arrow[Skin.DISABLED].getWidth(), 0);
        } else if (isActive()) {
            data.getPulldownButton().render(0, 0, getWidth(), Skin.ACTIVE);
            arrow[Skin.ACTIVE].render(
                    getWidth() - data.getArrowOffsetRight() - arrow[Skin.ACTIVE].getWidth(), 0);
        } else {
            data.getPulldownButton().render(0, 0, getWidth(), Skin.NORMAL);
            arrow[Skin.NORMAL].render(
                    getWidth() - data.getArrowOffsetRight() - arrow[Skin.NORMAL].getWidth(), 0);
        }
    }

    protected final void mousePressed(int button, int x, int y) {
        // Sync with actual menu state in case it was externally removed/closed
        menu_active = menu.isActive();
        if (menu_active) {
            deactivateMenu();
        } else {
            activateMenu();
        }
    }

    @Override
    protected void mouseEntered() {
        menu_active = menu.isActive();
    }

    @Override
    protected final void mouseReleased(int button, int x, int y) {
        // If the menu isn’t open anymore (e.g., closed due to a tool switch),
        // do not attempt to click items or adjust focus here.
        if (!menu.isActive()) return;
        menu.clickItem(button, x, y, 1);
    }

    private void activateMenu() {
        menu_active = true;
        int x = (int) (getRootX() + getWidth() - menu.getWidth());
        int y = (int) (getRootY() - menu.getHeight());
        // If opening above would go off-screen, open below the button instead
        if (y < 0) {
            y = (int) (getRootY() + getHeight());
        }
        // Clamp to viewport
        int maxX = gui_root.getWidth() - menu.getWidth();
        int maxY = gui_root.getHeight() - menu.getHeight();
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        if (x > maxX) x = maxX;
        if (y > maxY) y = maxY;
        menu.setPos(x, y);
        // Ensure any previously attached instance is detached before re-adding
        menu.remove();
        // Attach to modal layer when present so it renders above the modal form and can receive focus
        if (gui_root.getModalDelegate() != null) {
            gui_root.getModalDelegate().addChild(menu);
        } else {
            gui_root.getDelegate().addChild(menu);
        }
        // Give the menu focus so clicking elsewhere will defocus and close it
        menu.setFocus();
    }

    private void deactivateMenu() {
        menu_active = false;
        // Defocus menu to clear its active state, then remove it
        menu.focusNotifyAll(false);
        menu.remove();
    }

    public final PulldownMenu getMenu() {
        return menu;
    }

    @Override
    protected final void doRemove() {
        super.doRemove();
        // Always remove the menu to avoid leaving a dangling active menu in the tree
        menu.remove();
    }

    private class ItemListener implements ItemChosenListener {
        @Override
        public final void itemChosen(PulldownMenu menu, int item_index) {
            PulldownItem item = menu.getItem(item_index);
            label.set(item.getLabelString());
            // Always close the menu after a selection to reset state
            deactivateMenu();
        }
    }
}
