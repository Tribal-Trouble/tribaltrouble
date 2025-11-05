package com.oddlabs.tt.render;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.util.StateChecksum;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public final class RespondManager implements Animated {
	private final static float SECONDS_PER_PICK_RESPOND = 1f/3f;

	private final NavigableMap<Timeout,Object> respond_timeouts = new TreeMap<>();
	private final Map<Object,Timeout> respond_targets = new HashMap<>();

	private int current_id;

	private float time;

	RespondManager(@NonNull AnimationManager manager) {
		manager.registerAnimation(this);
	}

        @Override
	public void animate(float t) {
		time += t;
		timeout();
	}

	private void timeout() {
		Timeout head_timeout;
		while (!respond_timeouts.isEmpty() && (head_timeout = respond_timeouts.firstKey()).timeout <= time) {
			removeResponder(head_timeout.target);
		}
	}

	public void addResponder(Object target) {
		addResponder(target, null);
	}

	void addResponder(Object target, Runnable stop_action) {
		addResponder(SECONDS_PER_PICK_RESPOND, target, null);
	}

	private void addResponder(float respond_time, Object target, Runnable stop_action) {
		removeResponder(target);
		Timeout timeout = new Timeout(time + respond_time, current_id++, target, stop_action);
		respond_targets.put(target, timeout);
		respond_timeouts.put(timeout, target);
	}

	private void removeResponder(Object target) {
		Timeout timeout = respond_targets.remove(target);
		if (timeout != null) {
			respond_timeouts.remove(timeout);
			if (timeout.stop_action != null)
				timeout.stop_action.run();
		}
	}

	boolean isResponding(Object target) {
		if (respond_targets.isEmpty())
			return false; // Quick exit in the common case of no responding targets
		else
			return isResponding(respond_targets.get(target));
	}

	private boolean isResponding(@Nullable Timeout timeout) {
		if (timeout == null)
			return false;
		float time_diff = timeout.timeout - time;
		float blink = SECONDS_PER_PICK_RESPOND/4f;
		return time_diff > 0 && (time_diff >= SECONDS_PER_PICK_RESPOND - blink || time_diff <= blink);
	}

    @Override
	public void updateChecksum(StateChecksum checksum) {
	}

	private final static class Timeout implements Comparable<Timeout> {
		private final float timeout;
		private final int id;
		private final Object target;
		private final Runnable stop_action;

		Timeout(float timeout, int id, Object target, Runnable stop_action) {
			this.timeout = timeout;
			this.id = id;
			this.target = target;
			this.stop_action = stop_action;
		}

        @Override
		public boolean equals(Object other) {
            if (other instanceof Timeout timeout_obj) {
                return timeout_obj.timeout == timeout && timeout_obj.id == id;
            }
            return false;
		}

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 59 * hash + Float.floatToIntBits(this.timeout);
            hash = 59 * hash + this.id;
            return hash;
        }

        @Override
		public int compareTo(@NonNull Timeout other) {
			float diff = timeout - other.timeout;
			if (diff != 0f)
				return (int)diff;
			else
				return id - other.id;
		}
	}
}
