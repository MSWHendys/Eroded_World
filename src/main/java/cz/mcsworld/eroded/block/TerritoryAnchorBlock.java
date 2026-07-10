package cz.mcsworld.eroded.block;

import com.mojang.serialization.MapCodec;
import cz.mcsworld.eroded.core.ErodedItems;
import cz.mcsworld.eroded.protection.TerritoryClaim;
import cz.mcsworld.eroded.protection.TerritoryProtectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TerritoryAnchorBlock extends Block {

    public static final MapCodec<TerritoryAnchorBlock> CODEC = simpleCodec(TerritoryAnchorBlock::new);

    public static final BooleanProperty HAS_MODULE = BooleanProperty.create("has_module");
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public TerritoryAnchorBlock(BlockBehaviour.Properties settings) {
        super(settings);

        this.registerDefaultState(
                this.getStateDefinition()
                        .any()
                        .setValue(HAS_MODULE, false)
                        .setValue(ACTIVE, false)
        );
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HAS_MODULE, ACTIVE);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level world,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        ItemStack stack = player.getMainHandItem();

        if (world.isClientSide()) {
            if (stack.is(ErodedItems.TERRITORY_MODULE) || state.getValue(HAS_MODULE)) {
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS;
        }

        if (!(world instanceof ServerLevel serverWorld)) {
            return InteractionResult.PASS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        if (state.getValue(ACTIVE)) {
            TerritoryClaim claim = TerritoryProtectionManager.getAnchorClaim(serverWorld, pos);

            if (claim == null) {
                serverPlayer.displayClientMessage(
                        Component.translatable("eroded.territory.trust.no_claim"),
                        true
                );
                return InteractionResult.SUCCESS;
            }

            if (!TerritoryProtectionManager.canManageClaim(serverPlayer, claim)) {
                serverPlayer.displayClientMessage(
                        Component.translatable("eroded.territory.trust.no_permission"),
                        true
                );
                return InteractionResult.SUCCESS;
            }

            TerritoryProtectionManager.openTerritoryModule(
                    serverWorld,
                    pos,
                    serverPlayer
            );

            return InteractionResult.SUCCESS;
        }

        if (state.getValue(HAS_MODULE)) {
            serverPlayer.displayClientMessage(
                    Component.translatable("eroded.territory.anchor.stabilizing"),
                    true
            );
            return InteractionResult.SUCCESS;
        }

        if (!stack.is(ErodedItems.TERRITORY_MODULE)) {
            serverPlayer.displayClientMessage(
                    Component.translatable("eroded.territory.anchor.needs_module"),
                    true
            );
            return InteractionResult.SUCCESS;
        }

        if (!TerritoryProtectionManager.validateNewClaim(serverWorld, pos, serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        if (!serverPlayer.isCreative()) {
            stack.shrink(1);
        }

        serverWorld.setBlock(
                pos,
                state.setValue(HAS_MODULE, true).setValue(ACTIVE, false),
                Block.UPDATE_ALL
        );

        TerritoryProtectionManager.playModuleInsertedSound(serverWorld, pos);

        TerritoryProtectionManager.createPendingClaim(
                serverWorld,
                pos,
                serverPlayer
        );

        serverWorld.scheduleTick(
                pos,
                this,
                TerritoryProtectionManager.getActivationDelayTicks()
        );

        serverPlayer.displayClientMessage(
                Component.translatable("eroded.territory.anchor.module_inserted"),
                true
        );

        return InteractionResult.SUCCESS;
    }

    @Override
    protected void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (!state.is(this)) {
            return;
        }

        if (!state.getValue(HAS_MODULE)) {
            return;
        }

        if (state.getValue(ACTIVE)) {
            return;
        }

        world.setBlock(
                pos,
                state.setValue(ACTIVE, true),
                Block.UPDATE_ALL
        );

        TerritoryProtectionManager.activateClaim(world, pos);
        TerritoryProtectionManager.playAnchorActivatedSound(world, pos);

        TerritoryClaim claim = TerritoryProtectionManager.getAnchorClaim(world, pos);

        if (claim != null) {
            ServerPlayer owner = world.getServer()
                    .getPlayerList()
                    .getPlayer(claim.ownerUuid());

            if (owner != null) {
                owner.displayClientMessage(
                        Component.translatable("eroded.territory.anchor.activated"),
                        true
                );
            }
        }
    }

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (!world.isClientSide()
                && world instanceof ServerLevel serverWorld
                && player instanceof ServerPlayer serverPlayer) {

            TerritoryProtectionManager.handleAnchorBroken(
                    serverWorld,
                    pos,
                    state,
                    serverPlayer
            );
        }

        return super.playerWillDestroy(world, pos, state, player);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel world, BlockPos pos, boolean moved) {
        BlockState newState = world.getBlockState(pos);

        if (!newState.is(this)) {
            TerritoryProtectionManager.removeClaim(world, pos);
        }

        super.affectNeighborsAfterRemoval(state, world, pos, moved);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state) {
        return Shapes.empty();
    }
}