package com.oddlabs.tt.form;

import com.oddlabs.tt.delegate.MainMenu;
import com.oddlabs.tt.gui.*;
import com.oddlabs.tt.font.Font;

/**
 * A non-interactive demo form that visually mocks up the Trigger Editor concept.
 * It does not wire any behavior; it simply shows how the UI could look using existing widgets.
 */
public final class TriggerEditorDemoForm extends Form {
    private static final int LIST_HEIGHT = 300;
    private static final int DETAILS_HEIGHT = 360;

    private final MainMenu main_menu;

    public TriggerEditorDemoForm(GUIRoot gui_root, MainMenu main_menu) {
        super();
        this.main_menu = main_menu;

        // Left: Trigger list panel
        Panel leftPanel = new Panel("Custom Triggers");
    Label leftHeadline = new Label("Design your own triggers", Skin.getSkin().getHeadlineFont());
        leftPanel.addChild(leftHeadline);

        ColumnInfo[] triggerListColumns = new ColumnInfo[] {
                new ColumnInfo("Name", 180),
                new ColumnInfo("Type", 140),
                new ColumnInfo("Enabled", 90)
        };
    MultiColumnComboBox triggerList = new MultiColumnComboBox(gui_root, triggerListColumns, LIST_HEIGHT);
    // Seed with a couple of example rows
    Font font = Skin.getSkin().getMultiColumnComboBoxData().getFont();
    Row row1 = new Row(new GUIObject[] { new Label("Ritual Timer", font), new Label("Time", font), new Label("Yes", font) }, null);
    Row row2 = new Row(new GUIObject[] { new Label("Ambush East Path", font), new Label("Near Army", font), new Label("No", font) }, null);
        triggerList.addRow(row1);
        triggerList.addRow(row2);
        leftPanel.addChild(triggerList);

        // List controls (non-functional)
        HorizButton addBtn = new HorizButton("Add", 80);
        HorizButton editBtn = new HorizButton("Edit", 80);
        HorizButton removeBtn = new HorizButton("Remove", 100);
        HorizButton duplicateBtn = new HorizButton("Duplicate", 120);
    ArrowButton upBtn = new ArrowButton(
        Skin.getSkin().getScrollBarData().getScrollUpButtonPressed(),
        Skin.getSkin().getScrollBarData().getScrollUpButtonUnpressed(),
        Skin.getSkin().getScrollBarData().getScrollUpArrow());
    ArrowButton downBtn = new ArrowButton(
        Skin.getSkin().getScrollBarData().getScrollDownButtonPressed(),
        Skin.getSkin().getScrollBarData().getScrollDownButtonUnpressed(),
        Skin.getSkin().getScrollBarData().getScrollDownArrow());
        leftPanel.addChild(addBtn);
        leftPanel.addChild(editBtn);
        leftPanel.addChild(removeBtn);
        leftPanel.addChild(duplicateBtn);
        leftPanel.addChild(upBtn);
        leftPanel.addChild(downBtn);

        // Layout left panel
        leftHeadline.place();
        triggerList.place(leftHeadline, BOTTOM_LEFT);
        addBtn.place(triggerList, BOTTOM_LEFT);
        editBtn.place(addBtn, RIGHT_MID);
        removeBtn.place(editBtn, RIGHT_MID);
        duplicateBtn.place(removeBtn, RIGHT_MID);
        upBtn.place(duplicateBtn, RIGHT_MID);
        downBtn.place(upBtn, RIGHT_MID);
        leftPanel.compileCanvas();

        // Right: Details panel
        Panel rightPanel = new Panel("Trigger Details");

        // Condition section
    Label conditionHeadline = new Label("Condition", Skin.getSkin().getHeadlineFont());
        rightPanel.addChild(conditionHeadline);

        // Type row (static preview)
    Label typeLbl = new Label("Type:", font);
    LabelBox typeVal = new LabelBox("Time", font, 180);
        rightPanel.addChild(typeLbl);
        rightPanel.addChild(typeVal);

        // Parameter mock rows
        // Time parameters
    Label timeLbl = new Label("Time (mm:ss):", font);
    LabelBox timeVal = new LabelBox("09:00", font, 180);
        rightPanel.addChild(timeLbl);
        rightPanel.addChild(timeVal);

        // NearPoint parameters
    Label nearPointLbl = new Label("Near Point (grid):", font);
    LabelBox nearPointVal = new LabelBox("X:128  Y:128  r:6", font, 240);
        rightPanel.addChild(nearPointLbl);
        rightPanel.addChild(nearPointVal);

        // MagicUsed parameters
    Label magicLbl = new Label("Magic Used:", font);
    LabelBox magicVal = new LabelBox("Index: 0  area r:10m", font, 240);
        rightPanel.addChild(magicLbl);
        rightPanel.addChild(magicVal);

        // Orchestration flags
        Label orchestrationHeadline = new Label("Orchestration", Skin.getSkin().getHeadlineFont());
        rightPanel.addChild(orchestrationHeadline);
        CheckBox runOnce = new CheckBox(true, "Run once");
        CheckBox abortOnEnd = new CheckBox(true, "Abort on victory/defeat");
        CheckBox repeatable = new CheckBox(false, "Repeatable");
        rightPanel.addChild(runOnce);
        rightPanel.addChild(abortOnEnd);
        rightPanel.addChild(repeatable);

        // Actions section
    Label actionsHeadline = new Label("Actions", Skin.getSkin().getHeadlineFont());
        rightPanel.addChild(actionsHeadline);

        ColumnInfo[] actionCols = new ColumnInfo[] {
                new ColumnInfo("Action", 220),
                new ColumnInfo("Params", 260)
        };
        MultiColumnComboBox actionList = new MultiColumnComboBox(gui_root, actionCols, DETAILS_HEIGHT);
    Row a1 = new Row(new GUIObject[] { new Label("Show dialog", font), new Label("\"The ritual begins...\"", font) }, null);
    Row a2 = new Row(new GUIObject[] { new Label("Deploy + Attack", font), new Label("Enemy: 12x Iron -> (128,128)", font) }, null);
    Row a3 = new Row(new GUIObject[] { new Label("Start timer", font), new Label("09:00 -> Victory", font) }, null);
        actionList.addRow(a1);
        actionList.addRow(a2);
        actionList.addRow(a3);
        rightPanel.addChild(actionList);

        HorizButton addAction = new HorizButton("Add Action", 120);
        HorizButton editAction = new HorizButton("Edit", 80);
        HorizButton removeAction = new HorizButton("Remove", 100);
        rightPanel.addChild(addAction);
        rightPanel.addChild(editAction);
        rightPanel.addChild(removeAction);

        // Layout right panel
        conditionHeadline.place();
        typeLbl.place(conditionHeadline, BOTTOM_LEFT);
        typeVal.place(typeLbl, RIGHT_MID);

        timeLbl.place(typeLbl, BOTTOM_LEFT);
        timeVal.place(timeLbl, RIGHT_MID);

        nearPointLbl.place(timeLbl, BOTTOM_LEFT);
        nearPointVal.place(nearPointLbl, RIGHT_MID);

        magicLbl.place(nearPointLbl, BOTTOM_LEFT);
        magicVal.place(magicLbl, RIGHT_MID);

        orchestrationHeadline.place(magicLbl, BOTTOM_LEFT);
        runOnce.place(orchestrationHeadline, BOTTOM_LEFT);
        abortOnEnd.place(runOnce, BOTTOM_LEFT);
        repeatable.place(abortOnEnd, BOTTOM_LEFT);

        actionsHeadline.place(repeatable, BOTTOM_LEFT);
        actionList.place(actionsHeadline, BOTTOM_LEFT);

        addAction.place(actionList, BOTTOM_LEFT);
        editAction.place(addAction, RIGHT_MID);
        removeAction.place(editAction, RIGHT_MID);

        rightPanel.compileCanvas();

        // Position both panels on the form
        addChild(leftPanel);
        addChild(rightPanel);

        leftPanel.place(ORIGIN_TOP_LEFT);
        rightPanel.place(leftPanel, RIGHT_TOP);

        // Close button at bottom-right
        HorizButton closeBtn = new HorizButton("Close", 110);
        addChild(closeBtn);
        closeBtn.addMouseClickListener(new CancelListener(this));

        // Place close button relative to the form before compiling
        closeBtn.place(ORIGIN_BOTTOM_RIGHT);

        compileCanvas();
    }
}
