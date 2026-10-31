package com.oddlabs.tt.camera;

import com.oddlabs.tt.resource.FogInfo;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.util.StateChecksum;
import com.oddlabs.util.Color;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;

public final class CameraState {
    static final float MIN_ANGLE = -(float) Math.PI / 2f;//+ 0.01f;
    //  private static final float MAX_ANGLE = (float)Math.PI/2f;// - 0.0001f;
    static final float MAX_ANGLE = -0.0001f;
    static final float MAX_ANGLE_UNLOCKED = (float) Math.PI / 6f;
    private static final float HORIZON_EPSILON = 0.01f;

    private final Matrix4f modl = new Matrix4f();
    private final Matrix4f proj = new Matrix4f();
    private final Matrix4f proj_modl = new Matrix4f();
    private final float[][] frustum = new float[6][4];
    private final Vector3f f = new Vector3f();
    private final Vector3f s = new Vector3f();
    private final Vector3f u = new Vector3f();

    private float target_camera_x;
    private float target_camera_y;
    private float target_camera_z;
    private float target_vert_angle;
    private float target_horiz_angle;

    private float camera_x;
    private float camera_y;
    private float camera_z;
    private float vert_angle;
    private float horiz_angle;
    private float max_vert_angle = MAX_ANGLE;

    private @NonNull FogInfo fog;
    private boolean no_detail_mode;
    private int width;
    private int height;

    public CameraState() {
        this(new FogInfo(FogInfo.Mode.NONE, Color.BLACK, 0f));
    }

    public CameraState(@NonNull FogInfo fog) {
        this.fog = fog;
    }

    public void setFog(@NonNull FogInfo fog) {
        this.fog = fog;
    }

    public void updateChecksum(@NonNull StateChecksum checksum) {
        //System.out.println("camera_x = " + camera_x + " | camera_y = " + camera_y + " | camera_z = " + camera_z + " | dir_x = " + dir_x + " | dir_y = " + dir_y + " | dir_z = " + dir_z);
        checksum.update(camera_x);
        checksum.update(camera_y);
        checksum.update(camera_z);
        checksum.update(vert_angle);
        checksum.update(horiz_angle);
    }

    public float getTargetX() {
        return target_camera_x;
    }

    public float getCurrentX() {
        return camera_x;
    }

    void setTargetX(float x) {
        target_camera_x = x;
    }

    void setCurrentX(float x) {
        camera_x = target_camera_x = x;
    }

    public float getTargetY() {
        return target_camera_y;
    }

    public float getCurrentY() {
        return camera_y;
    }

    void setTargetY(float y) {
        target_camera_y = y;
    }

    void setCurrentY(float y) {
        camera_y = target_camera_y = y;
    }

    public float getTargetZ() {
        return target_camera_z;
    }

    public float getCurrentZ() {
        return camera_z;
    }

    public @NonNull FogInfo getFog() {
        return fog;
    }

    public void snapToTarget() {
        camera_x = target_camera_x;
        camera_y = target_camera_y;
        camera_z = target_camera_z;
        vert_angle = target_vert_angle;
        horiz_angle = target_horiz_angle;
    }

    public void animate(float delta_t, float smoothness_factor) {
        camera_x = animateValue(delta_t, camera_x, target_camera_x, smoothness_factor);
        camera_y = animateValue(delta_t, camera_y, target_camera_y, smoothness_factor);
        camera_z = animateValue(delta_t, camera_z, target_camera_z, smoothness_factor);
        vert_angle = animateValue(delta_t, vert_angle, target_vert_angle, smoothness_factor);
        horiz_angle = animateValue(delta_t, horiz_angle, target_horiz_angle, smoothness_factor);
    }

    private float animateValue(float delta_t, float current, float target, float smoothness_factor) {
        return current + (target - current) * Math.min(delta_t * smoothness_factor, 1f);
    }

    void setTargetZ(float z) {
        target_camera_z = z;
    }

    void setCurrentZ(float z) {
        camera_z = target_camera_z = z;
    }

    void setTargetVertAngle(float angle) {
        target_vert_angle = capVertAngle(angle);
    }

    public boolean inNoDetailMode() {
        return no_detail_mode;
    }

    void setNoDetailMode(boolean s) {
        no_detail_mode = s;
    }

