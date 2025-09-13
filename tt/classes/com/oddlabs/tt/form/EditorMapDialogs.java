package com.oddlabs.tt.form;

import com.oddlabs.tt.editor.EditorColormapReblender;
import com.oddlabs.tt.editor.EditorGridRecalculator;
import com.oddlabs.tt.editor.EditorResourceValidity;
import com.oddlabs.tt.gui.*;
import com.oddlabs.tt.guievent.MouseClickListener;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.mapio.MapIO;
import com.oddlabs.tt.render.DefaultRenderer;
import com.oddlabs.tt.render.LandscapeRenderer;

import java.io.File;
// no collections needed

/**
 * Simple modal dialogs for saving/loading .ttmap files from the editor.
 * Uses existing GUI primitives. Intended to be lightweight and resilient.
 */
public final class EditorMapDialogs {
    private EditorMapDialogs() {}

    // Full regeneration helper: apply + recompute grids + rebuild water + full colormap
    public static void applyAndRegen(World world, LandscapeRenderer lr, DefaultRenderer dr, int terrainType, MapIO.LoadedMap lm) {
        try {
            MapIO.applyToEditorWorld(world, lm, terrainType);
            // If grids weren't provided in file, recompute; otherwise preserve
            boolean hasGrids = (lm.access != null && lm.dock != null && lm.water != null && lm.build != null);
            if (!hasGrids) {
                try { EditorGridRecalculator.recomputeAll(world, terrainType); } catch (Exception ignore) {}
            }
            try { if (dr != null) dr.rebuildWater(); } catch (Exception ignore) {}
            // Rebuild all colormap tiles
            try {
                int n = world.getHeightMap().getGridUnitsPerWorld();
                EditorColormapReblender.reblendROIFromScratch(world, lr, terrainType, 0, 0, n - 1, n - 1);
            } catch (Exception ignore) {}
            // Refresh resource placement validity grid used by editor tools
            try {
                int n = world.getHeightMap().getGridUnitsPerWorld();
                EditorResourceValidity.recomputeROI(world, 0, 0, n - 1, n - 1);
            } catch (Exception ignore) {}
        } catch (Exception t) {
            System.err.println("Map apply failed: " + t.getMessage());
        }
    }

    // Save dialog: filename entry + overwrite confirmation
    public static final class SaveDialog extends Form {
        private final GUIRoot guiRoot;
        private final World world;
        private final int terrainType;
        private final MultiColumnComboBox listBox;
        private java.util.List<MapIO.MapSummary> summaries = java.util.Collections.emptyList();
        private final EditLine nameEdit;
        private final Label errorLabel;
        private final Label metaLabel;

        public SaveDialog(GUIRoot guiRoot, World world, int terrainType) {
            this.guiRoot = guiRoot;
            this.world = world;
            this.terrainType = terrainType;

        Label title = new Label("Save Map", Skin.getSkin().getHeadlineFont());
        addChild(title);
        // Left list
        ColumnInfo[] infos = new ColumnInfo[] {
            new ColumnInfo("Name", 220),
            new ColumnInfo("Size", 90),
            new ColumnInfo("Modified", 200)
        };
    listBox = new MultiColumnComboBox(guiRoot, infos, 300);
        addChild(listBox);
        Label nameLbl = new Label("File name:", Skin.getSkin().getEditFont());
        addChild(nameLbl);
            // Allow letters, numbers, space, underscore and dash in filenames
            final String allowed = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 _-";
            nameEdit = new EditLine(240, 32, allowed, EditLine.LEFT_ALIGNED);
            addChild(nameEdit);
            errorLabel = new Label("", Skin.getSkin().getEditFont());
            addChild(errorLabel);
            errorLabel.setColor(new float[] {1f, 0.3f, 0.3f, 1f});
            metaLabel = new Label("Select a map on the left to prefill.", Skin.getSkin().getEditFont(), 340);
            addChild(metaLabel);

            HorizButton ok = new OKButton(100);
            ok.addMouseClickListener(new MouseClickListener() {
                @Override
                public void mouseClicked(int button, int x, int y, int clicks) { onSave(); }
            });
            addChild(ok);
            HorizButton cancel = new CancelButton(100);
            cancel.addMouseClickListener(new CancelListener(this));
            addChild(cancel);

            title.place();
            listBox.place(title, BOTTOM_LEFT);
            nameLbl.place(listBox, RIGHT_TOP, Skin.getSkin().getFormData().getSectionSpacing());
            nameEdit.place(nameLbl, BOTTOM_LEFT);
            metaLabel.place(nameEdit, BOTTOM_LEFT);
            errorLabel.place(metaLabel, BOTTOM_LEFT);
            cancel.place(errorLabel, BOTTOM_RIGHT);
            ok.place(cancel, LEFT_MID);

            refreshList();
            // Hook selection signals after list is populated
            listBox.addRowListener(new com.oddlabs.tt.guievent.RowListener() {
                @Override
                public void rowDoubleClicked(Object row_context) {
                    if (row_context instanceof MapIO.MapSummary) onRowDoubleClicked((MapIO.MapSummary) row_context);
                }
                @Override
                public void rowChosen(Object row_context) {
                    if (row_context instanceof MapIO.MapSummary) onRowChosen((MapIO.MapSummary) row_context);
                }
            });
            compileCanvas();
            centerPos();
            setFocus();
        }

