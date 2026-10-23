# The geometry registry

`assets/geometry/geometry.xml` lists every model the game draws. The geometry converter turns it into binary sprites plus `registry.json`, which lists every item, skin, decoration and light and which the game reads at startup, so a new entry here needs no Java. The Blender add-on writes these entries; this page is for reading them.

**Keep `assets/schemas/geometry.rng` in step.** The schema declares every attribute below. When a new attribute is added here, add it to the schema in the same commit; the add-on test `every_registry_attribute_is_declared_in_the_schema` fails otherwise.

## Layout

```xml
<geometry>
  <group name="vikings">
    <sprite name="peon" scale="1">
      <skeleton path="vikings/peon/peon_skeleton.xml"/>
      <model path="vikings/peon/peon_mesh.xml" r="255" g="255" b="255">
        <texture name="viking_peon" team="viking_peon_team"/>
      </model>
      <model path="vikings/peon/peon_low_poly_mesh.xml" r="255" g="255" b="255">
        <texture name="viking_peon" team="viking_peon_team"/>
      </model>
      <animation name="run" wpc="1" type="loop" path="vikings/peon/peon_run.xml"/>
    </sprite>
  </group>
</geometry>
```

A group is a folder of related sprites: `vikings`, `natives`, `misc`. A sprite is one drawable thing. Its first `model` is the close-up mesh, a second one is the far-away mesh. `r g b` tint the model, `255 255 255` for none. Each `texture` names a PNG in `assets/textures/models` without the extension; several textures on one model are tiers the game picks between (rock, iron, rubber). `animation` lines name clip files in `path`; `type` is `loop` or `plain` (plays once), `wpc` is world units travelled per cycle for walks.

## Sprite attributes

| Attribute | Value | Meaning |
|---|---|---|
| `name` | letters, digits, underscores | Unique within the group. Becomes the file name of the binary sprite. |
| `scale` | float | Uniform scale applied on load. |
| `base` | a sprite name in the same group | This sprite inherits the base's skeleton and clips and carries none of its own. Required with `slot`. On a unit skin it keeps the clips of the unit it replaces. |
| `slot` | `hat`, `weapon`, `belt`, `magic`, `carried`, `prop` | Makes the sprite an item of its base. A unit shows at most one item per slot; `prop` is for things placed around buildings, trees and scenery. |
| `default` | `true` | Every unit of the base starts wearing this item. Items in a slot cycle default first, then by name. |
| `event` | an event name such as `halloween` | The sprite exists only while that event is on. Without it the sprite is loaded all year. |
| `skin` | a skin name | This sprite is another look for the sprite in `replaces`. With `event` everyone sees it during the event; without one, only players who own the skin. Requires `replaces`. |
| `replaces` | a sprite name in the same group | The stock sprite this skin stands in for. A skin of an item keeps the item's `base` and `slot`. |
| `decoration` | ground names, comma separated: `grass`, `dirt`, `beach`, `snow`, `land` | Map scenery the game scatters on that ground on every map. Cannot combine with `slot` or `skin`. |
| `count` | 1 to 1000 | How many of a decoration one map gets, 20 when absent. Only valid with `decoration`. |

Rules the converter enforces: a `slot` without a `base`, a `skin` without a matching `replaces`, a `count` without a `decoration`, and an item skin whose `base` or `slot` differs from the item it replaces are all build errors.

## Texture attributes

| Attribute | Value | Meaning |
|---|---|---|
| `name` | PNG name | The texture, in `assets/textures/models`. |
| `team` | PNG name | The team color decal, in `assets/textures/teamdecals`. |
| `normal` | PNG name | Reserved for a normal map; nothing writes it yet. |
| `emissive` | PNG name | A glow image added on top of the lit texture. Black adds nothing. |
| `light_color` | `#rrggbb` | With `emissive`, the glow also lights its surroundings at night in this color. |
| `light_strength` | float | Brightness of that light, 1 is normal. |
| `light_reach` | float, meters | How far the light reaches. |

The three `light_` attributes go together and need `emissive`. The converter finds up to four bright spots of the glow image on the mesh and writes them to `registry.json`; the game draws the eight lights nearest the camera.

## What the game does with them

Everything below is render-only. Nothing an item, prop, skin or decoration does reaches the simulation, so players in one game may see different looks and replays stay in step.

- **Items** (`base` + `slot`): the game reads `registry.json` at load and wires each item onto every template of its base unit. A unit shows one item per slot, default items first, then by name. An item with one texture uses it for every tier; one with as many textures as the unit follows the tier, as the warrior's axe does. Nothing switches items in game yet; a chat command for trying them on is planned.
- **Carried items** (`slot="carried"`): the same mechanism, but the simulation decides what a peon holds and the game asks for wood, rock, rubber and the paddles by name. They never enter the player-facing slots.
- **Props** (`slot="prop"`): no bone. Drawn where they were placed relative to the base and following its position, rotation and scale; a falling tree takes its props with it. A building prop shows only on its stage. Trees and their props draw the full mesh at any distance; other sprites draw props at their own detail level and drop them where the sprite is a dot. The chicken is animated, so its prop is skinned to its bones like a unit item. Props are not picked and do not change the base's bounds.
- **Skins** (`skin` + `replaces`): drawn wherever the stock sprite would be, with the same texture tier or the first texture when the skin has fewer. An owned skin (no `event`) belongs to a player and every client draws that player's units, buildings and items with it; nothing assigns one yet, `RacesResources.getSkins` hands one to `Player.setSkins`. An event skin belongs to nobody and replaces the default look for everyone while the event is on, scenery included; an owned skin for the same sprite wins over it. A unit skin must have the clips of the unit it replaces. A building skin replaces one stage and keeps that stage's props plus any whose `base` is the skin sprite. An item skin is listed among the skins only, never the items, so nobody wears it as an extra item.
- **Decorations** (`decoration`): scattered after trees, rocks and iron on dry, reachable ground no tree or resource holds, one per unit grid cell, from a random generator seeded with the map seed so a map always gets the same ones. Drawn at any distance, not picked; units walk through them.

## Event stand-ins for carried items

A carried item (`slot="carried"` on the peon: wood, rock, rubber and the paddles) is asked for by name from code. During an event a sprite named `<name>_<event>` with that `event` stands in for `<name>`, so `wood_resource_halloween` replaces `wood_resource` in October. Event skins work on carried items too.

## Which events are on

By date, `autumn` through October and `halloween` from October 16 to 31. The system property `com.oddlabs.tt.event=a,b` overrides the date; `-` turns every event off. Events are render-only: players in one game may see different looks.
