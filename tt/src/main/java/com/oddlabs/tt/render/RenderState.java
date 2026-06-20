package com.oddlabs.tt.render;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.BuildingType;
import com.oddlabs.tt.model.Element;
import com.oddlabs.tt.model.Model;
import com.oddlabs.tt.model.Plants;
import com.oddlabs.tt.model.PointEmitterModel;
import com.oddlabs.tt.model.Race;
import com.oddlabs.tt.model.RubberSupply;
import com.oddlabs.tt.model.SceneryModel;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Shadowable;
import com.oddlabs.tt.model.SupplyModel;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.model.behaviour.StunController;
import com.oddlabs.tt.model.weapon.DirectedThrowingWeapon;
import com.oddlabs.tt.model.weapon.RotatingThrowingWeapon;
import com.oddlabs.tt.model.weapon.SonicBlast;
import com.oddlabs.tt.net.PeerHub;
import com.oddlabs.tt.particle.BalancedParametricEmitter;
import com.oddlabs.tt.particle.Emitter;
import com.oddlabs.tt.particle.Lightning;
import com.oddlabs.tt.particle.SonicBlastEffect;
import com.oddlabs.tt.particle.StunFunction;
import com.oddlabs.tt.model.weapon.LightningCloud;
import com.oddlabs.tt.model.weapon.PoisonFog;
import com.oddlabs.tt.model.weapon.Stun;
import com.oddlabs.tt.player.Player;
import org.lwjgl.opengl.GL11;
import com.oddlabs.tt.procedural.GeneratorRing;
import com.oddlabs.tt.model.BoundingBox;
import com.oddlabs.tt.viewer.Selection;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

/**
 * Manages the rendering state and visit logic for world entities and their accessories.
 */
final class RenderState {
    private final Queue<@NonNull Emitter<?>> emitter_queue = new ArrayDeque<>();
    private final Queue<@NonNull Lightning> lightning_queue = new ArrayDeque<>();
    private final Queue<@NonNull SonicBlastEffect> sonic_blast_queue = new ArrayDeque<>();
    private final @NonNull SpriteSorter sprite_sorter;
    private final @NonNull RenderStateCache<@NonNull ElementRenderState<Model>> render_state_cache;
    private final @NonNull RenderStateCache<@NonNull AttachedRenderState> attached_state_cache;
    private final @NonNull RenderQueues render_queues;
    private final @NonNull TargetRespondRenderer target_respond_renderer;
    private final @NonNull SelectableShadowRenderer default_shadow_renderer;
    private final @NonNull CrackDecalRenderer crack_shadow_renderer;
    private final @NonNull Picker picker;
    private final @Nullable Selection selection;
    private final @NonNull Player local_player;
    private final @NonNull MatrixStack model_view_stack = new MatrixStack();

    private boolean picking;
    private boolean visible_override;
    private @Nullable CameraState camera;

    public RenderState(@NonNull Player local_player, @NonNull SpriteSorter sprite_sorter,
            @NonNull RenderQueues render_queues, @NonNull Picker picker, @Nullable Selection selection) {
        this.local_player = local_player;
        this.selection = selection;
        this.picker = picker;
        this.sprite_sorter = sprite_sorter;
        this.render_queues = render_queues;
        ShadowListKey key = render_queues.registerRespondRenderer(new GeneratorRing(DecalRenderer.HALO_LUT_RESOLUTION,
                new float[][]{{0.40f, 0f}, {0.41f, 1f}, {0.48f, 1f}, {0.49f, 0f}}));
        this.target_respond_renderer = (TargetRespondRenderer) render_queues.getShadowRenderer(key);
        this.default_shadow_renderer = (SelectableShadowRenderer) render_queues.getShadowRenderer(
                render_queues.registerSelectableShadowList(VisualRegistry.DEFAULT_SHADOW_DESC));
        this.crack_shadow_renderer = (CrackDecalRenderer) render_queues.getShadowRenderer(
                render_queues.registerCrackDecalList(VisualRegistry.CRACK_DECAL_DESC));
        this.render_state_cache = new RenderStateCache<>(() -> new ElementRenderState<>(RenderState.this));
        this.attached_state_cache = new RenderStateCache<>(AttachedRenderState::new);
    }

