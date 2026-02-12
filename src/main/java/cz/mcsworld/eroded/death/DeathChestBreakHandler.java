package cz.mcsworld.eroded.death;

import cz.mcsworld.eroded.death.block.ErodedBlocks;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class DeathChestBreakHandler {

    private DeathChestBreakHandler() {}

    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register(
                DeathChestBreakHandler::beforeBreak
        );
    }

    private static boolean beforeBreak(
            World world,
            PlayerEntity player,
            BlockPos pos,
            BlockState state,
            @Nullable BlockEntity blockEntity
    ) {

        if (world.isClient) return true;
        if (!(world instanceof ServerWorld sw)) return true;
        if (!(player instanceof ServerPlayerEntity sp)) return true;

        if (!state.isOf(ErodedBlocks.DEATH_ENDER_CHEST)) {
            return true;
        }

        DeathChestState st = DeathChestState.get(sw);
        DeathChestState.Entry e = st.get(pos);

        if (e == null) {
            return true;
        }

        if (e.owner().equals(sp.getUuid())) {
            return true;
        }

        if (e.isProtected(System.currentTimeMillis())) {

            sp.sendMessage(
                    Text.translatable("eroded.death.chest.protected"),
                    true
            );
            return false;
        }

        return true;
    }
}
