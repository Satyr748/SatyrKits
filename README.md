# SatyrKits

OP-only kit manager plugin for Paper 1.21.4, built with Java 21.

## Features
- `/kit` opens the management menu.
- Create Kit saves your current inventory, armor, and offhand.
- Claim Kit lets an OP choose a saved kit.
- Give Kit All gives a selected kit to all online players.
- Delete Kit lets an OP select and permanently delete a saved kit.
- Kits are saved in `plugins/SatyrKits/config.yml`.

## Build
Run `mvn package` with Java 21 installed. The resulting JAR is `target/SatyrKits.jar`.
