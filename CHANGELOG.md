# Changelog

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