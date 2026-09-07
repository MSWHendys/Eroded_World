package cz.mcsworld.eroded.item;

import cz.mcsworld.eroded.config.energy.EnergyConfig;
import cz.mcsworld.eroded.skills.SkillData;
import cz.mcsworld.eroded.skills.SkillManager;
import net.minecraft.ChatFormatting;
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
import org.jetbrains.annotations.NotNull;

public class AdrenalineShotItem extends Item {
    public AdrenalineShotItem(Properties settings) {
        super(settings);
    }

    @NotNull
    @Override
    public  ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level world, @NotNull LivingEntity user) {
        if (user instanceof ServerPlayer player) {
            if (!EnergyConfig.get().server.enabled) {
                return stack;
            }
            SkillData data = SkillManager.get(player);

            int immunitySeconds = Math.max(
                    1,
                    EnergyConfig.get().server.adrenalineShot.immunitySeconds
            );

            data.setImmunity(immunitySeconds);
            SkillManager.save(player);

            player.sendSystemMessage(Component.translatable("eroded.adrenaline.active").withStyle(ChatFormatting.GOLD), true);
        }

        if (user instanceof Player player && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return stack;
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity user) {
        return 16;
    }

    @Override
    public @NotNull ItemUseAnimation getUseAnimation(@NotNull ItemStack stack) {
        return ItemUseAnimation.DRINK;
    }

    @Override
    public @NotNull InteractionResult use(@NotNull Level world, @NotNull Player user, @NotNull InteractionHand hand) {
        if (!world.isClientSide() && !EnergyConfig.get().server.enabled) {
            return InteractionResult.FAIL;
        }
        return ItemUtils.startUsingInstantly(world, user, hand);
    }
}
