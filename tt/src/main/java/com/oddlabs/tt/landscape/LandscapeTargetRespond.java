package com.oddlabs.tt.landscape;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.model.Element;
import com.oddlabs.tt.model.ElementVisitor;
import com.oddlabs.tt.util.StateChecksum;
import org.jspecify.annotations.NonNull;

public final class LandscapeTargetRespond extends Element<LandscapeTargetRespond> implements Animated {
	public final static int SIZE = 128;
	private final static float SECOND_PER_PICK_RESPOND = 1f/3f;

	private float time;

	public LandscapeTargetRespond(@NonNull World world, float x, float y) {
		super(world.getElementRoot());
		setPosition(x, y);
		setPositionZ(world.getHeightMap().getNearestHeight(x, y));
		setBounds(x - SIZE/2, x + SIZE/2, y - SIZE/2, y + SIZE/2, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
		register();
	}

    @Override
	public void animate(float t) {
		if (time > 0) {
			time = Math.max(0, time - t);
		} else {
			remove();
		}
	}

	public float getProgress() {
		return time/SECOND_PER_PICK_RESPOND;
	}

    @Override
	public void updateChecksum(StateChecksum checksum) {
	}

    @Override
	protected void register() {
		super.register();
		time = SECOND_PER_PICK_RESPOND;
		LocalEventQueue.getQueue().getManager().registerAnimation(this);
	}

    @Override
	public void visit(@NonNull ElementVisitor visitor) {
		visitor.visitRespond(this);
	}

    @Override
	protected void remove() {
		super.remove();
		LocalEventQueue.getQueue().getManager().removeAnimation(this);
	}
}
