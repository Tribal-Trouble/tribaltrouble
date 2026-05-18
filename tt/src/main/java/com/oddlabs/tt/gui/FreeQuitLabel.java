package com.oddlabs.tt.gui;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.net.PeerHub;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.ResourceBundle;

public final class FreeQuitLabel extends Label implements Animated {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(FreeQuitLabel.class.getName());

    private @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private final @NonNull World world;
    private final @NonNull AnimationManager manager;

    public FreeQuitLabel(@NonNull World world, @NonNull AnimationManager manager) {
        super("", Skin.getSkin().getEditFont(), 300);
        this.world = world;
        this.manager = manager;
    }

    @Override
    protected void doAdd() {
        super.doAdd();
        manager.registerAnimation(this);
    }

    @Override
    protected void doRemove() {
        super.doRemove();
        manager.removeAnimation(this);
    }

    @Override
    public void animate(float t) {
        int time_left = (int) PeerHub.getFreeQuitTimeLeft(world);
        if (time_left > 0) {
            clear();
            append(i18n("quit_time_left", time_left));
        }
    }
}
