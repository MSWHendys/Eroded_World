package cz.mcsworld.eroded.item;

import net.minecraft.block.Block;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.Consumer;

public class TerritoryAnchorItem extends BlockItem {

    public TerritoryAnchorItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public void appendTooltip(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplayComponent displayComponent,
            Consumer<Text> textConsumer,
            TooltipType type
    ) {
        textConsumer.accept(Text.translatable("tooltip.eroded.territory_anchor.1").formatted(Formatting.GRAY));
        textConsumer.accept(Text.translatable("tooltip.eroded.territory_anchor.2").formatted(Formatting.DARK_GRAY));
        textConsumer.accept(Text.translatable("tooltip.eroded.territory_anchor.3").formatted(Formatting.AQUA));

        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }
}