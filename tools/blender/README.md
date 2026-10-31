# Tribal Trouble Blender Add-on

Make models, hats, props, skins and animations for Tribal Trouble inside Blender and publish them straight into the game files. No code needed for any of that.

Maintainer notes (code layout, tests, file format) are in `docs/blender-plugin/internals.md`.

## Install

1. Copy the `tools/blender/io_tribaltrouble` folder into Blender's add-ons folder. On Windows, from the repo root in PowerShell (put your Blender version in the path):
   `Copy-Item -Recurse tools\blender\io_tribaltrouble "$env:APPDATA\Blender Foundation\Blender\5.2\scripts\addons\"`
2. Start Blender and enable "Tribal Trouble Mesh (.xml)" in Edit > Preferences > Add-ons.
3. Press N in the 3D view, open the **Tribal Trouble** tab and set **Repo** to your tribaltrouble checkout. It is remembered.

When someone updates the add-on, pull and press **Update add-on to x.y.z** in the Models panel.

## The panels

All of them live in the Tribal Trouble tab of the N sidebar.

- **Models**: pick a category, click a model to load it. Publish, Reload, New Model and the glow controls live here.
- **Skins**: other looks for the loaded model.
- **Preview**: clips, texture tiers, high or low detail, team color. Only for models that have clips.
- **Props**: the hats, weapons, carried things and decorations on the loaded model.

Loading a model brings in its mesh, skeleton, every clip and every item the game knows about. Your own objects are never removed. Right click a row for **Add To Scene** to put a second model next to the loaded one for comparison.

## Publish

**Publish** in the Models panel is the one save button. It shows only while something is unpublished. It runs the checks, then writes everything you changed on the loaded model back into the repo: meshes at every detail level, items and props, clips, painted textures and glow. Files you did not touch stay as they are. Loading another model or pressing Reload asks first if work is unpublished.

## Put a hat or weapon on a unit

1. Load the unit from the Models list.
2. Model the item. For something held, put the grip at the object origin.
3. In Props, press **New Prop...**. Pick **Where** (head, back, a hand...), your **Mesh**, an **Event** if it is seasonal, and **On by default** if every unit should wear it. Leave **Snap** and **Make a texture for it** on.
4. OK hangs it on the bone. Press Play in Preview to judge it, nudge it, press **Snap again** if needed.
5. Press **Paint It** to paint its texture, then **Done Painting**.
6. Publish.

The item becomes a row in the Props list. Click a row to open the item on its own, with its paint, glow and skins. The eye hides it, the bin removes it from the game.

An item that is still drawn from its unit's texture, like the warrior's axe, offers **Give It Its Own Texture**. That copies its pixels off the unit atlas into a texture of its own.

## Decorate a building, tree or rock

1. Load it from the Models list. Buildings load their Built stage; the Half built and Start stages are skins in the Skins panel.
2. Place your mesh around it.
3. In Props, press **New Prop...**. On a building the prop attaches to the stage showing. On a tree choose **Sits on** trunk or crown. Pick an **Event** or All year.
4. OK writes it where you placed it. Publish saves later changes.

## Make a skin

A skin is another look for a model. With no event, players who own it see it. With an event, everyone sees it while the event is on.

1. Load the model. In Skins, press **New Skin...**. Give it a name, optionally one of your own meshes as its shape, and the event.
2. The banner reads **Editing skin 'name'**. Reshape the mesh, paint, or give it another image.
3. Press **Save Skin**, or **Cancel** to drop the edits.

Save the same skin name on the peon, the warrior and the quarters and a player who owns it gets all three. An open item in the Props panel has its own Skins box that works the same way. The eye on a skin row previews it, the brush paints it, the bin removes it.

## Make something glow at night

1. Load the model. Under Publish, press **Add Glow...** and pick an image or **New black image**.
2. Paint it with the brush. Black adds nothing, brighter glows more.
3. Tick **Light** to make it light the ground and models around it at night. Set **Strength** and **Reach** in meters.
4. Publish.

An image wired into the material's Emission Color does the same thing when you make a texture by hand.

## Animate a unit

1. Load the unit. In Preview, press a clip button to see it.
2. Press **New Clip**. Start from the pose on screen with a key on the first and last frame, or from a copy of the clip showing. Say whether it loops or plays once and, for a walk, the distance per loop.
3. Animate as usual. Publish writes new clips and changed clips.

A new clip plays in game only once a programmer asks for it. **Delete Clip** removes an unsaved clip, or the last saved one; earlier clips are numbered by the game and cannot be deleted.

## Make a new model

Press **New Model...** at the bottom of the Models panel and pick your **Mesh**. The name follows the mesh. It gets its own folder in the repo and a registry entry.

- A plain mesh becomes a static model. Add **Half Built** and **Start** meshes for a building.
- A mesh on a loaded unit's armature becomes a new unit on that rig.
- A mesh on your own armature becomes a unit with its own skeleton and one clip per action.
- Tick **Scatter on map** for map scenery such as pumpkin patches. Pick the **Terrain**, the **Count** per map and an **Event**. The game scatters it by itself, no code needed. Loaded decorations offer **Scatter Settings...** to change those later.

Anything other than an item, a prop, a skin or a scattered decoration still needs a programmer to use it in game.

## Events and seasons

Every form with an **Event** dropdown offers All year, the events the registry already has, and any you made this session. The **+** next to it makes a new event. The game turns `autumn` on through October and `halloween` from October 16 to 31. To see another event in game, start it with `-Dcom.oddlabs.tt.event=<event>`.

## When Publish refuses

The check button next to Publish lists the problems. The ones that stop a publish:

- No faces, no UV map, or a UV map collapsed to one point.
- No image texture in the material.
- A model or image name with anything but letters, digits and underscores (hyphens are fine in image names).
- An image named like another model's texture in the repo.
- A new item or skin named like one that already exists, a file name already taken in the model's folder, or a clip named like one the unit already has.

Warnings do not stop it: UVs outside 0 to 1, a texture that is not a square power of two, colored vertex colors, vertices with no bone weight, or an item bigger or heavier than the unit.

## Raw import and export

File > Import > Tribal Trouble Mesh, Skeleton and Animation read any file under `assets/geometry` into Blender. File > Export writes the selected objects out. The Models panel does this for you; use these only to look at a file the panels do not reach.
