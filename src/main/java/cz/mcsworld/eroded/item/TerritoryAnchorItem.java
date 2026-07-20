package cz.mcsworld.eroded.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

public class TerritoryAnchorItem extends BlockItem {

    public TerritoryAnchorItem(Block block, Properties settings) {
        super(block, settings);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay displayComponent,
            Consumer<Component> textConsumer,
            TooltipFlag type
    ) {
        textConsumer.accept(Component.translatable("tooltip.eroded.territory_anchor.1").withStyle(ChatFormatting.GRAY));
        textConsumer.accept(Component.translatable("tooltip.eroded.territory_anchor.2").withStyle(ChatFormatting.DARK_GRAY));
        textConsumer.accept(Component.translatable("tooltip.eroded.territory_anchor.3").withStyle(ChatFormatting.AQUA));

        super.appendHoverText(stack, context, displayComponent, textConsumer, type);
    }
}