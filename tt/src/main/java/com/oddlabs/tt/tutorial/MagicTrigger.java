package com.oddlabs.tt.tutorial;

import com.oddlabs.tt.form.TutorialForm;
import com.oddlabs.tt.model.MagicType;
import com.oddlabs.tt.model.Unit;
import org.jspecify.annotations.NonNull;

import java.util.EnumSet;

/**
 * Tutorial trigger tracking when a chieftain successfully uses magic spells.
 */
public final class MagicTrigger extends TutorialTrigger {
    private final EnumSet<MagicType> magic_used = EnumSet.noneOf(MagicType.class);

    private final @NonNull Unit chieftain;

    public MagicTrigger(@NonNull Unit chieftain) {
        super(.1f, 20f, "magic");
        this.chieftain = chieftain;
    }

    @Override
    protected void run(@NonNull Tutorial tutorial) {
        MagicType last = chieftain.getLastMagicType();
        if (last != null)
            magic_used.add(last);
        for (MagicType type : chieftain.getOwner().getRaceInfo().getMagics()) {
            if (!magic_used.contains(type))
                return;
        }
        tutorial.done(TutorialForm.TUTORIAL_CHIEFTAIN);
    }
}
