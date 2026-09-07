package cz.mcsworld.eroded.loot;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Placement is detected from UseBlockCallback, but metadata is committed only
 * at END_SERVER_TICK after vanilla has actually placed the container. This
 * prevents cancelled/failed placements from leaving ghost loot-state entries.
 */
public final class ErodedContainerPlacementHandler {

    private static final List<PendingPlacement> PENDING = new ArrayList<>();

    private ErodedContainerPlacementHandler() {
    }

    private record Candidate(BlockPos pos, Block oldBlock) {
    }

    private record PendingPlacement(
            ResourceKey<Level> worldKey,
            Block intendedBlock,
            boolean adminPlaced,
            Candidate first,
            Candidate second
    ) {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClientSide() || !(world instanceof ServerLevel serverWorld)) {
                return InteractionResult.PASS;
            }

            ItemStack stack = player.getItemInHand(hand);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) {
                return InteractionResult.PASS;
            }

            Block block = blockItem.getBlock();
            if (!(block instanceof ChestBlock) && !(block instanceof BarrelBlock)) {
                return InteractionResult.PASS;
            }

            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            boolean admin = data != null
                    && data.copyTag().getBoolean("eroded_loot_chest").orElse(false);

            BlockPos hitPos = hit.getBlockPos().immutable();
            BlockPos adjacent = hitPos.relative(hit.getDirection()).immutable();

            PENDING.add(new PendingPlacement(
                    serverWorld.dimension(),
                    block,
                    admin,
                    new Candidate(hitPos, serverWorld.getBlockState(hitPos).getBlock()),
                    new Candidate(adjacent, serverWorld.getBlockState(adjacent).getBlock())
            ));

            return InteractionResult.PASS;
        });

        ServerTickEvents.END_SERVER_TICK.register(ErodedContainerPlacementHandler::flushPending);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> PENDING.clear());
    }

    private static void flushPending(MinecraftServer server) {
        if (PENDING.isEmpty()) return;

        List<PendingPlacement> pending = new ArrayList<>(PENDING);
        PENDING.clear();

        for (PendingPlacement placement : pending) {
            ServerLevel world = server.getLevel(placement.worldKey());
            if (world == null) continue;

            BlockPos placedPos = findPlacedPosition(world, placement);
            if (placedPos == null) continue;

            ErodedLootState state = ErodedLootState.get(world);
            long placedKey = placedPos.asLong();

            // Coordinate reuse must never inherit old generated/opened state.
            state.resetPosition(placedKey);

            if (placement.adminPlaced()) {
                state.markAdminPlaced(placedKey);
            } else {
                state.markPlayerPlaced(placedKey);
            }

            // If the placement formed a double chest, immediately merge both
            // halves into one canonical identity.
            BlockState blockState = world.getBlockState(placedPos);
            state.normalize(ErodedContainerIdentity.resolve(world, placedPos, blockState));
        }
    }

    private static BlockPos findPlacedPosition(ServerLevel world, PendingPlacement placement) {
        BlockPos found = changedToIntended(world, placement.first(), placement.intendedBlock());
        if (found != null) return found;
        return changedToIntended(world, placement.second(), placement.intendedBlock());
    }

    private static BlockPos changedToIntended(ServerLevel world, Candidate candidate, Block intendedBlock) {
        Block current = world.getBlockState(candidate.pos()).getBlock();
        if (current == intendedBlock && candidate.oldBlock() != intendedBlock) {
            return candidate.pos();
        }
        return null;
    }
}
