package com.oddlabs.tt.tutorial;

import com.oddlabs.tt.animation.TimerAnimation;
import com.oddlabs.tt.delegate.TutorialOverDelegate;
import com.oddlabs.tt.gui.GUIObject;
import com.oddlabs.tt.gui.LabelBox;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.util.Utils;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;

/**
 * Manages the interactive tutorial system, guiding players through game mechanics.
 */
public final class Tutorial {
    private static final int BORDER_OFFSET = 90;

    private final WorldViewer viewer;
    private final TutorialInGameInfo tutorial_info;
    private GUIObject info;
    private TimerAnimation timer;
    private float old_after_done_time;

    public Tutorial(WorldViewer viewer, TutorialInGameInfo tutorial_info, @NonNull TutorialTrigger first_trigger) {
        this.viewer = viewer;
        this.tutorial_info = tutorial_info;
        next0(first_trigger);
        old_after_done_time = first_trigger.getAfterDoneTime();
    }

    WorldViewer getViewer() {
        return viewer;
    }

    private void removeInfo() {
        if (info != null)
            info.remove();
    }

    void done(int next_tutorial) {
        timer.stop();
        removeInfo();
        viewer.getGUIRoot().pushDelegate(new TutorialOverDelegate(viewer, tutorial_info, viewer.getGUIRoot()
                .getDelegate().getCamera(), next_tutorial));
    }

    void next(final @NonNull TutorialTrigger trigger) {
        timer.stop();
        TimerAnimation delay_timer = new TimerAnimation(viewer.getAnimationManagerLocal(), (TimerAnimation anim) -> {
            anim.stop();
            next0(trigger);
        }, old_after_done_time);
        delay_timer.start();
        old_after_done_time = trigger.getAfterDoneTime();
    }

    private void next0(final @NonNull TutorialTrigger trigger) {
        removeInfo();
        TimerAnimation delay_timer = new TimerAnimation(viewer.getAnimationManagerLocal(), (TimerAnimation anim) -> {
            anim.stop();
            next1(trigger);
        }, .5f);
        delay_timer.start();
    }

    private void next1(final @NonNull TutorialTrigger trigger) {
        String text = Utils.getBundleString(ResourceBundle.getBundle(TutorialTrigger.class.getName()), trigger
                .getTextKey(), trigger.getFormatArgs());
        info = new LabelBox(text, Skin.getSkin().getEditFont(), 400);
        info.setPos(BORDER_OFFSET, viewer.getGUIRoot().getHeight() - BORDER_OFFSET - info.getHeight());
        viewer.getGUIRoot().addChild(info);
        viewer.getWorld().getAudio().newAudio(0f, 0f, 0f, viewer.getLocalPlayer().getRaceInfo()
                .getBuildingNotificationAudio());
        timer = new TimerAnimation(viewer.getAnimationManagerLocal(), _ -> trigger.run(Tutorial.this), trigger
                .getCheckInterval());
        timer.start();
    }
}
