package cz.mcsworld.eroded.block;

import com.mojang.serialization.MapCodec;
import cz.mcsworld.eroded.core.ErodedItems;
import cz.mcsworld.eroded.protection.TerritoryClaim;
import cz.mcsworld.eroded.protection.TerritoryProtectionManager;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;

public class TerritoryAnchorBlock extends Block {

    public static final MapCodec<TerritoryAnchorBlock> CODEC = createCodec(TerritoryAnchorBlock::new);

    public static final BooleanProperty HAS_MODULE = BooleanProperty.of("has_module");
    public static final BooleanProperty ACTIVE = BooleanProperty.of("active");

    public TerritoryAnchorBlock(AbstractBlock.Settings settings) {
        super(settings);

        this.setDefaultState(
                this.getStateManager()
                        .getDefaultState()
                        .with(HAS_MODULE, false)
                        .with(ACTIVE, false)
        );
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(HAS_MODULE, ACTIVE);
    }

    @Override
    protected ActionResult onUse(
            BlockState state,
            World world,
            BlockPos pos,
            PlayerEntity player,
            BlockHitResult hit
    ) {
        ItemStack stack = player.getMainHandStack();

        if (world.isClient()) {
            if (stack.isOf(ErodedItems.TERRITORY_MODULE) || state.get(HAS_MODULE)) {
                return ActionResult.SUCCESS;
            }

            return ActionResult.PASS;
        }

        if (!(world instanceof ServerWorld serverWorld)) {
            return ActionResult.PASS;
        }

        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return ActionResult.PASS;
        }

        if (state.get(ACTIVE)) {
            TerritoryClaim claim = TerritoryProtectionManager.getAnchorClaim(serverWorld, pos);

            if (claim == null) {
                serverPlayer.sendMessage(
                        Text.translatable("eroded.territory.trust.no_claim"),
                        true
                );
                return ActionResult.SUCCESS;
            }

            if (!TerritoryProtectionManager.canManageClaim(serverPlayer, claim)) {
                serverPlayer.sendMessage(
                        Text.translatable("eroded.territory.trust.no_permission"),
                        true
                );
                return ActionResult.SUCCESS;
            }

            TerritoryProtectionManager.openTerritoryModule(
                    serverWorld,
                    pos,
                    serverPlayer
            );

            return ActionResult.SUCCESS;
        }

        if (state.get(HAS_MODULE)) {
            serverPlayer.sendMessage(
                    Text.translatable("eroded.territory.anchor.stabilizing"),
                    true
            );
            return ActionResult.SUCCESS;
        }

        if (!stack.isOf(ErodedItems.TERRITORY_MODULE)) {
            serverPlayer.sendMessage(
                    Text.translatable("eroded.territory.anchor.needs_module"),
                    true
            );
            return ActionResult.SUCCESS;
        }

        if (!TerritoryProtectionManager.validateNewClaim(serverWorld, pos, serverPlayer)) {
            return ActionResult.SUCCESS;
        }

        if (!serverPlayer.isCreative()) {
            stack.decrement(1);
        }

        serverWorld.setBlockState(
                pos,
                state.with(HAS_MODULE, true).with(ACTIVE, false),
                Block.NOTIFY_ALL
        );

        TerritoryProtectionManager.playModuleInsertedSound(serverWorld, pos);

        TerritoryProtectionManager.createPendingClaim(
                serverWorld,
                pos,
                serverPlayer
        );

        serverWorld.scheduleBlockTick(
                pos,
                this,
                TerritoryProtectionManager.getActivationDelayTicks()
        );

        serverPlayer.sendMessage(
                Text.translatable("eroded.territory.anchor.module_inserted"),
                true
        );

        return ActionResult.SUCCESS;
    }

    @Override
    protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (!state.isOf(this)) {
            return;
        }

        if (!state.get(HAS_MODULE)) {
            return;
        }

        if (state.get(ACTIVE)) {
            return;
        }

        world.setBlockState(
                pos,
                state.with(ACTIVE, true),
                Block.NOTIFY_ALL
        );

        TerritoryProtectionManager.activateClaim(world, pos);
        TerritoryProtectionManager.playAnchorActivatedSound(world, pos);

        TerritoryClaim claim = TerritoryProtectionManager.getAnchorClaim(world, pos);

        if (claim != null) {
            ServerPlayerEntity owner = world.getServer()
                    .getPlayerManager()
                    .getPlayer(claim.ownerUuid());

            if (owner != null) {
                owner.sendMessage(
                        Text.translatable("eroded.territory.anchor.activated"),
                        true
                );
            }
        }
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient()
                && world instanceof ServerWorld serverWorld
                && player instanceof ServerPlayerEntity serverPlayer) {

            TerritoryProtectionManager.handleAnchorBroken(
                    serverWorld,
                    pos,
                    state,
                    serverPlayer
            );
        }

        return super.onBreak(world, pos, state, player);
    }

    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        BlockState newState = world.getBlockState(pos);

        if (!newState.isOf(this)) {
            TerritoryProtectionManager.removeClaim(world, pos);
        }

        super.onStateReplaced(state, world, pos, moved);
    }

    @Override
    protected VoxelShape getCullingShape(BlockState state) {
        return VoxelShapes.empty();
    }
}