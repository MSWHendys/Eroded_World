package cz.mcsworld.eroded.energy;

import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.skills.SkillData;
import cz.mcsworld.eroded.skills.SkillManager;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class EnergySleepHandler {

    private EnergySleepHandler() {}

    public static void register() {
        EntitySleepEvents.STOP_SLEEPING.register(EnergySleepHandler::onWakeUp);
    }

    private static void onWakeUp(LivingEntity entity, BlockPos pos) {
        if (!(entity instanceof ServerPlayer player)) return;

        var root = EnergyConfig.get();
        var cfg = root.server.sleep;

        if (!cfg.sleepRestoresFull) return;

        SkillData data = SkillManager.get(player);
        data.addEnergy(data.getMaxEnergy());
        SkillManager.save(player);
        SkillManager.sync(player);
    }
}