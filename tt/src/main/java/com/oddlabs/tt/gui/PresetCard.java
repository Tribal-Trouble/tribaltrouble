package com.oddlabs.tt.gui;

import com.oddlabs.matchmaking.Preset;
import com.oddlabs.tt.font.Font;
import com.oddlabs.tt.guievent.PresetChosenListener;
import com.oddlabs.tt.guievent.PresetDeleteListener;
import com.oddlabs.tt.render.GUIRenderer;
import org.jspecify.annotations.NonNull;

public final class PresetCard extends RadioButtonGroupElement {
    private static final int PADDING_X = 12;
    private static final int PADDING_Y = 10;
    private static final int LABEL_SPACING = 4;
    private static final int DELETE_INSET = 4;

    private final @NonNull Preset preset;
    private final @NonNull PresetChosenListener choose_listener;
    private final @NonNull DeleteButton delete_button;
    private boolean pressed;

    public PresetCard(@NonNull Preset preset, @NonNull String tagline, int width, boolean marked,
            @NonNull RadioButtonGroup group, @NonNull PresetChosenListener choose_listener,
            @NonNull PresetDeleteListener delete_listener) {
        super(marked, group);
        this.preset = preset;
        this.choose_listener = choose_listener;

        Font title_font = Skin.getSkin().getButtonFont();
        Font tagline_font = Skin.getSkin().getEditFont();
        int content_width = width - 2 * PADDING_X;

        Label title_label = new Label(preset.getName(), title_font, content_width);
        Label tagline_label = new Label(tagline, tagline_font, content_width);

        int height = PADDING_Y + title_label.getHeight() + LABEL_SPACING + tagline_label.getHeight() + PADDING_Y;
        setDim(width, height);

        title_label.setPos(PADDING_X, height - PADDING_Y - title_label.getHeight());
        tagline_label.setPos(PADDING_X, PADDING_Y);
        addChild(title_label);
        addChild(tagline_label);

        delete_button = new DeleteButton(this);
        delete_button.setPos(width - delete_button.getWidth() - DELETE_INSET,
                height - delete_button.getHeight() - DELETE_INSET);
        addChild(delete_button);
        delete_button.addMouseClickListener((_, _, _, _) -> delete_listener.presetDeleted(preset));

        setCanFocus(true);
    }

    public @NonNull Preset getPreset() {
        return preset;
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
        choose_listener.presetChosen(preset);
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

    private static final class DeleteButton extends IconButton {
        private final @NonNull PresetCard card;

        DeleteButton(@NonNull PresetCard card) {
            super(Skin.getSkin().getFormData().formClose());
            this.card = card;
        }

        @Override
        protected void mouseClicked(@NonNull MouseButton button, int x, int y, int clicks) {
            // Suppress propagation to the card so clicking × does not also select the preset.
        }

        @Override
        protected void renderGeometry(@NonNull GUIRenderer renderer) {
            if (!card.isHovered() && !isHovered() && !isActive()) {
                return;
            }
            super.renderGeometry(renderer);
        }
    }
}
