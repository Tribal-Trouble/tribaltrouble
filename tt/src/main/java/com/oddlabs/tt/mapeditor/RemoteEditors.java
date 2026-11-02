package com.oddlabs.tt.mapeditor;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.gui.GUIObject;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.render.state.RenderContext;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The other players in a shared session as this player sees them: each one's camera where it is, with their name over
 * it, their brush on the ground and a fading outline round each edit of theirs, all in the player's colour. A list of
 * everyone in the session, this player too, stands at the left below the toolbar, with how to chat with them.
 */
final class RemoteEditors implements AutoCloseable {
    /** How fast (1/seconds) a shown camera closes on where its player last said it was. */
    private static final float EASE_RATE = 12f;
    private static final float FLASH_SECONDS = 1.5f;
    private static final int MAX_FLASHES = 32;
    /** Meters above a camera its name stands, before it is scaled up with distance like the camera. */
    private static final float NAME_RISE = 2.4f;
    private static final int NAME_WIDTH = 220;
    private static final int ROSTER_MARGIN = 10;

    private static final class Remote {
        final @NonNull String nick;
        final @NonNull Vector4fc color;
        final @NonNull Label label;
        EditorSession.@Nullable Presence presence;
        // Where the camera is shown, easing towards the presence.
        float x, y, z, horiz, vert;
        boolean placed;
        boolean label_shown;

        Remote(@NonNull String nick, @NonNull Vector4fc color) {
            this.nick = nick;
            this.color = color;
            this.label = new Label(nick, Skin.getSkin().getEditFont(), NAME_WIDTH,
                    Origin.AT_MIDDLE);
            label.setColor(color);
        }
    }

    /** The outline of an edit, in meters, as it fades. */
    private static final class Flash {
        final @NonNull Vector4fc color;
        final float x0, y0, x1, y1;
        float age;

        Flash(@NonNull Vector4fc color, float x0, float y0, float x1, float y1) {
            this.color = color;
            this.x0 = x0;
            this.y0 = y0;
            this.x1 = x1;
            this.y1 = y1;
        }
    }

    private final @NonNull GUIObject owner;
    private final int size;
    private final Map<Integer, Remote> remotes = new TreeMap<>();
    private final Map<Integer, String> everyone = new TreeMap<>();
    private final List<Flash> flashes = new ArrayList<>();
    private final List<Label> roster = new ArrayList<>();
    private final @NonNull CameraModel model = new CameraModel();
    private final @NonNull Vector4f projected = new Vector4f();
    private int own_slot = -1;
    // Where the top of the list of everyone goes, or below the top of the screen when not given.
    private int roster_top = -1;
    private boolean closed;

    /**
     * @param owner where the names and the list go
     * @param size  the island's size in cells
     */
    RemoteEditors(@NonNull GUIObject owner, int size) {
        this.owner = owner;
        this.size = size;
    }

    /** A player's colour, by their slot in the session: the game's team colours, in turn. */
    static @NonNull Vector4fc colorOf(int slot) {
        Vector4f[] colours = Settings.getSettings().team_colours;
        return colours[Math.floorMod(slot, colours.length)];
    }

    void memberJoined(int slot, @NonNull String nick, boolean self) {
        everyone.put(slot, nick);
        if (self)
            own_slot = slot;
        else if (!remotes.containsKey(slot))
            remotes.put(slot, new Remote(nick, colorOf(slot)));
        layoutRoster();
    }

    void memberLeft(int slot) {
        everyone.remove(slot);
        Remote remote = remotes.remove(slot);
        if (remote != null && remote.label_shown)
            remote.label.remove();
        layoutRoster();
    }

    /** Forgets everyone, as when the session ended. */
    void clear() {
        for (Remote remote : remotes.values())
            if (remote.label_shown)
                remote.label.remove();
        remotes.clear();
        everyone.clear();
        flashes.clear();
        own_slot = -1;
        layoutRoster();
    }

    void presence(int slot, EditorSession.@NonNull Presence presence) {
        Remote remote = remotes.get(slot);
        if (remote == null)
            return;
        remote.presence = presence;
        if (!remote.placed) {
            remote.placed = true;
            remote.x = presence.x();
            remote.y = presence.y();
            remote.z = presence.z();
            remote.horiz = presence.horiz_angle();
            remote.vert = presence.vert_angle();
        }
    }

    /** Outlines what an edit of another player's changed. */
    void edited(int slot, @NonNull EditOp op) {
        Remote remote = remotes.get(slot);
        int[] bounds = op.bounds(size);
        if (remote == null || bounds[0] > bounds[2])
            return;
        float m = HeightMap.METERS_PER_UNIT_GRID;
        if (flashes.size() >= MAX_FLASHES)
            flashes.removeFirst();
        flashes.add(new Flash(remote.color, bounds[0] * m, bounds[1] * m, bounds[2] * m, bounds[3] * m));
    }

