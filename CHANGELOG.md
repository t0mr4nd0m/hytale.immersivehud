# Changelog

## v1.2.1 - Technical update

**Core Changes**

* Refactored dynamic HUD system to use a unified rules-based model
* Cleaner and more predictable behaviour across components

**Dynamic Rules & Triggers**

* Improved trigger consistency and naming
* Better alignment between runtime context and rule evaluation
* Reduced duplication in trigger sources (packet vs tick)
* Added new triggers: PLAYER_IDLE and PLAYER_SLEEPING

**Configuration**

* Refactored configuration architecture
* Cleaner separation between GlobalConfig and PlayerConfig
* Improved config copying and default propagation
* Automatic sanitization and missing entry recovery
* Standardized rules handling
* Ensured all dynamic components always have entries

**Runtime & Performance**

* Improved HUD update pipeline
* Clear separation between:
  * per-tick evaluation 
  * throttled reticle scanning
* Reduced unnecessary processing and clarified tick responsibilities
* Better state tracking
* Cleaner handling of held item detection
* Reduced redundancy between packet-driven and snapshot-based systems

**Code Quality**

* General refactoring and clean-up
* Reduced code duplication
* Improved naming consistency
* Better separation of responsibilities (config, runtime, UI)

## v1.2.0

* Added Oxygen bar to dynamic components
* Added threshold configuration for HUD bar components (health, stamina, mana and oxygen)
* Removed triggers HEALTH_LOW, HEALTH_CRITICAL, STAMINA_LOW, STAMINA_CRITICAL, MANA_LOW and MANA_CRITICAL
* Config UI redesign (still a WIP)

## v1.1.0

* New feature: Added Config UI. Run command `/ihud config` to configure the mod using custom ui interface. 
* Fixed compatibility issue with the latest game version. // 2026.03.26-89796e57b

## v1.0.3

* Improved config file system (sorry guys but if you are not using the default configuration you will need to customize your configuration again)
* Centralized all HUD component definition (Ex. config key, default visibility and default rules) in one class to make it easier to maintain and more stable
* Added more rules related to player movement status: PLAYER_SWIMMING, PLAYER_FLYING, PLAYER_GLIDING, PLAYER_JUMPING, PLAYER_CROUCHING, PLAYER_CLIMBING, PLAYER_IN_FLUID, PLAYER_ON_GROUND, PLAYER_FALLING, PLAYER_SITTING, PLAYER_ROLLING
* Fixed bug with packet_watcher not detecting the item in hand properly and not releasing CHARGING_WEAPON rule

## v1.0.2

* Added method getPluginVersion to get plugin version from manifest and pass it to GlobalConfig 

## v1.0.1

* Removed rule 'ALWAYS_HIDDEN' and all the related logic. To get the same behaviour for a component, simply toggle its visibility to hide and clear all its rules

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