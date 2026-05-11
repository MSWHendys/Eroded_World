package cz.mcsworld.eroded.item;

import cz.mcsworld.eroded.energy.EnergySyncHandler;
import cz.mcsworld.eroded.skills.SkillData;
import cz.mcsworld.eroded.skills.SkillManager;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.consume.UseAction;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class EnergyDrinkItem extends Item {

    public EnergyDrinkItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {

        if (user instanceof ServerPlayerEntity player) {
            SkillData data = SkillManager.get(player);

            if (data.getEnergy() >= data.getMaxEnergy()) {
                player.sendMessage(Text.translatable("eroded.energy.full").formatted(Formatting.GOLD), true);
                return ActionResult.FAIL;
            }
        }

        return ItemUsage.consumeHeldItem(world, user, hand);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {

        if (user instanceof ServerPlayerEntity player) {
            SkillData data = SkillManager.get(player);

            if (data.getEnergy() >= data.getMaxEnergy()) {
                return stack;
            }

            data.setEnergy(data.getMaxEnergy());
            SkillManager.save(player);

            EnergySyncHandler.forceSync(player);

            Criteria.CONSUME_ITEM.trigger(player, stack);
        }


        if (user instanceof PlayerEntity player && !player.getAbilities().creativeMode) {
            stack.decrement(1);
        }

        return stack;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 32;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }
}