    public void set(@NonNull CameraState camera) {
        max_vert_angle = camera.max_vert_angle;
        setTargetX(camera.getTargetX());
        setTargetY(camera.getTargetY());
        setTargetZ(camera.getTargetZ());
        setTargetVertAngle(camera.getTargetVertAngle());
        setTargetHorizAngle(camera.getTargetHorizAngle());
        setNoDetailMode(camera.inNoDetailMode());
        setFog(camera.getFog());
        camera_x = camera.camera_x;
        camera_y = camera.camera_y;
        camera_z = camera.camera_z;
        vert_angle = camera.vert_angle;
        horiz_angle = camera.horiz_angle;
        modl.set(camera.modl);
        proj.set(camera.proj);
        proj_modl.set(camera.proj_modl);
        for (int i = 0; i < frustum.length; i++) {
            System.arraycopy(camera.frustum[i], 0, frustum[i], 0, frustum[i].length);
        }
        f.set(camera.f);
        s.set(camera.s);
        u.set(camera.u);
        width = camera.width;
        height = camera.height;
    }

    void setCurrentVertAngle(float angle) {
        vert_angle = target_vert_angle = capVertAngle(angle);
    }

    private float capVertAngle(float angle) {
        if (angle < MIN_ANGLE)
            return MIN_ANGLE;
        else return Math.min(angle, max_vert_angle);
    }

    void setMaxVertAngle(float angle) {
        max_vert_angle = Math.max(MIN_ANGLE, Math.min(angle, MAX_ANGLE_UNLOCKED));
        target_vert_angle = capVertAngle(target_vert_angle);
    }

    public float getTargetVertAngle() {
        return target_vert_angle;
    }

    public float getCurrentVertAngle() {
        return vert_angle;
    }

    void setTargetHorizAngle(float angle) {
        target_horiz_angle = angle;
    }

    void setCurrentHorizAngle(float angle) {
        horiz_angle = target_horiz_angle = angle;
    }

    public float getTargetHorizAngle() {
        return target_horiz_angle;
    }

    public float getHorizAngle() {
        return horiz_angle;
    }

    public @NonNull CameraState reflectCamera(float seaLevel) {
        CameraState m = new CameraState(fog);
        m.width = width;
        m.height = height;
        m.no_detail_mode = no_detail_mode;
        m.camera_x = camera_x;
        m.target_camera_x = target_camera_x;
        m.camera_y = camera_y;
        m.target_camera_y = target_camera_y;
        m.camera_z = camera_z - 2 * (camera_z - seaLevel);
        m.target_camera_z = target_camera_z - 2 * (target_camera_z - seaLevel);
        m.vert_angle = -vert_angle;
        m.target_vert_angle = -target_vert_angle;
        m.horiz_angle = horiz_angle;
        m.target_horiz_angle = target_horiz_angle;
        m.doSetView(m.camera_x, m.camera_y, m.camera_z, m.horiz_angle, m.vert_angle, new Matrix4f(proj));
        return m;
    }

    private Vector3f solveForZ(Vector3f p0, Vector3f p1, float z) {
        float dz = p1.z() - p0.z();
        float t = (z - p0.z()) / dz;
        return new Vector3f(
                p0.x() + t * (p1.x() - p0.x()),
                p0.y() + t * (p1.y() - p0.y()),
                z);
    }

