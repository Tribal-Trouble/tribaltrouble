package com.oddlabs.tt.form;

import com.oddlabs.tt.font.Font;
import com.oddlabs.tt.gui.ColumnInfo;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.MultiColumnComboBox;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.gui.Panel;
import com.oddlabs.tt.gui.Row;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.gui.SortedLabel;
import com.oddlabs.tt.guievent.RowListener;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputBinding;
import com.oddlabs.tt.input.KeyBindingConflicts;
import com.oddlabs.tt.render.GUIRenderer;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.RIGHT_MID;

/**
 * UI Panel for remapping game actions to keyboard and controller inputs.
 */
public class KeyBindingPanel extends Panel {
    private static final int COL_ACTION_WIDTH = 200;
    private static final int COL_BINDINGS_WIDTH = 300;
    private static final Color CONFLICT_COLOR = new Color.Standard(1.0f, 0.3f, 0.3f, 1.0f);
    private static final Color HEADER_COLOR = new Color.Standard(0.9f, 0.75f, 0.4f, 1.0f);
    private static final int CATEGORY_STRIDE = 10_000;

    private enum Category {
        GENERAL("category.general"),
        CAMERA("category.camera"),
        UI("category.ui"),
        UNIT("category.unit"),
        ARMY_GROUPS("category.army_groups"),
        PRODUCTION("category.production"),
        TRAINING("category.training"),
        RESOURCES("category.resources"),
        MAGIC("category.magic"),
        GAME("category.game"),
        CHEATS("category.cheats"),
        DEBUG("category.debug");

        final @NonNull String i18nKey;

        Category(@NonNull String i18nKey) {
            this.i18nKey = i18nKey;
        }
    }

    private static @NonNull Category categorize(@NonNull GameAction action) {
        String name = action.name();
        if (name.startsWith("CAMERA_")) return Category.CAMERA;
        if (name.startsWith("UI_")) return Category.UI;
        if (name.startsWith("ARMY_")) return Category.ARMY_GROUPS;
        if (name.startsWith("PROD_")) return Category.PRODUCTION;
        if (name.startsWith("TRAIN_")) return Category.TRAINING;
        if (name.startsWith("RES_")) return Category.RESOURCES;
        if (name.startsWith("MAGIC_")) return Category.MAGIC;
        if (name.startsWith("CHEAT_")) return Category.CHEATS;
        if (name.startsWith("DEBUG_")) return Category.DEBUG;
        if (name.startsWith("UNIT_") || name.equals("GAMEPLAY_BACK")) return Category.UNIT;
        if (name.startsWith("GAME_SPEED_") || name.equals("NOTIFICATION_JUMP")) return Category.GAME;
        return Category.GENERAL;
    }

    private final @NonNull MultiColumnComboBox<GameAction> list_box;
    private final @NonNull GUIRoot gui_root;
    private @Nullable GameAction last_selected_action;

    public KeyBindingPanel(@NonNull GUIRoot gui_root) {
        super(AbstractOptionsMenu.i18n("key_bindings_title"));
        this.gui_root = gui_root;

        ColumnInfo[] infos = new ColumnInfo[]{
                new ColumnInfo(AbstractOptionsMenu.i18n("column_action"), COL_ACTION_WIDTH),
                new ColumnInfo(AbstractOptionsMenu.i18n("column_bindings"), COL_BINDINGS_WIDTH)
        };

        list_box = new MultiColumnComboBox<>(gui_root, infos, 300, false);
        addChild(list_box);

        updateList();

        list_box.addRowListener(new RowListener<>() {
            @Override
            public void rowDoubleClicked(@NonNull GameAction action) {
                last_selected_action = action;
                gui_root.addModalForm(new KeyBindingDialog(gui_root, action, bindings -> {
                    Renderer.getLocalInput().getInputManager().setBindings(action, bindings);
                    updateList();
                }));
            }
        });

        // Buttons
        Group button_group = new Group();
        addChild(button_group);

        HorizButton btn_reset = new HorizButton(AbstractOptionsMenu.i18n("btn_reset_all"), 100);
        btn_reset.addMouseClickListener((_, _, _, _) -> gui_root.addModalForm(new QuestionForm(AbstractOptionsMenu.i18n(
                "confirm_reset_all"), (_, _, _, _) -> {
                    Renderer.getLocalInput().getInputManager().resetToDefaults();
                    updateList();
                })));
        button_group.addChild(btn_reset);

        HorizButton btn_save = new HorizButton(AbstractOptionsMenu.i18n("btn_save_bindings"), 100);
        btn_save.addMouseClickListener((_, _, _, _) -> saveMappings());
        button_group.addChild(btn_save);

        HorizButton btn_load = new HorizButton(AbstractOptionsMenu.i18n("btn_load_bindings"), 100);
        btn_load.addMouseClickListener((_, _, _, _) -> loadMappings());
        button_group.addChild(btn_load);

        btn_reset.place();
        btn_save.place(btn_reset, RIGHT_MID);
        btn_load.place(btn_save, RIGHT_MID);
        button_group.compileCanvas();

        list_box.place();
        button_group.place(list_box, BOTTOM_LEFT);

        compileCanvas();
    }

    private static @NonNull String labelFor(@NonNull GameAction action) {
        try {
            return AbstractOptionsMenu.i18n("action." + action.name());
        } catch (Exception e) {
            return action.name();
        }
    }

