package com.oddlabs.tt.window;

import com.oddlabs.tt.render.SerializableDisplayMode;
import org.jspecify.annotations.NonNull;

public interface Window extends AutoCloseable {
    void create(@NonNull SerializableDisplayMode mode, boolean fullscreen);

    void close();

    void update();

    void pollEvents();

    boolean isOpen();

    boolean isCloseRequested();

    void setCloseRequested(boolean value);

    boolean isActive();

    boolean isVisible();

    boolean isIconified();

    boolean wasResized();

    /**
     * Returns the physical framebuffer width in pixels. This is the size used for
     * OpenGL viewport and buffer allocations.
     *
     * @return int width in pixels
     */
    int getWidth();

    /**
     * Returns the physical framebuffer height in pixels.
     *
     * @return int height in pixels
     */
    int getHeight();

    /**
     * Returns the logical window width in screen coordinates.
     * Screen coordinates are used for window positioning and cursor input.
     * On high-DPI displays (e.g. Retina), this may be smaller than the framebuffer width.
     *
     * @return int width in screen coordinates
     */
    int getLogicalWidth();

    /**
     * Returns the logical window height in screen coordinates.
     *
     * @return int height in screen coordinates
     */
    int getLogicalHeight();

    void setTitle(String title);

    void setVSyncEnabled(boolean enabled);

    void setFullscreen(boolean fullscreen) throws Exception;

    @NonNull SerializableDisplayMode @NonNull [] getAvailableDisplayModes();

    @NonNull SerializableDisplayMode getDisplayMode();

    void setDisplayMode(@NonNull SerializableDisplayMode mode) throws Exception;

    void setIcon(java.nio.file.Path imagePath);

    void restore();

    void minimize();

    void show();

    void focus();

    void makeCurrent() throws Exception;

    boolean isFullscreen();

    /**
     * Returns the physical size of the monitor in millimeters.
     *
     * @return int array [widthMM, heightMM]
     */
    int[] getMonitorPhysicalSize();

    /**
     * Returns the content scale of the monitor.
     *
     * @return float array [xScale, yScale]
     */
    float[] getMonitorContentScale();

    /**
     * Returns the content scale of the window.
     *
     * @return float array [xScale, yScale]
     */
    float[] getWindowContentScale();
}
