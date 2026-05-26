package com.oddlabs.tt.gui;

import com.oddlabs.matchmaking.GameMode;
import com.oddlabs.tt.font.Font;
import com.oddlabs.tt.guievent.ModeChosenListener;
import com.oddlabs.tt.render.GUIRenderer;
import org.jspecify.annotations.NonNull;

public final class ModeCard extends RadioButtonGroupElement {
    private static final int PADDING_X = 12;
    private static final int PADDING_Y = 10;
    private static final int LABEL_SPACING = 4;

    private final @NonNull GameMode mode;
    private final @NonNull ModeChosenListener listener;
    private boolean pressed;

    public ModeCard(@NonNull GameMode mode, @NonNull String title, @NonNull String tagline, int width, boolean marked,
            @NonNull RadioButtonGroup group, @NonNull ModeChosenListener listener) {
        super(marked, group);
        this.mode = mode;
        this.listener = listener;

        Font title_font = Skin.getSkin().getButtonFont();
        Font tagline_font = Skin.getSkin().getEditFont();
        int content_width = width - 2 * PADDING_X;

        Label title_label = new Label(title, title_font, content_width);
        Label tagline_label = new Label(tagline, tagline_font, content_width);

        int height = PADDING_Y + title_label.getHeight() + LABEL_SPACING + tagline_label.getHeight() + PADDING_Y;
        setDim(width, height);

        title_label.setPos(PADDING_X, height - PADDING_Y - title_label.getHeight());
        tagline_label.setPos(PADDING_X, PADDING_Y);
        addChild(title_label);
        addChild(tagline_label);

        setCanFocus(true);
    }

    public @NonNull GameMode getMode() {
        return mode;
    }

    @Override
    protected void mousePressed(@NonNull MouseButton button, int x, int y) {
        pressed = true;
    }

    @Override
    protected void mouseReleased(@NonNull MouseButton button, int x, int y) {
        pressed = false;
    }

    @Override
    protected void mouseClicked(@NonNull MouseButton button, int x, int y, int clicks) {
        super.mouseClicked(button, x, y, clicks);
        listener.modeChosen(mode);
    }

    @Override
    protected void renderGeometry(@NonNull GUIRenderer renderer) {
        ModeIconQuads.Mode skinMode = resolveMode();
        Skin.getSkin().getGroupData().group().render(renderer, 0f, 0f, getWidth(), getHeight(), skinMode);
    }

    private ModeIconQuads.@NonNull Mode resolveMode() {
        if (isDisabled()) {
            return ModeIconQuads.Mode.DISABLED;
        }
        if (isMarked() || isActive() || (isHovered() && pressed)) {
            return ModeIconQuads.Mode.ACTIVE;
        }
        return ModeIconQuads.Mode.NORMAL;
    }
}
