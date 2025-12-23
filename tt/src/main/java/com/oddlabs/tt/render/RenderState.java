package com.oddlabs.tt.render;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.landscape.LandscapeTargetRespond;
import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.ElementVisitor;
import com.oddlabs.tt.model.Model;
import com.oddlabs.tt.model.Plants;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.model.RubberSupply;
import com.oddlabs.tt.model.SceneryModel;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.SupplyModel;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.model.UnitSupplyContainer;
import com.oddlabs.tt.model.weapon.DirectedThrowingWeapon;
import com.oddlabs.tt.model.weapon.RotatingThrowingWeapon;
import com.oddlabs.tt.net.PeerHub;
import com.oddlabs.tt.particle.Emitter;
import com.oddlabs.tt.particle.Lightning;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.procedural.GeneratorRing;
import com.oddlabs.tt.util.BoundingBox;
import com.oddlabs.tt.viewer.Selection;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

final class RenderState implements ElementVisitor {
	private final List<@NonNull Emitter> emitter_queue = new ArrayList<>();
	private final List<@NonNull Lightning> lightning_queue = new ArrayList<>();
	private final @NonNull SpriteSorter sprite_sorter;
	private final @NonNull RenderStateCache<ElementRenderState<Model>> render_state_cache;
	private final @NonNull RenderQueues render_queues;
	private final @NonNull TargetRespondRenderer target_respond_renderer;
	private final @NonNull SelectableShadowRenderer default_shadow_renderer;
	private final @NonNull Picker picker;
	private final Selection selection;
	private final @NonNull Player local_player;
	private final @NonNull MatrixStack model_view_stack = new MatrixStack();

	private boolean picking;
	private boolean visible_override;
	private CameraState camera;

	public RenderState(@NonNull Player local_player, @NonNull SpriteSorter sprite_sorter, @NonNull RenderQueues render_queues, @NonNull Picker picker, Selection selection) {
		this.local_player = local_player;
		this.selection = selection;
		this.picker = picker;
		this.sprite_sorter = sprite_sorter;
		this.render_queues = render_queues;
		ShadowListKey key = render_queues.registerRespondRenderer(new GeneratorRing(LandscapeTargetRespond.SIZE, new float[][]{{0.40f, 0f}, {0.41f, 1f}, {0.48f, 1f}, {0.49f, 0f}}));
		this.target_respond_renderer = (TargetRespondRenderer)render_queues.getShadowRenderer(key);
		this.default_shadow_renderer = (SelectableShadowRenderer)render_queues.getShadowRenderer(
				render_queues.registerSelectableShadowList(RacesResources.DEFAULT_SHADOW_DESC));
		this.render_state_cache = new RenderStateCache<>(() -> new ElementRenderState<>(RenderState.this));
	}

	@NonNull Player getLocalPlayer() {
		return local_player;
	}

	boolean isResponding(Object target) {
		return picker.getRespondManager().isResponding(target);
	}

	@NonNull RenderQueues getRenderQueues() {
		return render_queues;
	}

    @NonNull MatrixStack getModelViewStack() {
        return model_view_stack;
    }

	public void setVisibleOverride(boolean override) {
		this.visible_override = override;
	}

	public void setup(boolean picking, CameraState camera_state) {
		this.picking = picking;
		this.camera = camera_state;
		render_state_cache.clear();
		model_view_stack.clear().set(camera_state.getModelView());
	}

	CameraState getCamera() {
		return camera;
	}

	boolean isPicking() {
		return picking;
	}

	boolean overrideVisibility() {
		return visible_override;
	}

	private static final ModelVisitor<Unit> unit_visitor = new SelectableVisitor<>() {
		@Override
		public void markDetailPolygon(@NonNull ElementRenderState<Unit> render_state, @NonNull PolyDetail detail) {
			Unit unit = render_state.model;
			super.markDetailPolygon(render_state, detail);
			UnitSupplyContainer supply_container = unit.getSupplyContainer();
			if (!render_state.render_state.isPicking() && unit.getAbilities().hasAbilities(Abilities.BUILD) && supply_container.getSupplyType() != null) {
				if (supply_container.getNumSupplies() > 0) {
					SpriteRenderer supply_sprite = render_state.getRenderer(supply_container.getSupplySpriteRenderer(supply_container.getSupplyType()));
					supply_sprite.addToRenderList(detail, render_state, false);
				}
			}
		}
	};
	@Override
	public void visitUnit(final @NonNull Unit unit) {
		float z_offset = getVisuallyCorrectHeight(unit.getPositionX(), unit.getPositionY()) + unit.getOffsetZ();
		visitSelectable(unit_visitor, unit, z_offset, unit.getTemplate().getSelectionRadius(), unit.getTemplate().getSelectionHeight());
	}

