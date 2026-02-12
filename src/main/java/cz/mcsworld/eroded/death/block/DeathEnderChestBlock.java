package cz.mcsworld.eroded.death.block;

import cz.mcsworld.eroded.death.DeathChestProtection;
import cz.mcsworld.eroded.death.DeathChestState;
import cz.mcsworld.eroded.death.gui.DeathInventoryScreenFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class DeathEnderChestBlock extends Block {

    public DeathEnderChestBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected ActionResult onUse(
            BlockState state,
            World world,
            BlockPos pos,
            PlayerEntity player,
            BlockHitResult hit
    ) {
        if (world.isClient) return ActionResult.SUCCESS;
        if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
        if (!(world instanceof ServerWorld sw)) return ActionResult.PASS;

        DeathChestState st = DeathChestState.get(sw);
        DeathChestState.Entry e = st.get(pos);

        if (e == null) {
            sp.sendMessage(
                    Text.translatable("eroded.death.chest.empty"),
                    true
            );
            return ActionResult.CONSUME;
        }

        if (!e.owner().equals(sp.getUuid())
                && DeathChestProtection.isProtected(sw, pos)) {

            sp.sendMessage(
                    Text.translatable("eroded.death.chest.not_owner"),
                    true
            );
            return ActionResult.CONSUME;
        }

        sp.openHandledScreen(new DeathInventoryScreenFactory(sw, pos));
        return ActionResult.CONSUME;
    }
}
