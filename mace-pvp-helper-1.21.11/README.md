# Mace PvP Helper — 1.21.11 / Lunar Client

Client-side Fabric mod for Minecraft 1.21.11. Adds configurable hotbar presets
plus a **one-key stun slam combo** that fires axe → spear → mace in a single
game tick (50 ms).

## ⚠ Server rules

Macros are bannable on most public servers (Hypixel, most ranked networks).
Only use on servers where macros are explicitly allowed.

## The stun slam combo

Press one key → three hits land in the same tick:

```
Tick 0:  select axe slot   → attack
         select spear slot → attack
         select mace slot  → attack
```

**Why this works mechanically:**
- Swapping weapons instantly resets the attack-strength cooldown on the new
  weapon, so every hit is fully charged.
- The server processes all incoming packets from a client *before* advancing
  invulnerability frames, so the 2nd and 3rd hits land before i-frames apply.
- The axe disables the shield, the spear lands during the shield-down window,
  the mace smash applies stun on the same tick.

**What the mod does NOT do:**
- Doesn't aim for you. You still need to be looking at the target.
- Doesn't auto-launch you. You need to be airborne (elytra/jump) yourself.
- Doesn't rearrange your hotbar. Arrange axe / spear / mace in the 3 configured
  slots manually (default: slots 1 / 2 / 3).

## Setup

1. Build the jar (see "Building" below)
2. Drop the jar into Lunar Client's Fabric add-on mods folder, or your regular
   Fabric `mods/` folder
3. Launch 1.21.11 with Fabric
4. In game, press `]` to open the Mace PvP menu
5. At the bottom of the menu, confirm Axe/Spear/Mace slots match your loadout
   (defaults: 1/2/3)
6. Open **Options → Controls → Mace PvP** and bind "Stun slam combo" to
   whatever key you like (mouse buttons work)
7. Arrange your hotbar so slot 1 = axe, slot 2 = spear, slot 3 = mace
8. In combat: get airborne → aim at target → press stun slam key

## Building

Requires **JDK 21**. The project needs a Gradle wrapper (not included in this
zip because my build sandbox couldn't generate it). Two options:

**Option A — install Gradle yourself:**
```
winget install Gradle.Gradle      # Windows
brew install gradle               # macOS
```
Then in the project folder: `gradle build`

**Option B — borrow a wrapper:**
1. Download the Fabric example mod from
   <https://github.com/FabricMC/fabric-example-mod>
2. Copy `gradlew`, `gradlew.bat`, and the `gradle/` folder into this project
3. Run `./gradlew build` (or `gradlew.bat build` on Windows)

Either way, the jar lands at `build/libs/mace-pvp-helper-1.0.0.jar`.

## Preset system (from the base mod)

Separate from stun slam, you still have 9 configurable hotbar presets that
rearrange your inventory to a saved loadout on a keypress. Useful for e.g. a
"recovery" preset that front-loads golden apples and totems.

- `]` opens the main menu
- Preset 1–9 keybinds under **Controls → Mace PvP** (unbound by default)
- Each preset can auto-select a specific slot after applying

## Config file

Saved to `config/macepvp.json` in your Minecraft folder. Edit by hand if you
want; the mod reloads it on launch.

## Known gaps

- **No enchantment matching.** If you have two maces in your inventory, the
  preset swapper grabs whichever it finds first — it can't tell Breach from
  plain Density. Doesn't affect the stun slam combo (which uses fixed slots).
- **No fall-distance trigger.** The stun slam fires the moment you press the
  key. Press it too early and the mace hit lands before you reach the target.
  Press it too late and you eat fall damage. Your call on timing.
