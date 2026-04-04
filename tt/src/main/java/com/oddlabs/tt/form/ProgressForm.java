package com.oddlabs.tt.form;

import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.audio.AudioManager;
import com.oddlabs.tt.camera.NullCamera;
import com.oddlabs.tt.delegate.CameraDelegate;
import com.oddlabs.tt.delegate.NullDelegate;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.gui.Fadable;
import com.oddlabs.tt.gui.GUI;
import com.oddlabs.tt.gui.GUIImage;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.LabelBox;
import com.oddlabs.tt.gui.ProgressBar;
import com.oddlabs.tt.gui.ProgressBarInfo;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.render.UIRenderer;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Random;
import java.util.ResourceBundle;
import java.util.stream.IntStream;

public final class ProgressForm {
    private static final int PROGRESSBAR_LOADINGTIP_SPACING = 45;
    private static final int NUM_TIPS = 39;
    private static final String TIP_PREFIX = "tip";
    private static final ResourceBundle bundle = ResourceBundle.getBundle(ProgressForm.class.getName());

    private static @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull ... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private static final String[] LOADING_TIPS = IntStream.range(0, NUM_TIPS)
            .mapToObj(idx -> i18n(TIP_PREFIX + idx))
            .toArray(String[]::new);

    private static @Nullable ProgressForm current_progress = null;

    private final @NonNull ProgressBar progress_bar;
    private final @NonNull GUI gui;

    public static void setProgressForm(@NonNull NetworkSelector network, @NonNull GUI gui, @NonNull LoadCallback callback) {
        setProgressForm(network, gui, callback, false);
    }

    public static @Nullable Runnable setProgressForm(@NonNull NetworkSelector network, final @NonNull GUI gui, final @NonNull LoadCallback callback, final boolean first_progress) {
        String texture;
        int texture_width;
        int texture_height;
        int image_width;
        int image_height;
        int progress_x;
        int progress_y;
        int progress_width;
        boolean show_tip;

        if (first_progress) {
            texture = "/textures/gui/oddlabs";
            texture_width = 1024;
            texture_height = 1024;
            image_width = 800;
            image_height = 600;
            progress_x = 320;
            progress_y = 145;
            progress_width = 200;
            show_tip = false;
        } else {
            texture = "/textures/gui/startup";
            texture_width = 1024;
            texture_height = 1024;
            image_width = 800;
            image_height = 600;
            progress_x = 250;
            progress_y = 145;
            progress_width = 300;
            show_tip = true;
        }

        Fadable load_fadable = () -> callback(gui, callback, first_progress);
        current_progress = new ProgressForm(network, gui, load_fadable, first_progress, new ProgressBarInfo[]{new ProgressBarInfo(""/*"Loading landscape resources"*/, 10),
                new ProgressBarInfo(""/*"Loading races resources"*/, 30),
                new ProgressBarInfo(""/*"Generating textures"*/, 5),
                new ProgressBarInfo(""/*"Generating terrain"*/, 5),
                new ProgressBarInfo(""/*"Generating alpha maps"*/, 5),
                new ProgressBarInfo(""/*"Blending textures"*/, 2f),
                new ProgressBarInfo(""/*"Generating pathfinding grids"*/, 5),
                new ProgressBarInfo(""/*"Generating quadtrees"*/, 6)},
                texture, texture_width, texture_height, image_width, image_height, progress_x, progress_y, progress_width, show_tip);

        return first_progress ? load_fadable::fadingDone : null;
    }

    private ProgressForm(@NonNull NetworkSelector network, final @NonNull GUI gui, final Fadable load_fadable, boolean first_progress, ProgressBarInfo @NonNull [] info, @NonNull String texture_name, int texture_width, int texture_height, int image_width, int image_height, int progress_x, int progress_y, int progress_width, boolean show_tip) {
        this.gui = gui;
        AudioManager.getManager().stopSources();
        var gui_root = first_progress ? gui.getGUIRoot() : gui.newFade(load_fadable, null);
        CameraDelegate<NullCamera> delegate = new NullDelegate(gui_root, false);
        gui_root.pushDelegate(delegate);

        int screen_width = gui_root.getWidth();
        int screen_height = gui_root.getHeight();
        progress_width = (int) (progress_width * (float) screen_width / image_width);
        progress_x = (int) (progress_x * (float) screen_width / image_width);
        progress_y = (int) (progress_y * (float) screen_height / image_height);

        GUIImage image = new GUIImage(screen_width, screen_height, 0f, 0f, (float) image_width / texture_width, (float) image_height / texture_height, texture_name);
        image.setPos(0, 0);

        progress_bar = new ProgressBar(network, progress_width, info, false);
        progress_y -= progress_bar.getHeight();
        progress_bar.setPos(progress_x, progress_y);
        delegate.addChild(image);
        delegate.addChild(progress_bar);
        if (show_tip) {
            Random random = new Random(LocalEventQueue.getQueue().getHighPrecisionManager().getTick());
            CharSequence tip_string = LOADING_TIPS[random.nextInt(LOADING_TIPS.length)];
            int tip_width = Math.min(gui_root.getWidth() - 10, Skin.getSkin().getEditFont().getWidth(tip_string));
            LabelBox tip = new LabelBox(tip_string, Skin.getSkin().getEditFont(), tip_width);
//			Label tip = new Label(LOADING_TIPS[random.nextInt(LOADING_TIPS.length)], Skin.getSkin().getEditFont());
            tip.setPos(progress_bar.getX() + progress_bar.getWidth() / 2 - tip.getWidth() / 2, progress_bar.getY() - tip.getHeight() - PROGRESSBAR_LOADINGTIP_SPACING);
            delegate.addChild(tip);
        }

        // Force an initial render to show the progress screen immediately
        Renderer.getRenderer().updateProgress(gui);
    }

    private static void callback(@NonNull GUI gui, @NonNull LoadCallback callback, boolean first_progress) {
        Fadable start_sources_fadable = () -> AudioManager.getManager().startSources();

        GUIRoot client_root = gui.createRoot();
        UIRenderer renderer = callback.load(client_root);
        gui.newFade(start_sources_fadable, client_root, renderer);
    }

    public static void progress() {
        if (null != current_progress) {
            current_progress.progress_bar.progress();
            Renderer.getRenderer().updateProgress(current_progress.gui);
        }
    }

    public static void progress(float step) {
        if (null != current_progress) {
            current_progress.progress_bar.progress(step);
            Renderer.getRenderer().updateProgress(current_progress.gui);
        }
    }
}
