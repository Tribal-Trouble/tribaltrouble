package com.oddlabs.tt.form;

/** Centralized color palette for keybind UI elements. All colors are RGBA floats in range [0,1]. */
public final class KeybindColors {
    private KeybindColors() {}

    // Slightly green-tinted off-white for default labels (dark UI friendly)
    // Hex: #E6F3EA
    public static final float[] DEFAULT_LABEL = new float[] {0.902f, 0.953f, 0.918f, 1f};

    // Section header and info/subtext
    // Section header: blue-grey — #9FB3C8
    public static final float[] SECTION_HEADER = new float[] {0.624f, 0.702f, 0.784f, 1f};
    // Info/subtext: steel-grey — #B6C2CF
    public static final float[] INFO_SUBTEXT = new float[] {0.714f, 0.761f, 0.812f, 1f};

    // States
    // Custom/Changed: soft blue — #64B5F6
    public static final float[] CUSTOM = new float[] {0.392f, 0.710f, 0.965f, 1f};
    // Conflict (same-section duplicate): error red — #E53935
    public static final float[] CONFLICT = new float[] {0.898f, 0.224f, 0.208f, 1f};
    // Overlap (cross-section duplicate, allowed): gold — #E0C36E
    public static final float[] OVERLAP = new float[] {0.878f, 0.765f, 0.431f, 1f};
    // Unbound: amber — #FFB74D
    public static final float[] UNBOUND = new float[] {1.000f, 0.718f, 0.302f, 1f};

    // Status toasts (unchanged, included for convenience)
    public static final float[] SUCCESS = new float[] {0.298f, 0.686f, 0.314f, 1f};
    public static final float[] ERROR = new float[] {0.900f, 0.200f, 0.200f, 1f};
}
