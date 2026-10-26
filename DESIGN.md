# Waking Nights — Design

Target pack: RLCraft: Dregora Edition (1.12.2).

Waking Nights introduces the Waking Night event, a semi-skippable event that rewards players for participating.

Items marked *(suggested)* are proposals that haven't been confirmed yet.

## 1. Waking Night level (buildup)

Every player has their own Waking Night counter.
Players can't stay awake while others sleep: a night is either skipped for everyone or not at all.
When a night is skipped, every online player is checked:
- No Comfort IV and no Liquid Dreams effect: counter +1.
- Comfort IV or higher: drop one whole level (see below).
- Liquid Dreams active (without Comfort IV): no change.

### Brave Night (default)
If the night is **not** skipped, every player who stayed awake through it drops one whole level at dawn.
- **Exception, after a Waking Night event:** surviving it puts the counter just below III (normal thresholds: 7), so a
  single uncomfortable night puts the player back at III. Comfort sleep or another Brave Night lowers it as usual.
- "Stayed awake" = online and awake for at least 75% of the night (configurable).
  A player who logged in late and stays awake keeps their level (no change).
  A player who logged in late and the night gets skipped still gets the normal skip check (+1 without comfort).
- Optional config (off by default): the awake time only counts outside a comfort zone, so AFKing in the base doesn't count.

The current level is shown to the player as a status icon.

| Level            | Grace period (first time) | After that | Effect                         |
|------------------|---------------------------|------------|--------------------------------|
| Waking Night I   | 5 nights                  | 3 nights   | Random sounds play             |
| Waking Night II  | 9 nights                  | 5 nights   | Random insomnia potion effect  |
| Waking Night III | 14 nights                 | 8 nights   | Night can no longer be skipped |

All thresholds are configurable.

### Grace period
- Each player starts in the grace period, using the longer thresholds.
- It gives a new player enough time to build a comfortable house (Comfort IV, roughly 5–8 in-game days),
  so they can easily keep the first Waking Night from happening.
- It ends once the player has been through their first Waking Night event.

### Comfort check
When the night is skipped, check the player's ChunkComfort effect (`chunkcomfort:comfort`; the amplifier is the comfort tier).
- No comfort effect, or tier below the minimum: counter +1.
- Tier at or above the minimum: drop one whole level. The counter snaps to the start of the level below
  (normal: III -> 5, II -> 3, I -> 0; grace: III -> 9, II -> 5, I -> 0).
- Default minimum: Comfort IV (150 comfort), configurable. The config uses the tier number shown in game
  (Comfort IV = potion amplifier 3).
- ChunkComfort is looked up at runtime, not a build dependency. Without it, every sleep counts as uncomfortable.

### Other ways the level changes
- Potion of Liquid Dreams: prevents the level from rising (does not lower it).
- Potion of Liquid Nightmares: Waking Night III after you wake up.
- Later: some entities can inflict a Waking Nightmare (same effect as Liquid Nightmares).

## 2. Waking potions

| | Potion of Liquid Dreams | Potion of Liquid Nightmares |
|---|---|---|
| Tagline | "Makes sure you have a good night!" | "Makes sure you have a terrible night!" |
| Effect | Level can't rise when you wake up. Immune to Liquid Nightmares for 20 minutes. | Waking Night III after you wake up. |
| Herbalists | Sold for herbs | Sold for herbs |
| Brewing (later game) | The same herbs the herbalists take; early-to-mid-game ingredients so players explore | Super common ingredients: a dirt cheap horror drink |
| Splash / lingering / tipped arrows | Yes (vanilla gives these for free) | Yes, so friends can troll each other right as they go to sleep |

- Herb idea *(suggested)*: Rustic chamomile (a real-life sleep aid) for Liquid Dreams.
- Brewing ingredients are configurable.
- Trades are set up in the pack (herbalist structures), not in this mod.

## 3. The Waking Night event: "The Night Wakes"

