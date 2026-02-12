package cz.mcsworld.eroded.world.territory;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;

public final class TerritoryMobSpawnHandler {

    private static final String TAG = "eroded_territory_threat";

    private TerritoryMobSpawnHandler() {}

    public static void register() {

        ServerEntityEvents.ENTITY_LOAD.register(
                (Entity entity, ServerWorld world) -> {

                    if (!(entity instanceof HostileEntity mob)) return;
                    if (mob.getCommandTags().contains(TAG)) return;

                    long tick = world.getServer().getTicks();
                    ChunkPos cp = new ChunkPos(mob.getBlockPos());

                    TerritoryData data =
                            TerritoryStorage.get(world, cp);

                    float threat =
                            TerritoryThreatResolver.computeThreat(data, tick);

                    if (threat < 0.35f) return;

                    applyBuffs(mob, threat);
                    mob.addCommandTag(TAG);
                }
        );
    }

    private static void applyBuffs(HostileEntity mob, float threat) {

        if (threat > 0.40f) {
            mob.addStatusEffect(
                    new StatusEffectInstance(
                            StatusEffects.HEALTH_BOOST,
                            Integer.MAX_VALUE,
                            threat > 0.70f ? 1 : 0,
                            true,
                            false
                    )
            );
            mob.setHealth(mob.getMaxHealth());
        }

        if (threat > 0.55f) {
            mob.addStatusEffect(
                    new StatusEffectInstance(
                            StatusEffects.STRENGTH,
                            Integer.MAX_VALUE,
                            threat > 0.80f ? 1 : 0,
                            true,
                            false
                    )
            );
        }

        if (threat > 0.65f) {
            mob.addStatusEffect(
                    new StatusEffectInstance(
                            StatusEffects.SPEED,
                            Integer.MAX_VALUE,
                            0,
                            true,
                            false
                    )
            );
        }
    }
}
