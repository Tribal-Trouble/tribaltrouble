package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.landscape.AbstractTreeGroup;
import com.oddlabs.tt.landscape.TreeGroup;
import com.oddlabs.tt.landscape.TreeLeaf;
import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.resource.Resources;
import com.oddlabs.tt.resource.SpriteFile;
import com.oddlabs.tt.model.BoundingBox;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.oddlabs.tt.landscape.AbstractTreeGroup.TreeType;


/**
 * Base class that manages the culling, level-of-detail selection, and picking of trees.
 */
class TreePicker {
    private static final int CROWN_MIPMAP_CUTOFF = Globals.NO_MIPMAP_CUTOFF;
    private static final float SELECTION_RADIUS = 1.5f;

    @SuppressWarnings("unchecked")
    private final List<TreeSupply> @NonNull [] render_lists = (List<TreeSupply>[]) new List[]{new ArrayList<>(),
            new ArrayList<>(), new ArrayList<>(), new ArrayList<>()};
    @SuppressWarnings("unchecked")
    private final List<TreeSupply> @NonNull [] respond_render_lists = (List<TreeSupply>[]) new List<?>[]{
            new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()};

    private final BoundingBox picking_selection_box = new BoundingBox();
    private final @NonNull SpriteSorter sprite_sorter;
    private final @NonNull RenderStateCache<@NonNull TreeRenderState> render_state_cache
            = new RenderStateCache<>(() -> new TreeRenderState(TreePicker.this));
    private final @NonNull Map<@NonNull TreeType, @NonNull Tree> trees = loadTrees();
    private final @NonNull RespondManager respond_manager;
    private CameraState camera;

    private boolean visible_override;

    TreePicker(@NonNull SpriteSorter sprite_sorter, @NonNull RespondManager respond_manager) {
        this.respond_manager = respond_manager;
        this.sprite_sorter = sprite_sorter;
    }

    private static @NonNull Map<@NonNull TreeType, @NonNull Tree> loadTrees() {
        SpriteList jungle_crown = Resources.findResource(new SpriteFile("/geometry/misc/jungle_tree_crown.binsprite",
                CROWN_MIPMAP_CUTOFF, false, false, true, false, true));
        SpriteList jungle_trunk = Resources.findResource(new SpriteFile("/geometry/misc/jungle_tree_trunk.binsprite",
                CROWN_MIPMAP_CUTOFF, true, true, true, false));

        SpriteList palm_crown = Resources.findResource(new SpriteFile("/geometry/misc/palm_crown.binsprite",
                CROWN_MIPMAP_CUTOFF, false, false, true, false, true));
        SpriteList palm_trunk = Resources.findResource(new SpriteFile("/geometry/misc/palm_trunk.binsprite",
                CROWN_MIPMAP_CUTOFF, true, true, true, false));

        SpriteList oak_crown = Resources.findResource(new SpriteFile("/geometry/misc/oak_tree_crown.binsprite",
                CROWN_MIPMAP_CUTOFF, false, false, true, false, true));
        SpriteList oak_trunk = Resources.findResource(new SpriteFile("/geometry/misc/oak_tree_trunk.binsprite",
                CROWN_MIPMAP_CUTOFF, true, true, true, false));

        SpriteList pine_crown = Resources.findResource(new SpriteFile("/geometry/misc/pine_tree_crown.binsprite",
                CROWN_MIPMAP_CUTOFF, false, false, true, false, true));
        SpriteList pine_trunk = Resources.findResource(new SpriteFile("/geometry/misc/pine_tree_trunk.binsprite",
                CROWN_MIPMAP_CUTOFF, true, true, true, false));

        var trees = new EnumMap<TreeType, @NonNull Tree>(TreeType.class);
        trees.put(TreeType.JUNGLE, new Tree(jungle_trunk, jungle_crown));
        trees.put(TreeType.PALM, new Tree(palm_trunk, palm_crown));
        trees.put(TreeType.OAK, new Tree(oak_trunk, oak_crown));
        trees.put(TreeType.PINE, new Tree(pine_trunk, pine_crown));
        return Collections.unmodifiableMap(trees);
    }

