package com.oddlabs.tt.gui;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Text dump of the live GUI tree for scripted input: one object per line as
 * {@code depth class x y width height [text]} in framebuffer pixels with a top-left origin, matching screenshots.
 */
public final class GUITreeDump {
    private GUITreeDump() {
    }

    public static @NonNull String dump(@NonNull GUIRoot root, int framebuffer_height) {
        StringBuilder out = new StringBuilder();
        append(out, root, 0, 0, 0, root.getGlobalScale(), framebuffer_height);
        return out.toString();
    }

    private static void append(@NonNull StringBuilder out, @NonNull GUIObject object, int depth, int parent_x,
            int parent_y, float scale, int framebuffer_height) {
        int abs_x = parent_x + object.getX();
        int abs_y = parent_y + object.getY();
        int px = Math.round(abs_x * scale);
        int py = framebuffer_height - Math.round((abs_y + object.getHeight()) * scale);
        int pw = Math.round(object.getWidth() * scale);
        int ph = Math.round(object.getHeight() * scale);
        out.append(depth).append(' ').append(object.getClass().getSimpleName()).append(' ').append(px).append(
                ' ').append(py).append(' ').append(pw).append(' ').append(ph);
        String text = textOf(object);
        if (!text.isEmpty()) {
            out.append(' ').append(text);
        }
        out.append('\n');
        GUIObject child = object.getFirstChild();
        while (child != null) {
            append(out, child, depth + 1, abs_x, abs_y, scale, framebuffer_height);
            child = child.getNext();
        }
    }

    private static @NonNull String textOf(@NonNull GUIObject object) {
        if (object instanceof PasswordLine field) {
            return "*".repeat(field.getContents().length());
        }
        if (object instanceof TextField field) {
            return field.getContents().replace('\n', ' ');
        }
        if (object instanceof MenuButton button) {
            return button.getText().toString();
        }
        if (object instanceof Diode diode) {
            return diode.isLit() ? "on" : "off";
        }
        return "";
    }

    private record Placed(@NonNull GUIObject object, int x, int y, int w, int h, @NonNull String text,
                          boolean clickable) {
        int centerX() {
            return x + w / 2;
        }

        int centerY() {
            return y + h / 2;
        }
    }

    private static boolean isClickable(@NonNull GUIObject object) {
        return object instanceof ButtonObject || object instanceof PanelTab || object instanceof PulldownButton
                || object instanceof EditLine;
    }

    private static void collect(@NonNull List<Placed> out, @NonNull GUIObject object, int parent_x, int parent_y,
            float scale, int framebuffer_height, boolean inside_clickable) {
        int abs_x = parent_x + object.getX();
        int abs_y = parent_y + object.getY();
        int px = Math.round(abs_x * scale);
        int py = framebuffer_height - Math.round((abs_y + object.getHeight()) * scale);
        boolean clickable = inside_clickable || isClickable(object);
        out.add(new Placed(object, px, py, Math.round(object.getWidth() * scale),
                Math.round(object.getHeight() * scale), textOf(object), clickable));
        GUIObject child = object.getFirstChild();
        while (child != null) {
            collect(out, child, abs_x, abs_y, scale, framebuffer_height, clickable);
            child = child.getNext();
        }
    }

    private static void collect(@NonNull List<Placed> out, @NonNull GUIObject object, int parent_x, int parent_y,
            float scale, int framebuffer_height) {
        collect(out, object, parent_x, parent_y, scale, framebuffer_height, false);
    }

    // A widget that is clickable, or sits inside one, beats a plain label with the same text; among equals the
    // last match wins so a form on top of another form takes priority over the one underneath.
    private static @Nullable Placed last(@NonNull GUIRoot root, int framebuffer_height, @NonNull String text) {
        List<Placed> placed = new ArrayList<>();
        collect(placed, root, 0, 0, root.getGlobalScale(), framebuffer_height);
        Placed match = null;
        for (Placed p : placed) {
            if (p.text().equalsIgnoreCase(text.strip()) && (match == null || p.clickable() || !match.clickable())) {
                match = p;
            }
        }
        return match;
    }

    /** Framebuffer centre of the last widget whose text matches, or null. */
    public static int @Nullable [] find(@NonNull GUIRoot root, int framebuffer_height, @NonNull String text) {
        Placed p = last(root, framebuffer_height, text);
        return p == null ? null : new int[]{p.centerX(), p.centerY()};
    }

    // The nearest widget of the given type on the same row as, and to the right of, the matching label; the label
    // itself if it already is one.
    private static @Nullable Placed onRow(@NonNull GUIRoot root, int framebuffer_height, @NonNull String label,
            @NonNull Class<?> type) {
        Placed anchor = last(root, framebuffer_height, label);
        if (anchor == null) {
            return null;
        }
        if (type.isInstance(anchor.object())) {
            return anchor;
        }
        List<Placed> placed = new ArrayList<>();
        collect(placed, root, 0, 0, root.getGlobalScale(), framebuffer_height);
        Placed best = null;
        for (Placed p : placed) {
            if (type.isInstance(p.object()) && p.x() >= anchor.x()
                    && Math.abs(p.centerY() - anchor.centerY()) <= anchor.h()
                    && (best == null || p.x() < best.x())) {
                best = p;
            }
        }
        return best;
    }

    /** The edit field on the row of the label whose text matches, or the field itself. */
    public static @Nullable TextField findField(@NonNull GUIRoot root, int framebuffer_height,
            @NonNull String label) {
        Placed p = onRow(root, framebuffer_height, label, EditLine.class);
        return p == null ? null : (TextField) p.object();
    }

    /** Framebuffer centre of the pulldown button on the row of the label whose text matches, or null. */
    public static int @Nullable [] findPulldown(@NonNull GUIRoot root, int framebuffer_height,
            @NonNull String label) {
        Placed p = onRow(root, framebuffer_height, label, PulldownButton.class);
        return p == null ? null : new int[]{p.centerX(), p.centerY()};
    }

    /** True when a widget whose text matches is on screen. */
    public static boolean has(@NonNull GUIRoot root, int framebuffer_height, @NonNull String text) {
        return last(root, framebuffer_height, text) != null;
    }
}
