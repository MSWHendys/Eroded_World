package cz.mcsworld.eroded.death.block;

import cz.mcsworld.eroded.death.DeathChestProtection;
import cz.mcsworld.eroded.death.DeathChestState;
import cz.mcsworld.eroded.death.gui.DeathInventoryScreenFactory;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public class DeathEnderChestBlock extends Block {

    public DeathEnderChestBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(
            @NotNull BlockState state,
            Level world,
            @NotNull BlockPos pos,
            @NotNull Player player,
            @NotNull BlockHitResult hit
    ) {
        if (world.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
        if (!(world instanceof ServerLevel sw)) return InteractionResult.PASS;

        DeathChestState st = DeathChestState.get(sw);
        DeathChestState.Entry e = st.get(pos);

        if (e == null) {
            sp.sendOverlayMessage(
                    Component.translatable("eroded.death.chest.empty")
            );
            return InteractionResult.CONSUME;
        }

        if (!e.owner().equals(sp.getUUID())
                && DeathChestProtection.isProtected(sw, pos)) {

            sp.sendOverlayMessage(
                    Component.translatable("eroded.death.chest.not_owner")
            );
            return InteractionResult.CONSUME;
        }

        UUID sessionToken = st.tryOpen(pos, sp.getUUID());
        if (sessionToken == null) {
            sp.sendOverlayMessage(
                    Component.translatable("eroded.death.chest.in_use")
            );
            return InteractionResult.CONSUME;
        }

        var opened = sp.openMenu(
                new DeathInventoryScreenFactory(sw, pos, sessionToken)
        );

        if (opened.isEmpty()) {
            st.releaseOpen(pos, sessionToken);
        }

        return InteractionResult.CONSUME;
    }
}