    @Override
    public void setFocus() { nameEdit.setFocus(); }

        private void onSave() {
            String raw = nameEdit.getContents();
            String name = (raw == null) ? "" : raw.trim();
            if (!MapIO.isValidFilename(name)) {
                errorLabel.set("Invalid name. Use letters, numbers, space, _ or - (max 64).\nAvoid reserved names.");
                return;
            }
            File dir = MapIO.mapsDir();
            File target = new File(dir, name + ".ttmap");
            if (target.exists()) {
                guiRoot.addModalForm(new QuestionForm("Overwrite '" + target.getName() + "'?", new MouseClickListener() {
                    @Override
                    public void mouseClicked(int button, int x, int y, int clicks) { doSave(target); }
                }));
            } else {
                doSave(target);
            }
        }

        private void doSave(File target) {
            try {
                MapIO.saveEditorWorld(world, terrainType, target);
                guiRoot.getInfoPrinter().print("Saved: " + target.getName());
                remove();
            } catch (Exception t) {
                errorLabel.set("Save failed: " + t.getMessage());
            }
        }

        private void refreshList() {
            listBox.clear();
            summaries = MapIO.listMaps();
            for (MapIO.MapSummary s : summaries) {
                String fname = s.file.getName();
                long sizeKB = s.file.length() / 1024;
                String sizeStr = sizeKB + " KB";
                Row row = new Row(
                        new GUIObject[] {
                                new Label(fname, Skin.getSkin().getMultiColumnComboBoxData().getFont(), 220),
                                new Label(sizeStr, Skin.getSkin().getMultiColumnComboBoxData().getFont(), 90),
                                new DateLabel(s.lastModified, Skin.getSkin().getMultiColumnComboBoxData().getFont(), 200)
                        },
                        s);
                listBox.addRow(row);
            }
        }

        private void onRowDoubleClicked(MapIO.MapSummary s) {
            String base = s.file.getName();
            int dot = base.lastIndexOf('.');
            if (dot > 0) base = base.substring(0, dot);
            nameEdit.set(base);
            onSave();
        }

        private void onRowChosen(MapIO.MapSummary s) {
            String base = s.file.getName();
            int dot = base.lastIndexOf('.');
            if (dot > 0) base = base.substring(0, dot);
            nameEdit.set(base);
            StringBuilder sb = new StringBuilder();
            if (s.name != null && !s.name.isEmpty()) sb.append("Name: ").append(s.name).append("\n");
            if (s.author != null && !s.author.isEmpty()) sb.append("Author: ").append(s.author).append("\n");
            if (s.description != null && !s.description.isEmpty()) sb.append("Desc: ").append(s.description).append("\n");
            sb.append("Size: ").append(s.size).append(" gu, Terrain: ").append(s.terrainType);
            metaLabel.set(sb.toString());
        }
    }

    // Load dialog: list *.ttmap with metadata preview; applies full regeneration on confirm
    public static final class LoadDialog extends Form {
        private final GUIRoot guiRoot;
        private final World world;
        private final LandscapeRenderer lr;
        private final DefaultRenderer dr;
        private final int terrainType;

        private final MultiColumnComboBox listBox;
        private java.util.List<MapIO.MapSummary> summaries = java.util.Collections.emptyList();
        private int chosenIndex = -1;

        private final Label metaLabel;

