package com.oddlabs.tt.form;

import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.gui.EditLine;
import com.oddlabs.tt.gui.GUIObject;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.Panel;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.guievent.MouseClickListener;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.CRC32;

/**
 * A panel that shows a single shareable/importable code representing all keybinds at once.
 *
 * <p>Schemes: - TTKB3 (default, shorter): "TTKB3-" + base64url(binary) + "-" + checksum binary
 * payload: sequence of unsigned 16-bit little-endian ints for each action in canonical order
 * (sorted action names). No names included. Length may be >= number of known actions; extra values
 * are ignored. Missing values default to 0. - TTKB2 (legacy, verbose): "TTKB2-" + base64url(UTF-8
 * text) + "-" + checksum text payload: semi-colon separated pairs sorted by key name:
 * key=code;key=code;... checksum: 6-char base36 of CRC32(payload_bytes) & 0xFFFFFFF (uppercase)
 */
public class KeybindCodePanel extends Panel {
    private final EditLine currentCodeField;
    private final EditLine newCodeField;
    private final Label statusLabel;

    public KeybindCodePanel(com.oddlabs.tt.gui.GUIRoot gui_root, String caption) {
        super(caption);

        Group group = new Group();
        addChild(group);

        Label exportLabel = new Label("All-binds code", Skin.getSkin().getEditFont());
        group.addChild(exportLabel);

        currentCodeField = new EditLine(530, 4096);
        currentCodeField.set(generateCode(Settings.getSettings().getKeybinds()));
        group.addChild(currentCodeField);

        HorizButton regenBtn = new HorizButton("Refresh", 110);
        regenBtn.addMouseClickListener(
                new MouseClickListener() {
                    public void mouseClicked(int button, int x, int y, int clicks) {
                        currentCodeField.set(generateCode(Settings.getSettings().getKeybinds()));
                        setStatus("Refreshed.", true);
                    }
                });
        group.addChild(regenBtn);

        HorizButton copyBtn = new HorizButton("Copy", 90);
        copyBtn.addMouseClickListener(
                new MouseClickListener() {
                    public void mouseClicked(int button, int x, int y, int clicks) {
                        copyToClipboard(currentCodeField.getContents());
                        setStatus("Copied.", true);
                    }
                });
        group.addChild(copyBtn);

        HorizButton copyHintBtn = new HorizButton("Copy hint", 110);
        copyHintBtn.addMouseClickListener(
                new MouseClickListener() {
                    public void mouseClicked(int button, int x, int y, int clicks) {
                        setStatus("Select the code above and press Ctrl+C to copy.", true);
                    }
                });
        group.addChild(copyHintBtn);

        Label importLabel = new Label("Paste new code", Skin.getSkin().getEditFont());
        group.addChild(importLabel);

        newCodeField = new EditLine(530, 4096);
        group.addChild(newCodeField);

        HorizButton applyBtn = new HorizButton("Apply", 110);
        applyBtn.addMouseClickListener(
                new MouseClickListener() {
                    public void mouseClicked(int button, int x, int y, int clicks) {
                        String code = newCodeField.getContents();
                        Map<String, Integer> parsed = parseCode(code);
                        if (parsed == null) {
                            setStatus("Invalid code.", false);
                            return;
                        }
                        // Apply only for known actions
                        HashMap<String, Integer> keybinds = Settings.getSettings().getKeybinds();
                        int applied = 0;
                        for (Map.Entry<String, Integer> e : parsed.entrySet()) {
                            if (keybinds.containsKey(e.getKey())) {
                                Settings.getSettings().setKeybind(e.getKey(), e.getValue());
                                applied++;
                            }
                        }
                        // Persist and refresh code display
                        Settings.getSettings().save();
                        currentCodeField.set(generateCode(Settings.getSettings().getKeybinds()));
                        setStatus("Applied " + applied + " binds.", true);
                    }
                });
        group.addChild(applyBtn);

        HorizButton resetBtn = new HorizButton("Reset to defaults", 170);
        resetBtn.addMouseClickListener(
                new MouseClickListener() {
                    public void mouseClicked(int button, int x, int y, int clicks) {
                        // Reset to built-in defaults
                        Settings.getSettings().resetKeybindsToDefaults();
                        Settings.getSettings().save();
                        String code = generateCode(Settings.getSettings().getKeybinds());
                        currentCodeField.set(code);
                        copyToClipboard(code);
                        setStatus("Reset to defaults. Copied.", true);
                    }
                });
        group.addChild(resetBtn);

        statusLabel = new Label("", Skin.getSkin().getEditFont());
        group.addChild(statusLabel);

        // Layout
        exportLabel.place();
        currentCodeField.place(exportLabel, GUIObject.BOTTOM_LEFT);
        regenBtn.place(currentCodeField, GUIObject.BOTTOM_LEFT);
        copyBtn.place(regenBtn, GUIObject.RIGHT_MID);
        copyHintBtn.place(copyBtn, GUIObject.RIGHT_MID);
        importLabel.place(regenBtn, GUIObject.BOTTOM_LEFT);
        newCodeField.place(importLabel, GUIObject.BOTTOM_LEFT);
        applyBtn.place(newCodeField, GUIObject.BOTTOM_LEFT);
        resetBtn.place(applyBtn, GUIObject.RIGHT_MID);
        statusLabel.place(applyBtn, GUIObject.BOTTOM_LEFT);

        group.compileCanvas();
        group.place();
        compileCanvas();
    }

