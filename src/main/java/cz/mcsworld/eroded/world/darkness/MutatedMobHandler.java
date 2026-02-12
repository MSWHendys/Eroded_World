package cz.mcsworld.eroded.world.darkness;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.world.ServerWorld;

public final class MutatedMobHandler {

    private MutatedMobHandler() {}

    public static void register() {

        ServerEntityEvents.ENTITY_LOAD.register(
                (Entity entity, ServerWorld world) -> {

                    if (!(entity instanceof HostileEntity mob)) return;
                    if (mob.getCommandTags().contains(MutatedMobResolver.MUTATED_TAG)) return;

                    if (!MutatedMobResolver.shouldBeMutated(world, mob)) return;

                    applyMutation(mob);
                }
        );
    }

    private static void applyMutation(HostileEntity mob) {

        mob.addCommandTag(MutatedMobResolver.MUTATED_TAG);

        mob.addStatusEffect(
                new StatusEffectInstance(
                        StatusEffects.RESISTANCE,
                        Integer.MAX_VALUE,
                        1,
                        true,
                        false
                )
        );

        mob.setHealth(mob.getMaxHealth());
    }
}
