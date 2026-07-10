package cz.mcsworld.eroded.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class TerritoryModuleItem extends Item {

    public TerritoryModuleItem(Properties settings) {
        super(settings);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay displayComponent,
            Consumer<Component> textConsumer,
            TooltipFlag type
    ) {
        textConsumer.accept(Component.translatable("tooltip.eroded.territory_module.1").withStyle(ChatFormatting.GRAY));
        textConsumer.accept(Component.translatable("tooltip.eroded.territory_module.2").withStyle(ChatFormatting.DARK_GRAY));
        textConsumer.accept(Component.translatable("tooltip.eroded.territory_module.3").withStyle(ChatFormatting.AQUA));

        super.appendHoverText(stack, context, displayComponent, textConsumer, type);
    }
}