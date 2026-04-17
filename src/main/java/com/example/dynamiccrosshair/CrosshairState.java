package com.example.dynamiccrosshair;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared, thread-safe state for the Dynamic Crosshair mod.
 *
 * All fields are written on the render/game thread and read on the same thread,
 * so volatile is enough – no heavyweight locks needed (keeps it lightweight).
 */
public final class CrosshairState {

    // ─── Targeting ────────────────────────────────────────────────────────────

    /** True when the crosshair is resting on a living/attackable entity. */
    public static volatile boolean isTargetingEntity = false;

    /**
     * Network ID of the entity currently under the crosshair, or -1 if none.
     * Used to highlight that entity's hitbox in green.
     */
    public static volatile int targetedEntityId = -1;

    // ─── Crosshair flash ──────────────────────────────────────────────────────

    /** System-millis timestamp of the last registered player attack. */
    private static volatile long crosshairFlashStartMs = 0L;

    // ─── Per-entity hitbox flash ──────────────────────────────────────────────

    /**
     * Maps entity network-ID → timestamp (ms) when it was last hit.
     * ConcurrentHashMap because cleanup can happen from any call site.
     */
    private static final ConcurrentHashMap<Integer, Long> entityFlashTimes =
            new ConcurrentHashMap<>();

    /** How long a flash lasts in milliseconds (crosshair & hitbox). */
    public static final long FLASH_DURATION_MS = 220L;

    // ─── Query methods ────────────────────────────────────────────────────────

    /** @return true while the crosshair should display a red hit-flash. */
    public static boolean isCrosshairFlashing() {
        return System.currentTimeMillis() - crosshairFlashStartMs < FLASH_DURATION_MS;
    }

    /** @return true while the given entity's hitbox should display a red flash. */
    public static boolean isEntityFlashing(int entityId) {
        Long t = entityFlashTimes.get(entityId);
        return t != null && System.currentTimeMillis() - t < FLASH_DURATION_MS;
    }

    // ─── Mutation methods ─────────────────────────────────────────────────────

    /**
     * Call when the player successfully registers an attack (left-click hit).
     * Triggers both the crosshair flash and the entity hitbox flash.
     *
     * @param entityId network ID of the entity that was hit
     */
    public static void onEntityHit(int entityId) {
        long now = System.currentTimeMillis();
        crosshairFlashStartMs = now;
        entityFlashTimes.put(entityId, now);

        // Periodic cleanup – prevent unbounded growth in long sessions
        if (entityFlashTimes.size() > 80) {
            long cutoff = now - FLASH_DURATION_MS * 4;
            entityFlashTimes.entrySet().removeIf(e -> e.getValue() < cutoff);
        }
    }

    /** Resets all state – call when leaving a world so old data doesn't linger. */
    public static void reset() {
        isTargetingEntity  = false;
        targetedEntityId   = -1;
        crosshairFlashStartMs = 0L;
        entityFlashTimes.clear();
    }

    private CrosshairState() {} // utility class – no instances
          }
          