- Announced with the title **The Night Wakes** and a subtitle naming the night, e.g. `- BloodMoon -` or `- Lycanites -`.
- Happens only once **all online players** are at Waking Night III. It is server-wide: there's no escaping it.
- Cannot be skipped.
- Players stay at level III until comfort sleep lowers it.
- Picks one night type (see below).
  - Each night type has a weight. The night picked last time has its weight divided by 3, so the same night rarely
    repeats. With only Bloodmoon and Lycanites this gives exactly 75% for the other one.
- Thunderstrikes everywhere, coloured per night type.
  - *(suggested)* Visual-only bolts near players so bases don't burn down; optional small chance of a real strike.
- Additional spawns: each night type has a configurable list of entities that spawn close to players, but not on top
  of them, and following normal spawning rules.

### Night types
Night types are config entries (subtitle, lightning colour, optional mod hook, spawn list, weight), so new ones need no code.

| Subtitle         | What happens | Lightning | Essence |
|------------------|--------------|-----------|---------|
| `- BloodMoon -`  | Starts a Bloodmoon (Bloodmoon mod). | Black | Blood |
| `- Lycanites -`  | Starts a Lycanites mob event. *(suggested)* From a shortlist that fits the lore (e.g. aberrant_assimilation, shadowgames, blackplague, hellsfury), no joke events. | Purple | — |
| `- The Dread -`  | Ice and Fire dread mobs (dread knights, dread ghouls, ...) rise. | *(suggested)* Pale blue | Blight |
| `- The Mist -`   | Thick fog. A single wingless Assimilated Ender Dragon (`srparasites:sim_dragone`) hunts the player and always drops blood essence. Dispatcher tentacles (`srparasites:dispatcherten`) sometimes come up from the ground to trap the player. Other SRP mobs very rarely, to keep it creepy. | — | Blood (guaranteed from the dragon) |
| `- The Blight -` | Parasites surface (Project Arcturus lore). SRP mobs only; SRP evolution points are not touched. | *(suggested)* Sickly green | Blight |
- Participation rewards: the blight and blood essence that special entities drop.

### Pack-side config *(suggested)*
- Bloodmoon `Chance=0` and Lycanites random events off, so Waking Nights is the only source of these events.

## 4. Special entities (blighted / blooded)

- x% chance for an entity to be blighted or blooded.
- Blighted drop blight essence, blooded drop blood essence (item configurable, or a loot table).
  - *(suggested)* Default to the existing Wardlights items (`wardlights:blight_essence`, `wardlights:blood_essence`),
    which feed the Dormant Badge recipe.
- Both carry pulsating growths (Blockbench model) and are stronger than usual.
- *(suggested)* Config for whether a special entity can also be a Champion / Infernal mob.

## 5. Reward nights (future)

### Night Bloom
- On certain nights, rare herbs bloom in the wild and can only be harvested before sunrise.
- They are ingredients for Liquid Dreams, so venturing out at night earns the potion that protects you, and feeds the
  herbalist economy.
- *(suggested)* Plants from Pale Bloom.

## Mods hooked
- Bloodmoon
- Lycanites Mobs
- Ice and Fire (The Dread spawns)
- Scape and Run: Parasites (The Mist, The Blight spawns)
- ChunkComfort (runtime lookup)
- Wardlights (essence items)

## Build order
1. Core: per-player counter, comfort check on wake-up, levels I–III and their effects, grace period, status icon, debug command.
2. Waking potions: Liquid Dreams, Liquid Nightmares, splash variants, brewing recipes.
3. Waking Night event: all-players check, unskippable night, Bloodmoon / Lycanites hooks, coloured lightning, additional spawns.
4. Special entities: blighted/blooded chance, stats, essence drops, client sync.
5. Growth renderer: Blockbench model with pulse.

## Open questions
- Default brewing ingredients for both potions (later).
- Lycanites essence: blood, blight, or none?
- The Mist: one dragon per player or one for the server? Does it need its wings removed by us?
- The Mist: the dragon has 260 health / 30 damage — fine for a player's first Waking Night, or keep The Mist out of the first one?
- Dispatcher tentacles may need their parent dispatcher to behave; test spawning them on their own.