    final @NonNull Map<@NonNull TreeType, @NonNull Tree> getTrees() {
        return trees;
    }

    protected final @NonNull List<@NonNull TreeSupply> @NonNull [] getRenderLists() {
        return render_lists;
    }

    protected final @NonNull List<@NonNull TreeSupply> @NonNull [] getRespondRenderLists() {
        return respond_render_lists;
    }

    final void getAllPicks(@NonNull List<@NonNull TreeSupply> pick_list) {
        for (List<@NonNull TreeSupply> render_list : render_lists) {
            pick_list.addAll(render_list);
            render_list.clear();
        }
        for (List<@NonNull TreeSupply> respond_render_list : respond_render_lists) {
            pick_list.addAll(respond_render_list);
            respond_render_list.clear();
        }
    }

    private void addToHighDetailList(int index, @NonNull TreeSupply tree, boolean respond) {
        if (respond) {
            respond_render_lists[index].add(tree);
        } else {
            render_lists[index].add(tree);
        }
    }

    final void markDetailPolygon(@NonNull TreeSupply tree_supply, @NonNull PolyDetail level) {
        // Always render high detail (Instanced Sprites)
        addToHighDetailList(tree_supply.getTreeType().ordinal(), tree_supply, respond_manager.isResponding(
                tree_supply));
    }

    final void setup(CameraState camera_state) {
        this.camera = camera_state;
        render_state_cache.clear();
    }

    final void visit(@NonNull AbstractTreeGroup node) {
        switch (node) {
            case TreeGroup group -> {
                for (AbstractTreeGroup child : group.children()) {
                    visit(child);
                }
            }
            case TreeLeaf leaf -> {
                for (TreeSupply tree : leaf.getTrees()) {
                    visitTree(tree);
                }
            }
            case TreeSupply tree -> visitTree(tree);
        }
    }

    private boolean pickingInFrustum(@NonNull TreeSupply tree_supply, float[][] frustum) {
        picking_selection_box.setBounds(-SELECTION_RADIUS + tree_supply.getPositionX(), SELECTION_RADIUS + tree_supply
                .getPositionX(), -SELECTION_RADIUS + tree_supply.getPositionY(), SELECTION_RADIUS + tree_supply
                        .getPositionY(), tree_supply.bmin_z, tree_supply.bmin_z + (tree_supply.bmax_z
                                - tree_supply.bmin_z) * tree_supply.getTreeType().heightScale);
        return RenderTools.inFrustum(picking_selection_box, frustum) != RenderTools.FrustumIntersection.ALL_OUTSIDE;
    }

    private void addToRenderList(@NonNull TreeSupply tree, @NonNull CameraState camera) {
        if (isPicking())
            markDetailPolygon(tree, PolyDetail.HIGH_POLY);
        else
            sprite_sorter.add(getRenderState(tree), camera, false);
    }

    private @NonNull LODObject getRenderState(@NonNull TreeSupply tree_supply) {
        TreeRenderState render_state = render_state_cache.get();
        render_state.setup(tree_supply);
        return render_state;
    }

    private void visitTree(@NonNull TreeSupply tree_supply) {
        if (tree_supply.isHidden())
            return;

        boolean in_view;
        // ... culling logic ...
        if (isPicking())
            in_view = !tree_supply.isDead() && (visible_override || pickingInFrustum(tree_supply, camera.getFrustum()));
        else
            in_view = visible_override || RenderTools.inFrustum(tree_supply, camera.getFrustum())
                    != RenderTools.FrustumIntersection.ALL_OUTSIDE;
        if (in_view) {
            addToRenderList(tree_supply, camera);
        }
    }

    boolean isPicking() {
        return true;
    }

    final CameraState getCamera() {
        return camera;
    }
}