    public void visit(@NonNull Element<?> element) {
        if (!element.isRegistered() || element.isFinished()) {
            element.remove();
            if (element instanceof Animated animated) {
                local_player.getWorld().getAnimationManagerGameTime().removeAnimation(animated);
            }
            return;
        }
        switch (element) {
            case Unit unit -> visitUnit(unit);
            case Building building -> visitBuilding(building);
            case Lightning lightning -> visitLightning(lightning);
            case SonicBlastEffect effect -> visitSonicBlastEffect(effect);
            case LandscapeTargetRespond respond -> visitRespond(respond);
            case RubberSupply model -> visitRubberSupply(model);
            case SupplyModel model -> visitSupplyModel(model);
            case Plants plants -> visitPlants(plants);
            case SceneryModel model -> visitSceneryModel(model);
            case DirectedThrowingWeapon weapon -> addToRenderList(getCachedState(directed_weapon_model_visitor,
                    weapon));
            case RotatingThrowingWeapon weapon -> addToRenderList(getCachedState(rotating_weapon_model_visitor,
                    weapon));
            case PointEmitterModel emitterModel -> visitPointEmitterModel(emitterModel);
            case SonicBlast blast -> visitSonicBlast(blast);
            case LightningCloud cloud -> visitLightningCloud(cloud);
            case PoisonFog fog -> visitPoisonFog(fog);
            case Stun stun -> visitStun(stun);
            default -> throw new UnsupportedOperationException("element has no rendering defined " + element);
        }
    }

    private void visitLightningCloud(final @NonNull LightningCloud cloud) {
        if (picking) return;
        float z_offset = getVisuallyCorrectHeight(cloud.getPositionX(), cloud.getPositionY());
        ElementRenderState<LightningCloud> state = (ElementRenderState<LightningCloud>) getCachedState(
                WhiteModelVisitor.getInstance(), cloud, z_offset);
        visitAccessories(cloud, state);
    }

    private void visitPoisonFog(final @NonNull PoisonFog fog) {
        if (picking) return;
        float z_offset = getVisuallyCorrectHeight(fog.getPositionX(), fog.getPositionY());
        ElementRenderState<PoisonFog> state = (ElementRenderState<PoisonFog>) getCachedState(
                WhiteModelVisitor.getInstance(), fog, z_offset);
        visitAccessories(fog, state);
    }

    private void visitStun(final @NonNull Stun stun) {
        if (picking) return;
        float z_offset = getVisuallyCorrectHeight(stun.getPositionX(), stun.getPositionY());
        ElementRenderState<Stun> state = (ElementRenderState<Stun>) getCachedState(
                WhiteModelVisitor.getInstance(), stun, z_offset);
        visitAccessories(stun, state);
    }

    private void visitPointEmitterModel(final @NonNull PointEmitterModel emitterModel) {
        if (picking) return;
        emitter_queue.add(emitterModel.getEmitter());
        float z_offset = getVisuallyCorrectHeight(emitterModel.getPositionX(), emitterModel.getPositionY());
        ElementRenderState<PointEmitterModel> state = (ElementRenderState<PointEmitterModel>) getCachedState(
                WhiteModelVisitor.getInstance(), emitterModel, z_offset);
        visitAccessories(emitterModel, state);
    }

    private void visitSonicBlast(final @NonNull SonicBlast blast) {
        if (picking) return;
        sonic_blast_queue.add(blast.getSonicBlastEffect());
        float z_offset = getVisuallyCorrectHeight(blast.getPositionX(), blast.getPositionY());
        ElementRenderState<SonicBlast> state = (ElementRenderState<SonicBlast>) getCachedState(
                WhiteModelVisitor.getInstance(), blast, z_offset);
        visitAccessories(blast, state);
    }

    @NonNull
    Player getLocalPlayer() {
        return local_player;
    }

    boolean isResponding(Object target) {
        return picker.getRespondManager().isResponding(target);
    }

