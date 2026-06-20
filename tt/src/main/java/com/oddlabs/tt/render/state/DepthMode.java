package com.oddlabs.tt.render.state;

public enum DepthMode implements Mode {
    NONE {
        @Override
        public void apply(RenderContext context) {
            context.setDepthTest(false);
        }
    },
    READ_ONLY {
        @Override
        public void apply(RenderContext context) {
            context.setDepthTest(true);
            context.setDepthMask(false);
        }
    },
    READ_WRITE {
        @Override
        public void apply(RenderContext context) {
            context.setDepthTest(true);
            context.setDepthMask(true);
        }
    };
}
