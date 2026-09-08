package cz.mcsworld.eroded.block;


import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.NotNull;

public class ErodedBlock extends Block {



    public static final IntegerProperty VARIANT =
            IntegerProperty.create("variant", 0, 23);

    public ErodedBlock(BlockBehaviour.Properties settings) {
        super(settings);

        this.registerDefaultState(
                this.getStateDefinition()
                        .any()
                        .setValue(VARIANT, 0)
        );
    }


    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        return this.defaultBlockState().setValue(VARIANT, 0);
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<@NotNull Block, @NotNull BlockState> builder
    ) {
        builder.add(VARIANT);
    }
}