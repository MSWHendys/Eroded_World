package cz.mcsworld.eroded.loot;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

/**
 * Resolves a barrel/single chest to one storage key and a double chest to one
 * canonical key shared by both halves. This also gives us a safe migration
 * point for saves created before double-chest canonicalisation existed.
 */
public final class ErodedContainerIdentity {

    private ErodedContainerIdentity() {
    }

    public record Identity(long canonicalKey, long[] memberKeys) {
    }

    public static Identity resolve(ServerLevel world, BlockPos pos) {
        return resolve(world, pos, world.getBlockState(pos));
    }

    public static Identity resolve(ServerLevel world, BlockPos pos, BlockState state) {
        long first = pos.asLong();

        if (!(state.getBlock() instanceof ChestBlock)
                || state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
            return new Identity(first, new long[]{first});
        }

        BlockPos partnerPos = pos.relative(ChestBlock.getConnectedDirection(state));
        BlockState partnerState = world.getBlockState(partnerPos);

        if (!(partnerState.getBlock() instanceof ChestBlock)
                || partnerState.getBlock() != state.getBlock()) {
            return new Identity(first, new long[]{first});
        }

        long second = partnerPos.asLong();
        long canonical = Math.min(first, second);

        return new Identity(canonical, new long[]{first, second});
    }
}
