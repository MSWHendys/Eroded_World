package cz.mcsworld.eroded.death;

import cz.mcsworld.eroded.death.block.ErodedBlocks;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class DeathChestBreakHandler {

    private DeathChestBreakHandler() {}

    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register(
                DeathChestBreakHandler::beforeBreak
        );
    }

    private static boolean beforeBreak(
            Level world,
            Player player,
            BlockPos pos,
            BlockState state,
            @Nullable BlockEntity blockEntity
    ) {
        if (world.isClientSide()) return true;
        if (!(world instanceof ServerLevel sw)) return true;
        if (!(player instanceof ServerPlayer sp)) return true;

        if (!state.is(ErodedBlocks.DEATH_ENDER_CHEST)) {
            return true;
        }

        DeathChestState st = DeathChestState.get(sw);
        DeathChestState.Entry e = st.get(pos);

        if (e == null) {
            return true;
        }

        // Breaking an actively opened chest would create a second materialize
        // path (block break vs. GUI close), so nobody may break it while locked.
        if (st.isOpen(pos)) {
            sp.sendOverlayMessage(
                    Component.translatable("eroded.death.chest.in_use")
            );
            return false;
        }

        if (e.owner().equals(sp.getUUID())) {
            return true;
        }

        if (e.isProtected(System.currentTimeMillis())) {
            sp.sendOverlayMessage(
                    Component.translatable("eroded.death.chest.protected")
            );
            return false;
        }

        return true;
    }
}
