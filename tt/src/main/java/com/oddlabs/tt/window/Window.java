package com.oddlabs.tt.window;

import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.render.SerializableDisplayMode;
import org.jspecify.annotations.NonNull;

public interface Window extends AutoCloseable {
    void create(@NonNull SerializableDisplayMode mode, boolean fullscreen) throws Exception;
    void close();
    void update();
    void pollEvents();
    
    boolean isCloseRequested();
    boolean isActive();
    boolean isVisible();
    boolean isIconified();
    boolean wasResized();
    
    int getWidth();
    int getHeight();
    
    void setTitle(String title);
    void setVSyncEnabled(boolean enabled);
    void setFullscreen(boolean fullscreen) throws Exception;
    
    @NonNull SerializableDisplayMode @NonNull[] getAvailableDisplayModes();
    @NonNull SerializableDisplayMode getDisplayMode();
    void setDisplayMode(@NonNull SerializableDisplayMode mode) throws Exception;
    
    void setIcon(java.nio.file.Path imagePath);
    void restore();
    void minimize();
    void show();
    void focus();
    void makeCurrent() throws Exception;
    
    boolean isFullscreen();

    default float getViewAspect() {
        return (float) getWidth() / getHeight();
    }

    default float getUnitsPerPixel() {
        return (float) (Globals.VIEW_MIN * Math.tan(Globals.FOV * (Math.PI / 180.0f) * 0.5d) / (getHeight() * 0.5d));
    }

    default float getErrorConstant() {
        return Globals.VIEW_MIN / (getUnitsPerPixel() * Globals.ERROR_TOLERANCE);
    }
}
