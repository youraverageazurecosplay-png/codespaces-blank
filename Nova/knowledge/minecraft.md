# Minecraft Knowledge

Nova should prefer repo-specific Minecraft guidance.

Platform guidance:

- For Fabric mods, inspect `build.gradle`, `gradle.properties`, and `fabric.mod.json` first.
- For Paper or Spigot plugins, inspect `plugin.yml`, `paper-plugin.yml`, `build.gradle`, or `pom.xml`.
- For datapacks, inspect `pack.mcmeta`, namespace folders, functions, and tags.

Version guidance:

- Mention the Minecraft version when API details are version-sensitive.
- Keep loader and mapping versions aligned with the repo's build files.

Implementation habits:

- Match the platform already present in the repo instead of mixing ecosystems.
- Prefer small feature additions that fit the existing mod or plugin structure.
- When suggesting commands or gameplay logic, distinguish between client-side and server-side behavior.
- When debugging, look for version mismatch, wrong event hooks, bad registration order, or incorrect resource paths.

Advanced Minecraft modding guidance:

- Infer the ecosystem first: Fabric, Forge, NeoForge, Paper, Spigot, or datapack workflows have very different APIs and lifecycle rules.
- For Fabric or Forge mods, pay attention to registries, lifecycle timing, mixin targets, networking boundaries, serialization codecs, and world/thread access rules.
- Treat client thread, server thread, and render thread concerns separately when discussing state or events.
- When adding gameplay systems, think about save format, sync strategy, commands, config, and migration between versions.
- For mixins, prefer the least invasive injection point and mention brittleness around obfuscation, mappings, and upstream updates.
- When debugging desync, inspect packet direction, authority, tick timing, and whether data lives on the logical client or server.
- For rendering features, mention resource reload, atlas/model registration, and version-specific rendering APIs.
- For entity, block, or item systems, inspect registration order, data components, NBT or codec handling, and capability or component patterns used by the platform.
- For plugin ecosystems like Paper, distinguish event-driven server plugins from client mods and avoid suggesting mod-loader-only patterns.
- If a repo targets a specific Minecraft version, tailor API suggestions to that version instead of mixing examples from different eras.
