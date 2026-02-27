package cz.mcsworld.eroded.energy;

import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.network.EnergySyncPacket;
import cz.mcsworld.eroded.network.SafeNetworkUtil;
import cz.mcsworld.eroded.skills.SkillData;
import cz.mcsworld.eroded.skills.SkillManager;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public final class EnergySleepHandler {

    private EnergySleepHandler() {}

    public static void register() {
        EntitySleepEvents.STOP_SLEEPING.register(EnergySleepHandler::onWakeUp);
    }

    private static void onWakeUp(LivingEntity entity, BlockPos pos) {
        if (!(entity instanceof ServerPlayerEntity player)) return;

        var root = EnergyConfig.get();
        var cfg = root.server.sleep;

        if (!cfg.sleepRestoresFull) return;

        SkillData data = SkillManager.get(player);
        data.addEnergy(data.getMaxEnergy());

        SafeNetworkUtil.safeSend(
                player,
                new EnergySyncPacket(data.getEnergy())
        );
    }


}
