package cz.mcsworld.eroded.item;

import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.skills.SkillData;
import cz.mcsworld.eroded.skills.SkillManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.item.consume.UseAction;
import net.minecraft.world.World;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class AdrenalineShotItem extends Item {
    public AdrenalineShotItem(Settings settings) {
        super(settings);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (user instanceof ServerPlayerEntity player) {
            SkillData data = SkillManager.get(player);

            int immunitySeconds = Math.max(
                    1,
                    EnergyConfig.get().server.adrenalineShot.immunitySeconds
            );

            data.setImmunity(immunitySeconds);
            SkillManager.save(player);

            player.sendMessage(Text.translatable("eroded.adrenaline.active").formatted(Formatting.GOLD), true);
        }

        if (user instanceof PlayerEntity player && !player.getAbilities().creativeMode) {
            stack.decrement(1);
        }
        return stack;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 16;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        return ItemUsage.consumeHeldItem(world, user, hand);
    }
}