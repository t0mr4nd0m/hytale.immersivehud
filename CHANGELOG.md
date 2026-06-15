# Changelog

## v1.2.61

**Features**
* Changed in game plugin image

## v1.2.6 - Hytale 0.5.2

**Features**
* Updated available and default triggers of dynamic components
* Added in game plugin image
* 
**Technical Changes**
* Update to Hytale 0.5.2

## v1.2.5 - Hytale 0.5.1

**Technical Changes**
* Update to Hytale 0.5.1

## v1.2.4 - Hytale 0.5.0 + IN_COMBAT trigger

**Features**
* Added new dynamic trigger IN_COMBAT. Activates when a hostile npc is in front of player or a npc is attacking player
* Now world pauses when config UI is open
* When joining a world the HUD will remain visible for a brief moment before applying the HUD visibility configuration.
* Changed `ammo` to dynamic component with default rules `HOLDING_RANGED_WEAPON` and `IN_COMBAT`

**Technical Changes**
* Updated to Hytale 0.5.0
* Replaced deprecated method getActiveHotbarItem with IntentoryComponent.getItemInHand
* Removed obsolete commands: status, profile, toggle and rules
* Extensive refactor: code cleaning, improved logic and readability

## v1.2.3 - Technical update + added StatusIcons to dynamic components + Config UI redesign

**Features**
* Added StatusIcons to dynamic component catalogue, associated to trigger 'HOTBAR_INPUT'. However, doing so, I've found that the component cannot be hide using Hytale API (Am I doing something wrong? Maybe is it a bug from Hytale API?). Further testing and analysis will be required.
* Minor tweaks to **'Immersive'** profile to reduce even more hud visibility.
* Redesigned config UI. Final version? Maybe, probably, I don't know, but I like it more this way. :)

**Technical Changes**
* Extensive refactor and code cleaning to avoid duplication, remove dead code, improve logic and readability, etc...

## v1.2.2 - Technical update + new trigger BLOCKING_ATTACK

**Features**
* Added new trigger `BLOCKING_ATTACK` associated to `reticle` component

**Technical Changes**
* Extensive refactor and code cleaning, simplified package structure, removed redundant logic and renamed classes, improved structure, profiles, commands, and more maintainable runtime system focused on performance and flexibility

## v1.2.1 - Technical update

**Features**
* Added new trigger: PLAYER_IDLE, activates when player is not moving
* Added new trigger: PLAYER_SLEEPING, activates when player is sleeping

**Technical Changes**
* Core:
  * Refactored dynamic HUD system to use a unified rules-based model
  * Cleaner and more predictable behaviour across components
* Dynamic Rules & Triggers:
  * Improved trigger consistency and naming
  * Better alignment between runtime context and rule evaluation
  * Reduced duplication in trigger sources (packet vs tick)
* Configuration:
  * Refactored configuration architecture
  * Cleaner separation between GlobalConfig and PlayerConfig
  * Improved config copying and default propagation
  * Automatic sanitization and missing entry recovery
  * Standardized rules handling
  * Ensured all dynamic components always have entries
* Runtime & Performance:
  * Improved HUD update pipeline
  * Clear separation between:
    * per-tick evaluation 
    * throttled reticle scanning
  * Reduced unnecessary processing and clarified tick responsibilities
  * Better state tracking
  * Cleaner handling of held item detection
  * Reduced redundancy between packet-driven and snapshot-based systems
* Code Quality:
  * General refactoring and clean-up
  * Reduced code duplication
  * Improved naming consistency
  * Better separation of responsibilities (config, runtime, UI)

**Bug Fixes**
* Trigger logic `CHARGING_WEAPON` reticle not hiding when hotbar swap interrupt charging weapon
* Trigger logic `HOTBAR_INPUT` missing first slot swap

## v1.2.0

**Features**
* Added Oxygen bar to dynamic components
* Added threshold configuration for HUD bar components (`health`, `stamina`, `mana` and `oxygen`)
* Removed triggers: `HEALTH_LOW`, `HEALTH_CRITICAL`, `STAMINA_LOW`, `STAMINA_CRITICAL`, `MANA_LOW` and `MANA_CRITICAL`
* Config UI redesign (still a WIP)

## v1.1.0

**Features**
* Added Config UI. Run command `/ihud config` to configure the mod using custom ui interface.

**Bug Fixes**
* Fixed compatibility issue with the latest game version. // 2026.03.26-89796e57b

## v1.0.3

**Features**
* Added more rules related to player movement status: `PLAYER_SWIMMING`, `PLAYER_FLYING`, `PLAYER_GLIDING`, `PLAYER_JUMPING`, `PLAYER_CROUCHING`, `PLAYER_CLIMBING`, `PLAYER_IN_FLUID`, `PLAYER_ON_GROUND`, `PLAYER_FALLING`, `PLAYER_SITTING` and `PLAYER_ROLLING`

**Technical Changes** 
* Improved config file system (sorry guys but if you are not using the default configuration you will need to customize your configuration again)
* Centralized all HUD component definition (Ex. config key, default visibility and default rules) in one class to make it easier to maintain and more stable

**Bug Fixes**
* Fixed bug with packet_watcher not detecting the item in hand properly and not releasing `CHARGING_WEAPON` rule

## v1.0.2

**Technical Changes**
* Added method getPluginVersion to get plugin version from manifest and pass it to GlobalConfig 

## v1.0.1

**Technical Changes**
* Removed rule `ALWAYS_HIDDEN` and all the related logic. To get the same behaviour for a component, simply toggle its visibility to hide and clear all its rules

## v1.0.0 - Initial release

**Features**
* Hud visibility framework
* Dynamic Hud visibility based on triggers
* Quick configuration using profiles

**Commands**
* `/ihud status` : check ihud player configuration
* `/ihud toggle` <component> <action> : toggle hud components. actions:[show|hide]
* `/ihud rules` <component> <action> <rule> : list, clear, add or remove rules from dynamic hud components. actions:[list|clear|add|remove]
* `/ihud profile` <profile> : apply quick setup. profiles:[default|immersive|disabled]