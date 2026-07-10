package cz.mcsworld.eroded.entity;

import cz.mcsworld.eroded.config.territory.TerritoryConfig;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;

public final class ErodedMobSunBehaviour {

    public static final String TAG_ERODED = "eroded_special_mob";
    public static final String TAG_PERMANENT = "eroded_sun_proof";
    public static final String TAG_TEMPORARY = "eroded_temp_proof";
    public static final String TAG_BURN_PREFIX = "burn_at_";

    private ErodedMobSunBehaviour() {
    }

    public static void applyRandomSunBehaviour(Mob mob, RandomSource random) {
        var cfg = TerritoryConfig.get().server;

        mob.addTag(TAG_ERODED);

        if (mob.getTags().contains(TAG_PERMANENT)
                || mob.getTags().contains(TAG_TEMPORARY)) {
            return;
        }

        float sunProofChance = Mth.clamp(
                cfg.erodedMobSunProofChance,
                0.0F,
                1.0F
        );

        if (random.nextFloat() <= sunProofChance) {
            mob.addTag(TAG_PERMANENT);
            return;
        }

        mob.addTag(TAG_TEMPORARY);

        int minTicks = Math.max(0, cfg.erodedMobTempProtectionMinTicks);
        int randomTicks = Math.max(0, cfg.erodedMobTempProtectionRandomTicks);

        long extraTicks = randomTicks > 0
                ? random.nextInt(randomTicks)
                : 0L;

        long burnTime = mob.level().getGameTime() + minTicks + extraTicks;

        mob.addTag(TAG_BURN_PREFIX + burnTime);
    }

    public static void tickSunBehaviour(Mob mob, boolean manuallyBurnInDaylight) {
        if (!mob.isAlive()) {
            return;
        }

        if (mob.getTags().contains(TAG_PERMANENT)) {
            if (mob.isOnFire()) {
                mob.clearFire();
            }

            return;
        }

        if (mob.getTags().contains(TAG_TEMPORARY)) {
            long burnTime = getBurnTime(mob);
            long now = mob.level().getGameTime();

            if (burnTime < 0L || now < burnTime) {
                if (mob.isOnFire()) {
                    mob.clearFire();
                }

                return;
            }

            mob.removeTag(TAG_TEMPORARY);
            removeBurnTimeTag(mob);
        }

        if (manuallyBurnInDaylight && isInDirectDaylight(mob)) {
            var cfg = TerritoryConfig.get().server;

            float burnSeconds = Math.max(
                    1.0F,
                    cfg.erodedSkeletonBurnSeconds
            );

            mob.igniteForSeconds(burnSeconds);
        }
    }

    private static long getBurnTime(Mob mob) {
        for (String tag : mob.getTags()) {
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

    private static void removeBurnTimeTag(Mob mob) {
        String burnTag = null;

        for (String tag : mob.getTags()) {
            if (tag.startsWith(TAG_BURN_PREFIX)) {
                burnTag = tag;
                break;
            }
        }

        if (burnTag != null) {
            mob.removeTag(burnTag);
        }
    }

    private static boolean isInDirectDaylight(Mob mob) {
        return mob.level().isBrightOutside()
                && mob.level().canSeeSky(mob.blockPosition())
                && !mob.isInWaterOrRain();
    }
}