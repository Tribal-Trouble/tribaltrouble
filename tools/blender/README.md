# Tribal Trouble Blender Plugin

Official Blender import/export addon for Tribal Trouble geometry. Maintained in this repo; the game and the plugin version together.

## Install

1. Blender 4.x: Edit > Preferences > Add-ons > Install...
2. Pick `io_tribaltrouble.py`, then enable "Tribal Trouble Mesh (.xml)".

## Use

- **Import**: File > Import > Tribal Trouble Mesh (.xml). Point it at any mesh under `assets/geometry/`; shift-select several files to import them all at once, one object each. UVs, vertex colors, and bone weights (as vertex groups) come along. When importing from inside the repo, the matching atlas PNG from `assets/textures/models/` is found automatically and a material is built for it ("Load Textures" option, on by default). If a texture appears vertically flipped, re-import with "Flip V" checked.
- **Export**: File > Export > Tribal Trouble Mesh (.xml). Exports every selected mesh object, merged in world space into one mesh (the scene is untouched), triangulated, with corner normals written as they are in Blender (imported normals survive as custom split normals). Skinning comes from bone-named vertex groups when the object has them (an imported unit round-trips with its weights), otherwise every vertex goes to `dummy_bone` (the static-prop convention used by plants and rocks). Meshes bound to an armature are exported in the rest pose whatever frame is showing. Object positions matter: what you see relative to the world origin is what the game gets. Set the texture atlas name in the export options or a `tt_texture` custom property on an object; comma-separated atlas lists pass through untouched.
- **Split Mesh by Bone** (Object menu): moves every face whose corners are all weighted to one bone into a new object, keeping UVs, normals, weights and materials. This is how a held item baked into a unit mesh (the warrior's axe on `Prop1`) becomes a separate attachment file. Re-export the unit and the part separately afterwards. For kits of separate parts (a unit plus its held items), check "One File Per Object" to write each selected object to its own `<object name>.xml` instead of merging.
- **Attachments**: to export a hat or held item that follows a unit bone, set "Attach To" in the export dialog (head, back, hands, prop bones, belt) and pick the unit skeleton; every vertex is skinned to that bone with weight 1. "Custom bone" takes an exact bone name. A `tt_bone` custom property on an object overrides the dialog, so a mixed selection can carry per-object bones even when merged. Model attachments in the unit's bind pose: import the unit mesh and place the item on it where it should sit. Object > Snap to Tribal Trouble Bone moves the selected objects to a bone's rest position read from a `*_skeleton.xml` file and stamps `tt_bone` on them, so the item is placed and tagged for export in one step ("Align Rotation" is off by default because Biped bones point X along the bone). Bone names differ per skeleton file (`peon Head`, `warrior  Head` with two spaces, chieftain `Head`); the dialog resolves them for you. See `docs/blender-plugin/attachments.md` for the game side.

- **Skeletons and animations**: File > Import > Tribal Trouble Skeleton / Animation. Select a `*_skeleton.xml` and any clip files (`peon_run.xml`, `peon_idle.xml`, ...) together; the skeleton becomes an armature whose rest pose is exactly the file's, and each clip becomes an action keyed at frames 1..n. Meshes selected at import time whose vertex groups match the bones are parented with an Armature modifier, so importing `peon_mesh.xml` first and then the skeleton with the peon selected gives a rigged, animated unit. Clips alone can be imported onto the active armature later. File > Export > Tribal Trouble Skeleton / Animation writes the active armature's rest pose and every imported action back out, sampled at whole frames. Clip type and speed (loop or plain, cycles per world unit) live in `geometry.xml`, not in the clip files.
- **Attachments panel** (sidebar N panel, "Tribal Trouble" tab, with an armature or one of its meshes active): one slot per attachment point the skeleton has bones for. Pick a mesh in a slot and it is bone-parented at its current placement (relative to the bone as posed at that moment, so it is safe to assign while scrubbing), tagged with `tt_bone`, and follows the bone through every clip. The eye toggle hides it. "Export Visible Attachments" writes one file per visible attachment skinned to its bone, always in the rest pose. "Copy Registry Snippet" puts matching `geometry.xml` entries on the clipboard, using the `base=` shorthand that inherits the unit's skeleton and clips. Each entry carries a `slot=` (head gives `hat`, Prop1 gives `weapon`, other points use their own name). The game wires units from these entries at build time, so a new item needs no Java: paste the entry, put the texture in `assets/textures/models`, rebuild. Several items can share a slot; in game, H cycles the `hat` slot on the selected units. Add `default="true"` to an entry to have units start with it.

## Validation

`tools/blender/validate_roundtrip.py` imports files, re-exports them, and diffs every value. Give it a skeleton plus clips, or one or more mesh files. Run it from the repo root after touching the import or export code:

```
blender -b --python tools/blender/validate_roundtrip.py -- assets/geometry/vikings/peon/peon_skeleton.xml assets/geometry/vikings/peon/peon_run.xml
blender -b --python tools/blender/validate_roundtrip.py -- assets/geometry/vikings/warrior/warrior_mesh.xml
```

Positions, UVs, weights and matrices come back within the files' own print precision. Normals compare by direction only (the source files carry normals of any length, the game normalises at load) and a few corners can differ by up to a degree, which is Blender's custom normal storage, not a bug. Zero-weight skin entries in the source files are dropped and duplicate bone entries for one vertex are summed; both match how the game reads them.

## Scope

Static props, rigid attachments, skinned units, skeleton and clip round trips. Not covered yet: normal maps, and registering a new non-attachment model in `geometry.xml` and `RacesResources` still happens by hand. Attachments only need the registry snippet.

Getting a new model in game (registration is not automated yet): put the exported XML under `assets/geometry/`, add an entry to `geometry.xml`, and reference it from `RacesResources`.

## Format notes

- The game is Z-up like Blender; no axis conversion happens.
- Mesh XML: `mesh > polygons > polygon > vertex`, each vertex with position, normal, UV (optional second UV), RGBA vertex color, and one or more `skin` bone weights. Static meshes carry no `skeleton` section.
- Skeletons live in separate `*_skeleton.xml` files; animations in per-clip XMLs of per-frame, per-bone 4x4 matrices.

## Roadmap

- v2 (done): skeleton and clip import/export, skinned mesh export, split by bone, attachments panel, round-trip validation. Plan and remaining game-side work: `docs/blender-plugin/attachments.md`.
- v3: normal-map export convention (the engine already supports normal-mapped models with specular in the alpha channel).
