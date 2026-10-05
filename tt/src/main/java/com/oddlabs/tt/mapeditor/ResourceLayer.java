package com.oddlabs.tt.mapeditor;

import com.oddlabs.tt.landscape.AbstractTreeGroup;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.landscape.TreeGroup;
import com.oddlabs.tt.landscape.TreeLeaf;
import com.oddlabs.tt.landscape.TreeNodeVisitor;
import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.model.AbstractElementNode;
import com.oddlabs.tt.model.ElementLeaf;
import com.oddlabs.tt.model.ElementNode;
import com.oddlabs.tt.model.ElementNodeVisitor;
import com.oddlabs.tt.model.IronSupply;
import com.oddlabs.tt.model.Model;
import com.oddlabs.tt.model.RockSupply;
import com.oddlabs.tt.model.SupplyModel;
import com.oddlabs.tt.pathfinder.Occupant;
import com.oddlabs.tt.pathfinder.Region;
import com.oddlabs.tt.pathfinder.StaticOccupant;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.render.SpriteKey;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The trees, rocks and iron on the island, by grid cell, and painting them with a brush.
 *
 * <p>Resources go where the generator would put them: on the playable region of {@link AccessMap} and apart from
 * each other as the generator keeps them, which leaves
 * a resource's four neighbours clear of every other resource's surrounding cells. Density thins them further by
 * keeping resources of one kind a distance apart that grows as density drops, so the count under the brush goes
 * with the density; at zero only the one nearest the brush centre is placed.
 */
final class ResourceLayer {
    /**
     * The distance in cells two resources of one kind keep at full density. It matches how tightly the generator's
     * spacing rule packs them, so spreading them further as density drops keeps the count in step with density.
     */
    private static final float FULL_DENSITY_SPACING = 2.8f;
    /** Cells across the clumps and clearings of a natural spread. */
    private static final float CLUMP_FEATURE = 12f;
    /** The most cells a natural spread keeps between resources of one kind, however thin. */
    private static final float MAX_NATURAL_SPACING = 12f;
    /** Fixed, so painting the same ground again fills in the same spread rather than a new one. */
    private static final int NATURAL_SEED = 0x5eed;

    /** What one stroke placed and removed, to take it back. */
    static final class Stroke {
        private final List<int[]> added = new ArrayList<>();
        private final List<int[]> removed = new ArrayList<>();

        boolean isEmpty() {
            return added.isEmpty() && removed.isEmpty();
        }
    }

    /** Told about each rectangle of cells (inclusive) where resources came or went. */
    @FunctionalInterface
    interface ChangeListener {
        void resourcesChanged(@NonNull ResourceLayer layer, int x0, int y0, int x1, int y1);
    }

    private final @NonNull World world;
    private final @NonNull AccessMap access;
    private final int size;
    private final Landscape.@NonNull TerrainType terrain;
    private final @NonNull ChangeListener listener;

    private final @Nullable Resource @NonNull [] kinds;
    private final @Nullable Object @NonNull [] live;

    // Cells of a brush's disk, nearest the centre first; rebuilt when the radius changes.
    private int @NonNull [] disk = new int[0];
    private int disk_radius = -1;

    private boolean trees_changed;
    private @Nullable Region opened_region;

    ResourceLayer(@NonNull World world, @NonNull AccessMap access, @NonNull MapSettings settings,
            @NonNull ChangeListener listener) {
        this.world = world;
        this.access = access;
        this.size = access.getSize();
        this.terrain = Landscape.TerrainType.values()[settings.terrain()];
        this.listener = listener;
        this.kinds = new Resource[size * size];
        this.live = new Object[size * size];
        indexTrees(world.getTreeRoot());
        indexSupplies();
    }

