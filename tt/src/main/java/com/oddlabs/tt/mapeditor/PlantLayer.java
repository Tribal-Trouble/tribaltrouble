package com.oddlabs.tt.mapeditor;

import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.model.AbstractElementNode;
import com.oddlabs.tt.model.ElementLeaf;
import com.oddlabs.tt.model.ElementNode;
import com.oddlabs.tt.model.ElementNodeVisitor;
import com.oddlabs.tt.model.Model;
import com.oddlabs.tt.model.Plants;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.render.SpriteKey;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps the generator's decorative plants on ground they belong on. The generator only grows them on walkable land
 * above the sea; when an edit floods their ground or makes it too steep they are taken away, and they come back
 * when the ground is fit for them again, as after an undo.
 *
 * <p>Plants are not saved with a map: a game grows them afresh on the saved heights.
 */
final class PlantLayer {
    /** A plant as generated, and the model showing it while its ground is fit for it. */
    private static final class Spot {
        final float x;
        final float y;
        final float dir_x;
        final float dir_y;
        final @NonNull SpriteKey sprite;
        @Nullable
        Plants live;

        Spot(@NonNull Plants plant) {
            x = plant.getPositionX();
            y = plant.getPositionY();
            dir_x = plant.getDirectionX();
            dir_y = plant.getDirectionY();
            sprite = plant.getSpriteRenderer();
            live = plant;
        }
    }

    private final @NonNull World world;
    private final @NonNull AccessMap access;
    private final List<@NonNull Spot> spots = new ArrayList<>();

    PlantLayer(@NonNull World world, @NonNull AccessMap access) {
        this.world = world;
        this.access = access;
        // Only plants in the world's tree are shown; at graphics detail levels without plants there are none.
        AbstractElementNode<Model> root = world.getElementRoot();
        root.visit(new ElementNodeVisitor<>() {
            @Override
            public void visitNode(ElementNode<Model> node) {
                node.visitElements(this);
                node.visitChildren(this);
            }

            @Override
            public void visitLeaf(ElementLeaf<Model> leaf) {
                leaf.visitElements(this);
            }

            @Override
            public void visit(Model model) {
                if (model instanceof Plants plant)
                    spots.add(new Spot(plant));
            }
        });
    }

    /** Takes away the plants on ground no longer fit for them and brings back the others, from the last sort. */
    void update() {
        for (Spot spot : spots) {
            boolean fit = isFit(spot.x, spot.y);
            Plants live = spot.live;
            if (!fit && live != null) {
                live.remove();
                spot.live = null;
            } else if (fit && live == null && Globals.INSERT_PLANTS[Settings.getSettings().graphic_detail]) {
                // Only while the graphics detail shows plants: one made otherwise is never in the world to remove.
                spot.live = new Plants(world, spot.x, spot.y, spot.dir_x, spot.dir_y, spot.sprite);
            }
        }
    }

    /** Walkable land above the sea, where the generator grows plants. */
    private boolean isFit(float x, float y) {
        int size = access.getSize();
        int gx = Math.clamp(UnitGrid.toGridCoordinate(x), 0, size - 1);
        int gy = Math.clamp(UnitGrid.toGridCoordinate(y), 0, size - 1);
        AccessMap.Kind kind = access.get(gx, gy);
        return kind == AccessMap.Kind.REGION || kind == AccessMap.Kind.CUT_OFF;
    }
}
