package com.oddlabs.tt.trigger;

import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.animation.TimerAnimation;
import com.oddlabs.tt.landscape.World;

public abstract class IntervalTrigger {
	private final float after_done_time;
	private final AnimationManager animation_manager;
	private TimerAnimation timer;
	
	public IntervalTrigger(World world, float check_interval, float after_done_time) {
		this(check_interval, after_done_time, world.getAnimationManagerRealTime());
	}

	public IntervalTrigger(float check_interval, float after_done_time, AnimationManager animation_manager) {
		this.after_done_time = after_done_time;
		this.animation_manager = animation_manager;
		this.timer = new TimerAnimation(animation_manager, (Object anim) -> {
                    check();
                }, check_interval);
		timer.start();
	}

	protected void triggered() {
		timer.stop();
		timer = new TimerAnimation(animation_manager, (Object anim) -> {
                    ((TimerAnimation)anim).stop();
                    done();
                }, after_done_time);
		timer.start();
	}

	protected final void abort() {
		timer.stop();
		timer = null;
	}

	protected abstract void check();
	protected abstract void done();
}