        public LoadDialog(GUIRoot guiRoot, World world, LandscapeRenderer lr, DefaultRenderer dr, int terrainType) {
            this.guiRoot = guiRoot;
            this.world = world;
            this.lr = lr;
            this.dr = dr;
            this.terrainType = terrainType;

            Label title = new Label("Load Map", Skin.getSkin().getHeadlineFont());
            addChild(title);
            ColumnInfo[] infos = new ColumnInfo[] {
                    new ColumnInfo("Name", 220),
                    new ColumnInfo("Size", 90),
                    new ColumnInfo("Modified", 200)
            };
            listBox = new MultiColumnComboBox(guiRoot, infos, 300);
            addChild(listBox);

            // Right panel widgets
            metaLabel = new Label("Select a .ttmap on the left.", Skin.getSkin().getEditFont(), 340);
            addChild(metaLabel);

            HorizButton open = new OKButton(100);
            open.addMouseClickListener(new MouseClickListener() {
                @Override
                public void mouseClicked(int button, int x, int y, int clicks) { onOpen(); }
            });
            addChild(open);
            HorizButton cancel = new CancelButton(100);
            cancel.addMouseClickListener(new CancelListener(this));
            addChild(cancel);

            // Layout
            title.place();
            listBox.place(title, BOTTOM_LEFT);
            metaLabel.place(listBox, RIGHT_TOP, Skin.getSkin().getFormData().getSectionSpacing());
            cancel.place(metaLabel, BOTTOM_RIGHT);
            open.place(cancel, LEFT_MID);

            // Populate list
            refreshList();
            // Hook selection signals after list is populated
            listBox.addRowListener(new com.oddlabs.tt.guievent.RowListener() {
                @Override
                public void rowDoubleClicked(Object row_context) {
                    if (row_context instanceof MapIO.MapSummary) onRowDoubleClicked((MapIO.MapSummary) row_context);
                }
                @Override
                public void rowChosen(Object row_context) {
                    if (row_context instanceof MapIO.MapSummary) onRowChosen((MapIO.MapSummary) row_context);
                }
            });

            compileCanvas();
            centerPos();
        }

        private void refreshList() {
            listBox.clear();
            summaries = MapIO.listMaps();
            for (MapIO.MapSummary s : summaries) {
                String fname = s.file.getName();
                long sizeKB = s.file.length() / 1024;
                String sizeStr = sizeKB + " KB";
                Row row = new Row(
                        new GUIObject[] {
                                new Label(fname, Skin.getSkin().getMultiColumnComboBoxData().getFont(), 220),
                                new Label(sizeStr, Skin.getSkin().getMultiColumnComboBoxData().getFont(), 90),
                                new DateLabel(s.lastModified, Skin.getSkin().getMultiColumnComboBoxData().getFont(), 200)
                        },
                        s);
                listBox.addRow(row);
            }
        }

        private void selectIndex(int idx) {
            this.chosenIndex = idx;
            try {
                MapIO.MapSummary sum = summaries.get(idx);
                StringBuilder sb = new StringBuilder();
                sb.append(sum.file.getName()).append("\n\n");
                if (sum.name != null && !sum.name.isEmpty()) sb.append("Name: ").append(sum.name).append("\n");
                if (sum.author != null && !sum.author.isEmpty()) sb.append("Author: ").append(sum.author).append("\n");
                if (sum.description != null && !sum.description.isEmpty()) sb.append("Desc: ").append(sum.description).append("\n");
                sb.append("Size: ").append(sum.size).append(" gu, Terrain: ").append(sum.terrainType);
                metaLabel.set(sb.toString());
            } catch (Exception t) {
                metaLabel.set("Preview failed: " + t.getMessage());
            }
        }

        private void onOpen() {
            if (chosenIndex < 0 || chosenIndex >= summaries.size()) {
                guiRoot.getInfoPrinter().print("Choose a map first.");
                return;
            }
            File f = summaries.get(chosenIndex).file;
            try {
                MapIO.LoadedMap lm = MapIO.load(f);
                applyAndRegen(world, lr, dr, terrainType, lm);
                guiRoot.getInfoPrinter().print("Loaded: " + f.getName());
                remove();
            } catch (Exception t) {
                metaLabel.set("Load failed: " + t.getMessage());
            }
        }

        private void onRowDoubleClicked(MapIO.MapSummary s) {
            int idx = summaries.indexOf(s);
            if (idx >= 0) { selectIndex(idx); onOpen(); }
        }

        private void onRowChosen(MapIO.MapSummary s) {
            int idx = summaries.indexOf(s);
            if (idx >= 0) selectIndex(idx);
        }
    }
}
