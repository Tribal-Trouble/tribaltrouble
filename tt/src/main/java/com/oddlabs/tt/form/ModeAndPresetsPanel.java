package com.oddlabs.tt.form;

import com.oddlabs.matchmaking.GameMode;
import com.oddlabs.matchmaking.Preset;
import com.oddlabs.tt.font.Font;
import com.oddlabs.tt.gamemode.PresetLibrary;
import com.oddlabs.tt.gui.GUIObject;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.Panel;
import com.oddlabs.tt.gui.PresetCard;
import com.oddlabs.tt.gui.PulldownButton;
import com.oddlabs.tt.gui.PulldownItem;
import com.oddlabs.tt.gui.PulldownMenu;
import com.oddlabs.tt.gui.RadioButtonGroup;
import com.oddlabs.tt.gui.ScrollableGroup;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.guievent.MouseClickListener;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.RIGHT_MID;

/**
 * Tab 1 of the MP create-game dialog. Renders the mode pulldown (Standard only at v1), a banner showing which preset
 * (if any) is currently applied and whether the form has drifted from it, the grid of user-saved preset cards, and the
 * {@code +Save current as preset} action card. {@link #refreshPresets} rebuilds the grid after the host saves or
 * deletes a preset; {@link #setPresetState} updates the banner.
 */
public final class ModeAndPresetsPanel extends Panel {
    private static final int CARD_WIDTH = 220;
    private static final int GRID_VISIBLE_HEIGHT = 150;
    private static final int BANNER_BUTTON_WIDTH = 90;
    private static final int MODE_PULLDOWN_WIDTH = 180;
    private static final ResourceBundle bundle = ResourceBundle.getBundle(ModeAndPresetsPanel.class.getName());

    private final @NonNull PresetLibrary library;
    private final @NonNull ModeAndPresetsHandler handler;
    private final @NonNull Group mode_row;
    private final @NonNull PresetBanner banner;
    private @Nullable ScrollableGroup preset_grid;
    private @Nullable String selected_preset_id;

    public ModeAndPresetsPanel(@NonNull GUIRoot gui_root, @NonNull PresetLibrary library,
            @NonNull ModeAndPresetsHandler handler) {
        super(i18n("caption"));
        this.library = library;
        this.handler = handler;

        mode_row = new Group();
        Label mode_label = new Label(i18n("mode_label"), Skin.getSkin().getEditFont());
        PulldownMenu<GameMode> mode_menu = new PulldownMenu<>();
        mode_menu.addItem(new PulldownItem<>(i18n("standard_title"), GameMode.STANDARD));
        PulldownButton<GameMode> mode_button = new PulldownButton<>(gui_root, mode_menu, 0, MODE_PULLDOWN_WIDTH);
        mode_menu.addItemChosenListener((menu, index) -> {
            GameMode mode = menu.getItem(index).getAttachment();
            if (mode != null) {
                handler.modeChosen(mode);
            }
        });
        mode_row.addChild(mode_label);
        mode_row.addChild(mode_button);
        mode_label.place();
        mode_button.place(mode_label, RIGHT_MID);
        mode_row.compileCanvas();
        addChild(mode_row);
        mode_row.place();

        banner = new PresetBanner((_, _, _, _) -> handler.resetClicked(),
                (_, _, _, _) -> handler.updateClicked());
        addChild(banner);
        banner.place(mode_row, BOTTOM_LEFT, Skin.getSkin().getFormData().sectionSpacing());

        rebuildGrid();
        compileCanvas();
    }

    /**
     * Rebuilds the preset grid from the current library snapshot. The new grid keeps the same fixed dimensions as the
     * old one so the panel itself does not need to relayout. {@link com.oddlabs.tt.gui.Group#compileCanvas} is
     * deliberately not called here because it is not idempotent (each invocation re-anchors the bounding box from
     * {@code (0,0)} and the panel would grow by {@code top_offset} on every refresh).
     */
    public void refreshPresets() {
        rebuildGrid();
    }

