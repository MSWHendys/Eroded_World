package cz.mcsworld.eroded.entity;

import cz.mcsworld.eroded.config.territory.TerritoryConfig;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

public final class ErodedMobSunBehaviour {

    public static final String TAG_ERODED = "eroded_special_mob";
    public static final String TAG_PERMANENT = "eroded_sun_proof";
    public static final String TAG_TEMPORARY = "eroded_temp_proof";
    public static final String TAG_BURN_PREFIX = "burn_at_";

    private ErodedMobSunBehaviour() {
    }

    public static void applyRandomSunBehaviour(MobEntity mob, Random random) {
        var cfg = TerritoryConfig.get().server;

        mob.addCommandTag(TAG_ERODED);

        if (mob.getCommandTags().contains(TAG_PERMANENT)
                || mob.getCommandTags().contains(TAG_TEMPORARY)) {
            return;
        }

        float sunProofChance = MathHelper.clamp(
                cfg.erodedMobSunProofChance,
                0.0F,
                1.0F
        );

        if (random.nextFloat() <= sunProofChance) {
            mob.addCommandTag(TAG_PERMANENT);
            return;
        }

        mob.addCommandTag(TAG_TEMPORARY);

        int minTicks = Math.max(0, cfg.erodedMobTempProtectionMinTicks);
        int randomTicks = Math.max(0, cfg.erodedMobTempProtectionRandomTicks);

        long extraTicks = randomTicks > 0
                ? random.nextInt(randomTicks)
                : 0L;

        long burnTime = mob.getWorld().getTime() + minTicks + extraTicks;

        mob.addCommandTag(TAG_BURN_PREFIX + burnTime);
    }

    public static void tickSunBehaviour(MobEntity mob, boolean manuallyBurnInDaylight) {
        if (!mob.isAlive()) {
            return;
        }

        if (mob.getCommandTags().contains(TAG_PERMANENT)) {
            if (mob.isOnFire()) {
                mob.extinguish();
            }

            return;
        }

        if (mob.getCommandTags().contains(TAG_TEMPORARY)) {
            long burnTime = getBurnTime(mob);
            long now = mob.getWorld().getTime();

            if (burnTime < 0L || now < burnTime) {
                if (mob.isOnFire()) {
                    mob.extinguish();
                }

                return;
            }

            mob.removeCommandTag(TAG_TEMPORARY);
            removeBurnTimeTag(mob);
        }

        if (manuallyBurnInDaylight && isInDirectDaylight(mob)) {
            var cfg = TerritoryConfig.get().server;

            float burnSeconds = Math.max(
                    1.0F,
                    cfg.erodedSkeletonBurnSeconds
            );

            mob.setOnFireFor(burnSeconds);
        }
    }

    private static long getBurnTime(MobEntity mob) {
        for (String tag : mob.getCommandTags()) {
            if (!tag.startsWith(TAG_BURN_PREFIX)) {
                continue;
            }

            try {
                return Long.parseLong(tag.substring(TAG_BURN_PREFIX.length()));
            } catch (NumberFormatException ignored) {
                return -1L;
            }
        }

        return -1L;
    }

    private static void removeBurnTimeTag(MobEntity mob) {
        String burnTag = null;

        for (String tag : mob.getCommandTags()) {
            if (tag.startsWith(TAG_BURN_PREFIX)) {
                burnTag = tag;
                break;
            }
        }

        if (burnTag != null) {
            mob.removeCommandTag(burnTag);
        }
    }

    private static boolean isInDirectDaylight(MobEntity mob) {
        return mob.getWorld().isDay()
                && mob.getWorld().isSkyVisible(mob.getBlockPos())
                && !mob.isTouchingWaterOrRain();
    }
}