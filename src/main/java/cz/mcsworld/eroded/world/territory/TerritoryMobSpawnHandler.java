package cz.mcsworld.eroded.world.territory;

import cz.mcsworld.eroded.config.territory.TerritoryConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;

public final class TerritoryMobSpawnHandler {

    private static final String TAG = "eroded_territory_threat";

    private TerritoryMobSpawnHandler() {}

    public static void register() {
        var cfg = TerritoryConfig.get().server;

        if (!cfg.enabled || !cfg.mobBuffEnabled) return;

        ServerEntityEvents.ENTITY_LOAD.register((Entity entity, ServerWorld world) -> {

            if (!(entity instanceof HostileEntity mob)) return;
            if (mob.getCommandTags().contains(TAG)) return;

            long tick = world.getServer().getTicks();
            ChunkPos cp = new ChunkPos(mob.getBlockPos());

            TerritoryWorldState worldState = TerritoryWorldState.get(world);
            TerritoryCellKey key = TerritoryCellKey.fromChunk(cp.x, cp.z);
            TerritoryCell cell = worldState.getOrCreateCell(key);

            float threat = TerritoryThreatResolver.computeThreat(cell, tick);


            if (cfg.mobSpawnControlEnabled) {

                if (cfg.surfaceOnlySpawns) {
                    int x = mob.getBlockPos().getX();
                    int z = mob.getBlockPos().getZ();

                    int surfaceY = world.getTopY(
                            Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                            x, z
                    );

                    if (mob.getBlockPos().getY() < surfaceY - cfg.undergroundTolerance) {
                        mob.discard();
                        return;
                    }
                }

                float t0 = cfg.mobBuffThreshold;
                float tN = clamp((threat - t0) / (1.0f - t0), 0.0f, 1.0f);

                float keepChance =
                        1.0f - (tN * (1.0f - cfg.spawnKeepMinChance));

                if (world.random.nextFloat() > keepChance) {
                    mob.discard();
                    return;
                }
            }


            if (threat < cfg.mobBuffThreshold) return;

            applyBuffs(mob, threat, cfg);

            mob.addCommandTag(TAG);

        });
    }

    private static void applyBuffs(HostileEntity mob, float threat,
                                   TerritoryConfig.Server cfg) {

        float t0 = cfg.mobBuffThreshold;
        float tN = clamp((threat - t0) / (1.0f - t0), 0.0f, 1.0f);

        double hp = 20.0 + ((cfg.mobMaxHp - 20.0) * tN);

        EntityAttributeInstance maxHp =
                mob.getAttributeInstance(EntityAttributes.MAX_HEALTH);

        if (maxHp != null) {
            maxHp.setBaseValue(hp);
            mob.setHealth((float) hp);
        }

        EntityAttributeInstance dmg =
                mob.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE);

        if (dmg != null) {
            dmg.setBaseValue(3.0 + (tN * 6.0));
        }

        if (threat > 0.55f) {
            mob.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.STRENGTH,
                    Integer.MAX_VALUE,
                    threat > 0.80f ? 1 : 0,
                    true,
                    false));
        }

        if (threat > 0.65f) {
            mob.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SPEED,
                    Integer.MAX_VALUE,
                    0,
                    true,
                    false));
        }

    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}