# Custom Weapon SFX

A RuneLite plugin that plays configurable sound effects on player actions, with per-weapon and per-trigger control.

## Features

- **Per-weapon sounds** — assign custom sounds to specific weapons via an in-game item search
- **Unarmed attacks** — configure sounds for hits dealt while unarmed
- **Received attacks** — configure sounds for incoming hits on your player
- **Granular triggers** — each sound group fires independently based on the hit type:
  - Regular attack zero / hit / max
  - Special attack zero / hit / max
  - All attacks
- **Multiple sound groups per entry** — stack several sounds on different triggers for one weapon
- **Custom sound files** — drop `.wav` files into `.runelite/customweaponsfx/` and use them in-game
- **Built-in sounds** — bundled sounds (including `squeak`) available out of the box
- **Per-group volume** — independent volume slider (0–100) for each sound group
- **Sound preview** — test any sound directly from the panel before using it in-game

## Setup

1. Open the plugin panel via the sidebar icon.
2. Click **Add Weapon** and search for a weapon while logged in.
3. Expand the weapon row, pick a sound from the dropdown, and check the triggers you want.
4. For unarmed or received-hit sounds, expand the **Player Attacks (Unarmed)** or **Received Attacks** sections at the top of the panel.

### Custom sounds

1. Place `.wav` files in `.runelite/customweaponsfx/`.
2. Click **Refresh Sounds** in the panel.
3. The files will appear in the sound dropdowns alongside the built-in options.

### Adding multiple sounds to one weapon

Each weapon (or the unarmed/received sections) supports multiple sound groups. Click **+ Add Sound Group** inside an expanded entry to add another group with its own sound, volume, and trigger set.

### Resetting all data

Click **Reset All Data** in the panel to wipe all saved weapon entries and sound groups and restore the unarmed and received sections to their defaults. A confirmation dialog will appear before anything is deleted.
