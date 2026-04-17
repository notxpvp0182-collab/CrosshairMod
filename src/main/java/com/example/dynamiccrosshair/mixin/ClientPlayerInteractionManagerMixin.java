package com.example.dynamiccrosshair.mixin;

import com.example.dynamiccrosshair.CrosshairState;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Detects when the local player successfully performs a melee attack and
 * notifies {@link CrosshairState} so both the crosshair and the target's
 * hitbox flash red briefly.
 *
 * We inject at HEAD (before the actual packet is sent) so the flash
 * appears on the same frame the click was registered — feels instant.
 */
@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

    /**
     * Fires every time {@code attackEntity} is called on the client side,
     * i.e. when the player left-clicks on an entity within melee range.
     *
     * @param player the local player
     * @param target the entity being attacked
     * @param ci     mixin callback (not cancelled — vanilla attack still fires)
     */
    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void dynamicCrosshair_onAttackEntity(
            PlayerEntity player,
            Entity target,
            CallbackInfo ci) {

        // Record the hit — this stamps both the crosshair flash and the
        // hitbox flash for the given entity with the current timestamp.
        CrosshairState.onEntityHit(target.getId());
    }
}
