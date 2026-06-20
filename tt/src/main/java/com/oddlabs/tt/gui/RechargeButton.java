package com.oddlabs.tt.gui;

import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.model.MagicType;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.player.PlayerInterface;
import com.oddlabs.tt.render.GUIRenderer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * A button representing a magical ability that charges over time and allows triggering magic.
 */
public class RechargeButton extends NonFocusIconButton {
    private final @NonNull PlayerInterface player_interface;
    private final @NonNull MagicType magic_type;
    private Unit unit;

    public RechargeButton(@NonNull PlayerInterface player_interface, @NonNull ModeIconQuads icon,
            @Nullable GameAction action, @NonNull Supplier<@NonNull String> tool_tip, @NonNull MagicType magic_type) {
        super(icon, action, tool_tip);
        this.player_interface = player_interface;
        this.magic_type = magic_type;
        setCanFocus(true);
        var normal = icon.quad(ModeIconQuads.Mode.NORMAL);
        setDim(normal.getWidth(), normal.getHeight());
    }

    public final void setUnit(Unit unit) {
        this.unit = unit;
    }

    @Override
    public final void mouseClicked(@NonNull MouseButton button, int x, int y, int clicks) {
        if (unit.canDoMagic(magic_type))
            player_interface.doMagic(unit, magic_type);
    }

    @Override
    protected final void postRender(@NonNull GUIRenderer renderer) {
        float progress = unit.getMagicProgress(magic_type);
        if (!unit.isDead() && progress < 1f) {
            IconQuad watchQuad = GUIIcons.getIcons().getWatch(progress);
            renderer.drawIcon(watchQuad, getWidth() - watchQuad.getWidth(), getHeight() - watchQuad.getHeight());
        }
    }
}
