# Attachments and attachment points

Feature list for bone-attached items (hats, held items, back items) across the game and the Blender plugin. Written 2026-09-06.

Status: steps 1 to 3 of the suggested order are implemented. Plugin items 1, 4, 5, 6, 7, 8 and 10 are on branch `blender-plugin` (addon 1.5.0); game items 1, 2 and 3 (local toggle only) are on branch `unit-attachments` with a placeholder viking peon hat. Remaining: plugin items 2, 3 and 9, the content splits, and game items 3 (wire) to 7.

## What the game does today

- **Companion sprites are already bone attachments.** The wood, rock and rubber bundles and both paddles are separate sprites in `assets/geometry/geometry.xml` that reuse the peon skeleton and the peon's full clip list. Bundles are skinned entirely to `peon Spine2`, paddles to `peon R Hand`. The unit visitor in `render/RenderState.java` draws the supply sprite right after the unit with the same position, rotation and animation frame. This is the pattern; it is just hardwired to the supply container (one sprite, builders only, only while carrying).
- **Every humanoid skeleton has prop bones.** Peons and warriors have `Prop1`; both chieftains have `Prop1`, `Prop2`, `Prop3`. These are the 3ds Max Biped held-item bones. Held items are baked into the unit meshes but weighted to these bones, which is what makes separating them cheap (see content work below).
- **Warrior weapon tiers are texture swaps.** The axe or spear is part of the warrior mesh; rock, iron and rubber are three atlases on the same geometry.
- **Thrown axe and spear** are separate static sprites (`axe`, `spear`) used only as projectiles.
- **Seasonal hats (PR #130)** were whole-mesh duplicates (a second peon mesh with the hat modeled in, plus duplicate atlas and team decal), chosen globally by calendar month. Those assets are not in the tree today.
- **Night-mode torches** (branch `night-mode`) are world-positioned props at building torch positions, not bone attachments.
- **Runtime pre-bakes vertices per frame** at load (`render/Sprite.java`), so an attachment costs memory proportional to frames times vertices, same as any sprite.

Bone naming is inconsistent per skeleton and attachments must use the exact string: `peon Head`, `warrior  Head` (two spaces), chieftain `Head`.

## Attachment points

Proposed logical names, mapped per skeleton. The plugin and the registry should speak the logical name; the mapping table resolves the bone string.

| Logical point | Peon (both races) | Viking warrior | Native warrior | Viking chieftain | Native chieftain | Typical use |
|---|---|---|---|---|---|---|
| head | `peon Head` | `warrior  Head` | `Head` | `Head` | `Head` | hats, masks, horns |
| back | `peon Spine2` | `warrior  Spine1` | `Spine2` | `Spine1` | `Spine2` | packs, capes, carried bundles (existing) |
| hand_r | `peon R Hand` | `warrior  R Hand` | `R Hand` | `R Hand` | `R Hand` | paddles (existing), tools |
| hand_l | `peon L Hand` | `warrior  L Hand` | `L Hand` | `L Hand` | `L Hand` | shields, torches |
| prop1 | `peon Prop1` | `warrior  Prop1` | `Prop1` | `Prop1` | `Prop1` | primary held item (weapon, staff) |
| prop2 | none | none | none | `Prop2` | `Prop2` | secondary held item |
| prop3 | none | none | `Prop3` | `Prop3` | `Prop3` | tertiary held item |
| belt | `peon Pelvis` | `warrior  Pelvis` | `Pelvis` | `Pelvis` | `Pelvis` | pouches, hanging items |

The viking warrior has no `Spine2`, so its back point is `Spine1`. The native warrior's bones carry no prefix, unlike the viking warrior's. The plugin implements this table as `ATTACHMENT_POINTS` (done in step 1); the game-side registry should share that source once it exists.

Custom points beyond this table need a bone. Options, in order of preference: reuse an existing bone with an authored offset baked into the attachment mesh (no format change), or add a bone to the skeleton file and every clip file (all 16 frames, every clip; the plugin's skeleton export would make this practical).

## Game features

1. **Generic attachment list.** A unit renders a list of extra sprite keys with the same render state, not just the one supply sprite. The supply hook becomes the first user of the list. Attachments are render-only and must never touch simulation state (determinism, replays, spectators).
2. **Registry shorthand.** A sprite entry that inherits skeleton and clip list from a base unit sprite, so each attachment does not repeat the unit's eight animation lines as the paddles do today.
3. **Per-unit toggles.** Turn an attachment on or off per unit at runtime (hat on, hat off). First cut is client-local. Showing it to other players means the choice travels over the wire, which is an API bump and fits the server-attested cosmetics plan.
4. **Team colours on attachments.** Support the texture plus team-decal pair on attachment sprites, as unit meshes have.
5. **Level of detail.** Units have hi and lo meshes. Attachments need a lo mesh, or a distance cutoff, or acceptance that small items pop.
6. **Weapon tier as attachment (optional, later).** Once weapons are separate meshes, rock, iron and rubber can be three small attachment meshes instead of three full warrior atlases. Not required for hats.
7. **Hidden parts (optional, later).** Hats over ponytails will clip. Either author hats around the hair, or support a "hair-less" unit variant. There is no per-vertex-group hide in the format; a variant mesh is the only mechanism.

## Plugin features

1. **Export skinned to a chosen bone.** Rigid attachments: every vertex weighted 1.0 to one bone picked from a dropdown (logical point plus skeleton, resolved to the bone string). Replaces the hard-coded `dummy_bone`. This alone unblocks hats and held items.
2. **Skinned export from vertex groups.** Multi-weight export, normalised per vertex. Needed to re-export a unit mesh after removing an item from it.
3. **Separate by bone.** One operator: pick vertex groups (for example `Prop1`), split those faces into a new object, keep both objects' UVs and colours. Used for the content work below.
4. **Snap to bone.** Place the active object at a bone's rest-pose transform (from the skeleton file) so items are authored in bind pose at the right spot. Rigid attachments only work if authored in bind pose; the paddles are the existing example.
5. **Skeleton import as armature, clips as actions.** Rest pose from `init_pose`, one action per clip file, 16 keyed frames, absolute armature-space matrices per bone. Enables scrubbing animations with an attachment parented to a bone to check grip, scale and clipping.
6. **Round-trip validator.** Import a mesh or clip, re-export, numeric diff must be zero. Gate for items 2 and 5; the matrix convention is the main risk.
7. **Registry snippet.** Generate the `geometry.xml` sprite entry for an attachment (name, mesh path, texture, base unit) to paste in, until registration is automated.
8. **Fix vertex colour export.** The exporter reads the bmesh byte-colour layer but import creates a float-colour layer, so re-exported models come out white. Independent of attachments, blocks any round trip.
9. **Atlas tier lists.** Already handled for import (comma-separated `tt_texture`); export should carry the same list per attachment.
10. **Attachments panel.** A sidebar panel on an imported unit listing its attachment points (from the mapping table). Each point has a slot: pick any attachment object in the scene, or none, and a visibility toggle. Toggling hides or shows the object and parents it to the point's bone (rigid, bind pose) so it follows when scrubbing clips. Several candidates for one point can be kept in the file and switched between, which is how variants (three hats, three weapon tiers) get compared. Export writes one mesh file per visible attachment, each skinned to its point's bone, plus the registry snippet from item 7. Mirrors the in-game per-unit toggle (game item 3) so what is switched on in Blender is what a player would see.

## Content work: separating items from existing models

Counts are vertices weighted to prop bones in the current hi meshes. What each prop actually is needs a look in Blender.

| Mesh | Prop bones used | Vertices | Likely item | Action |
|---|---|---|---|---|
| Viking warrior | Prop1 | 100 | axe | split to its own mesh, attach at prop1 |
| Native warrior | Prop1, Prop3 | 56, 60 | spear plus a second item | verify what Prop3 holds, split both |
| Viking chieftain | Prop1, Prop2, Prop3 | 129, 138, 125 | staff and two others | verify, split per prop |
| Native chieftain | Prop1, Prop2, Prop3 | 270, 114, 86 | staff or spear plus two others | verify, split per prop |
| Viking peon | Prop1 | 82 | tool | verify, split |
| Native peon | Prop1 | 72 | tool | verify, split |

Notes:

- Tail and ponytail geometry (37 to 327 vertices per mesh) is body, not props. Leave it.
- Splitting an item out keeps its UVs on the same atlas, so no texture work is needed for the split itself. The item can keep using the unit's atlas via the registry.
- Splitting changes the unit's `.binsprite` output and therefore the build. Do one unit end to end (viking warrior axe is the simplest) before the rest.
- After a split the unit mesh must still be exported skinned (plugin item 2), and the game must render the item as an attachment (game item 1), or the unit appears empty-handed. These two land together.

## Suggested order

1. Plugin 8, 1, 4 (colour fix, bone-targeted export, snap to bone). Hats become authorable, no animation import needed.
2. Game 1, 2 (attachment list, registry shorthand). First hat visible in game with a local toggle.
3. Plugin 5, 6, 10 (skeleton and clip import, validator, attachments panel). Attachment preview in Blender with per-point on and off switching.
4. Plugin 2, 3 plus the viking warrior axe split as the pilot; then the remaining units.
5. Game 3 wire change with the next API bump; game 4 to 7 as needed.

## Effort

Rough, in focused days. Steps refer to the suggested order above.

| Step | Effort | Risk | Notes |
|---|---|---|---|
| 1. Plugin colour fix, bone-targeted export, snap to bone | 1 day | low | small exporter changes plus reading rest transforms from the skeleton file |
| 2. Game attachment list, registry shorthand, local toggle | 2 to 3 days | low to medium | generalize the supply hook, extend registry parsing and converter |
| 3. Plugin skeleton and clip import, validator, attachments panel | 3 to 5 days | medium | matrix convention is the one real unknown; the validator bounds it |
| 4. Plugin skinned export, split by bone, viking warrior axe pilot | 2 days plus content | medium | needs step 2 first or the warrior renders empty-handed; chieftain props need identifying in Blender |
| 5. Game wire change, team colours, LOD | 2 to 3 days | medium | standard API bump cost |

A first hat visible in game is steps 1 and 2 only, about a week, with no animation import required.

## Open questions

- Where does the per-player cosmetic choice live before the wire change: settings file, Steam entitlement lookup, or both?
- Do attachments follow the unit's lo mesh at distance, or just cut off?
- Should the clip list shorthand be `geometry.xml` syntax or a converter-side default (attachments implicitly inherit from a base sprite)?
- Do we add new bones for custom points, or only reuse existing ones with baked offsets?
