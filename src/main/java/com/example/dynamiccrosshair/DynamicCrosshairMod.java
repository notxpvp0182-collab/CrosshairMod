package com.example.dynamiccrosshair;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the Dynamic Crosshair mod (client-side only).
 *
 * Responsibilities here:
 *   1. Register a {@link WorldRenderEvents#AFTER_ENTITIES} callback that
 *      draws colored hitbox overlays when F3+B is active.
 *   2. Reset {@link CrosshairState} when the player leaves a world so stale
 *      data from a previous session doesn't bleed through.
 *
 * Crosshair rendering itself lives in {@link com.example.dynamiccrosshair.mixin.InGameHudMixin}.
 * Attack detection lives in {@link com.example.dynamiccrosshair.mixin.ClientPlayerInteractionManagerMixin}.
 */
@Environment(EnvType.CLIENT)
public class DynamicCrosshairMod implements ClientModInitializer {

    public static final String MOD_ID = "dynamiccrosshair";
    public static final Logger LOGGER  = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("[DynamicCrosshair] Initialised.");

        // ── Hitbox overlay ────────────────────────────────────────────────────
        // Fires after all entity models are rendered, but before translucent
        // geometry.  matrixStack() and consumers() are both non-null here.
        WorldRenderEvents.AFTER_ENTITIES.register(this::renderHitboxOverlays);

        // ── Cleanup on world leave ────────────────────────────────────────────
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> CrosshairState.reset());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Hitbox overlay rendering
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Draws a translucent green or red wireframe box over entities that are
     * targeted or recently hit, on top of the vanilla (yellow-ish) hitbox.
     *
     * Only runs when the player has F3+B hitboxes enabled, so there is zero
     * overhead during normal gameplay.
     *
     * @param ctx world render context provided by Fabric API
     */
    private void renderHitboxOverlays(WorldRenderContext ctx) {
        MinecraftClient client = MinecraftClient.getInstance();

        // ── Early-exit guards (cheap checks first) ────────────────────────────

        // Only run when F3+B hitboxes are visible
        if (!client.getEntityRenderDispatcher().shouldRenderHitboxes()) return;

        if (client.world == null || client.player == null) return;

        // Fabric API guarantees these are non-null for AFTER_ENTITIES,
        // but we guard anyway to be safe.
        MatrixStack matrices = ctx.matrixStack();
        VertexConsumerProvider consumers = ctx.consumers();
        if (matrices == null || consumers == null) return;

        // Camera world position — all entity positions are relative to this.
        Vec3d cam = ctx.camera().getPos();

        // We reuse the same VertexConsumer for all boxes in this pass.
        VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());

        int   targetId  = CrosshairState.targetedEntityId;
        boolean targeting = CrosshairState.isTargetingEntity;

        for (Entity entity : client.world.getEntities()) {
            int id = entity.getId();

            boolean isTarget   = targeting && id == targetId;
            boolean isFlashing = CrosshairState.isEntityFlashing(id);

            // Skip entities that need no special colour
            if (!isTarget && !isFlashing) continue;

            // ── Determine overlay colour ──────────────────────────────────────
            // Flash takes priority over targeting (can overlap for <220 ms).
            float r, g, b;
            if (isFlashing) {
                r = 1.0f; g = 0.15f; b = 0.15f; // vivid red
            } else {
                r = 0.15f; g = 1.0f;  b = 0.15f; // vivid green
            }

            // ── Translate to entity position relative to camera ───────────────
            Box worldBox = entity.getBoundingBox();

            // Express the bounding box in entity-local space (origin = feet)
            Box localBox = worldBox.offset(
                -entity.getX(),
                -entity.getY(),
                -entity.getZ()
            );

            // Translate matrix to entity feet position (camera is origin)
            double dx = entity.getX() - cam.x;
            double dy = entity.getY() - cam.y;
            double dz = entity.getZ() - cam.z;

            matrices.push();
            matrices.translate(dx, dy, dz);

            // ── Draw the coloured wireframe box ───────────────────────────────
            // WorldRenderer.drawBox draws 12 edges as GL_LINES with RGBA colour.
            WorldRenderer.drawBox(
                matrices, lines,
                localBox,          // box in local entity space
                r, g, b, 0.9f      // colour + alpha (slightly transparent)
            );

            matrices.pop();
        }
    }
  }
                                                       
