package cz.mcsworld.eroded.loot;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;

public class ErodedContainerBreakHandler {

    public static void register() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, oldState, blockEntity) -> {
            if (!(world instanceof ServerLevel serverWorld)) return;
            if (!(oldState.getBlock() instanceof ChestBlock) && !(oldState.getBlock() instanceof BarrelBlock)) return;

            ErodedLootState lootState = ErodedLootState.get(serverWorld);
            ErodedContainerIdentity.Identity oldIdentity = ErodedContainerIdentity.resolve(serverWorld, pos, oldState);
            ErodedLootState.ContainerSnapshot snapshot = lootState.captureAndClear(oldIdentity.memberKeys());

            // If one half of a double chest survived, preserve the shared
            // history/status on that remaining physical container.
            if (oldIdentity.memberKeys().length == 2) {
                long broken = pos.asLong();
                long survivorKey = oldIdentity.memberKeys()[0] == broken
                        ? oldIdentity.memberKeys()[1]
                        : oldIdentity.memberKeys()[0];

                BlockPos survivorPos = BlockPos.of(survivorKey);
                BlockState survivorState = serverWorld.getBlockState(survivorPos);

                if (survivorState.getBlock() instanceof ChestBlock && !snapshot.isEmpty()) {
                    lootState.applySnapshot(survivorKey, snapshot);
                }
            }
        });
    }
}