    public CameraState sunCamera(Vector3f lightDirection, float minZ, float maxZ, float size, float seaLevel, int width,
            int height) {
        CameraState m = new CameraState();
        m.width = width;
        m.height = height;

        assert (lightDirection.y() < 0.0f);
        assert (lightDirection.x() == 0.0f);

        // The camera is the sun (or the moon).
        // It's located South at X=0 and Y=-1
        // The Y range is always from 0 to the max Y the player's camera can see.
        // The X range is bound by the min and max X the player's camera can see.
        // The Z range is always the minimum and maximum height in the map.

        float halfFov = (float) Math.toRadians(Globals.FOV) * 0.5f;
        var maxVertAngle = vert_angle + halfFov;
        boolean belowHorizon = Math.sin(maxVertAngle) < 0.0f;

        float upperPtRatio = belowHorizon ? 1.0f : (float) (Math.tan(-vert_angle) / Math.tan(
                halfFov)) - HORIZON_EPSILON;

        float maxX = size;
        float minX = 0.0f;
        float maxY = size;

        if (upperPtRatio > -1.0f) {

            Matrix4f invMat = new Matrix4f();
            getProjectionModelView().invert(invMat);

            Vector3f lowerLeft0 = new Vector3f();
            Vector3f lowerLeft1 = new Vector3f();
            Vector3f lowerRight0 = new Vector3f();
            Vector3f lowerRight1 = new Vector3f();
            Vector3f upperLeft0 = new Vector3f();
            Vector3f upperLeft1 = new Vector3f();
            Vector3f upperRight0 = new Vector3f();
            Vector3f upperRight1 = new Vector3f();

            invMat.transformProject(-1.0f, -1.0f, -1.0f, lowerLeft0);
            invMat.transformProject(-1.0f, -1.0f, +1.0f, lowerLeft1);
            invMat.transformProject(+1.0f, -1.0f, -1.0f, lowerRight0);
            invMat.transformProject(+1.0f, -1.0f, +1.0f, lowerRight1);
            invMat.transformProject(-1.0f, upperPtRatio, -1.0f, upperLeft0);
            invMat.transformProject(-1.0f, upperPtRatio, +1.0f, upperLeft1);
            invMat.transformProject(+1.0f, upperPtRatio, -1.0f, upperRight0);
            invMat.transformProject(+1.0f, upperPtRatio, +1.0f, upperRight1);

            Vector3f[] pts = new Vector3f[8];
            pts[0] = solveForZ(lowerLeft0, lowerLeft1, seaLevel);
            pts[1] = solveForZ(lowerRight0, lowerRight1, seaLevel);
            pts[2] = solveForZ(upperLeft0, upperLeft1, seaLevel);
            pts[3] = solveForZ(upperRight0, upperRight1, seaLevel);
            pts[4] = solveForZ(lowerLeft0, lowerLeft1, maxZ);
            pts[5] = solveForZ(lowerRight0, lowerRight1, maxZ);
            pts[6] = solveForZ(upperLeft0, upperLeft1, maxZ);
            pts[7] = solveForZ(upperRight0, upperRight1, maxZ);

            maxX = pts[0].x();
            minX = pts[0].x();
            maxY = pts[0].y();
            for (int i = 1; i < pts.length; i++) {
                maxX = Math.max(maxX, pts[i].x());
                minX = Math.min(minX, pts[i].x());
                maxY = Math.max(maxY, pts[i].y());
            }

            minX = (float) Math.floor(minX / 32.0f) * 32.0f;
            maxX = (float) Math.ceil(maxX / 32.0f) * 32.0f;
            maxY = (float) Math.ceil(maxY / 32.0f) * 32.0f;
            maxX = Math.clamp(maxX, 0.0f, size);
            minX = Math.clamp(minX, 0.0f, size);
            maxY = Math.clamp(maxY, 0.0f, size);
        }

        m.camera_x = m.target_camera_x = (maxX + minX) * 0.5f;
        m.camera_y = m.target_camera_y = 0.0f;
        m.camera_z = m.target_camera_z = maxZ;

        float sin = lightDirection.z();
        float lightAngle = (float) Math.asin(sin);
        float cos = (float) Math.cos(lightAngle);

        m.horiz_angle = m.target_horiz_angle = (float) Math.PI * 0.5f;
        m.vert_angle = m.target_vert_angle = -lightAngle;

        float rangeZ = maxZ - minZ;
        float opp = sin * rangeZ;
        float lowerY = (float) Math.sqrt(rangeZ * rangeZ - opp * opp);
        float upperY = sin * maxY;
        float halfRangeX = (maxX - minX) * 0.5f;
        float far = maxY / cos;
        Matrix4f ortho = new Matrix4f().ortho(-halfRangeX, halfRangeX, -lowerY, upperY, +0.0f, far);
        m.doSetView(m.camera_x, m.camera_y, m.camera_z, m.horiz_angle, m.vert_angle, ortho);

        return m;
    }

    public void setCamera(float x, float y, float z, float va, float ha) {
        setCurrentX(x);
        setCurrentY(y);
        setCurrentZ(z);
        setCurrentVertAngle(va);
        setCurrentHorizAngle(ha);
    }

    public void updateDirectionAndNormal(@NonNull Vector3f f, @NonNull Vector3f u, @NonNull Vector3f s) {
        updateDirectionAndNormal(horiz_angle, vert_angle, f, u, s);
    }

    private static final Vector3f tmp = new Vector3f();
    private static final Vector3f tmp2 = new Vector3f();

