package cz.mcsworld.eroded.item;

import cz.mcsworld.eroded.energy.EnergySyncHandler;
import cz.mcsworld.eroded.skills.SkillData;
import cz.mcsworld.eroded.skills.SkillManager;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;

public class EnergyDrinkItem extends Item {

    public EnergyDrinkItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {

        if (user instanceof ServerPlayer player) {
            SkillData data = SkillManager.get(player);

            if (data.getEnergy() >= data.getMaxEnergy()) {
                player.displayClientMessage(Component.translatable("eroded.energy.full").withStyle(ChatFormatting.GOLD), true);
                return InteractionResult.FAIL;
            }
        }

        return ItemUtils.startUsingInstantly(world, user, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {

        if (user instanceof ServerPlayer player) {
            SkillData data = SkillManager.get(player);

            if (data.getEnergy() >= data.getMaxEnergy()) {
                return stack;
            }

            data.setEnergy(data.getMaxEnergy());
            SkillManager.save(player);

            EnergySyncHandler.forceSync(player);

            CriteriaTriggers.CONSUME_ITEM.trigger(player, stack);
        }


        if (user instanceof Player player && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 32;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.DRINK;
    }
}