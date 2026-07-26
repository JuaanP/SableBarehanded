# Changelog

All notable changes to this project will be documented in this file.

## [1.6.5]

### Compatibility
- **Tree Mods:** Assembly compatibility with tree felling mods (*Tree Physics*, *FallingTree*, *HT's TreeChop*, *Dynamic Trees*, etc...).
- **Tree Logs Tag:** Added `#barehanded:tree_logs` block tag to manually register custom log blocks not automatically detected.
- **Config Options:**
  - `treeAssemblyMode` (Server): Controls tree log behavior when tree mods are present (`AUTO`, `BREAK`, `BLOCK`, `NONE`).
  - `unsneakOnTreeBreak` (Client): Toggle to temporarily un-sneak while breaking logs so tree mods trigger correctly.

## [1.6.4]

### Added
- **Ungrabbable Blocks Check:** New configuration option `preventGrabbingSubLevelsWithUngrabbableBlocks` (enabled by default) to prevent grabbing entire structures if they contain at least one block from the `#barehanded:ungrabbable` tag.

### Compatibility
- **Sable: Ragdolls:** Added new configuration options `sableRagdollsCompatAllowGrabbingPlayerRagdolls` (disabled by default), `sableRagdollsCompatAllowGrabbingMobRagdolls` (enabled by default) and `sableRagdollsCompatMobRagdollMaxSize` to allow or restrict interactions with player and mob ragdolls.
- **Sable: Ragdolls:** Strictly blocked *assemble/disassemble* actions on blocks or sub-levels that contain ragdoll parts or corpses.

## [1.6.3]

### Fixed & Compatibility
- **Sable: Ragdolls Compatibility:** Added compatibility with the *Sable: Ragdolls* mod to prevent interactions with ragdolls that cause server crashes and physics exploits.
- **HUD & Animation Flickering:** Fixed a visual bug where the grab HUD and arm animations would briefly flicker on the client when a grab attempt was rejected or canceled by the server. The client now properly delays rendering the grab state until it receives explicit server confirmation.

## [1.6.2]

### Fixed
- **Multiblock Ripping:** Fixed a bug where double chests and other multiblocks would break in half when ripped off from a sub-level.
- **Ungrabbable Config Logic:** Fixed bug where `allowGrabbingSpawners` and `allowGrabbingUnbreakableBlocks` were not properly denying interactions when set to false.
- **Tag Priorities:** Fixed `grabbable` and `ungrabbable` block tags not being respected under certain configurations. The `grabbable` tag now has absolute priority over all configuration rules.

## [1.6.1]

### Fixed
- **Grab:** Fixed a bug where players could not grab sub-levels resting on top of other sub-levels.
- **Prop-Surfing:** Added detection for mechanical bearing blocks to prevent players from flying by grabbing linked bearing parts while standing on the base.

## [1.6.0]

### Added
- **Assembling/Disassembling onto sub-levels:**  You can now merge/detach a grabbed sub-level directly into another existing sub-level.
- **Grab Keybind:** Added a dedicated keybind (default G) to toggle grabbing sub-levels without needing to hold both mouse buttons.
- **Distance Adjust Keybind:** Added new keybinds (default - and =) to push or pull grabbed objects using the keyboard, as an alternative to the mouse scroll wheel.
- **Camera-Locked Rotation:** Added new configuration options to automatically rotate the grabbed object synchronously with the player's camera pitch and/or yaw.
- **Grabbable Tag and Whitelist Mode:** Added the #barehanded:grabbable block tag and a "Whitelist Mode" config option. When enabled, only explicitly tagged blocks can be grabbed.
- **Spectator Grabbing:** Added a configuration option to allow players in Spectator mode to grab and interact with sub-levels.
- **Grab Unbreakable Blocks Config:** Added an explicit configuration option to allow grabbing unbreakable blocks (like Bedrock, Barriers, or Command Blocks).
- **Config Auto-Backups:** The mod now automatically creates .backup files of your outdated configuration files before migrating them to new versions, ensuring you never lose your custom settings during an update.

### Changed
- **Physics & Elasticity:** Grab physics have been refined. Added new parameters for "Grab Elasticity", heavy object mass curves, and sway edge factors to make handling heavy objects feel more realistic.
- **Scroll Speed:** Distance adjustment speed is now dynamically reduced based on the object's mass and the player's current encumbrance, making heavy objects feel harder to pull close.
- **Input Interception:** Changed how vanilla inputs (mining, using) are suppressed while grabbing to prevent accidental block interactions and continuous punching loops much more reliably.

### Fixed & Compatibility
- **Controller Support:** Fixed an issue where using a controller would cause the grab to drop due to input state overrides. The mod now correctly reads analog triggers without breaking the controller's internal state.
- **Prop-Surfing:** Added more physics checks and tethering to prevent players from flying or infinitely accelerating while standing on or interacting with sub-levels.

## [1.5.2]

### Added
- **Dynamic Scroll Distance:** The maximum scroll distance now automatically adapts to the player's dynamic reach.

### Fixed & Compatibility
- **Vista:** Fixed a bug where the player's body and arms would become invisible when viewed through TVs while the player was in first-person mode.
- **Grab Animation Flickering:** Fixed the visual flickering that occurred on the client when grabbing sub-levels at the maximum distance of the player's reach.

## [1.5.1]

### Fixed
- **Creative Assemble:** Fixed an issue introduced in version 1.5.0 where attempting to grab an object in Creative mode would accidentally mine/break the block instead. Adjusted the client's internal tick priority to properly intercept mouse inputs before the vanilla engine processes block destruction. (thanks to @HintSystem)

## [1.5.0]

### Changed
- **Mod ID:** Changed mod id from "sablebarehanded" to "barehanded".

### Added
- **Scroll Distance Adjustment:** You can now use the mouse scroll wheel to smoothly push or pull grabbed sub-levels closer or further away.
- **Single-Button Grab Retention:** After initially grabbing an object (Left + Right Click), you can now release one of the buttons and keep holding the object as long as either mouse button remains pressed.
- **Ungrabbable Block Tags:** Added the `#barehanded:ungrabbable` block tag, allowing modpack developers to easily blacklist specific blocks via datapacks or KubeJS from being detached or grabbed.
- **Disassembly Size Limit:** Added a new configuration option to restrict the maximum number of blocks a structure can contain in order to be disassembled.
- **Spawner Config Override:** Added a direct config toggle to explicitly allow or prevent grabbing Monster Spawners, overriding the tag system for quick and easy server setup.

### Fixed & Compatibility
- **ImmediatelyFast:** Fixed a rendering issue where first-person arms would completely disappear when grabbing objects alongside the ImmediatelyFast mod.
- **Falling Blocks Fix:** Grabbing or assembling gravity-affected blocks will no longer cause them to update and fall out of the sub-level into the void.
- **Animation Stuck:** Fixed a network desync issue where players would get permanently stuck in the grabbing animation pose if they disconnected, teleported, or died while holding an object.
- **Auto Mining:** Improved input interception to prevent accidental block mining while attempting to assemble or rotate a sub-level.
- **Continuous Punching:** Fully disabled vanilla attack/use inputs while carrying objects to prevent duplicate arms and infinite punching loops with combat animation mods like Punchy or Better Combat.

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