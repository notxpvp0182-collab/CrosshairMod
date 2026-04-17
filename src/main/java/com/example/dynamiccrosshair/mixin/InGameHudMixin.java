package com.example.dynamiccrosshair.mixin;

import com.example.dynamiccrosshair.CrosshairState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts the vanilla crosshair render to apply dynamic color and shape:
 *
 *   • Default   → white simple cross  (5 px arms)
 *   • Targeting → green bracket reticle (7 px arms, corner-bracket style)
 *   • Hit flash → red bracket reticle  (8 px arms)
 *
 * Because we call {@code ci.cancel()} at HEAD we take full ownership of
 * crosshair drawing for the frame; no vanilla crosshair is rendered.
 *
 * NOTE: If the game crashes on launch with a mixin error referencing
 * "renderCrosshair", check the Yarn mappings for your MC version at
 * https://fabricmc.net/develop/ — the method name is stable across 1.21.x.
 */
@Mixin(InGameHud.class)
public class InGameHudMixin {

    /**
     * Inject at the very start of renderCrosshair, cancel vanilla rendering,
     * then draw our own crosshair according to the current CrosshairState.
     *
     * @param context     the draw context for the current frame
     * @param tickCounter tick counter (used for interpolation by vanilla, unused here)
     * @param ci          mixin callback – we cancel it to suppress vanilla drawing
     */
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void dynamicCrosshair_renderCrosshair(
            DrawContext context,
            RenderTickCounter tickCounter,
            CallbackInfo ci) {

        MinecraftClient client = MinecraftClient.getInstance();

        // Guard: no player present (e.g. loading screen)
        if (client.player == null) return;

        // Guard: only draw crosshair in first-person view
        if (!client.options.getPerspective().isFirstPerson()) return;

        // ── Update targeting state ────────────────────────────────────────────
        // crosshairTarget is set by the game engine each tick; safe to read here.
        HitResult hit = client.crosshairTarget;

        if (hit instanceof EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            CrosshairState.isTargetingEntity  = true;
            CrosshairState.targetedEntityId   = target.getId();
        } else {
            CrosshairState.isTargetingEntity  = false;
            // Keep targetedEntityId alive briefly so the hitbox flash finishes.
            if (!CrosshairState.isCrosshairFlashing()) {
                CrosshairState.targetedEntityId = -1;
            }
        }

        // ── Suppress vanilla crosshair ────────────────────────────────────────
        ci.cancel();

        // ── Decide appearance ─────────────────────────────────────────────────
        int   color;   // ARGB
        int   arms;    // half-length of each arm in pixels
        boolean brackets; // bracket-style (true) or simple cross (false)

        if (CrosshairState.isCrosshairFlashing()) {
            // Red hit-flash
            color    = 0xFFFF4444;
            arms     = 8;
            brackets = true;
        } else if (CrosshairState.isTargetingEntity) {
            // Green targeting reticle
            color    = 0xFF44FF44;
            arms     = 7;
            brackets = true;
        } else {
            // Default white cross
            color    = 0xFFFFFFFF;
            arms     = 5;
            brackets = false;
        }

        int cx = client.getWindow().getScaledWidth()  / 2;
        int cy = client.getWindow().getScaledHeight() / 2;

        if (brackets) {
            drawBracketReticle(context, cx, cy, arms, color);
        } else {
            drawSimpleCross(context, cx, cy, arms, color);
        }
    }

    // ─── Drawing helpers ──────────────────────────────────────────────────────

    /**
     * Draws a plain + cross crosshair.
     * One pixel thick; arm length = {@code arms} pixels from center.
     *
     *    □□□□□X□□□□□
     *    □□□□□X□□□□□
     *    XXXXXXXXXXX
     *    □□□□□X□□□□□
     *    □□□□□X□□□□□
     */
    private static void drawSimpleCross(DrawContext ctx, int cx, int cy, int arms, int color) {
        // Horizontal bar
        ctx.fill(cx - arms, cy,     cx + arms + 1, cy + 1, color);
        // Vertical bar
        ctx.fill(cx,        cy - arms, cx + 1, cy + arms + 1, color);
    }

    /**
     * Draws a corner-bracket targeting reticle — visually distinct from the
     * default cross so the player clearly knows they are on a target.
     *
     *    [▄▄    ▄▄]
     *    ▌          ▐
     *
     *    ▌          ▐
     *    [▀▀    ▀▀]
     *
     * Each corner is an L-shape of {@code bracketLen} pixels per arm.
     * A 1-pixel dot is drawn at the exact center.
     */
    private static void drawBracketReticle(DrawContext ctx, int cx, int cy, int halfSize, int color) {
        int bl = Math.max(2, halfSize / 2); // bracket arm length

        // ── Top-left ──
        ctx.fill(cx - halfSize, cy - halfSize, cx - halfSize + bl, cy - halfSize + 1, color); // horiz
        ctx.fill(cx - halfSize, cy - halfSize, cx - halfSize + 1, cy - halfSize + bl, color); // vert
        // ── Top-right ──
        ctx.fill(cx + halfSize - bl + 1, cy - halfSize, cx + halfSize + 1, cy - halfSize + 1, color); // horiz
        ctx.fill(cx + halfSize,          cy - halfSize, cx + halfSize + 1, cy - halfSize + bl, color); // vert
        // ── Bottom-left ──
        ctx.fill(cx - halfSize, cy + halfSize,          cx - halfSize + bl, cy + halfSize + 1, color); // horiz
        ctx.fill(cx - halfSize, cy + halfSize - bl + 1, cx - halfSize + 1, cy + halfSize + 1, color); // vert
        // ── Bottom-right ──
        ctx.fill(cx + halfSize - bl + 1, cy + halfSize,          cx + halfSize + 1, cy + halfSize + 1, color); // horiz
        ctx.fill(cx + halfSize,          cy + halfSize - bl + 1, cx + halfSize + 1, cy + halfSize + 1, color); // vert

        // Center dot
        ctx.fill(cx, cy, cx + 1, cy + 1, color);
    }
  }
  
