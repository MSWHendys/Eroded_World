package cz.mcsworld.eroded.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;

public class ErodedBlock extends Block {

    public static final MapCodec<ErodedBlock> CODEC =
            createCodec(ErodedBlock::new);

    public static final IntProperty VARIANT =
            IntProperty.of("variant", 0, 23);

    public ErodedBlock(AbstractBlock.Settings settings) {
        super(settings);

        this.setDefaultState(
                this.getStateManager()
                        .getDefaultState()
                        .with(VARIANT, 0)
        );
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return this.getDefaultState().with(VARIANT, 0);
    }

    @Override
    protected void appendProperties(
            StateManager.Builder<Block, BlockState> builder
    ) {
        builder.add(VARIANT);
    }
}