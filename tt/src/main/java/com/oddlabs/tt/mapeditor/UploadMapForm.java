package com.oddlabs.tt.mapeditor;

import com.oddlabs.matchmaking.SharedMap;
import com.oddlabs.tt.form.MessageForm;
import com.oddlabs.tt.gui.CancelButton;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.Skin;
import org.jspecify.annotations.NonNull;

import java.nio.file.Path;
import java.util.function.Consumer;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.BOTTOM_MID;

/** Shows how an upload to the server goes, and hands over the shared map once the server has it. */
final class UploadMapForm extends Form implements SharedMaps.UploadListener {
    private static final int WIDTH = 320;

    private final @NonNull GUIRoot gui_root;
    private final @NonNull Label label_status;
    private final @NonNull Consumer<@NonNull SharedMap> done;
    private boolean finished;

    private UploadMapForm(@NonNull GUIRoot gui_root, @NonNull String name,
            @NonNull Consumer<@NonNull SharedMap> done) {
        this.gui_root = gui_root;
        this.done = done;
        Label label_headline = new Label(MapEditor.i18n("shared_uploading", name), Skin.getSkin().getHeadlineFont(),
                WIDTH);
        label_status = new Label(MapEditor.i18n("shared_progress", 0), Skin.getSkin().getEditFont(), WIDTH);
        HorizButton button_cancel = new CancelButton(120);
        button_cancel.addMouseClickListener((_, _, _, _) -> cancel());
        addChild(label_headline);
        addChild(label_status);
        addChild(button_cancel);
        label_headline.place();
        label_status.place(label_headline, BOTTOM_LEFT);
        button_cancel.place(label_status, BOTTOM_MID);
        compileCanvas();
        centerPos();
    }

    /** Uploads a saved map, showing how it goes, and hands over the shared map once the server has it. */
    static void upload(@NonNull GUIRoot gui_root, @NonNull Path path, @NonNull String name,
            @NonNull Consumer<@NonNull SharedMap> done) {
        UploadMapForm form = new UploadMapForm(gui_root, name, done);
        gui_root.addModalForm(form);
        SharedMaps.get().upload(path, name, form);
    }

    @Override
    public void progress(float fraction) {
        label_status.set(MapEditor.i18n("shared_progress", Math.round(fraction * 100)));
    }

    @Override
    public void uploaded(@NonNull SharedMap map) {
        finished = true;
        remove();
        done.accept(map);
    }

    @Override
    public void failed(@NonNull String reason) {
        finished = true;
        remove();
        gui_root.addModalForm(new MessageForm(MapEditor.i18n("shared_upload_failed", reason)));
    }

    @Override
    protected void doCancel() {
        if (!finished)
            SharedMaps.get().cancelUpload(this);
    }
}
