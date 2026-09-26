package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.landscape.TreeGroup;
import com.oddlabs.tt.landscape.TreeLeaf;
import com.oddlabs.tt.landscape.TreeNodeVisitor;
import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.resource.Resources;
import com.oddlabs.tt.resource.SpriteFile;
import com.oddlabs.tt.util.BoundingBox;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.oddlabs.tt.landscape.AbstractTreeGroup.TreeType;


class TreePicker implements TreeNodeVisitor {
    private static final int CROWN_MIPMAP_CUTOFF = Globals.NO_MIPMAP_CUTOFF;
    private static final float SELECTION_RADIUS = 1.5f;

    private final List<TreeSupply> @NonNull [] render_lists;
    private final List<TreeSupply> @NonNull [] respond_render_lists;

    private final BoundingBox picking_selection_box = new BoundingBox();
    private final SpriteSorter sprite_sorter;
    private final @NonNull RenderStateCache<@NonNull TreeRenderState> render_state_cache;
    private final @NonNull Map<@NonNull TreeType, @NonNull Tree> trees;
    private final RespondManager respond_manager;
    private CameraState camera;

    private boolean visible_override;

    TreePicker(SpriteSorter sprite_sorter, RespondManager respond_manager) {
        this.respond_manager = respond_manager;
        //noinspection unchecked
        this.render_lists = (List<TreeSupply>[]) new List[]{new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()};
        //noinspection unchecked
        this.respond_render_lists = (List<TreeSupply>[]) new List<?>[]{new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()};
        this.trees = loadTrees();

        this.sprite_sorter = sprite_sorter;
        render_state_cache = new RenderStateCache<>(() -> new TreeRenderState(TreePicker.this));
    }

    private static @NonNull Map<@NonNull TreeType, @NonNull Tree> loadTrees() {
        var trees = new EnumMap<TreeType, @NonNull Tree>(TreeType.class);
        trees.put(TreeType.JUNGLE, loadTree("jungle_tree_trunk", "jungle_tree_crown"));
        trees.put(TreeType.PALM, loadTree("palm_trunk", "palm_crown"));
        trees.put(TreeType.OAK, loadTree("oak_tree_trunk", "oak_tree_crown"));
        trees.put(TreeType.PINE, loadTree("pine_tree_trunk", "pine_tree_crown"));
        return Collections.unmodifiableMap(trees);
    }

    private static @NonNull Tree loadTree(@NonNull String trunk_name, @NonNull String crown_name) {
        SpriteFile trunk = new SpriteFile("/geometry/misc/" + trunk_name + ".binsprite",
                CROWN_MIPMAP_CUTOFF, true, true, true, false);
        SpriteFile crown = new SpriteFile("/geometry/misc/" + crown_name + ".binsprite",
                CROWN_MIPMAP_CUTOFF, false, false, true, false, true);
        List<SpriteList> props = Stream.of(trunk, crown).flatMap(part -> RacesResources.getProps(
                RacesResources.eventSkin(part).getLocation()).stream().map(part::withLocation)).map(
                        prop -> Resources.findResource(RacesResources.eventSkin(prop))).toList();
        return new Tree(Resources.findResource(RacesResources.eventSkin(trunk)), Resources.findResource(
                RacesResources.eventSkin(crown)), props);
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

    public final void getAllPicks(@NonNull List<@NonNull TreeSupply> pick_list) {
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

    public final void markDetailPolygon(@NonNull TreeSupply tree_supply, @NonNull PolyDetail level) {
        // Always render high detail (Instanced Sprites)
        addToHighDetailList(tree_supply.getTreeType().ordinal(), tree_supply, respond_manager.isResponding(
                tree_supply));
    }

    public final void setup(CameraState camera_state) {
        this.camera = camera_state;
        render_state_cache.clear();
    }

    @Override
    public final void visitLeaf(@NonNull TreeLeaf tree_leaf) {
        tree_leaf.visitTrees(this);
    }

    @Override
    public final void visitNode(@NonNull TreeGroup tree_group) {
        tree_group.visitChildren(this);
    }

    private float getHeightScale(@NonNull TreeType tree_type) {
        return switch (tree_type) {
            case JUNGLE -> .9f;
            case PALM -> .95f;
            case OAK -> .7f;
            case PINE -> .65f;
        };
    }

    private boolean pickingInFrustum(@NonNull TreeSupply tree_supply, float[][] frustum) {
        picking_selection_box.setBounds(-SELECTION_RADIUS + tree_supply.getPositionX(),
                SELECTION_RADIUS + tree_supply.getPositionX(), -SELECTION_RADIUS + tree_supply.getPositionY(),
                SELECTION_RADIUS + tree_supply.getPositionY(), tree_supply.bmin_z,
                tree_supply.bmin_z + (tree_supply.bmax_z - tree_supply.bmin_z) * getHeightScale(
                        tree_supply.getTreeType()));
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

    @Override
    public final void visitTree(@NonNull TreeSupply tree_supply) {
        if (tree_supply.isHidden())
            return;

        boolean in_view;
        // ... culling logic ...
        if (isPicking())
            in_view = !tree_supply.isDead() && (visible_override || pickingInFrustum(tree_supply, camera.getFrustum()));
        else
            in_view = visible_override || RenderTools.inFrustum(tree_supply,
                    camera.getFrustum()) != RenderTools.FrustumIntersection.ALL_OUTSIDE;
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
