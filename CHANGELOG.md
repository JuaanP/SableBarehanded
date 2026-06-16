# Changelog

All notable changes to this project will be documented in this file.

## [1.4.1]

### Fixed
- **Keybind Conflict:** Changed the default 'Toggle Pivot' keybind from `Shift` to `Alt` to resolve a conflict with the vanilla Sneak action that prevented the block detachment (assembly) mechanic from triggering properly. *(Note: Players updating from older versions will need to reset the Pivot Key to default).*

## [1.4.0]

### Added
- **Sub-Level Disassembly:** You can now disassemble grabbed sub-levels either by physically slamming them into world blocks or by using a dedicated keybind.
- **Physics Place Toggle:** Added a new keybind to toggle between normal block placement and placing blocks directly as sub-levels.
- **New Config Options:** Added settings to hide the HUD overlays while grabbing, hide custom arms, and a configuration to set a maximum block limit for grabbed structures.

### Fixed
- **Animation Mods Compatibility:** Resolved rendering conflicts with animation mods like Punchy that caused duplicate arms or visual glitches in first person.
- **Continuous Punching Bug:** Fixed an engine issue where the player character would continuously play the attack animation while holding an object.
- **Third Person Animations:** Fixed and properly synchronized third-person grabbing animations so they correctly appear for all players in multiplayer via tracking events.
- **Mount/Seat Ejection:** Fixed a bug where interacting or grabbing objects would unexpectedly kick the player out of their current seat or mount.

## [1.3.3]

### Changed
- **Sable Compatibility:** Updated to support Sable 2.0.1 or later.

## [1.3.2]

### Changed
- **Sable Compatibility:** Updated to support Sable 2.0.0 (thanks to @YassiGame for the PR)

## [1.3.1]

### Fixed
- **Block Interaction:** Fixed a bug where interactable blocks with graphical user interfaces (such as Chests and Barrels) could not be grabbed because their UI would open instead.

## [1.3.0]

### Added
- **Encumbrance System:** Object weight now realistically restricts player movement speed, prevents jumping, and reduces camera sensitivity.
- **Exhaustion System:** Holding, dragging, and pulling heavy objects dynamically drains hunger based on physical effort and tension.
- **New Config Options:** Added multiple parameters in the config menu to fine-tune penalties, hunger drain, and grab physics.

### Improved
- **Grab Realism:** Refined applied force physics, improving the sense of weight, elastic tension (tether), and inertia of heavy structures.
- **Client-Server Communication:** More robust network synchronization.