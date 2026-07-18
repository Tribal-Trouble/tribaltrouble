package com.oddlabs.tt.gui;

import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.render.GUIRenderer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/** a click-able button represented by an icon */
public class IconButton extends ButtonObject {
    private final @NonNull ModeIconQuads icon;
    private @Nullable IconDisabler icon_disabler;

    public IconButton(@NonNull ModeIconQuads icon, @Nullable Supplier<@NonNull String> tool_tip) {
        this(icon, null, tool_tip);
    }

    public IconButton(@NonNull ModeIconQuads icon, @Nullable GameAction action,
            @Nullable Supplier<@NonNull String> tool_tip) {
        super(Skin.getSkin().getEditFont(), action, tool_tip);
        this.icon = icon;
        var normal = icon.quad(ModeIconQuads.Mode.NORMAL);
        setDim(normal.getWidth(), normal.getHeight());
    }

    public final void setIconDisabler(@Nullable IconDisabler icon_disabler) {
        this.icon_disabler = icon_disabler;
    }

    public final void doUpdate() {
        setDisabled(icon_disabler != null && icon_disabler.isDisabled());
    }

    protected @NonNull ModeIconQuads getIcon() {
        return icon;
    }

    @Override
    protected void renderGeometry(@NonNull GUIRenderer renderer) {
        ModeIconQuads.Mode skinMode = isDisabled() ? ModeIconQuads.Mode.DISABLED : isHovered()
                || isActive() ? ModeIconQuads.Mode.ACTIVE : ModeIconQuads.Mode.NORMAL;

        renderer.drawModeIcon(icon, skinMode, 0, 0);
    }
}