	private <M extends Model> @NonNull ElementRenderState<M> doGetCachedState() {
		return (ElementRenderState<M>) render_state_cache.get();
	}

	private @NonNull <M extends Model> ModelState<M> getCachedState(ModelVisitor<M> visitor, M model) {
		ElementRenderState<M> state = doGetCachedState();
		state.setup(visitor, model);
		return state;
	}

	private @NonNull <M extends Model> ModelState<M> getCachedState(ModelVisitor<M> visitor, M model, float dist_squared) {
		ElementRenderState<M> state = doGetCachedState();
		state.setup(visitor, model, dist_squared);
		return state;
	}

	private static final BoundingBox picking_selection_box = new BoundingBox();
	private static boolean pickingInFrustum(@NonNull Selectable selectable, float[][] frustum, float z_offset, float selection_radius, float selection_height) {
		picking_selection_box.setBounds(-selection_radius + selectable.getPositionX(), selection_radius + selectable.getPositionX(), -selection_radius + selectable.getPositionY(), selection_radius + selectable.getPositionY(), z_offset, z_offset + selection_height);
		return RenderTools.inFrustum(picking_selection_box, frustum) != RenderTools.FrustumIntersection.ALL_OUTSIDE;
	}

	boolean isHovered(Selectable selectable) {
		return selectable == picker.getCurrentHovered();
	}

	boolean isSelected(@NonNull Selectable selectable) {
		return selection.getCurrentSelection().contains(selectable);
	}

	private <S extends Selectable> void visitSelectable(@NonNull ModelVisitor<S> visitor, @NonNull S selectable, float z_offset, float selection_radius, float selection_height) {
		boolean in_view = !picking || (selectable.isEnabled() && (visible_override || pickingInFrustum(selectable, camera.getFrustum(), z_offset, selection_radius, selection_height)));
		if (in_view) {
			Player owner = selectable.getOwnerNoCheck();
			boolean point_on_map = !local_player.isEnemy(owner) || (!owner.teamHasBuilding() && PeerHub.getFreeQuitTimeLeft(local_player.getWorld()) < 0f);
			ModelState<S> state = getCachedState(visitor, selectable, z_offset);
			int sort_status = addToRenderList(state, point_on_map);
			if (!picking && selectable.isEnabled() && sort_status == SpriteSorter.DETAIL_POLYGON) {
				SelectableShadowRenderer shadow_renderer = (SelectableShadowRenderer)render_queues.getShadowRenderer(selectable.getTemplate().getSelectableShadowRenderer());
				if (isHovered(selectable) || isSelected(selectable)) {
					shadow_renderer.addToSelectionList(state);
				} else {
					shadow_renderer.addToShadowList(state);
				}
			}
		}
	}

	private float getVisuallyCorrectHeight(float x_f, float y_f) {
		return local_player.getWorld().getHeightMap().computeInterpolatedHeight(0, x_f, y_f);
	}

	private static float getBuildingSelectionRadius(@NonNull Building building) {
		Building.BuildState render_level = building.getRenderLevel();
        return switch (render_level) {
            case START -> building.getTemplate().getStartSelectionRadius();
            case HALFBUILT -> building.getTemplate().getHalfbuiltSelectionRadius();
            case BUILT -> building.getTemplate().getBuiltSelectionRadius();
        };
	}

	private static float getBuildingSelectionHeight(@NonNull Building building) {
		Building.BuildState render_level = building.getRenderLevel();
        return switch (render_level) {
            case START -> building.getTemplate().getStartSelectionHeight();
            case HALFBUILT -> building.getTemplate().getHalfbuiltSelectionHeight();
            case BUILT -> building.getTemplate().getBuiltSelectionHeight();
        };
	}

