package com.oddlabs.tt.landscape;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.model.ModelToolTip;
import com.oddlabs.tt.model.Supply;
import com.oddlabs.tt.model.ToolTipVisitor;
import com.oddlabs.tt.pathfinder.Occupant;
import com.oddlabs.tt.pathfinder.Region;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.util.StateChecksum;
import com.oddlabs.tt.util.Target;
import org.jspecify.annotations.NonNull;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

public final class TreeSupply extends AbstractTreeGroup implements Supply, Target, Animated, ModelToolTip {
	private final static int INITIAL_SUPPLIES = 10;
	private final static float SECOND_PER_TREEFALL = 3f;

	private static final Vector3f low_detail_x_axis = new Vector3f();
	private static final Vector3f low_detail_translate = new Vector3f();
	private static final Vector3f low_detail_scale = new Vector3f();
	private static final Matrix4f low_detail_matrix = new Matrix4f();

	private final @NonNull Matrix4f matrix;
	private final int tree_type_index;
	private final float x;
	private final float y;
	private final int grid_x;
	private final int grid_y;
	private final int grid_size;
	private final float size;
	private final @NonNull World world;

	private int low_detail_start_vertex_index;
	private int num_supplies = INITIAL_SUPPLIES;
	private float animation_time;
	private boolean hide = false;
	private float scale = 1f;
	private int hit_counter = 0;

	static {
		low_detail_x_axis.set(1f, 0f, 0f);
	}

	public TreeSupply(@NonNull World world, AbstractTreeGroup parent, float x, float y, int grid_x, int grid_y, int grid_size, float size, @NonNull Matrix4f matrix, int tree_type_index, float @NonNull [] vertices) {
		super(parent);
		this.world = world;
		this.x = x;
		this.y = y;
		this.grid_x = grid_x;
		this.grid_y = grid_y;
		this.grid_size = grid_size;
		this.size = size;
		this.tree_type_index = tree_type_index;
		this.matrix = matrix;
		Vector4f src = new Vector4f();
		Vector4f dest = new Vector4f();
		for (int i = 0; i < vertices.length; i += 3) {
			src.set(vertices[i], vertices[i + 1], vertices[i + 2], 1f);
			Matrix4f.transform(matrix, src, dest);
			checkBoundsX(dest.x);
			checkBoundsY(dest.y);
			checkBoundsZ(dest.z);
		}
		if (world.getUnitGrid().getOccupant(grid_x, grid_y) == null)
			occupyTree();
		world.getSupplyManager(getClass()).newSupply();
	}

        @Override
	public @NonNull World getWorld() {
		return world;
	}

	public int getLowDetailStartIndex() {
		return low_detail_start_vertex_index;
	}

        @Override
	public void visit(@NonNull ToolTipVisitor visitor) {
		visitor.visitSupply(this);
	}

	public void setLowDetailStartIndex(int index) {
		low_detail_start_vertex_index = index;
	}

	public float getScale() {
		return scale;
	}

	public float getTreeFallProgress() {
		return animation_time/SECOND_PER_TREEFALL;
	}

        @Override
	public @NonNull Supply respawn() {
		occupyTree();
		hide = false;
		num_supplies = INITIAL_SUPPLIES;
		return this;
	}

        @Override
	public void animateSpawn(float t, float progress) {
		float inv = 1 - progress;
		scale = 1 - inv*inv*inv*inv*inv*inv;

		low_detail_scale.set(scale, scale, scale);
		Matrix4f.scale(low_detail_scale, matrix, low_detail_matrix);
		world.getNotificationListener().updateTreeLowDetail(low_detail_matrix, this);
	}

        @Override
	public void spawnComplete() {
	}

        @Override
	public @NonNull String toString() {
		return "Tree at " + grid_x + " " + grid_y + " isEmpty() " + isEmpty();
	}

