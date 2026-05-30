package com.oddlabs.tt.form;

import com.oddlabs.matchmaking.GameMode;
import com.oddlabs.matchmaking.Preset;
import com.oddlabs.tt.font.Font;
import com.oddlabs.tt.gamemode.PresetLibrary;
import com.oddlabs.tt.gui.GUIObject;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.ModeCard;
import com.oddlabs.tt.gui.ModeIconQuads;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.gui.Panel;
import com.oddlabs.tt.gui.PresetCard;
import com.oddlabs.tt.gui.RadioButtonGroup;
import com.oddlabs.tt.gui.ScrollableGroup;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.guievent.MouseClickListener;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.ResourceBundle;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;

/**
 * Tab 1 of the MP create-game dialog. Renders the row of mode cards (Standard only at v1), the grid of user-saved
 * preset cards, and the {@code +Save current as preset} action card. The grid is empty until the host saves their
 * first preset.
 */
public final class ModeAndPresetsPanel extends Panel {
    private static final int CARD_WIDTH = 220;
    private static final int GRID_VISIBLE_HEIGHT = 240;
    private static final ResourceBundle bundle = ResourceBundle.getBundle(ModeAndPresetsPanel.class.getName());

    public ModeAndPresetsPanel(@NonNull PresetLibrary library, @NonNull ModeAndPresetsHandler handler) {
        super(i18n("caption"));

        Group mode_row = new Group();
        RadioButtonGroup mode_group = new RadioButtonGroup();
        ModeCard standard_card = new ModeCard(GameMode.STANDARD, i18n("standard_title"), i18n("standard_tagline"),
                CARD_WIDTH, true, mode_group, handler::modeChosen);
        mode_row.addChild(standard_card);
        standard_card.place();
        mode_row.compileCanvas();

        ScrollableGroup preset_grid = new ScrollableGroup(GRID_VISIBLE_HEIGHT, 8);
        RadioButtonGroup preset_group = new RadioButtonGroup();
        List<Preset> presets = library.forMode(GameMode.STANDARD);
        GUIObject last_card = null;
        for (Preset preset : presets) {
            PresetCard card = new PresetCard(preset, CARD_WIDTH, false, preset_group, handler::presetChosen,
                    handler::presetDeleted);
            preset_grid.addChild(card);
            if (last_card == null) {
                card.place();
            } else {
                card.place(last_card, BOTTOM_LEFT);
            }
            last_card = card;
        }
        SavePresetCard save_card = new SavePresetCard(CARD_WIDTH, (_, _, _, _) -> handler.saveClicked());
        preset_grid.addChild(save_card);
        if (last_card == null) {
            save_card.place();
        } else {
            save_card.place(last_card, BOTTOM_LEFT);
        }
        preset_grid.compileCanvas();

        addChild(mode_row);
        addChild(preset_grid);
        mode_row.place();
        preset_grid.place(mode_row, BOTTOM_LEFT, Skin.getSkin().getFormData().sectionSpacing());
        compileCanvas();
    }

    private static @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    /**
     * Card-styled clickable that opens the {@link SavePresetDialog}. Visually mirrors {@link ModeCard} /
     * {@link PresetCard} so the grid keeps a consistent look. Encapsulated here because nothing else builds one.
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
            ModeIconQuads.Mode skin_mode = isDisabled() ? ModeIconQuads.Mode.DISABLED
                    : (isActive() || isHovered()) ? ModeIconQuads.Mode.ACTIVE : ModeIconQuads.Mode.NORMAL;
            Skin.getSkin().getGroupData().group().render(renderer, 0f, 0f, getWidth(), getHeight(), skin_mode);
        }
    }
}