    @NonNull
    RenderQueues getRenderQueues() {
        return render_queues;
    }

    @NonNull
    MatrixStack getModelViewStack() {
        return model_view_stack;
    }

    public void setVisibleOverride(boolean override) {
        this.visible_override = override;
    }

    public void setup(boolean picking, @NonNull CameraState camera_state) {
        this.picking = picking;
        this.camera = camera_state;
        render_state_cache.clear();
        attached_state_cache.clear();
        model_view_stack.clear().set(camera_state.getModelView());
        // Clear queues for new frame
        emitter_queue.clear();
        lightning_queue.clear();
        sonic_blast_queue.clear();
    }

    @Nullable
    CameraState getCamera() {
        return camera;
    }

    boolean isPicking() {
        return picking;
    }

    boolean overrideVisibility() {
        return visible_override;
    }

    private static final ModelVisitor<Unit> unit_visitor = new SelectableVisitor<>();

    private void visitUnit(final @NonNull Unit unit) {
        float z_offset = getVisuallyCorrectHeight(unit.getPositionX(), unit.getPositionY()) + unit.getOffsetZ();
        visitSelectable(unit_visitor, unit, z_offset, unit.getTemplate().getSelectionRadius(), unit.getTemplate()
                .getSelectionHeight());
    }

    private <M extends Model> @NonNull ElementRenderState<M> doGetCachedState() {
        return (ElementRenderState<M>) render_state_cache.get();
    }

    private @NonNull <M extends Model> ModelState<M> getCachedState(@NonNull ModelVisitor<M> visitor,
            @NonNull M model) {
        ElementRenderState<M> state = doGetCachedState();
        state.setup(visitor, model);
        return state;
    }

    private @NonNull <M extends Model> ModelState<M> getCachedState(@NonNull ModelVisitor<M> visitor, @NonNull M model,
            float dist_squared) {
        ElementRenderState<M> state = doGetCachedState();
        state.setup(visitor, model, dist_squared);
        return state;
    }

    private static boolean pickingInFrustum(@NonNull Selectable<?> selectable, float[][] frustum, float z_offset,
            float selection_radius, float selection_height) {
        BoundingBox picking_selection_box = new BoundingBox();
        picking_selection_box.setBounds(-selection_radius + selectable.getPositionX(), selection_radius + selectable
                .getPositionX(), -selection_radius + selectable.getPositionY(), selection_radius + selectable
                        .getPositionY(), z_offset, z_offset + selection_height);
        return RenderTools.inFrustum(picking_selection_box, frustum) != RenderTools.FrustumIntersection.ALL_OUTSIDE;
    }

    boolean isHovered(Selectable<?> selectable) {
        return selectable == picker.getCurrentHovered();
    }

    boolean isSelected(@NonNull Selectable<?> selectable) {
        return selection != null && selection.getCurrentSelection().contains(selectable);
    }

    private <S extends Selectable<?>> void visitSelectable(@NonNull ModelVisitor<S> visitor, @NonNull S selectable,
            float z_offset, float selection_radius, float selection_height) {
        boolean in_view = !picking || (selectable.isEnabled() && (visible_override || pickingInFrustum(selectable,
                camera.getFrustum(), z_offset, selection_radius, selection_height)));
        if (in_view) {
            Player owner = selectable.getOwnerNoCheck();
            boolean point_on_map = !local_player.isEnemy(owner) || (!owner.teamHasBuilding() && PeerHub
                    .getFreeQuitTimeLeft(local_player.getWorld()) < 0f);
            ElementRenderState<S> state = (ElementRenderState<S>) getCachedState(visitor, selectable, z_offset);
            SpriteSorter.DetailMode sort_status = addToRenderList(state, point_on_map);
            if (!picking && selectable.isEnabled() && sort_status == SpriteSorter.DetailMode.POLYGON) {
                ShadowListKey shadowKey = null;
                Race race = selectable.getOwnerNoCheck().getRaceInfo().getRaceType();
                if (selectable instanceof Unit) {
                    shadowKey = VisualRegistry.getInstance().getDefaultUnitShadow();
                } else if (selectable instanceof Building building) {
                    BuildingType bvt = building.getTemplate().getBuildingType();
                    shadowKey = VisualRegistry.getInstance().getBuildingVisuals(race, bvt).shadow();
                }
                if (shadowKey != null) {
                    SelectableShadowRenderer shadow_renderer = (SelectableShadowRenderer) render_queues
                            .getShadowRenderer(shadowKey);
                    if (isHovered(selectable) || isSelected(selectable)) {
                        shadow_renderer.addToSelectionList(state);
                    } else if (selectable.getShadowDiameter() > 0f) {
                        shadow_renderer.addToShadowList(state);
                    }
                }
            }
            visitAccessories(selectable, state);
        }
    }

