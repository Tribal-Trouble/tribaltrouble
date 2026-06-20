package com.oddlabs.tt.delegate;

import com.oddlabs.tt.model.UnitType;

import com.oddlabs.tt.camera.Camera;
import com.oddlabs.tt.camera.StaticCamera;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.IronSupply;
import com.oddlabs.tt.model.RockSupply;
import com.oddlabs.tt.model.SupplySpawnAnimation;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.resource.FogInfo;
import com.oddlabs.tt.viewer.Cheat;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * Base class for all delegates active during actual gameplay. Provides common functionality
 * such as cheat code handling and opening the in-game main menu.
 */
public abstract class InGameDelegate<C extends Camera> extends CameraDelegate<C> {
    private final @NonNull WorldViewer viewer;

    protected InGameDelegate(@NonNull WorldViewer viewer, @Nullable C camera) {
        super(viewer.getGUIRoot(), camera);
        this.viewer = viewer;
    }

    private boolean cheat(@NonNull Set<GameAction> actions) {
        // cheats
        Cheat cheat = viewer.getCheat();
        if (!cheat.isEnabled())
            return false;

        var pickLocation = viewer.getPicker().pickLocation(getCamera().getState());

        if (actions.contains(GameAction.CHEAT_1)) {
            // F1 creates a peon at the center of the view unless the player already has maximum units.
            if (pickLocation.isPresent() && viewer.getLocalPlayer().getUnitCountContainer().getNumSupplies() != viewer
                    .getParameters().getMaxUnitCount()) {
                var location = pickLocation.get();
                new Unit(viewer.getLocalPlayer(), location.x(), location.y(), null,
                        viewer.getLocalPlayer().getRaceInfo().getUnitTemplate(UnitType.PEON));
                return true;
            }
        }
        if (actions.contains(GameAction.CHEAT_2)) {
            // F2 creates a rock warrior at the center of the view unless the player already has maximum units.
            if (pickLocation.isPresent() && viewer.getLocalPlayer().getUnitCountContainer().getNumSupplies() != viewer
                    .getParameters().getMaxUnitCount()) {
                var location = pickLocation.get();
                new Unit(viewer.getLocalPlayer(), location.x(), location.y(), null,
                        viewer.getLocalPlayer().getRaceInfo().getUnitTemplate(UnitType.WARRIOR_ROCK));
                return true;
            }
        }
        if (actions.contains(GameAction.CHEAT_3)) {
            // F3 creates an iron warrior at the center of the view unless the player already has maximum units.
            if (pickLocation.isPresent() && viewer.getLocalPlayer().getUnitCountContainer().getNumSupplies() != viewer
                    .getParameters().getMaxUnitCount()) {
                var location = pickLocation.get();
                new Unit(viewer.getLocalPlayer(), location.x(), location.y(), null,
                        viewer.getLocalPlayer().getRaceInfo().getUnitTemplate(UnitType.WARRIOR_IRON));
                return true;
            }
        }
        if (actions.contains(GameAction.CHEAT_4)) {
            // F4 creates a chicken warrior at the center of the view unless the player already has maximum units.
            if (pickLocation.isPresent() && viewer.getLocalPlayer().getUnitCountContainer().getNumSupplies() != viewer
                    .getParameters().getMaxUnitCount()) {
                var location = pickLocation.get();
                new Unit(viewer.getLocalPlayer(), location.x(), location.y(), null,
                        viewer.getLocalPlayer().getRaceInfo().getUnitTemplate(UnitType.WARRIOR_RUBBER));
                return true;
            }
        }
        if (actions.contains(GameAction.CHEAT_5)) {
            // F5 creates a chieftain at the center of the view unless the player already has one or is training one
            if (pickLocation.isPresent() && !viewer.getLocalPlayer().hasActiveChieftain() && !viewer.getLocalPlayer()
                    .isTrainingChieftain()) {
                var location = pickLocation.get();
                Unit chieftain = new Unit(viewer.getLocalPlayer(), location.x(), location.y(), null,
                        viewer.getLocalPlayer().getRaceInfo().getUnitTemplate(UnitType.CHIEFTAIN));
                viewer.getLocalPlayer().setActiveChieftain(chieftain);
                return true;
            }
        }
        if (actions.contains(GameAction.CHEAT_6)) {
            // F6 does massive damage on whatever is selected.
            viewer.getLocalPlayer().killSelection(viewer.getSelection().getCurrentSelection().filter(Abilities.NONE));
            return true;
        }
        if (actions.contains(GameAction.CHEAT_7)) {
            // F7 hides and shows trees
            cheat.draw_trees = !cheat.draw_trees;
            return true;
        }
        if (actions.contains(GameAction.CHEAT_8)) {
            // F8 hides and shows terrain grid.
            cheat.line_mode = !cheat.line_mode;
            return true;
        }
        if (actions.contains(GameAction.CHEAT_9)) {
            // F9 toggles fog
            FogInfo fog_info = viewer.getGUIRoot().getDelegate().getCamera().getState().getFog();
            fog_info.setEnabled(!fog_info.isEnabled());
            return true;
        }

        if (actions.contains(GameAction.CHEAT_11)) {
            // ALT-F11 spawns a rock
            if (pickLocation.isPresent()) {
                var loc = pickLocation.get();
                int gx = UnitGrid.toGridCoordinate(loc.x());
                int gy = UnitGrid.toGridCoordinate(loc.y());
                var world = viewer.getWorld();
                if (!world.getUnitGrid().isGridOccupied(gx, gy)) {
                    RockSupply rock = new RockSupply(world, gx, gy, loc.x(), loc.y(), false);
                    new SupplySpawnAnimation(rock, rock.getSpawnTime());
                    return true;
                }
            }
        }
        if (actions.contains(GameAction.CHEAT_12)) {
            // ALT-F12 spawns a meteor
            if (pickLocation.isPresent()) {
                var loc = pickLocation.get();
                int gx = UnitGrid.toGridCoordinate(loc.x());
                int gy = UnitGrid.toGridCoordinate(loc.y());
                var world = viewer.getWorld();
                if (!world.getUnitGrid().isGridOccupied(gx, gy)) {
                    IronSupply iron = new IronSupply(world, gx, gy, loc.x(), loc.y(), false);
                    new SupplySpawnAnimation(iron, iron.getSpawnTime());
                    return true;
                }
            }
        }

        // If in developer mode
        if (!Renderer.getRenderer().getSettings().inDeveloperMode())
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