    private void updateList() {
        int savedOffset = list_box.getOffsetY();
        list_box.clear();
        Row<GameAction, Label> rowToReselect = null;

        EnumMap<Category, List<GameAction>> byCategory = new EnumMap<>(Category.class);
        for (GameAction action : GameAction.values()) {
            if (action.name().startsWith("DEBUG_") && !Renderer.getRenderer().getSettings().inDeveloperMode()) {
                continue;
            }
            if (action.name().startsWith("CHEAT_") && !Renderer.getRenderer().isCheater()) {
                continue;
            }
            byCategory.computeIfAbsent(categorize(action), k -> new ArrayList<>()).add(action);
        }

        for (Category category : Category.values()) {
            List<GameAction> actions = byCategory.get(category);
            if (actions == null || actions.isEmpty()) continue;

            actions.sort((a, b) -> labelFor(a).compareToIgnoreCase(labelFor(b)));

            int categoryBase = category.ordinal() * CATEGORY_STRIDE;
            String headerText = "-- " + AbstractOptionsMenu.i18n(category.i18nKey) + " --";
            Label headerLeft = new SortedLabel(headerText, categoryBase, Skin.getSkin().getMultiColumnComboBoxData()
                    .font());
            headerLeft.setColor(HEADER_COLOR);
            Label headerRight = new Label("", Skin.getSkin().getMultiColumnComboBoxData().font());
            list_box.addRow(new Row<>(new Label[]{headerLeft, headerRight}, null));

            int withinCategory = 1;
            for (GameAction action : actions) {
                String name = labelFor(action);

                var bindings = Renderer.getLocalInput().getInputManager().getBindings(action);
                Label bindingLabel;

                if (bindings.isEmpty()) {
                    bindingLabel = new InvertedLabel(AbstractOptionsMenu.i18n("unassigned"), Skin.getSkin()
                            .getMultiColumnComboBoxData().font(), COL_BINDINGS_WIDTH);
                } else {
                    var desc = bindings.stream()
                            .map(InputBinding::toString)
                            .collect(Collectors.joining(", "));
                    bindingLabel = new Label(desc, Skin.getSkin().getMultiColumnComboBoxData().font());
                }

                Label actionLabel = new SortedLabel(name, categoryBase + withinCategory, Skin.getSkin()
                        .getMultiColumnComboBoxData().font());
                if (!KeyBindingConflicts.findExistingConflicts(action, Renderer.getLocalInput().getInputManager())
                        .isEmpty()) {
                    actionLabel.setColor(CONFLICT_COLOR);
                    bindingLabel.setColor(CONFLICT_COLOR);
                }
                Row<GameAction, Label> row = new Row<>(new Label[]{actionLabel, bindingLabel}, action);
                list_box.addRow(row);
                if (action == last_selected_action) {
                    rowToReselect = row;
                }
                withinCategory++;
            }
        }

        list_box.setOffsetY(savedOffset);
        if (rowToReselect != null) {
            list_box.selectRow(rowToReselect);
        }
    }

    private void saveMappings() {
        boolean wasFullscreen = Renderer.getRenderer().getSettings().fullscreen;
        if (wasFullscreen) {
            Renderer.getRenderer().toggleFullscreen();
        }

        String path = TinyFileDialogs.tinyfd_saveFileDialog(AbstractOptionsMenu.i18n("dialog_save_bindings"), "", null,
                AbstractOptionsMenu.i18n("json_files"));
        if (path != null) {
            String json = Renderer.getLocalInput().getInputManager().exportBindings();
            try {
                Files.writeString(Path.of(path), json);
            } catch (IOException e) {
                gui_root.addModalForm(new MessageForm(AbstractOptionsMenu.i18n("error_save_failed", e.getMessage())));
            }
        }

        if (wasFullscreen) {
            Renderer.getRenderer().toggleFullscreen();
        }
    }

    private void loadMappings() {
        boolean wasFullscreen = Renderer.getRenderer().getSettings().fullscreen;
        if (wasFullscreen) {
            Renderer.getRenderer().toggleFullscreen();
        }

        String path = TinyFileDialogs.tinyfd_openFileDialog(AbstractOptionsMenu.i18n("dialog_load_bindings"), "", null,
                AbstractOptionsMenu.i18n("json_files"), false);
        if (path != null) {
            try {
                String json = Files.readString(Path.of(path));
                Renderer.getLocalInput().getInputManager().importBindings(json);
                updateList();
            } catch (IOException e) {
                gui_root.addModalForm(new MessageForm(AbstractOptionsMenu.i18n("error_load_failed", e.getMessage())));
            }
        }

        if (wasFullscreen) {
            Renderer.getRenderer().toggleFullscreen();
        }
    }

    private static final class InvertedLabel extends Label {
        InvertedLabel(@NonNull String text, @NonNull Font font, int width) {
            super(text, font, width, Origin.AT_MIDDLE);
            setColor(Color.Standard.BLACK);
        }

        @Override
        protected void renderGeometry(@NonNull GUIRenderer renderer) {
            renderer.drawColoredQuad(0, 0, getWidth(), getHeight(), Label.DEFAULT_COLOR);
            super.renderGeometry(renderer);
        }
    }
}