	private static final ModelVisitor<Building> building_visitor = new SelectableVisitor<>();
	@Override
	public void visitBuilding(final @NonNull Building building) {
		visitSelectable(building_visitor, building, building.getPositionZ(), getBuildingSelectionRadius(building), getBuildingSelectionHeight(building));
	}

	int addToRenderList(@NonNull LODObject model) {
		return addToRenderList(model, false);
	}

	int addToRenderList(@NonNull LODObject model, boolean point_on_map) {
		return sprite_sorter.add(model, camera, point_on_map);
	}

	@Override
	public void visitEmitter(final @NonNull Emitter emitter) {
		if (!picking)
			emitter_queue.add(emitter);
	}

	@Override
	public void visitLightning(@NonNull Lightning lightning) {
		if (!picking)
			lightning_queue.add(lightning);
	}

	@Override
	public void visitRespond(final @NonNull LandscapeTargetRespond respond) {
		if (!picking)
			target_respond_renderer.addToTargetList(respond);
	}

	private static final ModelVisitor<SupplyModel> supply_model_visitor = new SupplyModelVisitor<>() {
		@Override
		public void transform(@NonNull ElementRenderState<SupplyModel> render_state) {
			SupplyModel model = render_state.getModel();
            render_state.getModelViewStack().translate(model.getPositionX(), model.getPositionY(), model.getPositionZ())
                .rotate(model.getRotation(), 0f, 0f, 1f);
		}
        @Override
        public void getTransform(@NonNull ElementRenderState<SupplyModel> render_state, @NonNull Matrix4f dest) {
            SupplyModel model = render_state.getModel();
            dest.translation(model.getPositionX(), model.getPositionY(), model.getPositionZ())
                .rotate(model.getRotation(), 0f, 0f, 1f);
        }
	};
	@Override
	public void visitSupplyModel(final @NonNull SupplyModel model) {
		addToRenderList(getCachedState(supply_model_visitor, model));
	}

	private static final ModelVisitor<RubberSupply> rubber_model_visitor = new SupplyModelVisitor<>() {
		@Override
		public void transform(@NonNull ElementRenderState<RubberSupply> render_state) {
			Model model = render_state.model;
			RenderTools.translateAndRotate(model.getPositionX(), model.getPositionY(), render_state.f, model.getDirectionX(), model.getDirectionY(), render_state.getModelViewStack());
		}

        @Override
        public void getTransform(@NonNull ElementRenderState<RubberSupply> render_state, @NonNull Matrix4f dest) {
            Model model = render_state.model;
            float angle = (float) Math.toDegrees(Math.atan2(model.getDirectionY(), model.getDirectionX()));
            dest.translation(model.getPositionX(), model.getPositionY(), render_state.f)
                .rotate(angle, 0f, 0f, 1f);
        }
	};
	@Override
	public void visitRubberSupply(final @NonNull RubberSupply model) {
		float z_offset = getVisuallyCorrectHeight(model.getPositionX(), model.getPositionY()) + model.getOffsetZ();
		ModelState<RubberSupply> state = getCachedState(rubber_model_visitor, model, z_offset);
		addToRenderList(state);
		if (!picking && !model.isHit())
			default_shadow_renderer.addToShadowList(state);
	}

