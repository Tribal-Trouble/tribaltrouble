package com.oddlabs.tt.window;

import com.oddlabs.tt.render.SerializableDisplayMode;
import org.jspecify.annotations.NonNull;

public interface Window {
    void create(@NonNull SerializableDisplayMode mode, boolean fullscreen) throws Exception;
    void destroy();
    void update();
    
    boolean isCloseRequested();
    boolean isActive();
    boolean isVisible();
    boolean wasResized();
    
    int getWidth();
    int getHeight();
    
    void setTitle(String title);
    void setVSyncEnabled(boolean enabled);
    void setFullscreen(boolean fullscreen) throws Exception;
    
    @NonNull SerializableDisplayMode[] getAvailableDisplayModes() throws Exception;
    @NonNull SerializableDisplayMode getDisplayMode();
    void setDisplayMode(@NonNull SerializableDisplayMode mode) throws Exception;
    
    void makeCurrent() throws Exception;
}