    private @NonNull VisualModel getOrCreateVisualModel(@NonNull Model model) {
        return model.getClientState(VisualModel.class).orElseGet(() -> {
            VisualModel visualModel = new VisualModel(model);
            model.setClientState(visualModel);
            return visualModel;
        });
    }

    private <M extends Model> void visitAccessories(@NonNull M model, @NonNull ElementRenderState<M> parentState) {
        VisualModel visualModel = getOrCreateVisualModel(model);

        // Dynamically add/remove stun star accessory for units
        if (model instanceof Unit unit) {
            if (unit.getCurrentController() instanceof StunController stunController) {
                boolean hasStunStar = false;
                for (Accessory acc : visualModel.getAccessories()) {
                    if (acc instanceof EmitterAttachedAccessory) {
                        hasStunStar = true;
                        break;
                    }
                }
                if (!hasStunStar) {
                    float timeLeft = stunController.getTime();
                    float velocity = (float) Math.PI / 2;
                    BalancedParametricEmitter emitter = new BalancedParametricEmitter(
                            local_player.getWorld(),
                            new StunFunction(.4f, .15f), new Vector3f(0f, 0f, 0f),
                            velocity, 5f, (float) Math.PI * 2, (float) Math.PI * 2,
                            5, 0f, 2f,
                            Color.Linear.WHITE, Color.LinearDelta.ZERO,
                            new Vector3f(.1f, .1f, .1f), new Vector3f(0f, 0f, 0f), timeLeft,
                            GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                            VisualRegistry.getInstance().getStarTextures());

                    float mountOffset = unit.getMountOffset();
                    var offset = new Vector3f(
                            unit.getTemplate().getStunX(),
                            unit.getTemplate().getStunY(),
                            unit.getTemplate().getStunZ() + mountOffset);
                    visualModel.getAccessories().add(new EmitterAttachedAccessory(emitter, offset));
                }
            } else {
                visualModel.getAccessories().removeIf(acc -> acc instanceof EmitterAttachedAccessory);
            }
        }

        List<Accessory> accessories = visualModel.getAccessories();
        for (Accessory accessory : accessories) {
            if (accessory != null && accessory.isVisible(model, camera)) {
                visitAccessory(accessory, parentState);
            }
        }
    }

    private <M extends Model> void visitAccessory(@NonNull Accessory accessory,
            @NonNull ElementRenderState<M> parentState) {
        if (picking) return;

        switch (accessory) {
            case EmitterAccessory ea -> {
                updateEmitterWorldPosition(ea.getEmitter(), ea, parentState);
                emitter_queue.add(ea.getEmitter());
            }
            case StaticAccessory s -> {
                AttachedRenderState state = attached_state_cache.get();
                state.setup(parentState, s);
                addToRenderList(state);
            }
            default -> {
                accessory.addEmitters(emitter_queue);
                AttachedRenderState state = attached_state_cache.get();
                state.setup(parentState, accessory);
                addToRenderList(state);
            }
        }
    }

