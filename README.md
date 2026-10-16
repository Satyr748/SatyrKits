# SatyrKits

OP-only kit management for Paper 1.21.x.

## Features
- `/kit` opens a GUI.
- **Create Kit** saves your current storage inventory, armor, and offhand. Enter the kit name in chat.
- **Claim Kit** lets you select a saved kit.
- **Give Kit All** gives a selected kit to all currently online players.
- Kits are saved in `plugins/SatyrKits/config.yml`.
- Only OPs (or users explicitly granted `satyrkits.admin`) can use the plugin.

## Requirements
- Minecraft server running Paper 1.21.x
- Java 21 JDK
- Maven 3.9 or newer

## Build
1. Install Java 21 and Maven.
2. Open a terminal in the folder containing `pom.xml`.
3. Run:
   ```bash
   mvn clean package
   ```
4. The plugin jar will be at `target/SatyrKits.jar`.

The project currently compiles against Paper API `1.21.4`. For another 1.21.x server version, change the `paper-api` version in `pom.xml` to the matching available API version if needed.

## Install
1. Stop your server.
2. Copy `target/SatyrKits.jar` into the server's `plugins` folder.
3. Start the server.
4. Make sure your account is OP, then run `/kit`.

## Commands
- `/kit` — open the menu
- `/kit create <name>` — save your current inventory as a kit
- `/kit claim <name>` — receive a kit
- `/kit giveall <name>` — give a kit to all online players

## Notes
- Claiming a kit adds its stored items to the player's inventory; items that do not fit are dropped at the player's location.
- Give Kit All targets players who are online at the time the action is used.
