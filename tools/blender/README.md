# Tribal Trouble Blender Plugin

Official Blender import/export addon for Tribal Trouble geometry. Maintained in this repo; the game and the plugin version together.

## Install

1. Blender 4.x: Edit > Preferences > Add-ons > Install...
2. Pick `io_tribaltrouble.py`, then enable "Tribal Trouble Mesh (.xml)".

## Use

- **Import**: File > Import > Tribal Trouble Mesh (.xml). Point it at any mesh under `assets/geometry/`; shift-select several files to import them all at once, one object each. UVs, vertex colors, and bone weights (as vertex groups) come along. When importing from inside the repo, the matching atlas PNG from `assets/textures/models/` is found automatically and a material is built for it ("Load Textures" option, on by default). If a texture appears vertically flipped, re-import with "Flip V" checked.
- **Export**: File > Export > Tribal Trouble Mesh (.xml). Exports every selected mesh object, merged in world space into one mesh (the scene is untouched), triangulated, with every vertex skinned to `dummy_bone` (the static-prop convention used by plants and rocks). Object positions matter: what you see relative to the world origin is what the game gets. Set the texture atlas name in the export options or a `tt_texture` custom property on an object. For kits of separate parts (a unit plus its held items), check "One File Per Object" to write each selected object to its own `<object name>.xml` instead of merging.
- **Attachments**: to export a hat or held item that follows a unit bone, set "Attach To" in the export dialog (head, back, hands, prop bones, belt) and pick the unit skeleton; every vertex is skinned to that bone with weight 1. "Custom bone" takes an exact bone name. A `tt_bone` custom property on an object overrides the dialog, so a mixed selection can carry per-object bones even when merged. Model attachments in the unit's bind pose: import the unit mesh and place the item on it where it should sit. Bone names differ per skeleton file (`peon Head`, `warrior  Head` with two spaces, chieftain `Head`); the dialog resolves them for you. See `docs/blender-plugin/attachments.md` for the game side.

## Scope (v1)

Static props and rigid attachments. Skinned unit meshes import fine for viewing, but the exporter writes one bone per object, so do not re-export animated units yet.

Getting a new model in game (registration is not automated yet): put the exported XML under `assets/geometry/`, add an entry to `geometry.xml`, and reference it from `RacesResources`.

## Format notes

- The game is Z-up like Blender; no axis conversion happens.
- Mesh XML: `mesh > polygons > polygon > vertex`, each vertex with position, normal, UV (optional second UV), RGBA vertex color, and one or more `skin` bone weights. Static meshes carry no `skeleton` section.
- Skeletons live in separate `*_skeleton.xml` files; animations in per-clip XMLs of per-frame, per-bone 4x4 matrices.

## Roadmap

- v2: skeleton import as armature, animation clips as actions, skinned export with round-trip validation against existing animations.
- v3: normal-map export convention (the engine already supports normal-mapped models with specular in the alpha channel).
