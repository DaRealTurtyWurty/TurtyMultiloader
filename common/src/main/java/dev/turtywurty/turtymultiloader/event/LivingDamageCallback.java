package dev.turtywurty.turtymultiloader.event;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

@FunctionalInterface
public interface LivingDamageCallback {
    /**
     * Runs after damage has been applied to a living entity on the logical server.
     *
     * @param damageTaken the final amount of damage applied after reductions
     */
    void afterDamage(LivingEntity entity, DamageSource source, float damageTaken);
}
