# Custom Weapon SFX

Play custom sound effects when you attack or get hit in Old School RuneScape.

## Features

- Assign sounds to specific weapons
- Play different sounds for misses and max hits — on both regular and special attacks
- Separate sounds for when *you* take damage
- Use built-in sounds or drop in your own `.wav` files
- Per-sound volume control and in-panel preview

## Setup

1. Open the **Custom Weapon SFX** panel from the sidebar.
2. Add a weapon — either click **Add (Equipped)** while holding a weapon, or click **Add (Search)** to find one by name (requires being logged in).
3. Expand the weapon, pick a sound, and check the triggers you want it to play on.
4. To play a sound when you take damage, expand the **Received Attacks** section at the top.

### Using your own sounds

1. Place `.wav` files in `.runelite/customweaponsfx/`.
2. Click **Refresh Sounds** in the panel.
3. Your files will appear in the sound dropdowns.

### Multiple sounds per weapon

Click **+ Add Sound Group** inside an expanded weapon (or the Received Attacks section) to add another sound with its own trigger and volume settings.

### Resetting

Click **Reset All Data** to clear all weapons and sound groups. A confirmation dialog will appear first.

### Known "Issues"

If you have a thrall out, and you have a "Regular attack zero" trigger set, thrall zeros will cause this sfx to play. Sorry, this isn't worth trying to figure out, due to lack of data that is provided, thrall hits are basically indistinguishable from player hits when it comes to hitting zeros. If Jagex ever provides more data alongside thrall hits, i'd love to fix this.