    private void rebuildGrid() {
        int previous_offset = 0;
        if (preset_grid != null) {
            previous_offset = preset_grid.getOffsetY();
            removeChild(preset_grid);
        }
        ScrollableGroup new_grid = new ScrollableGroup(GRID_VISIBLE_HEIGHT, 8);
        RadioButtonGroup preset_group = new RadioButtonGroup();

        List<GUIObject> cards = new ArrayList<>();
        for (Preset preset : library.forMode(GameMode.STANDARD)) {
            boolean marked = preset.getId().equals(selected_preset_id);
            cards.add(new PresetCard(preset, CARD_WIDTH, marked, preset_group, handler::presetChosen,
                    handler::presetDeleted));
        }
        cards.add(new SavePresetCard(CARD_WIDTH, (_, _, _, _) -> handler.saveClicked()));

        // Two-column flow: even index = left column, odd index = right column. row_anchor is the left card of the
        // current row; previous_row_anchor lets the next row drop below it.
        GUIObject previous_row_anchor = null;
        GUIObject row_anchor = null;
        for (int i = 0; i < cards.size(); i++) {
            GUIObject card = cards.get(i);
            new_grid.addChild(card);
            if (i % 2 == 0) {
                if (previous_row_anchor == null) {
                    card.place();
                } else {
                    card.place(previous_row_anchor, BOTTOM_LEFT);
                }
                previous_row_anchor = card;
                row_anchor = card;
            } else {
                card.place(row_anchor, RIGHT_MID);
            }
        }
        new_grid.compileCanvas();

        preset_grid = new_grid;
        addChild(new_grid);
        new_grid.place(banner, BOTTOM_LEFT, Skin.getSkin().getFormData().sectionSpacing());
        // Preserve the prior scroll position so saving/deleting a preset does not jump the grid back to the top.
        // setOffsetY clamps to the new content height, so a shorter grid simply lands at its max.
        new_grid.setOffsetY(previous_offset);
    }

    /**
     * Reflects the host's current preset and modified state in the banner. Called by {@link TerrainMenu} whenever a
     * preset is applied, the form is reset to the preset, or any tracked field changes.
     */
    public void setPresetState(@Nullable Preset preset, boolean modified) {
        selected_preset_id = preset != null ? preset.getId() : null;
        banner.setState(preset, modified);
    }

    private static @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    /**
     * Card-styled clickable that opens the {@link SavePresetDialog}. Visually mirrors {@link PresetCard} so the grid
     * keeps a consistent look. Encapsulated here because nothing else builds one.
     */
    private static final class SavePresetCard extends Group {
        private SavePresetCard(int width, @NonNull MouseClickListener listener) {
            Font title_font = Skin.getSkin().getButtonFont();
            int padding_x = 12;
            int padding_y = 10;
            Label label = new Label(i18n("save_card"), title_font, width - 2 * padding_x);
            int height = padding_y + label.getHeight() + padding_y;
            setDim(width, height);
            label.setPos(padding_x, padding_y);
            addChild(label);
            setCanFocus(true);
            addMouseClickListener(listener);
        }

        @Override
        protected void renderGeometry(@NonNull GUIRenderer renderer) {
            PresetCard.renderCardBackground(renderer, this, isActive() || isHovered());
        }
    }

    /**
     * Status row showing which preset (if any) is currently applied, Reset/Update buttons, and a {@code (modified)}
     * indicator that lights up when the host has edited any tracked field since applying the preset. Update is only
     * enabled when a preset is selected AND the form has drifted from it; Reset is only enabled when a preset is
     * selected.
     */
    private static final class PresetBanner extends Group {
        private final @NonNull Label preset_label;
        private final @NonNull HorizButton reset_button;
        private final @NonNull HorizButton update_button;
        private final @NonNull Label modified_label;

        private PresetBanner(@NonNull MouseClickListener reset_listener, @NonNull MouseClickListener update_listener) {
            Font font = Skin.getSkin().getEditFont();
            int label_width = 180;
            preset_label = new Label(i18n("banner_preset", i18n("banner_no_preset")), font, label_width);
            reset_button = new HorizButton(i18n("reset_button"), BANNER_BUTTON_WIDTH);
            reset_button.addMouseClickListener(reset_listener);
            reset_button.setDisabled(true);
            update_button = new HorizButton(i18n("update_button"), BANNER_BUTTON_WIDTH);
            update_button.addMouseClickListener(update_listener);
            update_button.setDisabled(true);
            modified_label = new Label("", font, 80);

            addChild(preset_label);
            addChild(reset_button);
            addChild(update_button);
            addChild(modified_label);
            preset_label.place();
            reset_button.place(preset_label, RIGHT_MID);
            update_button.place(reset_button, RIGHT_MID);
            modified_label.place(update_button, RIGHT_MID);
            compileCanvas();
        }

        private void setState(@Nullable Preset preset, boolean modified) {
            preset_label.setText(i18n("banner_preset", preset != null ? preset.getName() : i18n("banner_no_preset")));
            boolean preset_dirty = preset != null && modified;
            reset_button.setDisabled(!preset_dirty);
            update_button.setDisabled(!preset_dirty);
            modified_label.setText(modified ? i18n("banner_modified") : "");
        }
    }
}
