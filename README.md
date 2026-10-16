# Dead by Daylight: The Forest — Fabric prototype

Minecraft Java **1.20.1 + Fabric**. This is an early multiplayer prototype.

## Build a JAR on GitHub
Open **Actions** → **Build Fabric mod JAR** → **Run workflow**. When it finishes, download `dbd-fabric-jar` from the run's **Artifacts** section.

## Included
- `/dbd start`, `/dbd stop`, `/dbd status`
- Generators: right-click 10 times to repair; the match tracks up to five
- Sacrificial hook placeholder, killer blade, field medkit
- Russian and English names

## Install
Use Minecraft Java 1.20.1, Fabric Loader, and Fabric API. Put the downloaded JAR in the `mods` folder on both client and server.

## Prototype limitations
Role assignment, carrying/hooking/rescue, exit gates, generated forest arena, custom textures, sounds, and animations are not implemented yet. The JAR must be built and tested via GitHub Actions before treating it as verified.

License: MIT
