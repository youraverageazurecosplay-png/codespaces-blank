# Client Utils (Fabric, Client-Side, 1.21.11)

Utility mod with toggle GUI, inventory automation, combat helpers, book tools, render/ESP stack, and integration bridges.

## Added Features

1. GUI module toggles (Tweakeroo-style panel)
2. Full written-book clipboard paste (`Ctrl+Shift+V` in book editor)
3. Formatting code support (`\u00a7`) + quick insert (`Ctrl+Alt+S` in book editor)
4. Litematica built-in bridge (detect + guide button)
5. Tweak-style helpers (auto sprint, full bright, auto walk, anti-AFK option)
6. Triggerbot
7. Auto attribute swap from configurable slots (`attributeSwapSlots`, default `1,2,3`)
8. Auto heavy-book writer ("bookban-style" fill, `Ctrl+Shift+B` in book editor when enabled)
9. Auto totem refill (to offhand)
10. Auto best-tool swap on block mining
11. Jade built-in bridge (detect + guide button)
12. Client-side friends list (GUI add/remove + middle click friend toggle)
13. Auto refill selected main hand stack
14. Simple macros (three configurable macro slots with keybinds)
15. Configurable hotkeys in GUI
16. Firework use while gliding without manually holding rocket
17. Extra meteor-like helpers (middle-click friends, anti-AFK, macro system, inventory automation)
18. Freecam (anchored freecam-lite mode)
19. Auto Crystal with selected target workflow
20. Clear Glass mode enabled by default (merge-glass style visibility helper)
21. Auto Anchor with optional pearl gap-close behavior
22. Macro directives: `#xray`, `#xray on`, `#xray off`, `#stripmine <ticks>`, `#waypoint <name>`
23. Xray toggle module (ore marker mode + fullbright)
24. Waypoint module (nearest waypoint HUD + add/remove in Render page)
25. ESP (entity highlight)
26. Block ESP
27. Storage ESP
28. Tracers
29. Advanced Auto PvP (smooth aim + target combat loop)
30. Projectile trajectory prediction (client particle arc)
31. Void pearl safety (blocks pearl throws into void unless `0` is held)
32. Trap detection alerts (TNT minecarts, arrows, crafting tables, buttons, dispensers)
33. Auto Pot hotkey flow (temporarily swap pot in, throw, restore original item)
34. Extra utility upgrades: safety systems + trap intel + trajectory HUD
35. Auto Eat (hunger-threshold based food usage)
36. Private message capture/logging for messages your client receives (e.g. `/tell` style messages)
37. Creative saved-hotbar -> ender chest quick dump hotkey
38. Auto Respawn
39. Auto Rejoin (reconnect to last server after delay)
40. Auto leave on low HP threshold
41. Ender chest command hotkey fallback (server command/plugin dependent)
42. ReplayMod integration bridge (detect + guide button)

## GUI Pages

- `Modules`: all toggles + Litematica/Jade status buttons
- `Combat`: target-based combat automation modules
- `Render`: freecam, xray, esp stack, waypoint controls
- `Friends`: add/remove client-side friends
- `Macros`: edit macro text and toggle macro engine
- `Hotkeys`: bind open menu, triggerbot toggle, and macro keys

## Default Hotkeys

- Open GUI: `Right Shift`
- Toggle Triggerbot: `` ` ``
- Macro 1: `R`
- Macro 2: `O`
- Macro 3: `P`
- Select PvP target under crosshair: `V`
- Toggle Freecam: `F`
- Toggle Xray: `X`
- Auto Pot: `H`
- Creative EChest Dump: `J` (open ender chest + creative mode)
- Open EChest Command: `K` (sends configured command, default `/echest`)
- Void pearl safety override: hold `0` while throwing pearl
- Book editor full paste: `Ctrl+Shift+V`
- Book editor insert section sign: `Ctrl+Alt+S`
- Book editor heavy-book fill: `Ctrl+Shift+B`

## Build

```bash
gradle build -x test
```

Jar output:

- `build/libs/clientutils-0.1.0.jar`

PM log file:

- `config/clientutils-pm.log`

Notes:

- PM capture only logs messages your client actually receives.
- ReplayMod is integrated as a detection/bridge (not bundled inside this jar).
- Ender chest without block placement is provided via command hotkey fallback and only works if the server allows such command(s).
