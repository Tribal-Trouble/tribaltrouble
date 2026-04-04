package com.oddlabs.tt.form;

import com.oddlabs.tt.gui.CancelButton;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUIIcon;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.IconQuad;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.LabelBox;
import com.oddlabs.tt.gui.OKButton;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import static com.oddlabs.tt.gui.Placement.LEFT_MID;
import static com.oddlabs.tt.gui.Placement.RIGHT_MID;
import static com.oddlabs.tt.gui.Placement.TOP_LEFT;

public class CampaignDialogForm extends Form {
    private static final int WIDTH = 300;

    private final @Nullable Runnable runnable;
    private final boolean cancel;
    private boolean dismissed = false;

    private final HorizButton ok_button = new OKButton(80) {
        @Override
        protected void handleInput(@NonNull InputEvent event) {
            if (event.getPhase() == InputPhase.PRESSED && event.consumeAction(GameAction.UI_ACTIVATE)) {
                if (dismissed) return;
                dismissed = true;
                run();
                try {
                    CampaignDialogForm.this.remove();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                event.consume();
                return;
            }
            super.handleInput(event);
        }
    };

    public CampaignDialogForm(@NonNull CharSequence header, @NonNull CharSequence text, @Nullable IconQuad image, @NonNull Origin align) {
        this(header, text, image, align, null);
    }

    public CampaignDialogForm(@NonNull CharSequence header, @NonNull CharSequence text, @Nullable IconQuad image, @NonNull Origin align, @Nullable Runnable runnable) {
        this(header, text, image, align, runnable, false);
    }

    public CampaignDialogForm(@NonNull CharSequence header, @NonNull CharSequence text, @Nullable IconQuad image, @NonNull Origin align, @Nullable Runnable runnable, boolean cancel) {
        this.runnable = runnable;
        this.cancel = cancel;
        buildForm(header, text, image, align, cancel);
        ok_button.addMouseClickListener((_, _, _, _) -> {
            if (dismissed) return;
            dismissed = true;
            run();
            try {
                remove();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        ok_button.addInputListener(event -> {
            if (event.getPhase() == InputPhase.PRESSED) {
                if (event.consumeAction(GameAction.UI_CANCEL)) {
                    this.cancel();
                    event.consume();
                }
            }
        });
    }

    protected void run() {
        if (runnable != null)
            runnable.run();
    }

    @Override
    public void cancel() {
        if (dismissed) return;
        dismissed = true;
        super.cancel();
    }

    @Override
    protected final void doCancel() {
        if (!cancel)
            run();
    }

    private void buildForm(@NonNull CharSequence header, @NonNull CharSequence text, @Nullable IconQuad image, @NonNull Origin align, boolean cancel) {
        GUIIcon gui_icon = null;
        if (image != null) {
            gui_icon = new GUIIcon(image);
            addChild(gui_icon);
        }
        Label header_label = new Label(header, Skin.getSkin().getHeadlineFont());
        addChild(header_label);
        LabelBox label_box = new LabelBox(text, Skin.getSkin().getEditFont(), WIDTH);
        addChild(label_box);
        addChild(ok_button);

        if (gui_icon != null) {
            gui_icon.place();
            label_box.place(gui_icon, align == Origin.AT_START ? RIGHT_MID : LEFT_MID);
        } else {
            label_box.place();
        }
        header_label.place(label_box, TOP_LEFT);
        ok_button.place(Origin.AT_END);
        if (cancel) {
            HorizButton cancel_button = new CancelButton(80);
            addChild(cancel_button);
            cancel_button.place(ok_button, RIGHT_MID);
            cancel_button.addMouseClickListener((_, _, _, _) -> this.cancel());
        }

        compileCanvas();
        centerPos();
    }

    @Override
    protected void handleInput(@NonNull InputEvent event) {
        super.handleInput(event);
    }

    @Override
    public final void setFocus() {
        ok_button.setFocus();
    }
}
