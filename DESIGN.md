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

### Staying awake
A normal night, not an event and not named: the mod just tracks each player through it.
If the night is **not** skipped, every player who stayed awake through it drops one whole level at dawn.
- **After a Waking Night event:** surviving it takes the Waking Night away completely (counter 0). Liquid Nightmares
  is there to force it back to III fast.
- "Stayed awake" on a normal night = online, awake and active (not AFK) for at least 0% of the night (configurable):
  by default anyone online at dawn stays awake, AFK or not, and logging out costs nothing (it just doesn't drop).
  With a higher percentage, a player who logged in late and stays awake can fall short and keep their level.
  A player who logged in late and the night gets skipped still gets the normal skip check (+1 without comfort).
- AFK = nothing done for 5 minutes (configurable): no looking around, walking, clicking, chatting.
- Optional config (off by default): the awake time only counts outside a comfort zone.
- Waking Night III never drops this way, see the event below.

The current level is shown to the player as a status icon.

| Level            | Grace period (first time) | After that | Effect                         |
|------------------|---------------------------|------------|--------------------------------|
| Waking Night I   | 5 nights                  | 3 nights   | Spot sounds somewhere near the player at night, a configurable list per level that gets worse: I household sounds (doors, chests, footsteps), II mobs plus `wakingnights:insomnia`, III a door being bashed, enderman stare, elder guardian curse plus `wakingnights:insomnia` |
| Waking Night II  | 9 nights                  | 5 nights   | Random insomnia potion effect; very rarely an apparition at night: a see-through mob at the edge of the player's sight, facing them, gone when looked at directly (only that player sees it, no AI) |
| Waking Night III | 14 nights                 | 8 nights   | Can't sleep (insomnia with Lycanites); background ambience (`wakingnights:background`) at night, which everyone hears during a Waking Night; the status icon is the warning that the event is coming |

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
- Potion of Liquid Dreams: clears Waking Night I and II completely (not III), and the level can't rise while it lasts.
- Potion of Liquid Nightmares (instant): takes Liquid Dreams away; without it, one Waking Night level up (none → I → II → III).

## 2. Waking potions

| | Potion of Liquid Dreams | Potion of Liquid Nightmares |
|---|---|---|
| Tagline | "Makes sure you have a good night!" | "Makes sure you have a terrible night!" |
| Effect | Clears Waking Night I and II (not III); level can't rise when you wake up, for 5 seconds. | Instant: removes Liquid Dreams, otherwise one Waking Night level up. |
| Herbalists | Sold for herbs | Sold for herbs |
| Brewing | Awkward Potion + a configurable ingredient (none by default: herbalists only) | Awkward Potion + `biomesoplenty:flower_0:2` (configurable) |
| Tooltip (configurable, & colour codes) | Gold: "Can be obtained through any Herbalist in the Herbalist Hut" | Gold: "An evil concoction brewed from the worst of flora found in biomes such as the Corrupted Scarlet Grove" |
| Splash / lingering / tipped arrows | Yes (vanilla gives these for free) | Yes, so friends can troll each other right as they go to sleep |
| Built | Effect + potion type `wakingnights:liquid_dreams`, white, own icon, 5 seconds, no particles | Instant effect + potion type `wakingnights:liquid_nightmares`, black, no icon (tipped arrows do nothing: instant effects get no arrow duration) |

- When a night is skipped: comfort first (drop a level), then Liquid Dreams (no change), otherwise +1 night.

- **Draught of Dawn:** the expensive way out of Waking Night III (e.g. when another player never reaches III, so no
  event happens). Removes Waking Night completely: counter 0, grace period marked as passed, so 8 nights (normal
  thresholds) until III again. Expensive brew and/or a steep herbalist trade; no ingredients that only drop during events.
- Herb idea *(suggested)*: Rustic chamomile (a real-life sleep aid) for Liquid Dreams.
- Brewing ingredients are configurable.
- Trades are set up in the pack (herbalist structures), not in this mod.

## 3. The Waking Night event: "The Night Wakes"

- No separate warning (unlike Bloodmoon's "A blood moon is rising"): the Waking Night III status icon is the warning.
- Starts at dusk (time 12000, the same moment Bloodmoon decides on its bloodmoons), when **all online players** are at
  Waking Night III. It is server-wide: there's no escaping it.
- Announced at nightfall with a banner at the top of the screen (like Lycanites' mob event images, fading in and out
  over about 7 seconds) and a start sound all around the player, both per night type: `textures/gui/events/<id>.png`
  (falls back to `the_night_wakes.png`) and `startSound=` (bloodmoon_start, lycanites_start, blight_start; dread_start
  is there for The Dread).
- Cannot be skipped.
- **Waking Night III only goes away by surviving a Waking Night:** being active (not AFK) for 30% of the event night
  (configurable, separate from normal nights).
  Comfort sleep and staying awake on a normal night don't lower it.
  - Not active enough (AFK) or logged out: the player keeps III and faces the next one.
  - Surviving takes the Waking Night away completely (counter 0) and ends the grace period.
- Known consequence: as long as one online player isn't at III, there's no event, so a player at III stays at III.
- Picks one night type (see below).
  - Each night type has a weight. The night picked last time has its weight divided by 3, so the same night rarely
    repeats. With only Bloodmoon and Lycanites this gives exactly 75% for the other one.
- Timeline:
  - **Dusk (12000):** the night type is picked. Sky, fog and clouds turn to the night type's colours and the stars go
    out while the sun sets (only the moon keeps shining): the warning of what's coming. Lightning starts at 1/10 of
    the night type's amount and builds up to all of it by nightfall.
  - **Nightfall (13000, the sun is gone):** the event truly starts: title banner, lightning, spawns.
  - **Dawn (22460 to 23460):** the colours fade back as the sun rises.
- Per night type (config): sky, fog, cloud and lightning colours (hex RGB, default black), a light colour the light
  from the sky is multiplied by (block light like torches keeps its colour), lightning strikes per minute around each
  player and the thunder pitch.
  - Bloodmoon: dark red sky and fog, pitch black clouds and lightning, faint dark red light outside.
  - Blight: black sky, fog and clouds, purple moon and lightning, faint muted dark red light outside.
  - Lycanites: near-black green sky, muted grey-green lightning and light, pale moon (the bright green got old fast)
  - Lycanites hook (`hook=lycanites`): from 10 s after nightfall, 2 or 3 waves (50/50) of Lycanites world events,
    each stretched to fill its share of the night (Lycanites' tick count is held back), 20 s quiet between them. Events
    come from the `Lycanites Events` whitelist as a bag without repeats (kept with the world): each plays once before any
    plays again; the wave still going at dawn is
    stopped. A wave's mobs vanish when the next one starts, the last wave's at dawn (`Lycanites Despawn Waves`,
    tamed ones stay); wave mobs are tagged (night type + wave number), so ones in unloaded chunks vanish when they
    load in again. A poopparty wave turns the sky, fog and clouds brown with brown light, a mustard yellow moon and yellow lightning
    (hardcoded), back to the night's colours once it's over. `/wakingnights lycanites` lists every loaded event (chat and log). Left out by default: boss altar events,
    seasonal events, cinderfall, dragonsroar, eruption, and blackplague/shadowgames (they set the time to 20000, skipping most of the night).
    outside (as dark as the bloodmoon's red), black fog and clouds, normal moon.
- The flash of a strike (sky, fog, ground) is in the lightning colour; black lightning is a pulse of darkness.
- Hooks: a night type can start another mod's night with `hook=<id>`, at nightfall, stopped at dawn.
  - `bloodmoon`: lumien's Bloodmoon, started through its private setBloodmoon(true) (its public force() only works at
    exactly time 12000 and could miss by a night). No "a blood moon is rising" message. Looked up at runtime.
- Lightning is visual only (no fire or damage), 16 to 80 blocks from each player. Dark colours are drawn with normal
  blending, since vanilla's additive lightning can't be darker than the sky behind it.
- Additional spawns: each night type can have a weighted list of entities (`spawns=`, `spawnInterval=`, `spawnGroup=`,
  `spawnCap=`, `spawnInSightAfter=`, `spawnCloseAtHalf=`): groups every few seconds; each spawn can be held back until
  the night has happened a few times (`<id> <weight> 3`: from the 3rd time on, counted per world), so the harder ones
  only join later (Blight and Dread: easy ones from the 1st, middle ones from the 2nd, the hardest from the 3rd, with
  low weights); that spawn around players once the sun is gone like vanilla spawning does: out of sight, 24 to 48 blocks
  away, on solid ground, not in lit places. Every one is blooded or blighted at random and hunts the player.
  - They're marked in their ForgeData: `EagleMixinsOverworldAllowed` (EagleMixins doesn't kill SRP mobs outside their biomes)
    and `wakingnights_event` = the night type. **They never outlive their night:** at dawn (or when another night
    starts) the loaded ones are removed, and one that loads in later from a chunk that wasn't loaded is removed on the spot.
  - The Blight is rarer than the others (weight 0.25, about 1 in 9 Waking Nights) and spawns SRP mobs: assimilated
    animals, humans and villagers, a few primitives and ferals; no ranged, flying, spreading or boss parasites.

### Night types
Night types are config entries (subtitle, lightning colour, optional mod hook, spawn list, weight), so new ones need no code.

| Subtitle         | What happens | Lightning | Essence |
|------------------|--------------|-----------|---------|
| `- BloodMoon -`  | Starts a Bloodmoon (Bloodmoon mod). | Black | Blood |
| `- Lycanites -`  | Starts a Lycanites mob event. *(suggested)* From a shortlist that fits the lore (e.g. aberrant_assimilation, shadowgames, blackplague, hellsfury), no joke events. | Purple | — |
| `- The Dread -`  | Ice and Fire dread mobs rise in a snowstorm. Rare (weight 0.25). Dark blue sky, cyan moon and lightning, very light blue light, storm with snow drawn everywhere (no snow layers), SimpleDifficulty temperature -5. Own spawns: dread thralls, ghouls, scuttlers, beasts, knights, horses, liches (not the lich skull, a projectile); 12% of them blooded or blighted. | Cyan | Blooded and blighted |
| `- The Mist -`   | Thick fog. A single wingless Assimilated Ender Dragon (`srparasites:sim_dragone`) hunts the player and always drops blood essence. Dispatcher tentacles (`srparasites:dispatcherten`) sometimes come up from the ground to trap the player. Other SRP mobs very rarely, to keep it creepy. | — | Blood (guaranteed from the dragon) |
| `- The Blight -` | Parasites surface (Project Arcturus lore). SRP mobs only; SRP evolution points are not touched. | *(suggested)* Sickly green | Blight |
- Participation rewards: the blight and blood essence that special entities drop.

### Pack-side config
- Natural bloodmoons keep happening (Bloodmoon's own `Chance`): a Waking Night uses Bloodmoon but is something else.
  Bloodmoon's own sky, moon and light are only switched off during a Waking Night.

## 4. Special entities (blighted / blooded)

- Only during a Waking Night, once the sun is gone: 12% of the hostile mobs spawning within 64 blocks of a player turn
  special (natural spawns and Bloodmoon's), until dawn: at most 15 alive at once per player, at least 3 seconds
  apart, and none for 60 seconds after the player dies. All configurable. Blooded or blighted per night type (`special=`): Bloodmoon blooded, Blight blighted,
  Lycanites mixed. Bosses, ranged mobs (Minecraft's ranged marker, plus blaze, ghast, shulker, guardian and evoker in
  the blacklist) and the rest of the blacklist never do.
  - Lycanites creatures don't carry Minecraft's hostile marker (IMob); hostile, untamed, non-minion Lycanites creatures
    count as hostile too (Lycanites' own isHostile()).
- Dimensions per night type (, default the overworld only). During a Waking Night every dimension of any
  night type has one going on: the picked one where it happens, elsewhere one picked by weight among those that do.
  Each runs its own specials, own spawns, Lycanites waves, temperature, banner and start sound; sky and weather follow
  the overworld's. Stored per dimension in the server-wide data.
  Defaults: every night type only in the overworld (the other dimensions didn't work out yet: parasites are refused
  in the Nether, see /wakingnights whoblocks). During a Waking Night, awake time only counts in a dimension the night
  is going on in, so a player who spends more than a quarter of it elsewhere hasn't survived it.
  Own spawn lists per dimension (`spawns.<dimension>=`): the Blight's End list adds flying parasites (Primitive Yelloweyes,
  carriers, light bombers). Lycanites event whitelists per dimension (`Lycanites Events Per Dimension`): Nether and End
  get their own fitting events; each dimension picks its own event per wave.
- Ramp per night type (): its own spawns' amount and strength the 1st, 2nd, ... time it happens (Blight
  0.6/0.8/1.0, the rest 1.0), counted server-wide.
- Hunting mobs (specials and a night's own spawns) don't push each other, so a crowd can't shove its own into lava.
- Which mobs can turn special depends on the night type (`specialMobs=`): Bloodmoon any hostile mob, Lycanites only
  Lycanites creatures (`lycanitesmobs:*`), the Blight only parasites (`srparasites:*`), the Dread only dread mobs
  (`iceandfire:dread_*`).
- Tougher (configurable): health x1.5, speed x1.15, armor x1.5, +50% knockback resistance (attribute modifiers),
  melee damage x1.25 (applied when the hit lands, for any mod's mobs; projectiles aren't boosted).
- Hits (also arrows) give effects, a list of `<potion> <amplifier> <ticks>`, default `srparasites:bleed 0 100`
  (5 seconds of bleeding). A night type can have its own with `hitEffects=a;b`.
- Walking special mobs break wooden doors (vanilla rules: Hard difficulty, mobGriefing on).
- Their growths and particles glow in the dark (fully lit), so they can be spotted from afar.
- Special mobs always know where the nearest player is (within 128 blocks) and hunt them for as long as the Waking
  Night lasts: their own AI stays active (they may switch to another player they see, or to whatever hurt them), and
  whenever they have nothing to go for they're pointed at the nearest player again. Far away targets are approached in
  stretches (follow range 64).
  A special mob only belongs in a Waking Night that makes its kind: at dawn, and when another night starts, the
  loaded ones that don't belong vanish in a puff of smoke; one that loads in with a chunk and doesn't belong (a blooded
  mob outside a bloodmoon, or in a blight) is removed right away. Nothing extra is stored per mob, its kind decides.
  `despawn=false` on a night type keeps its kind around outside its nights (they stay special until killed, but stop hunting).
- Killed by a player, blooded mobs drop blood essence and blighted ones blight essence (the Wardlights items, which
  feed the Dormant Badge recipe). Configurable lists, one drop per line as `<item> <chance in percent>`. Bloodmoon
  nights make blooded mobs, Lycanites and Blight nights blighted ones.
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
