package com.oddlabs.tt.delegate;

import com.oddlabs.tt.camera.Camera;
import com.oddlabs.tt.camera.StaticCamera;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.Race;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.render.LandscapeLocation;
import com.oddlabs.tt.viewer.Cheat;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Set;

public abstract class InGameDelegate extends CameraDelegate<Camera> {
    private final @NonNull WorldViewer viewer;

    protected InGameDelegate(@NonNull WorldViewer viewer, @Nullable Camera camera) {
        super(viewer.getGUIRoot(), camera);
        this.viewer = viewer;
    }

    private boolean cheat(@NonNull Set<GameAction> actions) {
        // cheats
        Cheat cheat = viewer.getCheat();
        if (!cheat.isEnabled())
            return false;
        LandscapeLocation landscape_hit = new LandscapeLocation();
        viewer.getPicker().pickLocation(getCamera().getState(), landscape_hit);
        float landscape_x = landscape_hit.x;
        float landscape_y = landscape_hit.y;

        if (actions.contains(GameAction.CHEAT_1)) {
            // F1 creates a peon at the center of the view unless the player already has maximum units.
            if (viewer.getLocalPlayer().getUnitCountContainer().getNumSupplies() != viewer.getParameters()
                    .getMaxUnitCount()) {
                new Unit(viewer.getLocalPlayer(), landscape_x, landscape_y, null,
                        viewer.getLocalPlayer().getRace().getUnitTemplate(Race.UNIT_PEON));
                return true;
            }
        }
        if (actions.contains(GameAction.CHEAT_2)) {
            // F2 creates a rock warrior at the center of the view unless the player already has maximum units.
            if (viewer.getLocalPlayer().getUnitCountContainer().getNumSupplies() != viewer.getParameters()
                    .getMaxUnitCount()) {
                new Unit(viewer.getLocalPlayer(), landscape_x, landscape_y, null,
                        viewer.getLocalPlayer().getRace().getUnitTemplate(Race.UNIT_WARRIOR_ROCK));
                return true;
            }
        }
        if (actions.contains(GameAction.CHEAT_3)) {
            // F3 creates an iron warrior at the center of the view unless the player already has maximum units.
            if (viewer.getLocalPlayer().getUnitCountContainer().getNumSupplies() != viewer.getParameters()
                    .getMaxUnitCount()) {
                new Unit(viewer.getLocalPlayer(), landscape_x, landscape_y, null,
                        viewer.getLocalPlayer().getRace().getUnitTemplate(Race.UNIT_WARRIOR_IRON));
                return true;
            }
        }
        if (actions.contains(GameAction.CHEAT_4)) {
            // F4 creates a chicken warrior at the center of the view unless the player already has maximum units.
            if (viewer.getLocalPlayer().getUnitCountContainer().getNumSupplies() != viewer.getParameters()
                    .getMaxUnitCount()) {
                new Unit(viewer.getLocalPlayer(), landscape_x, landscape_y, null,
                        viewer.getLocalPlayer().getRace().getUnitTemplate(Race.UNIT_WARRIOR_RUBBER));
                return true;
            }
        }
        if (actions.contains(GameAction.CHEAT_5)) {
            // F5 creates a chieftain at the center of the view unless the player already has one or is training one
            if (!viewer.getLocalPlayer().hasActiveChieftain() && !viewer.getLocalPlayer().isTrainingChieftain()) {
                Unit chieftain = new Unit(viewer.getLocalPlayer(), landscape_x, landscape_y, null,
                        viewer.getLocalPlayer().getRace().getUnitTemplate(Race.UNIT_CHIEFTAIN));
                viewer.getLocalPlayer().setActiveChieftain(chieftain);
                return true;
            }
        }
        if (actions.contains(GameAction.CHEAT_6)) {
            // F6 does massive damve on whatever is selected.
            viewer.getLocalPlayer().killSelection(viewer.getSelection().getCurrentSelection().filter(Abilities.NONE));
            return true;
        }
        if (actions.contains(GameAction.CHEAT_7)) {
            // F7 hides and shows trees
            cheat.draw_trees = !cheat.draw_trees;
            return true;
        }
        if (actions.contains(GameAction.CHEAT_8)) {
            // F7 hides and shows terrain grid.
            cheat.line_mode = !cheat.line_mode;
            return true;
        }
        if (actions.contains(GameAction.CHEAT_9)) {
            // F9 toggles fog
            com.oddlabs.tt.resource.FogInfo fog_info = viewer.getGUIRoot().getDelegate().getCamera().getState()
                    .getFog();
            fog_info.setEnabled(!fog_info.isEnabled());
            return true;
        }

        // If in developer mode
        if (!Settings.getSettings().inDeveloperMode())
            return false;

        if (actions.contains(GameAction.DEBUG_PRINT_INFO)) {
            // Ctrl-I prints building or unit info
            var set = viewer.getSelection().getCurrentSelection().getSet();
            if (!set.isEmpty()) {
                var s = set.iterator().next();
                if (s instanceof Building building) {
                    if (!building.isDead() && !building.getAbilities().hasAbilities(Abilities.ATTACK))
                        building.printDebugInfo();
                } else if (s instanceof Unit unit) {
                    if (!unit.isDead())
                        unit.printDebugInfo();
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void handleInput(@NonNull InputEvent event) {
        super.handleInput(event);
        if (event.isConsumed()) return;

        if (event.getPhase() == InputPhase.PRESSED) {
            if (event.consumeAction(GameAction.GLOBAL_MENU)) {
                getGUIRoot().pushDelegate(new InGameMainMenu(viewer, new StaticCamera(getCamera().getState())));
                event.consume();
                return;
            }

            if (cheat(event.getActions())) {
                event.consume();
                return;
            }
        }
    }

    public final @NonNull WorldViewer getViewer() {
        return viewer;
    }
}