    private void indexTrees(@NonNull AbstractTreeGroup group) {
        group.visit(new TreeNodeVisitor() {
            @Override
            public void visitLeaf(TreeLeaf leaf) {
                leaf.visitTrees(this);
            }

            @Override
            public void visitNode(TreeGroup node) {
                node.visitChildren(this);
            }

            @Override
            public void visitTree(TreeSupply tree) {
                if (!tree.isHidden())
                    put(tree.getGridX(), tree.getGridY(), Resource.ofTree(tree.getTreeType()), tree);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void indexSupplies() {
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
                if (model instanceof RockSupply rock && !rock.isEmpty())
                    put(rock.getGridX(), rock.getGridY(), Resource.ROCK, rock);
                else if (model instanceof IronSupply iron && !iron.isEmpty())
                    put(iron.getGridX(), iron.getGridY(), Resource.IRON, iron);
            }
        });
    }

    private void put(int x, int y, @Nullable Resource kind, @Nullable Object object) {
        kinds[y * size + x] = kind;
        live[y * size + x] = object;
    }

    /** Grid positions of every resource of a kind, in rows, as the generator lists them. */
    @NonNull
    List<int[]> positions(@NonNull Resource kind) {
        return positionsIn(kind, 0, 0, size - 1, size - 1);
    }

    /** Grid positions of the resources of a kind within a rectangle of cells (inclusive, clamped to the map). */
    @NonNull
    List<int[]> positionsIn(@NonNull Resource kind, int x0, int y0, int x1, int y1) {
        List<int[]> positions = new ArrayList<>();
        for (int y = Math.max(0, y0); y <= Math.min(size - 1, y1); y++)
            for (int x = Math.max(0, x0); x <= Math.min(size - 1, x1); x++)
                if (kinds[y * size + x] == kind)
                    positions.add(new int[]{x, y});
        return positions;
    }

    // ---- Painting ----

    /**
     * Places resources of a kind under a brush, up to the given density (0 to 1).
     *
     * <p>A natural spread gathers them into clumps with clearings between, the way the generator's noise maps do,
     * keeps them uneven distances apart and leaves some cells out, so no rows line up. It is the same spread each
     * time for the same ground, so painting over it again fills it in rather than piling more on.
     *
     * @param cx     brush centre in meters
     * @param radius brush radius in meters
     */
    void paint(@NonNull Resource kind, float cx, float cy, float radius, float density, boolean natural,
            @NonNull Stroke stroke) {
        int center_x = Math.round(cx / HeightMap.METERS_PER_UNIT_GRID);
        int center_y = Math.round(cy / HeightMap.METERS_PER_UNIT_GRID);
        int r = Math.max(0, Math.round(radius / HeightMap.METERS_PER_UNIT_GRID));
        int[] cells = diskCells(r);
        if (natural && density > 0f) {
            paintNatural(kind, center_x, center_y, cells, density, stroke);
            finish();
            return;
        }
        // Below this density no two fit in the disk, which leaves just the one nearest the centre.
        float spacing = density <= 0f ? Float.POSITIVE_INFINITY : FULL_DENSITY_SPACING / (float) Math.sqrt(density);
        if (spacing > 2 * r) {
            if (anyWithin(kind, center_x, center_y, r))
                return;
            spacing = Float.POSITIVE_INFINITY;
        }
        int reach = Float.isInfinite(spacing) ? 0 : (int) Math.ceil(spacing);
        for (int i = 0; i < cells.length; i += 2) {
            int x = center_x + cells[i];
            int y = center_y + cells[i + 1];
            if (!canPlace(x, y) || (reach > 0 && kindWithin(kind, x, y, spacing, reach)))
                continue;
            place(kind, x, y);
            stroke.added.add(new int[]{kind.ordinal(), x, y});
            changed(x, y);
            if (reach == 0)
                break;
        }
        finish();
    }

    private void paintNatural(@NonNull Resource kind, int center_x, int center_y, int @NonNull [] cells,
            float density, @NonNull Stroke stroke) {
        int seed = NATURAL_SEED + kind.ordinal() * 101;
        for (int i = 0; i < cells.length; i += 2) {
            int x = center_x + cells[i];
            int y = center_y + cells[i + 1];
            if (!inside(x, y))
                continue;
            float clump = Math.clamp((Noise.fractal(x, y, CLUMP_FEATURE, 3, seed) - .35f) / .3f, 0f, 1f);
            float local = density * clump;
            // Some cells are passed over, more of them where it is sparse, so nothing lines up.
            if (local < .02f || Noise.hash(x, y, seed + 7) > .35f + .65f * clump)
                continue;
            float spacing = Math.min(MAX_NATURAL_SPACING, FULL_DENSITY_SPACING / (float) Math.sqrt(
                    local) * (.75f + .6f * Noise.hash(x, y, seed + 3)));
            if (!canPlace(x, y) || kindWithin(kind, x, y, spacing, (int) Math.ceil(spacing)))
                continue;
            place(kind, x, y);
            stroke.added.add(new int[]{kind.ordinal(), x, y});
            changed(x, y);
        }
    }

    /** The resources in a rectangle of cells (inclusive), as their ordinal and their cell counted from (x0, y0). */
    @NonNull
    List<int @NonNull []> copyRect(int x0, int y0, int x1, int y1) {
        List<int[]> copied = new ArrayList<>();
        for (int y = Math.max(0, y0); y <= Math.min(size - 1, y1); y++) {
            for (int x = Math.max(0, x0); x <= Math.min(size - 1, x1); x++) {
                Resource found = kinds[y * size + x];
                if (found != null)
                    copied.add(new int[]{found.ordinal(), x - x0, y - y0});
            }
        }
        return copied;
    }

    /**
     * Clears a w by h rectangle of cells from (x0, y0) and places copied resources in it, where they fit as painted
     * ones would. The heights under them should be pasted, and the playable area sorted again, first.
     *
     * @param resources ordinals and cells counted from (x0, y0), as {@link #copyRect} gives them
     */
    void paste(@NonNull List<int @NonNull []> resources, int x0, int y0, int w, int h, @NonNull Stroke stroke) {
        for (int y = Math.max(0, y0); y <= Math.min(size - 1, y0 + h - 1); y++) {
            for (int x = Math.max(0, x0); x <= Math.min(size - 1, x0 + w - 1); x++) {
                Resource found = kinds[y * size + x];
                if (found != null) {
                    remove(x, y);
                    stroke.removed.add(new int[]{found.ordinal(), x, y});
                    changed(x, y);
                }
            }
        }
        for (int[] copied : resources) {
            int x = x0 + copied[1];
            int y = y0 + copied[2];
            if (!canPlace(x, y))
                continue;
            Resource kind = Resource.values()[copied[0]];
            place(kind, x, y);
            stroke.added.add(new int[]{kind.ordinal(), x, y});
            changed(x, y);
        }
        finish();
    }

    /** Removes every resource of a kind under a brush, or of every kind when the kind is null. */
    void erase(@Nullable Resource kind, float cx, float cy, float radius, @NonNull Stroke stroke) {
        int center_x = Math.round(cx / HeightMap.METERS_PER_UNIT_GRID);
        int center_y = Math.round(cy / HeightMap.METERS_PER_UNIT_GRID);
        int[] cells = diskCells(Math.max(0, Math.round(radius / HeightMap.METERS_PER_UNIT_GRID)));
        for (int i = 0; i < cells.length; i += 2) {
            int x = center_x + cells[i];
            int y = center_y + cells[i + 1];
            Resource found = inside(x, y) ? kinds[y * size + x] : null;
            if (found != null && (kind == null || found == kind)) {
                remove(x, y);
                stroke.removed.add(new int[]{found.ordinal(), x, y});
                changed(x, y);
            }
        }
        finish();
    }

    /**
     * Takes back a stroke: removes what it placed and puts back what it removed.
     *
     * @return what that changed, as a stroke whose undo redoes this one
     */
    @NonNull
    Stroke undo(@NonNull Stroke stroke) {
        Stroke undone = new Stroke();
        for (int[] added : stroke.added) {
            Resource found = kinds[added[2] * size + added[1]];
            if (found != null) {
                remove(added[1], added[2]);
                undone.removed.add(new int[]{found.ordinal(), added[1], added[2]});
                changed(added[1], added[2]);
            }
        }
        for (int[] removed : stroke.removed) {
            if (kinds[removed[2] * size + removed[1]] == null) {
                place(Resource.values()[removed[0]], removed[1], removed[2]);
                undone.added.add(removed);
                changed(removed[1], removed[2]);
            }
        }
        finish();
        return undone;
    }

    /**
     * Removes every resource on a cell the last {@link AccessMap#compute} took out of the playable region, where
     * units can no longer get to it.
     *
     * @return whether any was removed
     */
    boolean prune(@NonNull Stroke stroke) {
        int removed = stroke.removed.size();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                Resource found = kinds[y * size + x];
                if (found != null && access.leftRegion(x, y)) {
                    remove(x, y);
                    stroke.removed.add(new int[]{found.ordinal(), x, y});
                    changed(x, y);
                }
            }
        }
        finish();
        return stroke.removed.size() > removed;
    }

    private int @NonNull [] diskCells(int r) {
        if (r != disk_radius) {
            List<int[]> offsets = new ArrayList<>();
            for (int dy = -r; dy <= r; dy++)
                for (int dx = -r; dx <= r; dx++)
                    if (dx * dx + dy * dy <= r * r)
                        offsets.add(new int[]{dx, dy});
            offsets.sort(Comparator.comparingInt(o -> o[0] * o[0] + o[1] * o[1]));
            disk = new int[offsets.size() * 2];
            for (int i = 0; i < offsets.size(); i++) {
                disk[2 * i] = offsets.get(i)[0];
                disk[2 * i + 1] = offsets.get(i)[1];
            }
            disk_radius = r;
        }
        return disk;
    }

    private boolean inside(int x, int y) {
        return x > 0 && y > 0 && x < size - 1 && y < size - 1;
    }

    /** Playable ground with room to spare, as the generator places resources. */
    private boolean canPlace(int x, int y) {
        if (!inside(x, y) || kinds[y * size + x] != null || isOccupied(x, y))
            return false;
        // Landscape.placeSupplies places them only on the playable region.
        if (access.get(x, y) != AccessMap.Kind.REGION)
            return false;
        // Landscape.placeSupplies: the cell and its four neighbours are free of every resource's surroundings,
        // which keeps other resources out of the 5x5 square around it less its corners.
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                if (Math.abs(dx) == 2 && Math.abs(dy) == 2)
                    continue;
                int nx = x + dx;
                int ny = y + dy;
                if (nx >= 0 && ny >= 0 && nx < size && ny < size && kinds[ny * size + nx] != null)
                    return false;
            }
        }
        return true;
    }

    private boolean anyWithin(@NonNull Resource kind, int cx, int cy, int r) {
        return kindWithin(kind, cx, cy, r + 0.5f, r);
    }

    private boolean kindWithin(@NonNull Resource kind, int cx, int cy, float distance, int reach) {
        float limit = distance * distance;
        for (int dy = -reach; dy <= reach; dy++) {
            int y = cy + dy;
            if (y < 0 || y >= size)
                continue;
            for (int dx = -reach; dx <= reach; dx++) {
                int x = cx + dx;
                if (x >= 0 && x < size && dx * dx + dy * dy < limit && kinds[y * size + x] == kind)
                    return true;
            }
        }
        return false;
    }

    /** The region resources on opened up ground register in; nothing paths through it in the editor. */
    private @NonNull Region openedRegion(int x, int y) {
        Region region = opened_region;
        if (region == null) {
            region = new Region();
            region.setPosition(x, y);
            opened_region = region;
        }
        return region;
    }

    /**
     * Whether something stands on a cell. The world marks every cell units could not reach when it was built with
     * a static occupant, which stays when an edit opens the ground up; that one does not count.
     */
    private boolean isOccupied(int x, int y) {
        Occupant occupant = world.getUnitGrid().getOccupant(x, y);
        return occupant != null && !(occupant instanceof StaticOccupant);
    }

    private void place(@NonNull Resource kind, int x, int y) {
        UnitGrid grid = world.getUnitGrid();
        // Ground an edit opened up still carries the world's unreachable mark and has no pathfinding region, which
        // resources register themselves in. Clear the mark and give the cell the editor's region.
        Occupant mark = grid.getOccupant(x, y);
        if (mark instanceof StaticOccupant)
            grid.freeGrid(x, y, mark);
        if (grid.getRegion(x, y) == null)
            grid.setRegion(x, y, openedRegion(x, y));
        Object object;
        if (kind.isTree()) {
            object = world.getTreeRoot().plantTree(world, kind.treeType(terrain), x, y);
            trees_changed = true;
        } else {
            // As AbstractElementNode.buildSupplies places them: jittered in the cell, turned at random.
            SpriteKey[] sprites = kind == Resource.ROCK ? world.getLandscapeResources().getRockFragments() : world.getLandscapeResources().getIronFragments();
            SpriteKey sprite = sprites[world.getRandom().nextInt(sprites.length)];
            float px = UnitGrid.coordinateFromGrid(x) + (world.getRandom().nextFloat() - .5f);
            float py = UnitGrid.coordinateFromGrid(y) + (world.getRandom().nextFloat() - .5f);
            float rotation = world.getRandom().nextFloat() * 360f;
            object = kind == Resource.ROCK ? new RockSupply(world, sprite, 2f, x, y, px, py, rotation,
                    true) : new IronSupply(world, sprite, 2f, x, y, px, py, rotation, true);
        }
        put(x, y, kind, object);
    }

    private void remove(int x, int y) {
        Object object = live[y * size + x];
        if (object instanceof TreeSupply tree) {
            world.getTreeRoot().removeTree(tree);
            trees_changed = true;
        } else if (object instanceof SupplyModel supply) {
            supply.removeFromWorld();
        }
        put(x, y, null, null);
    }

    // Cells changed by the current call.
    private int changed_x0 = Integer.MAX_VALUE;
    private int changed_y0 = Integer.MAX_VALUE;
    private int changed_x1 = Integer.MIN_VALUE;
    private int changed_y1 = Integer.MIN_VALUE;

    private void changed(int x, int y) {
        changed_x0 = Math.min(changed_x0, x);
        changed_y0 = Math.min(changed_y0, y);
        changed_x1 = Math.max(changed_x1, x);
        changed_y1 = Math.max(changed_y1, y);
    }

    private void finish() {
        if (trees_changed) {
            world.getTreeRoot().refreshBounds();
            trees_changed = false;
        }
        if (changed_x0 <= changed_x1)
            listener.resourcesChanged(this, changed_x0, changed_y0, changed_x1, changed_y1);
        changed_x0 = Integer.MAX_VALUE;
        changed_y0 = Integer.MAX_VALUE;
        changed_x1 = Integer.MIN_VALUE;
        changed_y1 = Integer.MIN_VALUE;
    }

    /**
     * Puts resources on cells as another player's edit left them, whatever stands there and whatever painting
     * allows: the first count cells, as y * size + x, each with a {@link Resource} ordinal or -1 for none. A cell
     * something other than a resource stands on is left empty.
     */
    void applyShared(int @NonNull [] cells, byte @NonNull [] shared_kinds, int count) {
        for (int i = 0; i < count; i++) {
            int x = cells[i] % size;
            int y = cells[i] / size;
            if (kinds[cells[i]] != null)
                remove(x, y);
            if (shared_kinds[i] >= 0 && !isOccupied(x, y))
                place(Resource.values()[shared_kinds[i]], x, y);
            changed(x, y);
        }
        finish();
    }

    /** The resource on a cell, if any. */
    @Nullable
    Resource get(int x, int y) {
        return kinds[y * size + x];
    }
}
