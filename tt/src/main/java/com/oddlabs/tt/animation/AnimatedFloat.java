package com.oddlabs.tt.animation;

public final class AnimatedFloat {
    public float val;
    public float cval;
    public float dval;

    public AnimatedFloat() {
        reset(0f);
    }

    public void change(float animation_complete) {
        if (animation_complete >= 1.0f)
            val = dval;
        cval = (dval - val) * animation_complete + val;
    }

    public void updateVal() {
        val = cval;
    }

    public void reset(float new_val) {
        val = new_val;
        dval = new_val;
        cval = new_val;
    }
}