	private static final ModelVisitor<SceneryModel> scenery_model_visitor = new WhiteModelVisitor<>() {
		@Override
		public void transform(@NonNull ElementRenderState<SceneryModel> render_state) {
			RenderTools.translateAndRotate(render_state.getModel(), render_state.getModelViewStack());
		}
        @Override
        public void getTransform(@NonNull ElementRenderState<SceneryModel> render_state, @NonNull Matrix4f dest) {
            Model model = render_state.getModel();
            float angle = (float) Math.toDegrees(Math.atan2(model.getDirectionY(), model.getDirectionX()));
            dest.translation(model.getPositionX(), model.getPositionY(), model.getPositionZ())
                .rotate(angle, 0f, 0f, 1f);
        }
	};
	@Override
	public void visitSceneryModel(final @NonNull SceneryModel model) {
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
		public void transform(@NonNull ElementRenderState<Plants> render_state) {
			Plants plants = render_state.getModel();
			RenderTools.translateAndRotate(plants, render_state.getModelViewStack());
			float dist_squared = render_state.f;
			if (dist_squared > START_FADE_DIST*START_FADE_DIST) {
				float camera_dist = (float)Math.sqrt(dist_squared);
				float alpha = 1f - ((camera_dist - START_FADE_DIST)/(PLANTS_CUT_DIST - START_FADE_DIST));
				render_state.getColor().set(1f, 1f, 1f, alpha);
			}
		}

        @Override
        public void getTransform(@NonNull ElementRenderState<Plants> render_state, @NonNull Matrix4f dest) {
            Plants plants = render_state.getModel();
            float angle = (float) Math.toDegrees(Math.atan2(plants.getDirectionY(), plants.getDirectionX()));
            dest.translation(plants.getPositionX(), plants.getPositionY(), plants.getPositionZ())
                .rotate(angle, 0f, 0f, 1f);
        }
	};
	@Override
	public void visitPlants(final @NonNull Plants plants) {
		if (!picking && Globals.draw_plants) {
			float camera_dist_sqr = RenderTools.getEyeDistanceSquared(plants, camera.getCurrentX(), camera.getCurrentY(), camera.getCurrentZ());
			if (camera_dist_sqr <= PLANTS_CUT_DIST*PLANTS_CUT_DIST)
				addToRenderList(getCachedState(plants_model_visitor, plants, camera_dist_sqr));
		}
	}

	private static final ModelVisitor<DirectedThrowingWeapon> directed_weapon_model_visitor = new WhiteModelVisitor<>() {
		@Override
		public void transform(@NonNull ElementRenderState<DirectedThrowingWeapon> render_state) {
			DirectedThrowingWeapon model = render_state.getModel();
			RenderTools.translateAndRotate(render_state.getModel(), render_state.getModelViewStack());
            render_state.getModelViewStack().rotate(-model.getZSpeed(), 0f, 1f, 0f);
		}
        @Override
        public void getTransform(@NonNull ElementRenderState<DirectedThrowingWeapon> render_state, @NonNull Matrix4f dest) {
            DirectedThrowingWeapon model = render_state.getModel();
            float angle = (float) Math.toDegrees(Math.atan2(model.getDirectionY(), model.getDirectionX()));
            dest.translation(model.getPositionX(), model.getPositionY(), model.getPositionZ())
                .rotate(angle, 0f, 0f, 1f)
                .rotate(-model.getZSpeed(), 0f, 1f, 0f);
        }
	};
	@Override
	public void visitDirectedThrowingWeapon(final @NonNull DirectedThrowingWeapon model) {
		if (!picking) {
			addToRenderList(getCachedState(directed_weapon_model_visitor, model));
		}
	}

	private static final ModelVisitor<RotatingThrowingWeapon> rotating_weapon_model_visitor = new WhiteModelVisitor<>() {
		@Override
		public void transform(@NonNull ElementRenderState<RotatingThrowingWeapon> render_state) {
			RotatingThrowingWeapon model = render_state.getModel();
			RenderTools.translateAndRotate(render_state.getModel(), render_state.getModelViewStack());
            render_state.getModelViewStack().rotate(model.getAngle(), 0f, 1f, 0f);
		}

        @Override
        public void getTransform(@NonNull ElementRenderState<RotatingThrowingWeapon> render_state, @NonNull Matrix4f dest) {
            RotatingThrowingWeapon model = render_state.getModel();
            float angle = (float) Math.toDegrees(Math.atan2(model.getDirectionY(), model.getDirectionX()));
            dest.translation(model.getPositionX(), model.getPositionY(), model.getPositionZ())
                .rotate(angle, 0f, 0f, 1f)
                .rotate(model.getAngle(), 0f, 1f, 0f);
        }
	};
	@Override
	public void visitRotatingThrowingWeapon(final @NonNull RotatingThrowingWeapon model) {
		if (!picking) {
			addToRenderList(getCachedState(rotating_weapon_model_visitor, model));
		}
	}

	public @NonNull List<@NonNull Emitter> getEmitterQueue() {
		return emitter_queue;
	}

	public @NonNull List<@NonNull Lightning> getLightningQueue() {
		return lightning_queue;
	}
}
