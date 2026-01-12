package com.oddlabs.tt.camera;

import com.oddlabs.tt.delegate.SelectionDelegate;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.gui.KeyboardEvent;
import com.oddlabs.tt.input.Key;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.util.Target;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class GameCamera extends Camera {
    public static final int SCROLL_BUFFER = 5;
    private static final float INIT_DISTANCE = 50;
    private static final float ANGLE_DELTA = (float)(Math.PI/2);
    public static final float MAX_Z = 100f;
    private static final float GROUND_CLEARANCE = 1.0f;
    private static final float ZOOM_Z_DIR_MIN = -(float)Math.tan(Math.PI/6);
    private static final float SCROLL_ACCELERATION_SECONDS_MAX = 1f;
    private static final float SCROLL_ACCELERATION_FACTOR = 2.5f;
    private static final float SCROLL_START_MAX_SPEED = 60f;
    private static final float ROTATE_PICKING_ANGLE_MAX = (-(Globals.FOV) - 10)*((float)Math.PI/180)*.5f;
    private static final float ZOOM_SPEED = 50f;

    private final @NonNull WorldViewer viewer;

    private float left_dir_x;
    private float left_dir_y;
    private float scroll_x;
    private float scroll_y;
    private float scrolling_x;
    private float scrolling_y;
    private float scroll_acceleration_seconds;
    private float scroll_start_speed;
    private boolean scroll_start;
    private float zoom_time;
    private final float default_rotate_radius;
    private float last_zoom_factor;

    private @Nullable Target rotation_point = null;
    private SelectionDelegate owner;

    private boolean pitch_up;
    private boolean pitch_down;
    private boolean rotate_left;
    private boolean rotate_right;

    public GameCamera(@NonNull WorldViewer viewer, @NonNull CameraState camera) {
            super(viewer.getWorld().getHeightMap(), camera);
            this.default_rotate_radius = viewer.getWorld().getHeightMap().getMetersPerWorld()/4f;
            this.viewer = viewer;
            checkPosition();
            updateDirection();
    }

    public void setOwner(SelectionDelegate owner) {
        this.owner = owner;
    }

    public float getScrollX() {
        return scrolling_x;
    }

    public float getScrollY() {
        return scrolling_y;
    }

    public void resetLastZoomFactor() {
        last_zoom_factor = 0f;
    }

    public float getLastZoomFactor() {
        return last_zoom_factor;
    }

    public boolean pitchUp() {
        return pitch_up;
    }

    public boolean pitchDown() {
        return pitch_down;
    }

    public boolean rotateRight() {
        return rotate_right;
    }

    public boolean rotateLeft() {
        return rotate_left;
    }

/*
float radius = (float)Math.cos(old_vert_angle);
float old_dir_x = (float)Math.cos(getHorizAngle())*radius;
float old_dir_y = (float)Math.sin(getHorizAngle())*radius;
float old_dir_z = (float)Math.sin(old_vert_angle);
old_x = x - old_dir_x*distance_to_landscape;
old_y = y - old_dir_y*distance_to_landscape;
old_z = World.getHeightMap().getNearestHeight(x, y) - old_dir_z*distance_to_landscape;

*/
    public void reset() {
    }

    public void reset(float x, float y) {
        float dx = x - .5f*getHeightMap().getMetersPerWorld();
        float dy = y - .5f*getHeightMap().getMetersPerWorld();
        float r = (float)Math.sqrt(dx*dx + dy*dy);
        if (dy > 0) {
                getState().setCurrentHorizAngle((float)(Math.PI + Math.acos(dx/r)));
        } else {
                getState().setCurrentHorizAngle(-(float)(Math.PI + Math.acos(dx/r)));
        }
//              setHorizAngle(-(float)Math.PI/2f);
        getState().setCurrentVertAngle(-45f*(float)Math.PI/180f);

        setPos(x, y);

        zoom_time = 0f;
        updateDirection();
    }

    public void setPos(float x, float y) {
            float radius = (float)Math.cos(getState().getTargetVertAngle());
            float dir_x = (float)Math.cos(getState().getTargetHorizAngle())*radius;
            float dir_y = (float)Math.sin(getState().getTargetHorizAngle())*radius;
            float dir_z = (float)Math.sin(getState().getTargetVertAngle());
            getState().setCurrentX(x - dir_x*INIT_DISTANCE);
            getState().setCurrentY(y - dir_y*INIT_DISTANCE);
            getState().setCurrentZ(getHeightMap().getNearestHeight(x, y) - dir_z*INIT_DISTANCE);
            checkPosition();
    }

    private void updateDirection() {
            left_dir_x = -(float)Math.sin(getState().getTargetHorizAngle());
            left_dir_y = (float)Math.cos(getState().getTargetHorizAngle());
    }

    private void doZoom(float time_delta) {
        zoom(zoom_time*time_delta*ZOOM_SPEED*getState().getTargetZ());
        if (zoom_time < 0f)
                zoom_time = Math.min(0f, zoom_time + time_delta);
        else if (zoom_time > 0f)
                zoom_time = Math.max(0f, zoom_time - time_delta);
    }

    public void zoom(float zoom_factor) {
        if (zoom_factor != 0f) {
                last_zoom_factor = zoom_factor;
                float radius = (float)Math.cos(getState().getTargetVertAngle());
                float dir_x = (float)Math.cos(getState().getTargetHorizAngle())*radius;
                float dir_y = (float)Math.sin(getState().getTargetHorizAngle())*radius;
                float dir_z = (float)Math.sin(getState().getTargetVertAngle());
                if (dir_z > ZOOM_Z_DIR_MIN) {
                        dir_z = ZOOM_Z_DIR_MIN;
                        float inv_length = 1/(float)Math.sqrt(dir_x*dir_x + dir_y*dir_y + dir_z*dir_z);
                        dir_x *= inv_length;
                        dir_y *= inv_length;
                        dir_z *= inv_length;
                }
                float temp_x = getState().getTargetX() + dir_x*zoom_factor;
                float temp_y = getState().getTargetY() + dir_y*zoom_factor;
                float temp_z = getState().getTargetZ() + dir_z*zoom_factor;
                
                float min_z_level = getHeightMap().getSeaLevelMeters() + GROUND_CLEARANCE;
                temp_z = Math.max(temp_z, min_z_level);
                float backup_x = getState().getTargetX();
                float backup_y = getState().getTargetY();
                float backup_z = getState().getTargetZ();

                int mid = getHeightMap().getMetersPerWorld()/2;
                float dx = (temp_x - mid);
                float dy = (temp_y - mid);
                float squared_dist = dx*dx + dy*dy;
                if (squared_dist < getHeightMap().getMetersPerWorld()*getHeightMap().getMetersPerWorld() && temp_z < MAX_Z) {
                        getState().setTargetX(temp_x);
                        getState().setTargetY(temp_y);
                        getState().setTargetZ(temp_z);
                        if (bounce(getState().getTargetX(), getState().getTargetY(), getState().getTargetZ(), viewer.getGUIRoot().getWidth(), viewer.getGUIRoot().getHeight())) {
                                getState().setTargetX(backup_x);
                                getState().setTargetY(backup_y);
                                getState().setTargetZ(backup_z);
                        }
                        checkPosition();
                }
        }
    }

    private void doScroll(float time_delta) {
        if (!viewer.getGUIRoot().getDelegate().canScroll())
                return;
        var localInput = Renderer.getLocalInput();
        float scroll_speed = scroll_start_speed*(.4f + (scroll_acceleration_seconds/SCROLL_ACCELERATION_SECONDS_MAX)*SCROLL_ACCELERATION_FACTOR);
        float scroll_factor = time_delta*scroll_speed;
        boolean blocked = viewer.getGUIRoot().getDelegate().keyboardBlocked();
        boolean alt_down = isAltDown(localInput);
        if (localInput.isKeyDown(Key.LEFT) && !localInput.isKeyDown(Key.RIGHT) && !blocked && !alt_down)
                scrolling_x = -1f;
        else if (localInput.isKeyDown(Key.RIGHT) && !localInput.isKeyDown(Key.LEFT) && !blocked && !alt_down)
                scrolling_x = 1f;
        else
                scrolling_x = scroll_x;

        if (localInput.isKeyDown(Key.DOWN) && !localInput.isKeyDown(Key.UP) && !blocked && !alt_down)
                scrolling_y = -1f;
        else if (localInput.isKeyDown(Key.UP) && !localInput.isKeyDown(Key.DOWN) && !blocked && !alt_down)
                scrolling_y = 1f;
        else
                scrolling_y = scroll_y;

        float new_x = getState().getTargetX() - (scrolling_x*left_dir_x + scrolling_y*-left_dir_y)*scroll_factor;
        float new_y = getState().getTargetY() - (scrolling_x*left_dir_y + scrolling_y*left_dir_x)*scroll_factor;
        if (new_x != getState().getTargetX() || new_y != getState().getTargetY()) {
                getState().setTargetX(new_x);
                getState().setTargetY(new_y);
                checkPosition();
        }

        scroll_acceleration_seconds += time_delta;
        if (scroll_acceleration_seconds > SCROLL_ACCELERATION_SECONDS_MAX)
                scroll_acceleration_seconds = SCROLL_ACCELERATION_SECONDS_MAX;
    }

    private void doPitch(float time_delta) {
        checkKeys();
        if ((pitch_down && !Settings.getSettings().invert_camera_pitch) ||
                (pitch_up && Settings.getSettings().invert_camera_pitch)) {
                getState().setTargetVertAngle(getState().getTargetVertAngle() - time_delta*ANGLE_DELTA);
                checkPosition();
        }
        if ((pitch_up && !Settings.getSettings().invert_camera_pitch) ||
                (pitch_down && Settings.getSettings().invert_camera_pitch)) {
                getState().setTargetVertAngle(getState().getTargetVertAngle() + time_delta*ANGLE_DELTA);
                checkPosition();
        }
    }

    private void doRotate(float time_delta) {
        checkKeys();
        if (rotate_left || rotate_right) {
                float dx;
                float dy;
                float da;

                float[] point = getRotationPoint();
                if (insideWorld(point[0], point[1])) {
                        dx = getState().getTargetX() - point[0];
                        dy = getState().getTargetY() - point[1];
                } else {
                        dx = -left_dir_y*default_rotate_radius;
                        dy = left_dir_x*default_rotate_radius;
                }

                if (rotate_left) {
                        da = -time_delta*ANGLE_DELTA;
                } else {
                        da = time_delta*ANGLE_DELTA;
                }
                getState().setTargetHorizAngle(getState().getTargetHorizAngle() + da);
                getState().setTargetX(getState().getTargetX() - dx + (float)(dx*Math.cos(da) - dy*Math.sin(da)));
                getState().setTargetY(getState().getTargetY() - dy + (float)(dx*Math.sin(da) + dy*Math.cos(da)));
                checkPosition();
        }
    }

    public int getRotateY() {
        int center_y = viewer.getGUIRoot().getHeight()/2;
        if (getState().getTargetVertAngle() < ROTATE_PICKING_ANGLE_MAX) {
                return center_y;
        } else {
                float da = getState().getTargetVertAngle() - ROTATE_PICKING_ANGLE_MAX;
                // float pixels_per_unit = 1f/GUIRoot.getUnitsPerPixel(Globals.VIEW_MIN);
                // int pixels_to_screen = (int)(Globals.VIEW_MIN*pixels_per_unit);
                // int dy = (int)(((float)Math.tan(da))*pixels_to_screen);
                int dy = (int)(Math.tan(da) * Globals.VIEW_MIN);
                int y = center_y - dy;
                return y;
        }
    }

    private boolean insideWorld(float x, float y) {
        return x > 0 && x < getHeightMap().getMetersPerWorld() && y > 0 && y < getHeightMap().getMetersPerWorld();
    }

    @Override
    public void doAnimate(float t) {
        doZoom(t);
        doScroll(t);
        doPitch(t);
        doRotate(t);
        updateDirection();
        getState().setFog(viewer.getWorld().getFog());
    }

    @Override
    public void mouseScrolled(int amount) {
        zoom_time += amount*.05f;
        if (zoom_time > .15f)
                zoom_time = .15f;
        else if (zoom_time < -.15f)
                zoom_time = -.15f;
    }

    public void setRotationPoint(Target target) {
        rotation_point = target;
    }

    float[] getRotationPoint() {
        float[] point = new float[2];
        if (rotation_point != null) {
                point[0] = rotation_point.getPositionX();
                point[1] = rotation_point.getPositionY();
        } else {
                point[0] = getState().getTargetX();
                point[1] = getState().getTargetY();
        }
        return point;
    }

    @Override
    public void mouseMoved(int x, int y) {
        int view_width = viewer.getGUIRoot().getWidth();
        int view_height = viewer.getGUIRoot().getHeight();
        if ((owner == null || !owner.isSelecting()) && (x < SCROLL_BUFFER || y < SCROLL_BUFFER ||
                        x > view_width - 1 - SCROLL_BUFFER || y > view_height - 1 - SCROLL_BUFFER)) {
                if (scroll_start) {
                        scroll_start = false;
                        if (!scrollSpeedLocked(null)) {
                                scroll_acceleration_seconds = 0;
                                setScrollSpeed();
                        }
                }
                scroll_x = (x - view_width/2f);
                scroll_y = (y - view_height/2f);
                float inv_length = 1f/(float)Math.sqrt(scroll_x*scroll_x + scroll_y*scroll_y);
                scroll_x *= inv_length;
                scroll_y *= inv_length;
        } else {
                scroll_start = true;
                scroll_x = 0;
                scroll_y = 0;
        }
    }

    private boolean scrollSpeedLocked(@Nullable Key key) {
        var localInput = Renderer.getLocalInput();
        return scroll_x != 0
                || scroll_y != 0
                || (localInput.isKeyDown(Key.UP) && key != Key.UP)
                || (localInput.isKeyDown(Key.DOWN) && key != Key.DOWN)
                || (localInput.isKeyDown(Key.LEFT) && key != Key.LEFT)
                || (localInput.isKeyDown(Key.RIGHT) && key != Key.RIGHT);
    }

    private void setScrollSpeed() {
        viewer.getPicker().pickRotate(this);
        float[] landscape_point = getRotationPoint();
        float landscape_z = getHeightMap().getNearestHeight(landscape_point[0], landscape_point[1]);
        float dx = landscape_point[0] - getState().getTargetX();
        float dy = landscape_point[1] - getState().getTargetY();
        float dz = landscape_z - getState().getTargetZ();
        scroll_start_speed = Math.min((float)Math.sqrt(dx*dx + dy*dy + dz*dz), SCROLL_START_MAX_SPEED);
    }

    public @NonNull World getWorld() {
        return viewer.getWorld();
    }

    @Override
    public boolean keyPressed(@NonNull KeyboardEvent event) {
        switch (event.keyCode()) {
            case HOME, NUMPAD8 -> {
				return true;
            }
            case END, NUMPAD2 -> {
				return true;
            }
            case INSERT, NUMPAD6 -> {
				viewer.getPicker().pickRotate(this);
				return true;
			}
            case DELETE, NUMPAD4 -> {
				viewer.getPicker().pickRotate(this);
				return true;
			}
            case PAGE_UP, NUMPAD9 -> {
				mouseScrolled(-2);
				return true;
			}
            case PAGE_DOWN, NUMPAD3 -> {
				mouseScrolled(2);
				return true;
			}
            case UP -> {
                if (!scrollSpeedLocked(Key.UP)) {
                    scroll_acceleration_seconds = 0;
                    setScrollSpeed();
                }
				return true;
            }
            case DOWN -> {
                if (!scrollSpeedLocked(Key.DOWN)) {
                    scroll_acceleration_seconds = 0;
                    setScrollSpeed();
                }
				return true;
            }
            case LEFT -> {
                if (!scrollSpeedLocked(Key.LEFT)) {
                    scroll_acceleration_seconds = 0;
                    setScrollSpeed();
                }
				return true;
            }
            case RIGHT -> {
                if (!scrollSpeedLocked(Key.RIGHT)) {
                    scroll_acceleration_seconds = 0;
                    setScrollSpeed();
                }
				return true;
            }
        }
		return false;
    }

    private boolean isAltDown(com.oddlabs.tt.gui.LocalInput localInput) {
        return localInput.isKeyDown(Key.LALT) || localInput.isKeyDown(Key.RALT);
    }

    private void checkKeys() {
        if (viewer.getGUIRoot().getDelegate().keyboardBlocked() || viewer.getGUIRoot().getModalDelegate() != null) {
                pitch_up = false;
                pitch_down = false;
                rotate_right = false;
                rotate_left = false;
                return;
        }

        var localInput = Renderer.getLocalInput();
        boolean alt_down = isAltDown(localInput);

        pitch_up = localInput.isKeyDown(Key.HOME) || localInput.isKeyDown(Key.NUMPAD8)
                || (alt_down && localInput.isKeyDown(Key.UP));
        pitch_down = localInput.isKeyDown(Key.END) || localInput.isKeyDown(Key.NUMPAD2)
                || (alt_down && localInput.isKeyDown(Key.DOWN));
        rotate_right = localInput.isKeyDown(Key.INSERT) || localInput.isKeyDown(Key.NUMPAD6)
                || (alt_down && localInput.isKeyDown(Key.RIGHT));
        rotate_left = localInput.isKeyDown(Key.DELETE) || localInput.isKeyDown(Key.NUMPAD4)
                || (alt_down && localInput.isKeyDown(Key.LEFT));
    }

    @Override
    public void enable() {
        super.enable();
        var localInput = Renderer.getLocalInput();
        float scale = viewer.getGUIRoot().getGlobalScale();
        mouseMoved(Math.round(localInput.getMouseX() / scale), Math.round(localInput.getMouseY() / scale));
    }
}
