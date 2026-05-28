# Custom Weapon SFX

Play custom sound effects when you attack or get hit in Old School RuneScape.

## Features

- Assign sounds to specific weapons
- Play different sounds for misses, regular hits, and max hits — on both regular and special attacks
- Separate sounds for when *you* take damage (Received Attacks)
- Multiple sound groups per weapon, each with its own triggers, sounds, volume, and activation chance
- Multiple sounds per group — one is picked at random each time the group fires
- Per-group activation chance (0–100%) for randomized playback
- Enable or disable individual weapons without losing their configuration
- Use built-in sounds or drop in your own `.wav` files
- Per-sound volume control and in-panel preview

## Setup

1. Open the **Custom Weapon SFX** panel from the sidebar.
2. Add a weapon — click **Add (Equipped)** while holding a weapon, or click **Add (Search)** to find one by name (requires being logged in).
3. Expand the weapon and configure its sound group(s).
4. To play a sound when you take damage, expand the **Received Attacks** section at the top.

## Sound Groups

Each weapon (and the Received Attacks section) contains one or more **sound groups**. A sound group fires when any of its selected triggers match the outcome of an attack.

Click **+ Add Sound Group** inside an expanded weapon to add another group. Each group has:

- **Chance** — probability the group plays when its trigger fires (100% = always, 0% = never). Useful for adding occasional variation.
- **Sound(s)** — one or more sound files; if multiple are added, one is chosen at random each time the group fires. Click **▶** to preview a sound.
- **Volume** — playback volume per sound (0–100).
- **Triggers** — which hit outcomes cause this group to fire (see below).

Click **+ Add Sound** inside a group to add more sounds to the random pool.

## Triggers

| Trigger | When it fires                         |
|---|---------------------------------------|
| Regular attack zero | Regular attack that deals 0 damage    |
| Regular attack max | Regular attack that is a max hit      |
| Special attack zero | Special attack that deals 0 damage    |
| Special attack max | Special attack that is a max hit      |
| All attacks | All attacks |

The **Received Attacks** section supports a subset: Regular attack zero, Regular attack hit, and All attacks.

## Enabling and Disabling Weapons

Each weapon row has a checkbox in its header. Unchecking it disables that weapon — its sound groups will not fire — without removing any configuration. Check it again to re-enable.

## Using Your Own Sounds

1. Place `.wav` files in `.runelite/customweaponsfx/`.
2. Click **Refresh Sounds** in the panel.
3. Your files will appear in the sound dropdowns alongside the built-in sounds.

## Resetting

Click **Reset All Data** to clear all weapons and sound groups and restore defaults. A confirmation dialog will appear first.

## Known Issues

**Thrall hits** — if you have a thrall active and a *Regular attack zero* or *All attacks* trigger set, thrall zeros and damage will cause the sound to play. Thrall hits are indistinguishable from player hits in the data provided by the game client, so this cannot be filtered out.

**Splashing** - Your players splashing, like thrall zeros, cannot be distinguished from other players splashes, so I opted to not include them in the plugin (everyones splashes around you would cause a SFX).