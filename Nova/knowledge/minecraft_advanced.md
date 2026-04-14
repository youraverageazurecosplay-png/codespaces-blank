# Advanced Minecraft Modding Knowledge

Use advanced Minecraft guidance when the repository contains mod loader metadata, build files, or mod source structure.

Platform and ecosystem:

- Determine whether the project is Fabric, Forge, NeoForge, Paper, Spigot, or a datapack, and keep the answer within that ecosystem.
- For Fabric, check `fabric.mod.json`, `build.gradle`, and `gradle.properties` for loader and Minecraft versions.
- For Paper/Spigot plugins, inspect `plugin.yml`, `paper-plugin.yml`, and `pom.xml` or Gradle manifests.
- For datapacks, inspect `pack.mcmeta`, `data/<namespace>/functions`, and tags.

Timing and thread context:

- Keep client thread, server thread, and render thread concerns separate in Java/Kotlin code.
- For Fabric, use the correct lifecycle event for registration, and avoid registering mod content too late or too early.
- When recommending networking or synchronization, mention server-side authority, client-side prediction, and packet direction.
- Avoid accessing world-state from the wrong thread; use tick events, scheduled tasks, or appropriate thread-safe APIs.

Registration and state:

- In Fabric/Forge mods, prefer the least invasive registration path and keep registries consistent with the target version.
- For registry-backed objects, include proper identifiers, constructors, and data serialization.
- For systems with persistent state, choose a save format that matches the platform's expectations (NBT, component data, JSON, or custom storage).

Mixins and injection:

- Prefer the least intrusive mixin target and explain brittleness when mixins rely on exact method signatures.
- When modifying behavior, mention obfuscation-sensitive names or mappings if the build uses Yarn or Mojang mappings.
- Keep mixin callback logic small and avoid broad catches around injected code.

Rendering and resource management:

- For client-side rendering, pay attention to model registration, texture resource location, and atlas reload behavior.
- When adding UI screens, mention screen lifecycle, input handling, and focus changes.
- For commands and datapacks, keep behavior deterministic and avoid implicit side effects outside the expected game state.