    private static void updateDirectionAndNormal(float hangle, float vangle, @NonNull Vector3f f, @NonNull Vector3f u,
            @NonNull Vector3f s) {
        float radius = (float) Math.cos(vangle);
        float dir_x = (float) Math.cos(hangle);
        float dir_y = (float) Math.sin(hangle);
        float dir_z = (float) Math.sin(vangle);
        f.set(dir_x * radius, dir_y * radius, dir_z);
        tmp.set(-dir_y, dir_x, 0);
        f.cross(tmp, tmp2);
        f.cross(tmp2, s);
        s.normalize();
        s.cross(f, u);
        u.normalize();
    }

    public void setTargetView(@NonNull Matrix4f proj) {
        doSetView(target_camera_x, target_camera_y, target_camera_z, target_horiz_angle, target_vert_angle, proj);
    }

    public void setView(@NonNull Matrix4f proj, int width, int height) {
        this.width = width;
        this.height = height;
        doSetView(camera_x, camera_y, camera_z, horiz_angle, vert_angle, proj);
    }

    public @NonNull Matrix4f getModelView() {
        return modl;
    }

    public @NonNull Matrix4f getProjectionMatrix() {
        return proj;
    }

    public @NonNull Matrix4f getProjectionModelView() {
        return proj_modl;
    }

    public float[][] getFrustum() {
        return frustum;
    }

    private void doSetView(float cx, float cy, float cz, float hangle, float vangle, @NonNull Matrix4f proj) {
        updateDirectionAndNormal(hangle, vangle, f, u, s);
        // Eye position: (cx, cy, cz)
        // Center position: (cx + f.x, cy + f.y, cz + f.z) - looking along the forward vector
        // Up vector: (u.x, u.y, u.z) - the calculated up direction
        modl.identity().lookAt(cx, cy, cz,
                cx + f.x, cy + f.y, cz + f.z,
                u.x, u.y, u.z);

        this.proj.set(proj);
        proj.mul(modl, proj_modl);
        findFrustumPlanes(proj_modl);
    }

    public void findFrustumPlanes(@NonNull Matrix4f matrix) {
        /* Extract the numbers for the RIGHT plane */
        frustum[0][0] = matrix.m03() - matrix.m00();
        frustum[0][1] = matrix.m13() - matrix.m10();
        frustum[0][2] = matrix.m23() - matrix.m20();
        frustum[0][3] = matrix.m33() - matrix.m30();

        /* Extract the numbers for the LEFT plane */
        frustum[1][0] = matrix.m03() + matrix.m00();
        frustum[1][1] = matrix.m13() + matrix.m10();
        frustum[1][2] = matrix.m23() + matrix.m20();
        frustum[1][3] = matrix.m33() + matrix.m30();

        /* Extract the BOTTOM plane */
        frustum[2][0] = matrix.m03() + matrix.m01();
        frustum[2][1] = matrix.m13() + matrix.m11();
        frustum[2][2] = matrix.m23() + matrix.m21();
        frustum[2][3] = matrix.m33() + matrix.m31();

        /* Extract the TOP plane */
        frustum[3][0] = matrix.m03() - matrix.m01();
        frustum[3][1] = matrix.m13() - matrix.m11();
        frustum[3][2] = matrix.m23() - matrix.m21();
        frustum[3][3] = matrix.m33() - matrix.m31();

        /* Extract the FAR plane */
        frustum[4][0] = matrix.m03() - matrix.m02();
        frustum[4][1] = matrix.m13() - matrix.m12();
        frustum[4][2] = matrix.m23() - matrix.m22();
        frustum[4][3] = matrix.m33() - matrix.m32();

        /* Extract the NEAR plane */
        frustum[5][0] = matrix.m03() + matrix.m02();
        frustum[5][1] = matrix.m13() + matrix.m12();
        frustum[5][2] = matrix.m23() + matrix.m22();
        frustum[5][3] = matrix.m33() + matrix.m32();

        for (float[] frustum1 : frustum) {
            /* Normalize the result */
            float length_inv = 1f / ((float) java.lang.Math.sqrt(
                    frustum1[0] * frustum1[0] + frustum1[1] * frustum1[1] + frustum1[2] * frustum1[2]));
            frustum1[0] *= length_inv;
            frustum1[1] *= length_inv;
            frustum1[2] *= length_inv;
            frustum1[3] *= length_inv;
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
