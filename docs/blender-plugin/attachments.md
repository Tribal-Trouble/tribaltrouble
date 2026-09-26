# Attachments and attachment points

Feature list for bone-attached items (hats, held items, back items) across the game and the Blender plugin. Written 2026-09-06.

Status: every plugin item (1 to 10) is on branch `blender-plugin` (addon 1.6.0). Game items 1, 2 and 3 (local toggle only) are on branch `unit-attachments` with a placeholder viking peon hat, plus the viking warrior axe split pilot. Remaining: the other content splits, and game items 3 (wire) to 7.

Two source-file quirks the tools now handle, worth knowing when reading the XML by hand: corner normals are not unit length (the game normalises at load), and a vertex can list the same bone twice (the game sums the entries).

## What the game does today

- **Companion sprites are already bone attachments.** The wood, rock and rubber bundles and both paddles are separate sprites in `assets/geometry/geometry.xml` that reuse the peon skeleton and the peon's full clip list. Bundles are skinned entirely to `peon Spine2`, paddles to `peon R Hand`. The unit visitor in `render/RenderState.java` draws the supply sprite right after the unit with the same position, rotation and animation frame. This is the pattern; it is just hardwired to the supply container (one sprite, builders only, only while carrying).
- **Every humanoid skeleton has prop bones.** Peons and warriors have `Prop1`; both chieftains have `Prop1`, `Prop2`, `Prop3`. These are the 3ds Max Biped held-item bones. Held items are baked into the unit meshes but weighted to these bones, which is what makes separating them cheap (see content work below).
- **Warrior weapon tiers are texture swaps.** The axe or spear is part of the warrior mesh; rock, iron and rubber are three atlases on the same geometry.
- **Thrown axe and spear** are separate static sprites (`axe`, `spear`) used only as projectiles.
- **Seasonal hats (PR #130)** were whole-mesh duplicates (a second peon mesh with the hat modeled in, plus duplicate atlas and team decal), chosen globally by calendar month. Those assets are not in the tree today.
- **Night-mode torches** (branch `night-mode`) are world-positioned props at building torch positions, not bone attachments.
- **Runtime pre-bakes vertices per frame** at load (`render/Sprite.java`), so an attachment costs memory proportional to frames times vertices, same as any sprite.

Bone naming is inconsistent per skeleton and attachments must use the exact string: `peon Head`, `warrior  Head` (two spaces), chieftain `Head`.

## Slots and registration (implemented 2026-09-19)

An attachment is a sprite entry in `assets/geometry/geometry.xml` with three attributes:

```xml
<sprite name="warrior_pumpkin" base="warrior" slot="hat">
```

- `base` is the unit sprite in the same group. The attachment inherits its skeleton and clips.
- `slot` names the place it goes. A unit shows at most one item per slot. Slots are independent of each other.
- `default="true"` makes every new unit start with it. The viking warrior's axe does this in the `weapon` slot.

A sprite that inherits everything from its base (no skeleton or clips of its own) is built without clip data: its file names the base (`vikings/peon`) and the game reads that unit's clips once and shares them. An item is a few KB instead of several hundred.

The geometry converter writes every slotted sprite to `attachments.txt` next to the binary sprites. The game reads that file at load and wires each item onto every template of its base unit, so adding an item needs no Java. An item with one texture uses it for every tier. An item with as many textures as the unit has tiers follows the tier, as the axe does.

Items in a slot cycle in the order default first, then by name. In game, with cheats on (`/iamacheater` in chat), H cycles the `hat` slot on the selected units: bare, first item, second item, bare. It is a cheat, hidden from the key options, because the choice is local: other players do not see it until it travels on the wire. Every selected unit moves to the same item, taken from the first one in the selection. The choice is render-only and local until the wire change.

## Buildings: props and event textures (implemented 2026-09-19)

A building is a static sprite, so a prop on it is simpler than a hat: it has no bone, it just sits where it was placed relative to the building.

```xml
<sprite name="quarters_lantern" base="quarters" slot="prop" event="halloween">
```

- `base` is the building stage it belongs to: `quarters`, `quarters_halfbuilt` or `quarters_start`. A prop only shows on that stage.
- Every prop registered on a stage is drawn with it. There is nothing to toggle, so the slot name only groups them; the add-on writes `prop`.
- `event` is optional. Without it the prop shows all year. With it the prop only shows while that event is on.

A texture can carry the same attribute:

```xml
<model r="90" g="60" b="30">
    vikings/quarters/viking_main_built.xml
    <texture name="viking_buildings_hi" team="viking_buildings_hi_team"/>
    <texture name="viking_buildings_hi_halloween" team="viking_buildings_hi_team" event="halloween"/>
</model>
```

The game picks a texture by its place in the list, and that place is shared by every detail level of the sprite. So every model of the sprite needs an event texture in the same place; the converter refuses the registry otherwise. A model nobody painted for the event repeats its usual texture there. The converter writes `event_textures.txt` next to `attachments.txt`, and `attachments.txt` gained the event as a last column.

Which event is on comes from the `com.oddlabs.tt.event` system property, for example `-Dcom.oddlabs.tt.event=halloween`. It is render-only, so players in one game may differ. Turning an event on by calendar date is a follow-up. Event items on units obey the same attribute: outside their event they are not loaded at all. An event texture only replaces a sprite's first texture, so iron rocks and iron or chicken warriors keep their tier texture during an event.

## Carried items (moved into the registry 2026-09-19)

What a peon hauls or rows with is a registry item like any other, in the `carried` slot:

```xml
<sprite name="wood_resource" base="peon" slot="carried">
```

There are five per race: `wood_resource`, `rock_resource` (iron is its second texture), `rubber_resource`, `left_paddle` and `right_paddle`. They used to repeat the peon's skeleton and all eight clip lines; now they inherit them, and the built files are byte for byte the same.

The difference from a hat is who switches it on. The simulation decides what a peon holds, so the game asks for these five by name and they never enter the unit's player-facing slots; H does not touch them. That is also why they cannot be removed from the registry.

During an event a sprite named `<name>_<event>` in the same slot stands in for `<name>`, for example `wood_resource_christmas` with `event="christmas"`. An event texture on the item works too, as on buildings.

## Player skins (registry side implemented 2026-09-23)

A skin redraws everything one player owns: every unit and building of a template, not one instance. A skin sprite names the skin it belongs to and the sprite of its group it stands in for:

```xml
<sprite name="warrior_gold" base="warrior" skin="gold" replaces="warrior">
<sprite name="quarters_gold" skin="gold" replaces="quarters">
<sprite name="quarters_gold_banner" base="quarters_gold" slot="prop">
```

- `replaces` is the stock sprite. Every template drawn with it uses the skin sprite instead, with the same texture slot (rock, iron and chicken warriors share one mesh), or the first texture when the skin has fewer.
- A unit skin needs `base` on the unit it replaces so it has the same clips; the game refuses a skin whose clip list differs.
- A building skin replaces one stage. Props whose `base` is the skin sprite are drawn with it; stages the skin leaves alone keep their stock props.
- The converter writes `skins.txt` (`group skin replaces name textures`). Nothing picks a player's skin yet; `RacesResources.getSkins(name)` hands one to `Player.setSkins`.

## Map decorations (game side implemented 2026-09-25)

A decoration is a static sprite the game scatters over every generated map, for example pumpkin patches during Halloween. It is scenery only: units walk through it, it never enters the simulation, and players in one game may see different decorations.

```xml
<sprite name="pumpkin_patch" decoration="grass" count="12" event="halloween">
<sprite name="pumpkin" decoration="land">
```

- `decoration` is the ground it stands on:
  - `grass`: the grass layer.
  - `dirt`: dirt on native maps, soil on viking maps.
  - `beach`: the base layer near the water, sand on native maps and gravel on viking maps.
  - `snow`: snow, so viking maps only.
  - `land`: any of them.
- `count` is how many the game places on one map, from 1 to 1000; without it the converter writes 20. A map with less matching ground gets fewer.
- `event` is optional, as for props. Outside its event the sprite is not loaded or placed.
- A decoration cannot also have a `slot` or `skin`.

The game places decorations after trees, rocks and iron, only on dry accessible ground, one per unit grid cell, never on a cell a tree or resource holds. It draws them like plants: they fade out with distance and follow the same detail switch. Placement uses its own random seeded from the map seed, so one map always gets the same decorations. The converter writes `decorations.txt` (`group name ground count event`), sorted, next to `attachments.txt`.

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
