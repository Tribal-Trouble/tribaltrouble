package com.oddlabs.tt.mapeditor;

import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.gui.CancelButton;
import com.oddlabs.tt.gui.EditLine;
import com.oddlabs.tt.gui.FocusDirection;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.OKButton;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.util.WordsEncoding;
import org.jspecify.annotations.NonNull;

import java.math.BigInteger;
import java.util.Random;
import java.util.function.Predicate;

import static com.oddlabs.tt.gui.Placement.BOTTOM_RIGHT;
import static com.oddlabs.tt.gui.Placement.LEFT_MID;
import static com.oddlabs.tt.gui.Placement.RIGHT_MID;

/** Asks for a map code, like the skirmish menu's map code dialog. */
final class MapcodeDialog extends Form {
    private static final int BUTTON_WIDTH = 100;

    private final @NonNull Predicate<@NonNull String> apply;
    private final @NonNull EditLine editline_code;

    /**
     * @param apply takes the entered code and tells whether it was a valid map code
     */
    MapcodeDialog(@NonNull String current_code, @NonNull Predicate<@NonNull String> apply) {
        this.apply = apply;
        Label label_code = new Label(MapEditor.terrainI18n("map_code"), Skin.getSkin().getEditFont());
        editline_code = new EditLine(400, 100);
        editline_code.append(current_code);
        editline_code.addEnterListener(_ -> done());

        HorizButton button_ok = new OKButton(BUTTON_WIDTH);
        button_ok.addMouseClickListener((_, _, _, _) -> done());
        HorizButton button_cancel = new CancelButton(BUTTON_WIDTH);
        button_cancel.addMouseClickListener((_, _, _, _) -> cancel());
        HorizButton button_random = new HorizButton(MapEditor.mapcodeI18n("randomize"), BUTTON_WIDTH);
        button_random.addMouseClickListener((_, _, _, _) -> randomize());

        addChild(label_code);
        addChild(editline_code);
        addChild(button_ok);
        addChild(button_cancel);
        addChild(button_random);
        label_code.place();
        editline_code.place(label_code, RIGHT_MID);
        button_cancel.place(editline_code, BOTTOM_RIGHT);
        button_ok.place(button_cancel, LEFT_MID);
        button_random.place(button_ok, LEFT_MID);
        compileCanvas();
        centerPos();
    }

    @Override
    public void setFocus(@NonNull FocusDirection direction) {
        if (direction == FocusDirection.BACKWARD) {
            super.setFocus(direction);
        } else {
            editline_code.setFocus(direction);
        }
    }

    private void randomize() {
        long tick = LocalEventQueue.getQueue().getHighPrecisionManager().getTick();
        Random random = new Random(tick * tick);
        random.nextInt();
        editline_code.clear();
        editline_code.append(WordsEncoding.encode(new BigInteger(60, random)));
    }

    private void done() {
        if (apply.test(editline_code.getContents())) {
            remove();
        } else {
            editline_code.triggerError();
        }
    }
}
