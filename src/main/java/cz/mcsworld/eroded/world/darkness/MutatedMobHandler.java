package cz.mcsworld.eroded.world.darkness;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;

public final class MutatedMobHandler {

    private MutatedMobHandler() {}

    public static void register() {

        ServerEntityEvents.ENTITY_LOAD.register(
                (Entity entity, ServerLevel world) -> {

                    if (!(entity instanceof Monster mob)) return;
                    if (mob.entityTags().contains(MutatedMobResolver.MUTATED_TAG)) return;

                    if (!MutatedMobResolver.shouldBeMutated(world, mob)) return;

                    applyMutation(mob);
                }
        );
    }

    private static void applyMutation(Monster mob) {

        mob.addTag(MutatedMobResolver.MUTATED_TAG);

        mob.addEffect(
                new MobEffectInstance(
                        MobEffects.RESISTANCE,
                        Integer.MAX_VALUE,
                        1,
                        true,
                        false
                )
        );

        mob.setHealth(mob.getMaxHealth());
    }
}
