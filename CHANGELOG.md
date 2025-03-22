# Changelog

<!-- next-header -->

## [Unreleased]

> Released on ReleaseDate

## [0.13.0]

> Released on 2025-03-22

### Added

- Whiplash modifier
- Let Them Rest modifier
- More trials
- Casual Approach modifier

### Fixed

- Trial groups not updating their version on load
- Missing error message localization for trials

## [0.12.0]

> Released on 2025-03-21

### Added

- Exotic Goods modifier
- Over the Edge modifier
- Multiclassing modifier
- Drought modifier

### Changed

- Clarified that Golden Colossus disables master thieves' armband

### Fixed

- Original items being lost with some mimics modifiers
- Cursed not applying to items held by mimics or statues
- Toxic Gas Room not having any gas with Thunderstruck

## [0.11.0]

> Released on 2025-03-19

### Added

- Prison Express modifier
- Crumbled Stairs modifier
- Crooked Die modifier
- Repopulation modifier
- Resurrection modifier
- Fractal Hive modifier

### Changed

- Reworked Golden Colossus to introduce shield cap

### Fixed

- Traps not getting revealed properly with thunderstruck
- Supernova not using elemental effects for bombs
- Dwarf King not spawning mobs in phase 2
- Softlock on Dwarf King with Thunderstruck
- Tier 6 weapons not being affected by untiered

## [0.10.0]

> Released on 2025-03-17

### Added

- Constellation modifier

### Changed

- Bombermob has 1% to spawn a supernova instead of a bomb

### Fixed

- Missing heap serialization code, causing crashes with mimics and extermination challenges
- Incorrect contact info shown in crash logs

## [0.9.1]

> Released on 2025-03-16

### Fixed

- Missing dependencies for Postpaid loot and mimics challenges

## [0.9.0]

> Released on 2025-03-16

### Added

- Crash report utility for android builds
- Extermination modifier
- Postpaid Loot modifier
- Fake Dungeon modifier
- Fort Knox modifier
- Treasure Chest Party Quest modifier
- Shrouding Presence modifier

### Changed

- Allies no longer block your vision with Bulky Frame
- Custom modifiers are now saved across game restarts

### Fixed

- Wall destroying effects affecting diagonally adjacent doors
- Some mobs not getting affected by challenges
- Wrong buff ordering with Golden Colossus
- Crash with Loot Paradise

## [0.8.0]

> Released on 2025-03-16

### Major additions

- Trials system

### Added

- Updated to SHPD v3.0.1
- Cursed modifier
- Curse Magnet modifier
- Loot Paradise modifier
- Bombermob modifier
- Golden Colossus modifier

### Changed

- Wraiths spawned by Insomnia are no longer counted for Ascension
- Certainty of Steel now band Salt Cube instead of Chalice of Blood
- Certainty of Steel now wakes affected targets from maggical sleep by electrocuting them

### Fixed

- Certain actions taking hunger with Certainty of Steel
- Revenge Fury and Rage applying to eveyone on the floor, not just mobs in FoV
- Pandemonium not randomizing missile weapons
- Doors not visually disappearing with Barrier Breaker
- Wand duplication with Pandemonium

## [0.7.1]

> Released on 2025-03-11

### Changed

- Moles can no longer destroy special tiles like Alchemy

### Fixed

- Mobs losing infinite levitation on game relod
- Level generation freeze with Slippery Floor

## [0.7.0]

> Released on 2025-03-11

### Added

- Dungeon Loft modifier
- Copy and paste functionality to challenges window
- Bulky Frame modifier
- Insomnia modifier
- Slippery Floor modifier

### Changed

- Nerfed corrosion duration caused by intoxication

### Fixed

- Certainty of Steel giving shielding on game reload

## [0.6.0]

> Released on 2025-03-09

### Added

- Unstable Accessories modifier
- Pandemonium modifier
- Barrier Breaker modifier
- Retiered and Untiered modifiers
- Text and sound indication for when Intoxication level increases
- Certainty of Steel modifier
- Moles modifier
- Builder Paradox modifier

### Changed

- Shielding no longer protects from Plague effects

## [0.5.0]

> Released on 2025-03-07

### Added

- Bad Plumbing modifier
- Intoxication modifier
- Plague modifier

## [0.4.0]

> Released on 2025-03-07

### Added

- Colosseum modifier
- Patron Saints modifier
- Persistent Saints modifier
- Holy Water modifier

### Fixed

- Game not being able to save prefs on windows
- Head Start incorrectly incrementing dropped SoU amount

## [0.3.0]

> Released on 2025-03-06

### Added

- Revenge Rage modifier
- Head Start modifier
- Repeater modifier
- Bloodbag modifier
- Extreme Caution modifier
- Duplicator modifier
- Blindness modifier
- Revenge Fury modifier
- Prepared Enemies modifier
- Deeper Danger modifier