    private <M extends Model> void updateEmitterWorldPosition(@NonNull Emitter<?> emitter,
            @NonNull Accessory accessory, @NonNull ElementRenderState<M> parentState) {
        Matrix4f temp_matrix = new Matrix4f();
        Matrix4f rel_matrix = new Matrix4f();
        Vector3f pos_vector = new Vector3f();

        // Get parent world transform (pos and rot)
        parentState.getTransform(temp_matrix);

        // Get the relative offset in parent local space
        rel_matrix.identity();
        accessory.getRelativeTransform(rel_matrix, parentState.model);

        // Transform the LOCAL offset to WORLD space
        temp_matrix.transformPosition(rel_matrix.m30(), rel_matrix.m31(), rel_matrix.m32(), pos_vector);

        emitter.getPosition().set(pos_vector);
    }

    private float getVisuallyCorrectHeight(float x_f, float y_f) {
        return local_player.getWorld().getHeightMap().computeInterpolatedHeight(0, x_f, y_f);
    }

    private static float getBuildingSelectionRadius(@NonNull Building building) {
        Building.BuildStage render_level = building.getBuildStage();
        var template = building.getTemplate();
        return switch (render_level) {
            case START -> template.getStartSelectionRadius();
            case HALFBUILT -> template.getHalfbuiltSelectionRadius();
            case UNPLACED, BUILT -> template.getBuiltSelectionRadius();
        };
    }

    private static float getBuildingSelectionHeight(@NonNull Building building) {
        Building.BuildStage render_level = building.getBuildStage();
        var template = building.getTemplate();
        return switch (render_level) {
            case START -> template.getStartSelectionHeight();
            case HALFBUILT -> template.getHalfbuiltSelectionHeight();
            case UNPLACED, BUILT -> template.getBuiltSelectionHeight();
        };
    }

    private static final ModelVisitor<Building> building_visitor = new SelectableVisitor<>();

    private void visitBuilding(final @NonNull Building building) {
        float z_offset = getVisuallyCorrectHeight(building.getPositionX(), building.getPositionY());
        visitSelectable(building_visitor, building, z_offset, getBuildingSelectionRadius(building),
                getBuildingSelectionHeight(building));
    }

    SpriteSorter.@NonNull DetailMode addToRenderList(@NonNull LODObject model) {
        return addToRenderList(model, false);
    }

    SpriteSorter.@NonNull DetailMode addToRenderList(@NonNull LODObject model, boolean point_on_map) {
        return sprite_sorter.add(model, camera, point_on_map);
    }

    private void visitEmitter(final @NonNull Emitter<?> emitter) {
        if (!picking)
            emitter_queue.add(emitter);
    }

    private void visitLightning(@NonNull Lightning lightning) {
        if (!picking)
            lightning_queue.add(lightning);
    }

    private void visitSonicBlastEffect(@NonNull SonicBlastEffect effect) {
        if (!picking)
            sonic_blast_queue.add(effect);
    }

    private void visitRespond(final @NonNull LandscapeTargetRespond respond) {
        if (!picking)
            target_respond_renderer.addToTargetList(respond);
    }

    private static final ModelVisitor<SupplyModel> supply_model_visitor = new SupplyModelVisitor<>() {
        @Override
        public @NonNull Optional<SpriteKey> getSpriteKey(@NonNull ElementRenderState<SupplyModel> render_state) {
            return Optional.ofNullable(render_state.getModel().getBoundsProvider() instanceof SpriteKey spriteKey
                    ? spriteKey : null);
        }

        @Override
        public void getTransform(@NonNull ElementRenderState<SupplyModel> render_state, @NonNull Matrix4f dest) {
            SupplyModel model = render_state.getModel();
            dest.translation(model.getPositionX(), model.getPositionY(), model.getPositionZ())
                    .rotate(model.getRotation(), 0f, 0f, 1f);

            Color.Linear tint = model.getSpawnColorTint();
            if (tint != null) {
                render_state.setColor(tint);
            }
        }
    };