        @SuppressWarnings("unchecked")
	private void occupyTree() {
		UnitGrid grid = world.getUnitGrid();
		world.getNotificationListener().registerTarget(this);
		Region region = grid.getRegion(getGridX(), getGridY());
		region.registerObject((Class<TreeSupply>) getClass(), this);
		for (int y = 0; y < grid_size; y++) {
			int occ_y = grid_y + y - (grid_size - 1)/2;
			for (int x = 0; x < grid_size; x++) {
				int occ_x = grid_x + x - (grid_size - 1)/2;
				if (!grid.isGridOccupied(occ_x, occ_y)) {
					assert !(grid.getOccupant(occ_x, occ_y) instanceof TreeSupply): "Trees placed too close";
				}
			}
		}
		grid.occupyGrid(grid_x, grid_y, this);
	}

        @SuppressWarnings("unchecked")
	private void unoccupyTree() {
		UnitGrid grid = world.getUnitGrid();
		world.getNotificationListener().unregisterTarget(this);
		Region region = grid.getRegion(grid_x, grid_y);
		region.unregisterObject((Class<TreeSupply>) getClass(), this);
		grid.freeGrid(grid_x, grid_y, this);
	}

        @Override
	public float getSize() {
		return size;
	}

        @Override
	public float getPositionX() {
		return x;
	}

        @Override
	public float getPositionY() {
		return y;
	}

        @Override
	public int getGridX() {
		return grid_x;
	}

        @Override
	public int getGridY() {
		return grid_y;
	}

        @Override
	public int getPenalty() {
		return Occupant.STATIC;
	}

        @Override
	public boolean isEmpty() {
		return num_supplies == 0;
	}

        @Override
	public boolean hit() {
		hit_counter++;
		if (hit_counter == Supply.HITS_PER_HARVEST) {
			hit_counter = 0;
			decreaseSupply();
			return true;
		}
		return false;
	}

        @Override
	public boolean isDead() {
		return isEmpty();
	}

	private void decreaseSupply() {
		num_supplies --;
		if (isEmpty()) {
			unoccupyTree();
			world.getSupplyManager(getClass()).emptySupply(this);
			world.getAudio().newAudio(new AudioParameters<>(world.getRacesResources().getTreeFallSound()[tree_type_index%2]/* reusing native tree sounds*/, getCX(), getCY(), getCZ(), AudioPlayer.AUDIO_RANK_TREE_FALL, AudioPlayer.AUDIO_DISTANCE_TREE_FALL, AudioPlayer.AUDIO_GAIN_TREE_FALL, AudioPlayer.AUDIO_RADIUS_TREE_FALL));
			world.getAnimationManagerRealTime().registerAnimation(this);
			animation_time = 0f;
		}
	}

	public int getTreeTypeIndex() {
		return tree_type_index;
	}

        @Override
	protected boolean initBounds() {
		super.initBounds();
		return true;
	}

        @Override
	public void animate(float t) {
		animation_time += t;
		float time = getTreeFallProgress();
		low_detail_translate.set(0f, 0f, -13f*(time*time*time*time*time*time));
        matrix.translate(low_detail_translate, low_detail_matrix);
		low_detail_matrix.rotate((.5f*(float)Math.PI)*time*time, low_detail_x_axis);
		world.getNotificationListener().updateTreeLowDetail(low_detail_matrix, this);
		if (animation_time >= SECOND_PER_TREEFALL) {
			world.getAnimationManagerRealTime().removeAnimation(this);
			low_detail_matrix.setZero();
			world.getNotificationListener().updateTreeLowDetail(low_detail_matrix, this);
			hide = true;
		}
	}

	public @NonNull Matrix4f getMatrix() {
		return matrix;
	}

	public boolean isHidden() {
		return hide;
	}

        @Override
	public void updateChecksum(StateChecksum checksum) {}

        @Override
	public void visit(@NonNull TreeNodeVisitor visitor) {
		visitor.visitTree(this);
	}
}
