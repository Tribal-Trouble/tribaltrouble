package com.oddlabs.tt.gui;

import com.oddlabs.tt.guievent.ItemChosenListener;
import com.oddlabs.util.Quad;
import java.io.Console;

public final strictfp class PulldownButton extends GUIObject {
    private final PulldownMenu menu;
    private final Label label;
    private final GUIRoot gui_root;
    private boolean menu_active;

    public PulldownButton(GUIRoot gui_root, PulldownMenu menu, int width) {
        this.menu = menu;
        this.gui_root = gui_root;
        setCanFocus(true);
        menu.addItemChosenListener(new ItemListener());
        label = new Label("", Skin.getSkin().getEditFont(), 0, Label.ALIGN_LEFT);
        addChild(label);
        setDim(width, Skin.getSkin().getPulldownData().getPulldownButton().getHeight());
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
        if (menu.getWidth() < width) {
            System.out.println("Setting menu width to: " + width);
            menu.setDim(width, menu.getHeight());   
        } 
        else {
            System.out.println("Menu width is sufficient: " + menu.getWidth());
        }
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
        System.out.println("Mouse pressed: " + this);
        if (menu_active) {
            System.out.println("Deactivating menu");
            deactivateMenu();
        } else {
            System.out.println("Activating menu");
            activateMenu();
        }
    }

    protected void mouseEntered() {
        menu_active = menu.isActive();
    }

    protected final void mouseReleased(int button, int x, int y) {
        if (!menu.isActive()) menu.getItem(menu.getChosenItemIndex()).setFocus();
        // Only call clickItem if this mouseReleased is actually on the button itself
        // The menu should handle its own mouse events when they occur over the menu
        if (x >= 0 && x < getWidth() && y >= 0 && y < getHeight()) {
            menu.clickItem(button, x, y, 1);
        }
    }

    private final void activateMenu() {
        menu_active = true;
        menu.setPos(
                (int) (getRootX() + getWidth() - menu.getWidth()),
                (int) (getRootY() - menu.getHeight()));
        gui_root.getDelegate().addChild(menu);
    }

    private final void deactivateMenu() {
        menu_active = false;
        setFocus();
        menu.remove();
    }

    public final PulldownMenu getMenu() {
        return menu;
    }

    protected final void doRemove() {
        super.doRemove();
        if (!menu.isActive()) menu.remove();
    }

    private final strictfp class ItemListener implements ItemChosenListener {
        public final void itemChosen(PulldownMenu menu, int item_index) {
            PulldownItem item = menu.getItem(item_index);
            label.set(item.getLabelString());
            if (menu.isActive()) deactivateMenu();
        }
    }
}