    private void visitSupplyModel(final @NonNull SupplyModel model) {
        ElementRenderState<SupplyModel> state = (ElementRenderState<SupplyModel>) getCachedState(
                supply_model_visitor, model);
        addToRenderList(state);
        if (!picking) {
            if (model.getShadowDiameter() > 0f)
                default_shadow_renderer.addToShadowList(state);
            if (model.getCrackDecalOpacity() > 0.0f) {
                crack_shadow_renderer.addToCrackList(new Shadowable() {
                    @Override
                    public float getPositionX() {
                        return model.getPositionX();
                    }

                    @Override
                    public float getPositionY() {
                        return model.getPositionY();
                    }

                    @Override
                    public float getShadowDiameter() {
                        return model.getCrackDecalDiameter();
                    }

                    @Override
                    public float getShadowOpacity() {
                        return model.getCrackDecalOpacity();
                    }

                    @Override
                    public Color.@NonNull Linear getShadowColor() {
                        Color.Linear color = model.getCrackDecalColor();
                        return color != null ? color : Color.Linear.BLACK;
                    }

                    @Override
                    public float getShadowVerticalCenter() {
                        return 0.6f;
                    }

                    @Override
                    public float getShadowPattern() {
                        return model.getCrackDecalPattern();
                    }
                });
            }
        }
        visitAccessories(model, state);
    }

    private static final ModelVisitor<RubberSupply> rubber_model_visitor = new SupplyModelVisitor<>() {
        @Override
        public @NonNull Optional<SpriteKey> getSpriteKey(@NonNull ElementRenderState<RubberSupply> render_state) {
            return Optional.ofNullable(render_state.getModel().getBoundsProvider() instanceof SpriteKey spriteKey
                    ? spriteKey : null);
        }

        @Override
        public void getTransform(@NonNull ElementRenderState<RubberSupply> render_state, @NonNull Matrix4f dest) {
            Model model = render_state.model;
            float angle = (float) Math.atan2(model.getDirectionY(), model.getDirectionX());
            dest.translation(model.getPositionX(), model.getPositionY(), render_state.f).rotate(angle, 0f, 0f, 1f);
        }
    };

    private void visitRubberSupply(final @NonNull RubberSupply model) {
        float z_offset = getVisuallyCorrectHeight(model.getPositionX(), model.getPositionY()) + model.getOffsetZ();
        ElementRenderState<RubberSupply> state = (ElementRenderState<RubberSupply>) getCachedState(rubber_model_visitor,
                model, z_offset);
        addToRenderList(state);
        if (!picking && !model.isHit())
            default_shadow_renderer.addToShadowList(state);
        visitAccessories(model, state);
    }

    private static final ModelVisitor<SceneryModel> scenery_model_visitor = new WhiteModelVisitor<>() {
        @Override
        public @NonNull Optional<SpriteKey> getSpriteKey(@NonNull ElementRenderState<SceneryModel> render_state) {
            return Optional.ofNullable(render_state.getModel().getBoundsProvider() instanceof SpriteKey spriteKey
                    ? spriteKey : null);
        }
    };

    private void visitSceneryModel(final @NonNull SceneryModel model) {
        ModelState<SceneryModel> state = getCachedState(scenery_model_visitor, model);
        addToRenderList(state);
        if (!picking) {
            if (model.getShadowDiameter() > 0f)
                default_shadow_renderer.addToShadowList(state);
        }
    }

    private static final float PLANTS_CUT_DIST = 200;
    private static final ModelVisitor<Plants> plants_model_visitor = new WhiteModelVisitor<>() {
        private static final float START_FADE_DIST = 100;

        @Override
        public @NonNull Optional<SpriteKey> getSpriteKey(@NonNull ElementRenderState<Plants> render_state) {
            return Optional.ofNullable(render_state.getModel().getBoundsProvider() instanceof SpriteKey spriteKey
                    ? spriteKey : null);
        }

        @Override
        public void getTransform(@NonNull ElementRenderState<Plants> render_state, @NonNull Matrix4f dest) {
            Plants plants = render_state.getModel();
            float angle = (float) Math.atan2(plants.getDirectionY(), plants.getDirectionX());
            dest.translation(plants.getPositionX(), plants.getPositionY(), plants.getPositionZ()).rotate(angle, 0f, 0f,
                    1f);

            float dist_squared = render_state.f;
            if (dist_squared > START_FADE_DIST * START_FADE_DIST) {
                float camera_dist = (float) Math.sqrt(dist_squared);
                float alpha = 1f - ((camera_dist - START_FADE_DIST) / (PLANTS_CUT_DIST - START_FADE_DIST));
                render_state.setColor(new Color.Linear(1f, 1f, 1f, alpha));
            }
        }
    };

