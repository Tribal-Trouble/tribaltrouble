package com.oddlabs.tt.animation;

import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.util.StateChecksum;

public final class TimerAnimation implements Animated {
	private final AnimationManager manager;
	private float time = 0;
	private float interval;
	private Updatable timer_owner;
	private boolean running = false;

	public TimerAnimation(Updatable owner, float interval) {
		this(LocalEventQueue.getQueue().getManager(), owner, interval);
	}

	public TimerAnimation(AnimationManager manager, Updatable owner, float interval) {
		this.manager = manager;
		this.interval = interval;
		this.timer_owner = owner;
	}

    @Override
	public String toString() {
		return "TimerAnimation{owner=" + timer_owner + "}";
	}

    @Override
	public void updateChecksum(StateChecksum checksum) {
		checksum.update(time);
	}

    public boolean isRunning() {
		return running;
	}

	public void stop() {
		running = false;
		manager.removeAnimation(this);
	}

	public void start() {
		running = true;
		manager.registerAnimation(this);
	}

	public void setTimerOwner(Updatable obj) {
		this.timer_owner = obj;
	}

	public Updatable getTimerOwner() {
		return timer_owner;
	}

	public void setTimerInterval(float interval) {
		this.interval = interval;
	}

	public void resetTime() {
		time = 0;
	}

    @Override
	public void animate(float t) {
		time += t;
		while (time > interval) {
			time -= Math.max(t, interval);
			if (timer_owner != null)
				timer_owner.update(this);
		}
	}
}