    private static void copyToClipboard(String text) {
        try {
            java.awt.datatransfer.Clipboard cb =
                    java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
            cb.setContents(new java.awt.datatransfer.StringSelection(text), null);
        } catch (Throwable t) {
            // ignore if clipboard not available
        }
    }

    private void setStatus(String msg, boolean ok) {
        statusLabel.set(msg);
        if (ok) statusLabel.setColor(new float[] {0.298f, 0.686f, 0.314f, 1});
        else statusLabel.setColor(new float[] {0.9f, 0.2f, 0.2f, 1});
    }

    @Override
    public void onActivated() {
        // Refresh the displayed export code to match current settings
        currentCodeField.set(generateCode(Settings.getSettings().getKeybinds()));
        setStatus("", true);
    }

    public static String generateCode(HashMap<String, Integer> binds) {
        // Default to shorter scheme (TTKB3), but keep legacy generator available
        return generateCodeTTKB3(binds);
    }

    private static List<String> canonicalKeys(HashMap<String, Integer> binds) {
        List<String> keys = new ArrayList<String>(binds.keySet());
        Collections.sort(keys, String.CASE_INSENSITIVE_ORDER);
        return keys;
    }

    private static String generateCodeTTKB3(HashMap<String, Integer> binds) {
        List<String> keys = canonicalKeys(binds);
        byte[] payload = new byte[keys.size() * 2];
        int idx = 0;
        for (int i = 0; i < keys.size(); i++) {
            String k = keys.get(i);
            int v = 0;
            Integer vi = binds.get(k);
            if (vi != null) v = vi.intValue();
            // unsigned short little-endian
            payload[idx++] = (byte) (v & 0xFF);
            payload[idx++] = (byte) ((v >>> 8) & 0xFF);
        }
        String b64 = Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
        String chk = crcBase36(payload);
        return "TTKB3-" + b64 + '-' + chk;
    }

    private static String generateCodeTTKB2(HashMap<String, Integer> binds) {
        // Legacy verbose text format
        List<String> keys = canonicalKeys(binds);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            String k = keys.get(i);
            Integer v = binds.get(k);
            if (v == null) continue;
            if (sb.length() > 0) sb.append(';');
            String safeKey = k.replace(';', '_').replace('=', '_');
            sb.append(safeKey).append('=').append(v.intValue());
        }
        byte[] payload = sb.toString().getBytes(StandardCharsets.UTF_8);
        String b64 = Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
        String chk = crcBase36(payload);
        return "TTKB2-" + b64 + '-' + chk;
    }

    public static Map<String, Integer> parseCode(String code) {
        if (code == null) return null;
        code = code.trim();
        String upper = code.toUpperCase(Locale.ROOT);
        if (upper.startsWith("TTKB3-")) {
            return parseTTKB3(code);
        } else if (upper.startsWith("TTKB2-")) {
            return parseTTKB2(code);
        }
        return null;
    }

    private static Map<String, Integer> parseTTKB2(String code) {
        int lastDash = code.lastIndexOf('-');
        if (lastDash <= 6) return null;
        String b64 = code.substring(6, lastDash);
        String chk = code.substring(lastDash + 1);
        byte[] payload;
        try {
            payload = Base64.getUrlDecoder().decode(b64);
        } catch (IllegalArgumentException ex) {
            return null;
        }
        if (!crcBase36(payload).equalsIgnoreCase(chk)) return null;
        String body = new String(payload, StandardCharsets.UTF_8);
        HashMap<String, Integer> out = new HashMap<String, Integer>();
        if (body.length() == 0) return out;
        String[] pairs = body.split(";");
        for (int i = 0; i < pairs.length; i++) {
            String p = pairs[i];
            int eq = p.indexOf('=');
            if (eq <= 0) continue;
            String k = p.substring(0, eq);
            String v = p.substring(eq + 1);
            try {
                int codeInt = Integer.parseInt(v);
                out.put(k, Integer.valueOf(codeInt));
            } catch (NumberFormatException ex) {
                // skip
            }
        }
        return out;
    }

    private static Map<String, Integer> parseTTKB3(String code) {
        int lastDash = code.lastIndexOf('-');
        if (lastDash <= 6) return null;
        String b64 = code.substring(6, lastDash);
        String chk = code.substring(lastDash + 1);
        byte[] payload;
        try {
            payload = Base64.getUrlDecoder().decode(b64);
        } catch (IllegalArgumentException ex) {
            return null;
        }
        if (!crcBase36(payload).equalsIgnoreCase(chk)) return null;

        HashMap<String, Integer> current = Settings.getSettings().getKeybinds();
        List<String> keys = new ArrayList<String>(current.keySet());
        Collections.sort(keys, String.CASE_INSENSITIVE_ORDER);
        int pairs = Math.min(keys.size(), payload.length / 2);
        HashMap<String, Integer> out = new HashMap<String, Integer>();
        int idx = 0;
        for (int i = 0; i < pairs; i++) {
            int lo = payload[idx++] & 0xFF;
            int hi = payload[idx++] & 0xFF;
            int v = (hi << 8) | lo;
            out.put(keys.get(i), Integer.valueOf(v));
        }
        return out;
    }

    private static String crcBase36(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data);
        long val = crc.getValue() & 0x0FFFFFFFL; // 28 bits keeps it short
        String s = Long.toString(val, 36).toUpperCase(Locale.ROOT);
        // pad to 6 chars for fixed width
        if (s.length() < 6) {
            StringBuilder sb = new StringBuilder(6);
            for (int i = s.length(); i < 6; i++) sb.append('0');
            sb.append(s);
            s = sb.toString();
        }
        return s;
    }
}