### Changed

- Characters affected by Crystal Layers have their defense skill reduced to zero, making it near impossible to dodge enemy (or Hero's attacks)
- Rotten Luck now makes multiple rolls and picks the lowest one
- Enemy that survived death thanks to crystalline barrier will now instantly die once all barrier layers are destroyed
- Great Migration now only takes effect starting from floor 2

### Fixed

- Crystalline Blood affecting bosses instead of mobs

## [0.2.4]

> Released on 2025-03-05

### Fixed

- Application order of crystal barrier
- Crystal Barries SFX playing while out of view

## [0.2.3]

> Released on 2025-03-05

## [0.2.2]

> Released on 2025-03-05

### Changed

- Pointed update checker at the right repository
- Removed support buttons and windows
- Application titile to tcpd

## [0.2.1]

> Released on 2025-03-05

### Changed

- All classes and modifiers are unlocked by default

## [0.2.0]

> Released on 2025-03-05

### Added

- Second Try modifier
- Arrowhead modifier
- Thunderstruck modifier
- Crystalline Shelter modifier
- Crystalline Blood modifier

### Changed

- Disabled hero remains
- App icon to match TCPD

## [0.1.0]

> Released on 2025-03-04

### Added

- Great Migration modifier
- Racing The Death modifier
- Evolution modifier
- Mutagen modifier
- Cardinal Disability modifier
- Horde modifier
- Invasion modifier
- Rotten Luck modifier

## [0.0.0] - ???
- Project seeded on top of Shattered Pixel Dungeon v3.0.0

<!-- next-url -->
[Unreleased]: https://github.com/juh9870/TooCruelPixelDungeonTrials/compare/TCPD-0.13.0...HEAD
[0.13.0]: https://github.com/juh9870/TooCruelPixelDungeonTrials/compare/TCPD-0.12.0...TCPD-0.13.0
[0.12.0]: https://github.com/juh9870/TooCruelPixelDungeonTrials/compare/TCPD-0.11.0...TCPD-0.12.0
[0.11.0]: https://github.com/juh9870/TooCruelPixelDungeonTrials/compare/TCPD-0.10.0...TCPD-0.11.0
[0.10.0]: https://github.com/juh9870/TooCruelPixelDungeonTrials/compare/TCPD-0.9.1...TCPD-0.10.0
[0.9.1]: https://github.com/juh9870/TooCruelPixelDungeonTrials/compare/TCPD-0.9.0...TCPD-0.9.1
[0.9.0]: https://github.com/juh9870/TooCruelPixelDungeonTrials/compare/TCPD-0.8.0...TCPD-0.9.0
[0.8.0]: https://github.com/juh9870/TooCruelPixelDungeonTrials/compare/TCPD-0.7.1...TCPD-0.8.0
[0.7.1]: https://github.com/juh9870/TooCruelPixelDungeonTrials/compare/TCPD-0.7.0...TCPD-0.7.1
[0.7.0]: https://github.com/juh9870/TooCruelPixelDungeonTrials/compare/TCPD-0.6.0...TCPD-0.7.0
[0.6.0]: https://github.com/juh9870/TooCruelPixelDungeonTrials/compare/TCPD-0.5.0...TCPD-0.6.0
[0.5.0]: https://github.com/juh9870/TooCruelPixelDungeonTrials/compare/TCPD-0.4.0...TCPD-0.5.0
[0.4.0]: https://github.com/juh9870/TooCruelPixelDungeonTrials/compare/TCPD-0.3.0...TCPD-0.4.0
[0.3.0]: https://github.com/juh9870/TooCruelPixelDungeonTrials/compare/TCPD-0.2.4...TCPD-0.3.0
[0.2.4]: https://github.com/juh9870/TooCruelPixelDungeonTrials/compare/TCPD-0.2.3...TCPD-0.2.4
[0.2.3]: https://github.com/juh9870/TooCruelPixelDungeonTrials/compare/TCPD-0.2.2...TCPD-0.2.3
[0.2.2]: https://github.com/juh9870/TooCruelPixelDungeonTrials/compare/TCPD-0.2.1...TCPD-0.2.2
[0.2.1]: https://github.com/juh9870/TooCruelPixelDungeonTrials/compare/TCPD-0.2.0...TCPD-0.2.1
[0.2.0]: https://github.com/juh9870/TooCruelPixelDungeonTrials/compare/TCPD-0.1.0...TCPD-0.2.0
[0.1.0]: https://github.com/juh9870/TooCruelPixelDungeonTrials/compare/TCPD-0.0.0...TCPD-0.1.0
[0.0.0]: https://github.com/juh9870/TooCruelPixelDungeonTrials/tree/TCPD-0.0.0