# Blender add-on internals

Maintainer notes for `tools/blender/io_tribaltrouble`. Artists start with `tools/blender/README.md`, which covers every panel and button. The attributes the add-on writes into `geometry.xml` are in `registry.md` next to this file, with what the game does with them and the schema that must follow them.

The tests are the full description of behavior: `tools/blender/tests/test_addon.py` has one test per rule, named after it. When a question is "what exactly does this button do", read its test before reading its code.

## Code layout

Lower modules never import higher ones, in this order:

- `textures.py`: materials, team color nodes, texture files and atlas crops.
- `mesh_io.py`: mesh XML read and write, and the Blender meshes built from it.
- `rig.py`: skeleton and clip XML read and write, armatures, actions and `ATTACHMENT_POINTS`.
- `registry.py`: the repo folder and `geometry.xml`: reading it, adding and removing entries, skins.
- `scene.py`: the models loaded in the scene: loading, which level and item shows, attachments on bones, reference models, skin lookups.
- `unsaved.py`: whether the loaded model has unpublished work, worked out shortly after changes stop so drawing a panel never exports a mesh.
- `publish.py`: writing changed models, items, clips and textures to the repo, and the checks run first.
- `glow.py`: the Glow row and its Light controls.
- `forms.py`: what the popup forms share: the OK row, the event dropdown, name and mesh checks.
- `import_export.py`: File > Import, File > Export and Split Mesh by Bone.
- `models.py`, `preview.py`, `by_hand.py`, `skins.py`, `props.py`: one panel each.
- `__init__.py`: `bl_info`, the class list, register and unregister.

## Rules that are easy to break

- Publish and New Model write only files that changed since loading or the last Publish. A model loaded from the repo and published untouched writes nothing.
- The default files and registry entry of a model are never written while a skin is showing; edits go to the skin's files, copying a borrowed detail level first.
- Every form's OK stays greyed out until the form can be used; refusals are listed in the README under "When Publish refuses" and come from `publish.py`.
- A new registry attribute goes into `assets/schemas/geometry.rng` in the same commit; `every_registry_attribute_is_declared_in_the_schema` fails otherwise.
- Bone names differ per skeleton and an item must be skinned to the exact string: `peon Head`, `warrior  Head` (two spaces), `Head` on the native warrior and both chieftains.

## Tests

```
blender -b --factory-startup --python tools/blender/tests/test_addon.py
blender -b --python tools/blender/tests/validate_roundtrip.py -- assets/geometry/vikings/peon/peon_skeleton.xml assets/geometry/vikings/peon/peon_run.xml
blender -b --python tools/blender/tests/validate_roundtrip.py -- assets/geometry/vikings/warrior/warrior_mesh.xml
```

The first runs the whole add-on headless against a temporary copy of `assets/geometry` and never writes to the repo. The second imports files, re-exports them and diffs every value; run it after touching import or export code. Positions, UVs, weights and matrices come back within the files' own print precision. Normals compare by direction only, since the source files carry normals of any length and the game normalizes at load, and a few corners can differ by up to a degree from Blender's custom normal storage. Zero-weight skin entries are dropped and duplicate bone entries for one vertex are summed, both as the game reads them.

## Shipping a change

Bump the version in `bl_info`. Artists then see **Update add-on to x.y.z** in the Models panel, which copies the repo's add-on over the installed one and reloads. For Blender 4.2 and newer, `python tools/blender/build_extension.py` writes `tools/blender/dist/tribal_trouble_io-<version>.zip` to drag into Blender; `io_tribaltrouble` stays the single source and the two install methods must not be mixed.

## Format notes

- The game is Z-up like Blender; no axis conversion happens.
- Mesh XML: `mesh > polygons > polygon > vertex`, each vertex with position, normal, UV (optional second UV), RGBA vertex color, and one or more `skin` bone weights. Static meshes carry no `skeleton` section.
- Skeletons live in separate `*_skeleton.xml` files; animations in per-clip XMLs of per-frame, per-bone 4x4 matrices.

## Not covered yet

Normal maps. The engine already draws normal-mapped models with specular in the alpha channel; the add-on has no export convention for them. A new model other than an item, a prop, a skin or a scattered decoration still needs code to be used in game.
