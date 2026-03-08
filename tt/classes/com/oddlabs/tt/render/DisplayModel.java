package com.oddlabs.tt.render;

import com.oddlabs.tt.global.Settings;

import org.lwjgl.glfw.GLFWVidMode;

import java.util.ArrayList;
import java.util.List;

public final class DisplayModel {
    private static GLFWVidMode[] available_resolutions = Display.getVidModes();
    ;
    private static DisplayModelItem curr_resolution;
    private static Boolean fullscreen;

    // Check to see if resolution was changed at boot
    private static Boolean bad_mode = false;

    public static void init() {
        fullscreen = Settings.getSettings().fullscreen;

        curr_resolution = new DisplayModelItem();

        int status = checkCurrentResolutionAvailability();

        switch (status) {
            // Refreshrate not available
            case 0:
                int refreshRate = getBestRefreshrate(curr_resolution);
                curr_resolution.setRefreshRate(refreshRate);
                break;

            // Resolution not available
            case -1:
                curr_resolution = getNativeMode();
                break;

            // Return, no changes needed
            default:
                return;
        }
        // Set bad resolution variable if not first run
        if (!isFirstRun()) {
            bad_mode = true;
        }
        // Apply to config
        saveToConfig();
    }

    public static void saveToConfig() {
        Settings.getSettings().fullscreen = (boolean) fullscreen;
        curr_resolution.setToConfig();
        Settings.getSettings().save();
    }

    public static void setFullscreen(Boolean bool) {
        fullscreen = bool;
    }

    public static void setCurrentResolution(DisplayModelItem new_resolution) {
        curr_resolution = new_resolution;
    }

    public static DisplayModelItem getCurrentResolution() {
        return curr_resolution;
    }

    public static boolean inFullscreen() {
        return fullscreen;
    }

    public static DisplayModelItem[] getUniqueResolutions() {
        List<DisplayModelItem> items = getResolutionModes();
        List<DisplayModelItem> new_items = new ArrayList<DisplayModelItem>();

        for (int i = 0; i < items.size(); i++) {
            DisplayModelItem new_item = items.get(i);
            new_item.setRefreshRate(-1);
            if (!new_items.contains(new_item)) {
                new_items.add(new_item);
            }
        }

        return new_items.toArray(new DisplayModelItem[new_items.size()]);
    }

    public static int[] getRefreshRates(DisplayModelItem resolution) {
        List<DisplayModelItem> items = getResolutionModes(resolution);
        int[] refreshRates = new int[items.size()];

        if (refreshRates.length < 1) {
            throw new RuntimeException(
                    "getRefreshRates found zero refreshrates on this resolution");
        }

        for (int i = 0; items.size() > i; i++) {
            refreshRates[i] = items.get(i).refreshRate();
        }

        return refreshRates;
    }

    public static int[] getRefreshRates() {
        return getRefreshRates(curr_resolution);
    }

    public static List<DisplayModelItem> getResolutionModes(DisplayModelItem resolution) {
        List<DisplayModelItem> items = new ArrayList<DisplayModelItem>();

        for (int i = 0; i < available_resolutions.length; i++) {
            if (resolution.width() == available_resolutions[i].width()
                    && resolution.height() == available_resolutions[i].height())
                items.add(new DisplayModelItem(available_resolutions[i]));
        }

        return items;
    }

    public static List<DisplayModelItem> getResolutionModes() {
        List<DisplayModelItem> items = new ArrayList<DisplayModelItem>();

        for (int i = 0; i < available_resolutions.length; i++) {
            items.add(new DisplayModelItem(available_resolutions[i]));
        }

        return items;
    }

    public static Boolean getBadModeStatus() {
        return bad_mode;
    }

    public static void setBadModeStatus(Boolean _bad_mode) {
        bad_mode = _bad_mode;
    }

    /*
     * Checks if current resolution and refresh rate is available
     * Returns:
     * 1 All match
     * 0 Refresh rate doesn't match
     * -1 Width or height doesn't match
     */
    private static int checkCurrentResolutionAvailability() {
        List<DisplayModelItem> items = getResolutionModes(curr_resolution);

        // If no resolutions found, return -1
        if (items.size() < 1) {
            return -1;
        }

        // Check if refresh rate matches
        for (int i = 0; i < items.size(); i++) {
            if (curr_resolution.refreshRate() == items.get(i).refreshRate()) return 1;
        }

        return 0;
    }

    private static int getBestRefreshrate(DisplayModelItem resolution) {
        List<DisplayModelItem> items = getResolutionModes(resolution);

        if (items.size() < 1) {
            throw new RuntimeException(
                    "(selectBestRefreshrate) Couldn't get resolutions for current resolution:\n"
                            + "Width: "
                            + resolution.width()
                            + "\nHeight: "
                            + resolution.height());
        }

        int max_refreshRate = -1;

        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).refreshRate() > max_refreshRate) {
                max_refreshRate = items.get(i).refreshRate();
            }
        }

        return max_refreshRate;
    }

    private static DisplayModelItem getNativeMode() {
        int max_width = -1;
        int max_height = -1;

        for (int i = 0; i < available_resolutions.length; i++) {
            if (available_resolutions[i].width() > max_width) {
                max_width = available_resolutions[i].width();
            }
            if (available_resolutions[i].height() > max_height) {
                max_height = available_resolutions[i].height();
            }
        }

        DisplayModelItem new_mode = new DisplayModelItem(max_width, max_height, -1);
        int max_refreshRate = getBestRefreshrate(new_mode);

        new_mode.setRefreshRate(max_refreshRate);
        return new_mode;
    }

    private static boolean isFirstRun() {
        return Settings.getSettings().first_run;
    }
}
