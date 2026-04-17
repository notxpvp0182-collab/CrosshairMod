# Dynamic Crosshair Mod — Fabric 1.21.1

A lightweight, client-side Fabric mod that gives you visual feedback about what
your crosshair is looking at and when you land a hit.

---

## Features

| State | Crosshair | Hitbox (F3+B) |
|---|---|---|
| Default | White `+` cross | Vanilla colour |
| Targeting entity | **Green** bracket reticle | **Green** wire overlay |
| Just hit entity | **Red** bracket reticle (220 ms) | **Red** wire overlay (220 ms) |

---

## Requirements

- **Minecraft** 1.21.1
- **Fabric Loader** ≥ 0.15.0
- **Fabric API** (any version for 1.21.1)
- **Java** 21

---

## Building

### Step 1 — Set up the Gradle Wrapper (first time only)

The ZIP does **not** contain `gradle-wrapper.jar` (a binary that cannot be
generated offline). Choose one of:

**Option A — IntelliJ IDEA (recommended)**
Just open the project folder in IDEA (`File → Open`). IDEA detects
`build.gradle` and automatically configures the Gradle wrapper for you.
Then run the `build` Gradle task.

**Option B — system Gradle (CLI)**
```sh
gradle wrapper --gradle-version 8.8   # run once
./gradlew build
```

**Option C — helper script**
```sh
chmod +x setup-wrapper.sh
./setup-wrapper.sh
./gradlew build
```

### Step 2 — Build

```sh
./gradlew build
```

The built JAR will be in `build/libs/dynamic-crosshair-1.0.0.jar`.
Drop it into your `.minecraft/mods/` folder.

---

## Project Structure

```
src/main/java/com/example/dynamiccrosshair/
├── DynamicCrosshairMod.java          — client entrypoint; hitbox overlay renderer
├── CrosshairState.java               — lightweight shared state (targeting, flash timers)
└── mixin/
    ├── InGameHudMixin.java           — cancels & replaces vanilla crosshair each frame
    └── ClientPlayerInteractionManagerMixin.java  — detects melee hits
```

---

## How it works

```
Each render frame:
  InGameHudMixin.renderCrosshair()
    └─ reads MinecraftClient.crosshairTarget
    └─ updates CrosshairState.isTargetingEntity / targetedEntityId
    └─ cancels vanilla crosshair
    └─ draws custom crosshair (white cross / green bracket / red bracket)

Each left-click on entity:
  ClientPlayerInteractionManagerMixin.attackEntity()
    └─ CrosshairState.onEntityHit(entityId)
       └─ stamps crosshairFlashStartMs + entityFlashTimes[id]

Each frame (F3+B active):
  DynamicCrosshairMod.renderHitboxOverlays()
    └─ iterates world entities
    └─ for targeted/flashing ones: draws coloured wireframe via WorldRenderer.drawBox
```

---

## Performance notes

- Crosshair state is a few `volatile` fields — essentially free.
- Hitbox overlay only runs when F3+B is active (zero overhead in normal play).
- `ConcurrentHashMap` for entity flash times auto-cleans entries every 80 hits.
- Mixin count: 2 (the minimum needed for the feature set).
- Target RAM: works fine at -Xmx512m; well within 2 GB.

---

## Troubleshooting

**Game crashes on launch with mixin error about `renderCrosshair`**
The Yarn-mapped method name changed. Check the current name at
https://fabricmc.net/develop/ → search `InGameHud` in the Yarn viewer,
then update `InGameHudMixin.java` accordingly.

**Green/red overlay not showing on hitboxes**
Make sure F3+B is enabled (vanilla hitboxes must be visible for the overlay
to render).

**Crosshair not changing colour**
Verify the mod is loaded: `F3` screen should list `dynamiccrosshair`.