    private void visitPlants(final @NonNull Plants plants) {
        if (!picking && Globals.draw_plants) {
            float camera_dist_sqr = RenderTools.getEyeDistanceSquared(plants, camera.getCurrentX(), camera
                    .getCurrentY(), camera.getCurrentZ());
            if (camera_dist_sqr <= PLANTS_CUT_DIST * PLANTS_CUT_DIST)
                addToRenderList(getCachedState(plants_model_visitor, plants, camera_dist_sqr));
        }
    }

    private static final ModelVisitor<DirectedThrowingWeapon> directed_weapon_model_visitor
            = new WhiteModelVisitor<>() {
                @Override
                public @NonNull Optional<SpriteKey> getSpriteKey(@NonNull ElementRenderState<
                        DirectedThrowingWeapon> render_state) {
                    DirectedThrowingWeapon model = render_state.getModel();
                    Race race = model.getSrc().getOwner().getRaceInfo().getRaceType();
                    return Optional.of(VisualRegistry.getInstance().getWeaponSprite(race, model
                            .getWeaponVisualType()));
                }

                @Override
                public void getTransform(@NonNull ElementRenderState<DirectedThrowingWeapon> render_state,
                        @NonNull Matrix4f dest) {
                    DirectedThrowingWeapon model = render_state.getModel();
                    float yawRad = (float) Math.atan2(model.getDirectionY(), model.getDirectionX());
                    float pitchRad = (float) Math.toRadians(model.getAngle());
                    dest.translation(model.getPositionX(), model.getPositionY(), model.getPositionZ())
                            .rotate(yawRad, 0f, 0f, 1f)
                            .rotate(-pitchRad, 0f, 1f, 0f);
                }

                @Override
                public @NonNull Color getTeamColor(@NonNull ElementRenderState<DirectedThrowingWeapon> render_state) {
                    return render_state.getModel().getSrc().getOwner().getColor();
                }
            };

    private static final ModelVisitor<RotatingThrowingWeapon> rotating_weapon_model_visitor
            = new WhiteModelVisitor<>() {
                @Override
                public @NonNull Optional<SpriteKey> getSpriteKey(@NonNull ElementRenderState<
                        RotatingThrowingWeapon> render_state) {
                    RotatingThrowingWeapon model = render_state.getModel();
                    Race race = model.getSrc().getOwner().getRaceInfo().getRaceType();
                    return Optional.of(VisualRegistry.getInstance().getWeaponSprite(race, model
                            .getWeaponVisualType()));
                }

                @Override
                public void getTransform(@NonNull ElementRenderState<RotatingThrowingWeapon> render_state,
                        @NonNull Matrix4f dest) {
                    RotatingThrowingWeapon model = render_state.getModel();
                    float yawRad = (float) Math.atan2(model.getDirectionY(), model.getDirectionX());
                    float spinRad = (float) Math.toRadians(model.getAngle());
                    dest.translation(model.getPositionX(), model.getPositionY(), model.getPositionZ())
                            .rotate(yawRad, 0f, 0f, 1f)
                            .rotate(spinRad, 0f, 1f, 0f);
                }

                @Override
                public @NonNull Color getTeamColor(@NonNull ElementRenderState<RotatingThrowingWeapon> render_state) {
                    return render_state.getModel().getSrc().getOwner().getColor();
                }
            };

    public @NonNull Queue<@NonNull Emitter<?>> getEmitterQueue() {
        return emitter_queue;
    }

    public @NonNull Queue<@NonNull Lightning> getLightningQueue() {
        return lightning_queue;
    }

    public @NonNull Queue<@NonNull SonicBlastEffect> getSonicBlastQueue() {
        return sonic_blast_queue;
    }
}