    void tick(float t) {
        float ease = 1f - (float) Math.exp(-EASE_RATE * t);
        for (Remote remote : remotes.values()) {
            EditorSession.Presence p = remote.presence;
            if (p == null)
                continue;
            remote.x += (p.x() - remote.x) * ease;
            remote.y += (p.y() - remote.y) * ease;
            remote.z += (p.z() - remote.z) * ease;
            remote.vert += (p.vert_angle() - remote.vert) * ease;
            // The short way round.
            float turn = (float) Math.IEEEremainder(p.horiz_angle() - remote.horiz, 2 * Math.PI);
            remote.horiz += turn * ease;
        }
        for (Iterator<Flash> it = flashes.iterator(); it.hasNext();) {
            Flash flash = it.next();
            flash.age += t;
            if (flash.age >= FLASH_SECONDS)
                it.remove();
        }
    }

    /** Draws the others' brushes and the outlines of their edits on the ground. */
    void renderGround(BrushRenderer.@NonNull Batch batch) {
        for (Remote remote : remotes.values()) {
            EditorSession.Presence p = remote.presence;
            if (p == null || !p.hasCursor() || p.inMapMode())
                continue;
            Vector4fc c = remote.color;
            float alpha = p.strokeSign() != 0 ? .95f : .55f;
            batch.circle(p.cursor_x(), p.cursor_y(), p.radius(), c.x(), c.y(), c.z(), alpha);
            batch.dot(p.cursor_x(), p.cursor_y(), c.x(), c.y(), c.z(), alpha);
        }
        for (Flash flash : flashes) {
            Vector4fc c = flash.color;
            float alpha = .9f * (1f - flash.age / FLASH_SECONDS);
            batch.rectangle(flash.x0, flash.y0, flash.x1, flash.y1, c.x(), c.y(), c.z(), alpha);
        }
    }

    /** Draws the others' cameras, and moves their names over them. */
    void renderCameras(@NonNull RenderContext context, @NonNull CameraState state) {
        if (closed)
            return;
        float eye_x = state.getCurrentX();
        float eye_y = state.getCurrentY();
        float eye_z = state.getCurrentZ();
        boolean any = false;
        for (Remote remote : remotes.values())
            any |= remote.presence != null;
        if (any) {
            try (CameraModel.Batch cameras = model.begin(context)) {
                for (Remote remote : remotes.values()) {
                    if (remote.presence == null)
                        continue;
                    float distance = distance(eye_x, eye_y, eye_z, remote);
                    cameras.draw(remote.x, remote.y, remote.z, remote.horiz, remote.vert, remote.color, distance);
                }
            }
        }
        placeNames(state);
    }

    private static float distance(float eye_x, float eye_y, float eye_z, @NonNull Remote remote) {
        float dx = remote.x - eye_x, dy = remote.y - eye_y, dz = remote.z - eye_z;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** Puts each name just over its camera on the screen, hiding those behind the viewer. */
    private void placeNames(@NonNull CameraState state) {
        float eye_x = state.getCurrentX();
        float eye_y = state.getCurrentY();
        float eye_z = state.getCurrentZ();
        int width = owner.getWidth();
        int height = owner.getHeight();
        for (Remote remote : remotes.values()) {
            boolean show = false;
            if (remote.presence != null) {
                float rise = NAME_RISE * (1f + distance(eye_x, eye_y, eye_z, remote) * .015f);
                projected.set(remote.x, remote.y, remote.z + rise, 1f);
                state.getProjectionModelView().transform(projected);
                if (projected.w > 0f) {
                    float sx = (projected.x / projected.w * .5f + .5f) * width;
                    float sy = (projected.y / projected.w * .5f + .5f) * height;
                    show = sx > -NAME_WIDTH && sx < width + NAME_WIDTH && sy > -50 && sy < height + 50;
                    if (show)
                        remote.label.setPos(Math.round(sx) - remote.label.getWidth() / 2, Math.round(sy));
                }
            }
            if (show != remote.label_shown) {
                remote.label_shown = show;
                if (show)
                    owner.addChild(remote.label);
                else
                    remote.label.remove();
            }
        }
    }

    /** Moves the list of everyone so its top is at the given height. */
    void setRosterTop(int top) {
        roster_top = top;
        layoutRoster();
    }

    /** The list of everyone in the session, at the left, this player marked. */
    void layoutRoster() {
        for (Label label : roster)
            label.remove();
        roster.clear();
        if (everyone.isEmpty())
            return;
        var font = Skin.getSkin().getEditFont();
        roster.add(new Label(MapEditor.i18n("session_roster"), font));
        for (Map.Entry<Integer, String> member : everyone.entrySet()) {
            String nick = member.getValue();
            String text = member.getKey() == own_slot ? MapEditor.i18n("session_you", nick) : nick;
            roster.add(new Label(text, font).setColor(colorOf(member.getKey())));
        }
        roster.add(new Label(MapEditor.i18n("session_chat_hint"), font));
        int y = roster_top >= 0 ? roster_top : owner.getHeight() - ROSTER_MARGIN;
        for (Label label : roster) {
            y -= label.getHeight();
            owner.addChild(label);
            label.setPos(ROSTER_MARGIN, y);
        }
    }

    /** Takes the names and the list off the screen. */
    void hideLabels() {
        for (Label label : roster)
            label.remove();
        for (Remote remote : remotes.values()) {
            if (remote.label_shown) {
                remote.label_shown = false;
                remote.label.remove();
            }
        }
    }

    boolean isEmpty() {
        return everyone.isEmpty();
    }

    @Override
    public void close() {
        closed = true;
        model.close();
    }
}
