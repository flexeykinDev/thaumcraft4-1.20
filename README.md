<p align="center">
  <img src="docs/banner.svg" width="880" alt="Thaumcraft 4 — unofficial port to Minecraft 1.20.1">
</p>

<p align="center">
  <b>English</b> &nbsp;·&nbsp; <a href="README.ru.md">Русский</a>
</p>

<p align="center">
  <img src="docs/stats-en.svg" width="880" alt="290 automated checks · 201 research entries · 248 items · 186 blocks · 29 creatures · 23 languages">
</p>

An unofficial fan port of **Thaumcraft 4** (Minecraft 1.7.10) by Azanor to **Minecraft 1.20.1 / Forge**.
Mechanics, textures, models, sounds and the Thaumonomicon texts are carried over from the original; the code is rewritten for the modern game.
This project is not affiliated with or endorsed by Azanor.

## ▪ Install in 3 steps

<p align="center">
  <img src="docs/steps-en.svg" width="880" alt="1. Install Forge · 2. Drop in the mods · 3. Play">
</p>

1. **Install Forge or NeoForge.** Download the installer for **Minecraft 1.20.1** — [Forge 47.x](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html) (tested on 47.4.0) or [NeoForge 1.20.1](https://neoforged.net/) (tested on 47.1.106) — run it and choose *Install client*. The same mod jar works on both.
2. **Drop in the mods.** Put three jars into your `mods` folder (on Windows: `Win+R` → `%appdata%\.minecraft\mods`):
   - `thaumcraft4-1.20.1-<version>.jar` from the [Releases](../../releases) page;
   - [Curios API](https://www.curseforge.com/minecraft/mc-mods/curios) 5.9 or newer for Forge 1.20.1 — slots for rings, amulets and belts;
   - [TerraBlender](https://www.curseforge.com/minecraft/mc-mods/terrablender) 3.0.1.7 or newer for Forge 1.20.1 — places the new biomes.
3. **Play.** Pick the *forge* profile in the launcher and start the game. Craft a wand and right-click a bookshelf with it — that gives you the **Thaumonomicon**, the in-game guide to everything else.

For a server, put the same three jars into the server's `mods` folder; every player needs them too.

## ▪ What is inside

| | |
|---|---|
| **Research** | Thaumonomicon with 6 categories and 201 entries, the research table minigame, thaumometer scanning, 50 aspects, forbidden knowledge and warp |
| **Thaumaturgy** | Wands, staves and sceptres from 9 rods and 6 caps, 10 foci, the focal manipulator with upgrades, aura nodes, node jars, stabilizers, transducers and vis relays |
| **Alchemy** | Crucible, essentia, jars and labels, tubes, centrifuge, thaumatorium, alchemical furnaces, metal purification and transmutation |
| **Artifice** | Infusion altar and infusion enchanting, runic shield, thaumium and void metal gear, goggles of revealing, hover harness, arcane bore, mirrors, warded arcana |
| **Golemancy** | 8 golem materials, cores, upgrades and accessories, the crystal bell, the traveling trunk |
| **World** | Magical Forest, Eerie and Tainted biomes, greatwood and silverwood, crystal ores, cinnabar and amber, taint and its creatures, champion mobs |
| **Eldritch** | Obelisks, the Crimson Cult, the Outer Lands dimension with its labyrinth and four bosses |

Modern additions: a tree of **25 advancements** that walks new players from the first wand to the Outer Lands, **JEI support** (arcane workbench, crucible and infusion recipes; aspects as JEI ingredients — press R on an aspect to see which items give it, U to see where it is spent; hints for things made with a wand click), Curios slots instead of Baubles, deepslate ore variants, spawn eggs for every creature, Forge tags for ores and metals,
and optional tin, silver and lead content that appears only when another mod adds those metals.

## ▪ Compatibility

The mod uses standard Forge 1.20.1 mechanisms, so most content mods should simply coexist with it.

| Area | What to expect |
|---|---|
| **Forge / NeoForge** | One jar for both. All 290 automated scenarios pass on Forge 47.4.0 and on NeoForge 1.20.1 (47.1.106). |
| **JEI** | Optional. Shows arcane workbench, crucible and infusion recipes with their vis, essentia and research, and explains wand-click transformations. Items of absent metals are hidden. |
| **Curios** | Required. Baubles go into Curios slots and work alongside other Curios mods. |
| **World generation** | Biomes are added through TerraBlender and do not replace other mods' biomes. Ores are tagged (`forge:ores/...`) so other mods can recognise them. |
| **Metals from other mods** | Tin, silver and lead clusters, nuggets, wand caps and research unlock automatically if a mod provides those metals via Forge tags. |
| **Inventories** | Chests, the traveling trunk and other item containers use vanilla-style inventories, so hoppers, pipes and Create funnels can access them. Essentia is Thaumcraft-only. |
| **Create and other "moving block" mods** | Not tested. Avoid moving multiblock structures (infusion altar, thaumatorium, alchemical constructs), aura nodes and warded blocks with contraptions — they may lose their state. |
| **Renderer and shader mods** | Not tested. Embeddium, Oculus or OptiFine may change how nodes, portals and particles look. |
| **Other Thaumcraft ports** | Cannot be installed together: both use the mod id `thaumcraft`. |

## ▪ Status

**Early release — playable, not yet battle-tested.**
All content of the original is ported, and 290 automated in-game scenarios (crafting, rituals, mobs, bosses, world generation, GUIs) pass on every build.
Not tested yet: long survival playthroughs, dedicated multiplayer servers, large modpacks. Keep backups of your worlds and report problems in [Issues](../../issues).

**Languages:** English, Russian and Ukrainian are complete. 20 more languages come from the original's community translations; strings that are new in this port fall back to English there.

## ▪ Thaumic Tinkerer addon (in progress)

A port of **Thaumic Tinkerer** (by pixlepix and nekosune) is being built in the same repository as a **separate mod**: its own jar, `thaumictinkerer-1.20.1-<version>.jar`, which needs this Thaumcraft port. Put both jars into `mods` to play with it; without it Thaumcraft works as before.

It adds a «Thaumic Tinkering» tab to the Thaumonomicon. Ported so far (part 1 of 3): smokey quartz, Tome of Knowledge Sharing, gaseous light and shadow with the Fume Dissipator, Hyperenergetic Nitor, Essentia Funnel, Thaumic Restorer, Spellbinding Cloth, Talismans of Withhold and Remedium, Helmet of Revealing, Aspectalyzer and Ethereal Platform.
Next: foci, Osmotic Enchanter with its 14 enchantments, magnets and transvectors (part 2), then the late-game Kami/Ichor content (part 3). ComputerCraft, OpenComputers and Applied Energistics integrations are not planned.

## ▪ Screenshots

<table>
<tr><td width="50%"><img src="docs/screens/book_map.png" alt="The Thaumonomicon research map"><br><sub>The Thaumonomicon research map</sub></td><td width="50%"><img src="docs/screens/page_ASPECTS_1.png" alt="A book page about aspects"><br><sub>A book page about aspects</sub></td></tr>
<tr><td width="50%"><img src="docs/screens/altar_crafting.png" alt="Infusion in progress"><br><sub>Infusion in progress</sub></td><td width="50%"><img src="docs/screens/outer_eldritch_warden.png" alt="Outer Lands: the Eldritch Warden"><br><sub>Outer Lands: the Eldritch Warden</sub></td></tr>
<tr><td width="50%"><img src="docs/screens/crimson_leader.png" alt="Crimson Cult praetor"><br><sub>Crimson Cult praetor</sub></td><td width="50%"><img src="docs/screens/trees.png" alt="Silverwood and greatwood"><br><sub>Silverwood and greatwood</sub></td></tr>
<tr><td width="50%"><img src="docs/screens/taint_mobs.png" alt="Tainted land and its creatures"><br><sub>Tainted land and its creatures</sub></td><td width="50%"><img src="docs/screens/focus_fire.png" alt="Wand of fire"><br><sub>Wand of fire</sub></td></tr>
</table>

## ▪ Build from source

Requires JDK 17.

```bash
./gradlew build
```

The jars appear in `build/libs`: `thaumcraft4-1.20.1-<version>.jar` and the addon `thaumictinkerer-1.20.1-<version>.jar`. `./gradlew runClient` starts a development client.

## ▪ Credits

- **Azanor** — creator of Thaumcraft: design, code, textures, models, sounds and texts of the original.
- **pixlepix** and **nekosune** — authors of the original Thaumic Tinkerer addon.
- Translators of the original's language files.
- This port: rewritten code and adaptation to Minecraft 1.20.1.

All rights to Thaumcraft and its original assets belong to their author. This is a non-commercial fan project.
If you are the rights holder and object to it, please open an issue.
